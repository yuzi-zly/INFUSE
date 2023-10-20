package com.CC.Constraints.Formulas;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Runtime.LGUtils;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.GEAS_opt_c;
import com.CC.Middleware.Schedulers.Scheduler;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FExists extends Formula{
    private String var;
    private String pattern_id;
    private Formula subformula;

    //CPCC_NB
    private boolean canConcurrent;

    //DIS
    private final Map<ContextChange.Change_Type, Set<RuntimeNode.Virtual_Truth_Type>> rcSet;
    private final Map<ContextChange.Change_Type, Set<RuntimeNode.Virtual_Truth_Type>> reSet;

    //constructors
    public FExists(String var, String pattern_id){
        this.setFormula_type(Formula_Type.EXISTS);
        this.var = var;
        this.pattern_id = pattern_id;
        this.subformula = null;
        this.setAffected(false);
        //CPCC_NB
        this.canConcurrent = false;
        //DIS
        this.rcSet = new HashMap<>();
        this.reSet = new HashMap<>();
    }

    //getter and setter
    public Map<ContextChange.Change_Type, Set<RuntimeNode.Virtual_Truth_Type>> getRcSet() {
        return rcSet;
    }

    public Map<ContextChange.Change_Type, Set<RuntimeNode.Virtual_Truth_Type>> getReSet() {
        return reSet;
    }

    public String getVar() {
        return var;
    }

    public String getPattern_id() {
        return pattern_id;
    }

    public Formula getSubformula() {
        return subformula;
    }

    public boolean isCanConcurrent() {
        return canConcurrent;
    }

    public void setVar(String var) {
        this.var = var;
    }

    @Override
    public void setFormula_type(Formula_Type formula_type) {
        super.setFormula_type(formula_type);
    }

    public void setSubformula(Formula subformula) {
        this.subformula = subformula;
    }

    public void setCanConcurrent(boolean canConcurrent) {
        this.canConcurrent = canConcurrent;
    }

    public void setPattern_id(String pattern_id) {
        this.pattern_id = pattern_id;
    }

    @Override
    public void output(int offset) {
        for(int i = 0; i < offset; ++i)
            System.out.print(" ");
        System.out.println("exists: "+ " var: " + var + " pattern_id: " + pattern_id + "  affected:" + this.isAffected()
                            + "\trcSet: " + this.getRcSet() + "\treSet: " + this.getReSet()
        );
        subformula.output(offset + 2);
    }

    @Override
    public Formula formulaClone() {
        return new FExists(this.getVar(), this.getPattern_id());
    }

    //S-condition
    @Override
    public void deriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        incPlusSet.add(new AbstractMap.SimpleEntry<>(ContextChange.Change_Type.DELETION, pattern_id));
        this.subformula.deriveIncPlusSet(incPlusSet);
    }

    @Override
    public void deriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        incMinusSet.add(new AbstractMap.SimpleEntry<>(ContextChange.Change_Type.ADDITION, pattern_id));
        this.subformula.deriveIncMinusSet(incMinusSet);
    }

    //C-condition
    @Override
    public boolean evaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        boolean result = true;
        if(curNode.getChildren().size() == 0)
            return false;

        if(delChange.getPattern_id().equals(this.pattern_id)){
            assert var == null;
            int meet_cnt = 0;
            for(RuntimeNode child : curNode.getChildren()){
                HashMap<String, Context> varEnv = child.getVarEnv();
                if(varEnv.get(this.var).equals(delChange.getContext())){//找到了对应分
                    meet_cnt++;
                    boolean tv1 = child.isTruth();
                    boolean chk_flag = child.getFormula().evaluationAndEqualSideEffect(child, ((FExists)originFormula).getSubformula(), this.var, delChange, addChange, false, scheduler);
                    boolean tv2 = child.isTruth();
                    if(tv1 != tv2){
                        result = false;
                    }
                    break;
                }
            }
            assert meet_cnt == 1;
        }
        else{
            if(canConcurrent){
                //可以并发
                assert var == null;
                List<Future<Boolean>> retList = new ArrayList<>();
                for(RuntimeNode child : curNode.getChildren()){
                    assert scheduler instanceof GEAS_opt_c;
                    Future<Boolean> future = ((GEAS_opt_c) scheduler).ThreadPool.submit(
                            new GEAS_opt_c.evaluationAndEqualSideEffectCon(child, ((FExists)originFormula).getSubformula(), delChange, addChange, scheduler)
                    );
                    retList.add(future);
                }

                for(Future<Boolean> future : retList){
                    try {
                        boolean tmpResult = future.get();
                        result = result && tmpResult;
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }

                if(result){
                    boolean newTruth = false;
                    for(RuntimeNode child : curNode.getChildren()){
                        newTruth = newTruth || child.isTruth();
                    }
                    curNode.setOptTruth(curNode.isTruth());
                    curNode.setTruth(newTruth);
                }
            }
            else{
                //不可并发
                if(var != null){
                    curNode.getVarEnv().remove(var);
                    curNode.getVarEnv().put(var, addChange.getContext());
                }
                boolean newTruth = false;
                for(RuntimeNode child : curNode.getChildren()){
                    boolean tempresult = child.getFormula().evaluationAndEqualSideEffect(child, ((FExists)originFormula).getSubformula(), var, delChange, addChange, false, scheduler);
                    result = result && tempresult;
                    newTruth = newTruth || child.isTruth();
                }
                curNode.setOptTruth(curNode.isTruth());
                curNode.setTruth(newTruth);
            }
        }
        return result;
    }

    @Override
    public void sideeffectresolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(curNode.getChildren().size() == 0)
            return;
        if(delChange.getPattern_id().equals(this.pattern_id)){
            assert var == null;
            int meet_cnt = 0;
            for(RuntimeNode child : curNode.getChildren()){
                HashMap<String, Context> varEnv = child.getVarEnv();
                if(varEnv.get(this.var).equals(addChange.getContext())) {
                    meet_cnt++;
                    child.getFormula().sideeffectresolution(child, ((FExists)originFormula).getSubformula(), this.var, delChange, addChange, false, scheduler);
                }
            }
            assert meet_cnt == 1;
        }
        else{
            if(canConcurrent){
                assert var == null;
                List<Future<Void>> retList = new ArrayList<>();
                for(RuntimeNode child : curNode.getChildren()){
                    assert scheduler instanceof GEAS_opt_c;
                    Future<Void> future = ((GEAS_opt_c) scheduler).ThreadPool.submit(
                            new GEAS_opt_c.sideEffectResolutionCon(child, ((FExists)originFormula).getSubformula(), delChange, addChange, scheduler)
                    );
                    retList.add(future);
                }

                for(Future<Void> future : retList){
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }

            }
            else{
                if(var != null){
                    curNode.setTruth(curNode.isOptTruth());
                    curNode.setOptTruth(false);
                    curNode.getVarEnv().remove(var);
                    curNode.getVarEnv().put(var, delChange.getContext());
                }
                for(RuntimeNode child : curNode.getChildren()){
                    child.getFormula().sideeffectresolution(child, ((FExists)originFormula).getSubformula(), var, delChange, addChange, false, scheduler);
                }
            }
        }
    }

    //DIS
    @Override
    public void deriveRCRESets(boolean from) {
        if(from){
            //T to F
            this.rcSet.put(ContextChange.Change_Type.DELETION, new HashSet<RuntimeNode.Virtual_Truth_Type>(){
                {
                    add(RuntimeNode.Virtual_Truth_Type.UNKNOWN);
                    add(RuntimeNode.Virtual_Truth_Type.TRUE);
                }
            });
            this.reSet.put(ContextChange.Change_Type.ADDITION, new HashSet<RuntimeNode.Virtual_Truth_Type>(){
                {
                    add(RuntimeNode.Virtual_Truth_Type.UNKNOWN);
                }
            });
        }
        else{
            //F to T
            this.rcSet.put(ContextChange.Change_Type.ADDITION, new HashSet<RuntimeNode.Virtual_Truth_Type>(){
                {
                    add(RuntimeNode.Virtual_Truth_Type.UNKNOWN);
                }
            });
            this.reSet.put(ContextChange.Change_Type.DELETION, new HashSet<RuntimeNode.Virtual_Truth_Type>(){
                {
                    add(RuntimeNode.Virtual_Truth_Type.UNKNOWN);
                }
            });
        }
        this.subformula.deriveRCRESets(from);
    }

    //PCC
    @Override
    public boolean updateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        if(contextChange.getPattern_id().equals(this.pattern_id)){
            this.setAffected(true);
            return true;
        }
        else{
            boolean result = this.subformula.updateAffectedWithOneChange(contextChange, checker);
            this.setAffected(result);
            return result;
        }
    }

    //PCCM && CPCC
    @Override
    public boolean updateAffectedWithChanges(Checker checker) {
        int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
        int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
        int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
        boolean result = this.subformula.updateAffectedWithChanges(checker);
        result = result || AddSetSize != 0 || DelSetSize!= 0 || UpdSetSize != 0;
        this.setAffected(result);
        return result;
    }

    @Override
    public void cleanAffected() {
        this.setAffected(false);
        this.subformula.cleanAffected();
    }

    //CPCC_NB
    @Override
    public void updateCanConcurrent_INFUSE(boolean canConcurrent, Rule rule, Checker checker) {
        if(canConcurrent){
            int PoolSize = checker.getContextPool().getPoolSetSize(rule.getRule_id(), this.pattern_id);
            int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
            int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
            int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
            if(rule.getPatToDepth().get(this.pattern_id) >= 2){
                int entireNum = AddSetSize + UpdSetSize;
                int partialNum = PoolSize - DelSetSize - UpdSetSize;
                if(entireNum > 1 && !this.subformula.isAffected()){
                    this.setCanConcurrent(true);
                }
                if(partialNum > 1 && this.subformula.isAffected()){
                    this.setCanConcurrent(true);
                }
                if(entireNum > 0 && this.subformula.isAffected()){
                    this.setCanConcurrent(true);
                }
            }
        }
    }

    @Override
    public void cleanAffectedAndCanConcurrent() {
        this.setAffected(false);
        this.setCanConcurrent(false);
        this.subformula.cleanAffectedAndCanConcurrent();
    }

    //MG
    @Override
    public void taintSCCT(RuntimeNode curNode, Formula originFormula, Set<RuntimeNode> substantialNodes) {
        substantialNodes.add(curNode);
        if(curNode.isTruth()){
            for(RuntimeNode child : curNode.getChildren()){
                if(!child.isTruth()) continue;
                child.getFormula().taintSCCT(child, ((FExists) originFormula).getSubformula(), substantialNodes);
            }
        }
    }

    /*
                            ECC PCC
                         */
    @Override
    public void createBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        Set<Context> pool = checker.getContextPool().getPoolSet(rule_id, this.pattern_id);
        for(Context context : pool){
            RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
            runtimeNode.setDepth(curNode.getDepth() + 1);
            runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
            runtimeNode.getVarEnv().put(this.var, context);
            curNode.getChildren().add(runtimeNode);
            //递归调用
            runtimeNode.getFormula().createBranches_ECCPCC(rule_id, runtimeNode, ((FExists) originFormula).getSubformula(), checker);
        }
    }

    /*
        ECC
     */
    @Override
    public boolean truthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker)  {
        boolean result = false;
        for(RuntimeNode child : curNode.getChildren()){
            boolean tempresult = child.getFormula().truthEvaluation_ECC(child, ((FExists)originFormula).getSubformula(), checker);
            result = result || tempresult;
        }
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ECC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        LGUtils lgUtils = new LGUtils();
        if(!checker.isMG()) {
            // case 1: !MG --> all
            for(RuntimeNode child : curNode.getChildren()){
                Set<Link> childLink = child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                Set<Link> initialSet = new HashSet<>();
                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                initialSet.add(initialLink);
                if(!child.isTruth()) continue;
                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                result.addAll(res);
            }
        }
        else if(curNode.isTruth()) {
            // case 2: MG && true --> true
            for(RuntimeNode child : curNode.getChildren()){
                if(!child.isTruth()) continue;
                Set<Link> childLink = child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                Set<Link> initialSet = new HashSet<>();
                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                initialSet.add(initialLink);
                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                result.addAll(res);
            }
        }
        else{
            // case 3: MG && false --> none
            // do nothing
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */
    private void RemoveBranch_PCC(RuntimeNode curNode, Context context){
        boolean flag = false;
        for(RuntimeNode child : curNode.getChildren()){
            if(child.getVarEnv().get(this.var).equals(context)){
                flag = true;
                curNode.getChildren().remove(child);
                break;
            }
        }
        //for static
//        if(!flag){
//            System.out.println("PCC: [FExists] cannot remove context");
//            curNode.PrintRuntimeNode(0);
//            System.exit(1);
//        }
    }

    @Override
    public void modifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(contextChange.getPattern_id().equals(this.pattern_id)){
            //同一个pattern
            if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                runtimeNode.setDepth(curNode.getDepth() + 1);
                runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
                runtimeNode.getVarEnv().put(this.var, contextChange.getContext());
                curNode.getChildren().add(runtimeNode);
                //创建下面的分支
                runtimeNode.getFormula().createBranches_ECCPCC(rule_id, runtimeNode, ((FExists) originFormula).getSubformula(), checker);
                //因为只有一个change，无需对更小的语法结构进行修改
            }
            else{
                RemoveBranch_PCC(curNode, contextChange.getContext());
            }
        }
        else{
            //不是同一个pattern
            for(RuntimeNode child : curNode.getChildren()){
                child.getFormula().modifyBranch_PCC(rule_id, child, ((FExists)originFormula).getSubformula(), contextChange, checker);
            }
        }
    }

    @Override
    public boolean truthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(!originFormula.isAffected()){
            //no change and affected == false
            return curNode.isTruth();
        }
        else {
           if(contextChange.getPattern_id().equals(this.pattern_id)){
                if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                    boolean result = curNode.isTruth();
                    RuntimeNode addchild = curNode.getChildren().get(
                            curNode.getChildren().size() - 1);
                    boolean res = addchild.getFormula().truthEvaluation_ECC(addchild, ((FExists)originFormula).getSubformula(), checker);
                    result = result || res;
                    curNode.setTruth(result);
                    return result;
                }
                else{
                    boolean result = false;
                    for(RuntimeNode child : curNode.getChildren()){
                        result = result || child.isTruth();
                    }
                    curNode.setTruth(result);
                    return result;
                }
           }
           else{
               boolean result = false;
               for(RuntimeNode child : curNode.getChildren()){
                    boolean res = child.getFormula().truthEvaluation_PCC(child, ((FExists)originFormula).getSubformula(), contextChange, checker);
                    result = result || res;
               }
               curNode.setTruth(result);
               return result;
           }
        }
    }

    @Override
    public Set<Link> linksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()) {
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else{
                if(contextChange.getPattern_id().equals(this.pattern_id)){
                    if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                        if(curNode.getLinks() != null)
                            result.addAll(curNode.getLinks());
                        RuntimeNode addchild = curNode.getChildren().get(curNode.getChildren().size() - 1);
                        Set<Link> childLink = addchild.getFormula().linksGeneration_ECC(addchild, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, addchild.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        if(addchild.isTruth()){
                            result.addAll(lgUtils.cartesianSet(initialSet, childLink));
                        }
                    }
                    else{
                        for(RuntimeNode child : curNode.getChildren()){
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                            result.addAll(res);
                        }
                    }
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        Set<Link> childLink =  child.getFormula().linksGeneration_PCC(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        if(!child.isTruth()) continue;
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
        }
        else if(curNode.isTruth()) {
            // case 2: MG && true --> true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        if(!child.isTruth()) continue;
                        Set<Link> childLink =  child.getFormula().linksGeneration_PCC(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
            else{
                if(contextChange.getPattern_id().equals(this.pattern_id)){
                    if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                        if(curNode.getLinks() != null){
                            // check whether curNode.links reusable
                            if(prevSubstantialNodes.contains(curNode)){
                                result.addAll(curNode.getLinks());
                            }
                            else{
                                // except the last one (new add one)
                                for(int index = 0; index < curNode.getChildren().size() - 1; ++index){
                                    RuntimeNode child = curNode.getChildren().get(index);
                                    if(!child.isTruth()) continue;
                                    Set<Link> childLink =  child.getFormula().linksGeneration_PCC(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                                    Set<Link> initialSet = new HashSet<>();
                                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                    initialSet.add(initialLink);
                                    Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                    result.addAll(res);
                                }
                            }
                        }
                        RuntimeNode addchild = curNode.getChildren().get(curNode.getChildren().size() - 1);
                        if(addchild.isTruth()){
                            Set<Link> childLink = addchild.getFormula().linksGeneration_ECC(addchild, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, addchild.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            result.addAll(lgUtils.cartesianSet(initialSet, childLink));
                        }
                    }
                    else{
                        for(RuntimeNode child : curNode.getChildren()){
                            if(!child.isTruth()) continue;
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            // check whether child.links reusable
                            if(prevSubstantialNodes.contains(child)){
                                Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                result.addAll(res);
                            }
                            else{
                                Set<Link> childLink = child.getFormula().linksGeneration_PCC(child, ((FExists) originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                    }
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        if(!child.isTruth()) continue;
                        Set<Link> childLink =  child.getFormula().linksGeneration_PCC(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
        }
        else {
            // case 3: MG && false --> none
            // do nothing
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        ConC
     */
    @Override
    public void createBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        if(canConcurrent){
            Set<Context> pool = checker.getContextPool().getPoolSet(rule_id, this.pattern_id);
            List<Future<RuntimeNode>> returnNodes = new ArrayList<>();
            for(Context context : pool){
                assert checker instanceof ConC;
                Future<RuntimeNode> future = ((ConC) checker).ThreadPool.submit(
                        new ConC.CreateBranchesTask_ConC(rule_id, curNode.getDepth(),
                        curNode.getVarEnv(), context, originFormula, checker)
                );
                returnNodes.add(future);
            }
            //添加分支
            for(Future<RuntimeNode> future : returnNodes){
                try {
                    curNode.getChildren().add(future.get());
                } catch (Exception e) {
                   System.out.println("get returnNode error");
                   System.exit(1);
                }
            }
        }
        else{
            Set<Context> pool = checker.getContextPool().getPoolSet(rule_id, this.pattern_id);
            for(Context context : pool){
                RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                runtimeNode.setDepth(curNode.getDepth() + 1);
                runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
                runtimeNode.getVarEnv().put(this.var, context);
                curNode.getChildren().add(runtimeNode);
                //递归调用
                runtimeNode.getFormula().createBranches_ConC(rule_id, runtimeNode, ((FExists) originFormula).getSubformula(), false, checker);
            }
        }
    }

    @Override
    public boolean truthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        if(canConcurrent){
            List<Future<Boolean>> truthList = new ArrayList<>();
            for(RuntimeNode child : curNode.getChildren()){
                assert checker instanceof ConC;
                Future<Boolean> future = ((ConC) checker).ThreadPool.submit(
                        new ConC.TruthEvaluationTask_ConC(child, ((FExists)originFormula).getSubformula(), checker)
                );
                truthList.add(future);
            }
            //计算总的真值
            boolean result = false;
            for(Future<Boolean> truth : truthList){
                try {
                    boolean tempresult = truth.get();
                    result = result || tempresult;
                } catch (Exception e) {
                    System.out.println("get truth error");
                    System.exit(1);
                }
            }
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = false;
            for(RuntimeNode child : curNode.getChildren()){
                boolean tempresult = child.getFormula().truthEvaluation_ConC(child, ((FExists)originFormula).getSubformula(), false, checker);
                result = result || tempresult;
            }
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        LGUtils lgUtils = new LGUtils();
        if(canConcurrent){
            Map<Integer, Future<Set<Link>>> LSMap = new HashMap<>();
            Set<Link> result = new HashSet<>();
            if(!checker.isMG()){
                // case 1: !MG --> all
                for(int index = 0; index < curNode.getChildren().size(); ++index){
                    RuntimeNode child = curNode.getChildren().get(index);
                    assert checker instanceof ConC;
                    Future<Set<Link>> future = ((ConC) checker).ThreadPool.submit(
                            new ConC.LinksGenerationTask_ConC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                    );
                    LSMap.put(index, future);
                }
            }
            else if(curNode.isTruth()){
                // case 2: MG && true --> true
                for(int index = 0; index < curNode.getChildren().size(); ++index){
                    RuntimeNode child = curNode.getChildren().get(index);
                    if(!child.isTruth()) continue;
                    assert checker instanceof ConC;
                    Future<Set<Link>> future = ((ConC) checker).ThreadPool.submit(
                            new ConC.LinksGenerationTask_ConC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                    );
                    LSMap.put(index, future);
                }
            }
            else{
                // case 3: MG && false --> none
                // do nothing
            }
            // merge links
            for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                int index= entry.getKey();
                RuntimeNode child = curNode.getChildren().get(index);
                Set<Link> childLink = null;
                try {
                    childLink = entry.getValue().get();
                } catch (Exception e) {
                    System.out.println("get links error");
                    System.exit(1);
                }
                Set<Link> initialSet = new HashSet<>();
                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                initialSet.add(initialLink);
                if(!child.isTruth()) continue;
                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                result.addAll(res);
            }
            curNode.setLinks(result);
            return curNode.getLinks();
        }
        else{
            Set<Link> result = new HashSet<>();
            if(!checker.isMG()){
                // case 1: !MG --> all
                for(RuntimeNode child : curNode.getChildren()){
                    Set<Link> childLink = child.getFormula().linksGeneration_ConC(child,((FExists)originFormula).getSubformula(), false, prevSubstantialNodes, checker);
                    Set<Link> initialSet = new HashSet<>();
                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                    initialSet.add(initialLink);
                    if(!child.isTruth()) continue;
                    Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                    result.addAll(res);
                }
            }
            else if(curNode.isTruth()){
                // case 2: MG && true --> true
                for(RuntimeNode child : curNode.getChildren()){
                    if(!child.isTruth()) continue;
                    Set<Link> childLink = child.getFormula().linksGeneration_ConC(child,((FExists)originFormula).getSubformula(), false, prevSubstantialNodes, checker);
                    Set<Link> initialSet = new HashSet<>();
                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                    initialSet.add(initialLink);
                    Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                    result.addAll(res);
                }
            }
            else{
                // case 3: MG && false --> none
                // do nothing
            }
            curNode.setLinks(result);
            return curNode.getLinks();
        }
    }

    /*
        PCCM
     */
    private void RemoveBranch_PCCM(RuntimeNode curNode, Context context){
        boolean flag = false;
        for(RuntimeNode child : curNode.getChildren()){
            if(child.getVarEnv().get(this.var).equals(context)){
                flag = true;
                curNode.getChildren().remove(child);
                break;
            }
        }
        //for static
//        if(!flag){
//            curNode.PrintRuntimeNode(0);
//            System.out.println("[FExists] cannot remove context " + context.toString());
//            System.exit(1);
//        }
    }

    @Override
    public void modifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {

        if(contextChange.getPattern_id().equals(this.pattern_id)){
            //同一个pattern
            if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                runtimeNode.setDepth(curNode.getDepth() + 1);
                runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
                runtimeNode.getVarEnv().put(this.var, contextChange.getContext());
                curNode.getChildren().add(runtimeNode);
                //创建下面的分支
                runtimeNode.getFormula().createBranches_ECCPCC(rule_id, runtimeNode, ((FExists) originFormula).getSubformula(), checker);
                //因为只有一个change，无需对更小的语法结构进行修改
            }
            else{
                RemoveBranch_PCCM(curNode, contextChange.getContext());
            }
        }
        else{
            //不是同一个pattern
            for(RuntimeNode child : curNode.getChildren()){
                child.getFormula().modifyBranch_PCCM(rule_id, child, ((FExists)originFormula).getSubformula(), contextChange, checker);
            }
        }
    }

    @Override
    public boolean truthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else{
            if(((FExists)originFormula).getSubformula().isAffected()){
                int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                boolean result = false;
                for(int i = 0; i < curNode.getChildren().size() - AddSetSize - UpdSetSize; ++i){
                    RuntimeNode child = curNode.getChildren().get(i);
                    boolean tempresult = child.getFormula().truthEvaluation_PCCM(child, ((FExists)originFormula).getSubformula(), checker);
                    result = result || tempresult;
                }
                for(int i = curNode.getChildren().size() - AddSetSize - UpdSetSize; i < curNode.getChildren().size(); ++i){
                    RuntimeNode child = curNode.getChildren().get(i);
                    boolean tempresult = child.getFormula().truthEvaluation_ECC(child, ((FExists)originFormula).getSubformula(), checker);
                    result = result || tempresult;
                }
                curNode.setTruth(result);
                return result;
            }
            else{
                int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
                int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                if(DelSetSize == 0 && UpdSetSize == 0){
                    boolean result = curNode.isTruth();
                    for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        boolean tempresult = child.getFormula().truthEvaluation_ECC(child, ((FExists)originFormula).getSubformula(), checker);
                        result = result || tempresult;
                    }
                    curNode.setTruth(result);
                    return result;
                }
                else{
                    boolean result = false;
                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize - UpdSetSize; ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        result = result || child.isTruth();
                    }
                    for(int i = curNode.getChildren().size() - AddSetSize - UpdSetSize; i < curNode.getChildren().size(); ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        boolean tempresult = child.getFormula().truthEvaluation_ECC(child, ((FExists)originFormula).getSubformula(), checker);
                        result = result || tempresult;
                    }
                    curNode.setTruth(result);
                    return result;
                }
            }
        }
    }

    @Override
    public Set<Link> linksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else{
                if(((FExists)originFormula).getSubformula().isAffected()){
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize - UpdSetSize; ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        Set<Link> childLink = child.getFormula().linksGeneration_PCCM(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        if(!child.isTruth()) continue;
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                    for(int i = curNode.getChildren().size() - AddSetSize - UpdSetSize; i < curNode.getChildren().size(); ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        Set<Link> childLink =  child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        if(!child.isTruth()) continue;
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
                else{
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
                    int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    if(DelSetSize == 0 && UpdSetSize == 0){
                        if(curNode.getLinks() != null)
                            result.addAll(curNode.getLinks());
                        for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Set<Link> childLink = child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                    else{
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - UpdSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                            result.addAll(res);
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - UpdSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Set<Link> childLink =  child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                }
            }
        }
        else if(curNode.isTruth()){
            // case 2: MG && true --> true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        if(!child.isTruth()) continue;
                        Set<Link> childLink = child.getFormula().linksGeneration_PCCM(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
            else{
                if(((FExists)originFormula).getSubformula().isAffected()){
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize - UpdSetSize; ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        if(!child.isTruth()) continue;
                        Set<Link> childLink = child.getFormula().linksGeneration_PCCM(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                    for(int i = curNode.getChildren().size() - AddSetSize - UpdSetSize; i < curNode.getChildren().size(); ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        if(!child.isTruth()) continue;
                        Set<Link> childLink =  child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
                else{
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
                    int UpdSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    if(DelSetSize == 0 && UpdSetSize == 0){
                        if(curNode.getLinks() != null){
                            // check whether curNode.links reusable
                            if(prevSubstantialNodes.contains(curNode)){
                                result.addAll(curNode.getLinks());
                            }
                            else{
                                for(int i = 0; i < curNode.getChildren().size() - AddSetSize; ++i){
                                    RuntimeNode child = curNode.getChildren().get(i);
                                    if(!child.isTruth()) continue;
                                    Set<Link> childLink = child.getFormula().linksGeneration_PCCM(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                    Set<Link> initialSet = new HashSet<>();
                                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                    initialSet.add(initialLink);
                                    Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                    result.addAll(res);
                                }
                            }
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Set<Link> childLink = child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                    else{
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - UpdSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            // check whether child.links reusable
                            if(prevSubstantialNodes.contains(child)){
                                Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                result.addAll(res);
                            }
                            else{
                                Set<Link> childLink = child.getFormula().linksGeneration_PCCM(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - UpdSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Set<Link> childLink =  child.getFormula().linksGeneration_ECC(child,((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                }
            }
        }
        else{
            // case 3: MG && false --> none
            // do nothing
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_NB
     */

    private RuntimeNode RemoveBranch_CPCC(Rule rule, RuntimeNode curNode, Context context, boolean updating){
        for(RuntimeNode child : curNode.getChildren()){
            if(child.getVarEnv().get(this.var).equals(context)){
                child.setParent(null);
                curNode.getChildren().remove(child);
                if(!updating){
                    int patDepth = rule.getPatToDepth().get(this.pattern_id);
                    SortedMap<Integer, String> headMap =  rule.getDepthToPat().headMap(patDepth);
                    for(String lowerPat : headMap.values()){
                        rule.getPatToRuntimeNode().get(lowerPat).removeIf(runtimeNode -> runtimeNode.getVarEnv().containsValue(context));
                    }
                }
                return child;
            }
        }
        return null;
    }

    @Override
    public void createBranches_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        assert checker instanceof INFUSE_C;
        rule.getPatToRuntimeNode().get(this.pattern_id).add(curNode);
        if(((FExists)originFormula).isCanConcurrent()){
            Set<Context> pool = checker.getContextPool().getPoolSet(rule.getRule_id(), this.pattern_id);
            List<Future<RuntimeNode>> returnNodes = new ArrayList<>();
            for(Context context : pool){
                Future<RuntimeNode> future = ((INFUSE_C) checker).ThreadPool.submit(
                        new INFUSE_C.CreateBranchesTask_INFUSE(rule, curNode.getDepth(),
                                curNode.getVarEnv(), context, originFormula, checker)
                );
                returnNodes.add(future);
            }
            //添加分支
            for(Future<RuntimeNode> future : returnNodes){
                try {
                    RuntimeNode child = future.get();
                    child.setParent(curNode);
                    curNode.getChildren().add(child);
                } catch (Exception e) {
                    System.out.println("get returnNode error");
                    System.exit(1);
                }
            }
        }
        else{
            Set<Context> pool = checker.getContextPool().getPoolSet(rule.getRule_id(), this.pattern_id);
            for(Context context : pool){
                RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                runtimeNode.setDepth(curNode.getDepth() + 1);
                runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
                runtimeNode.getVarEnv().put(this.var, context);
                runtimeNode.setParent(curNode);
                curNode.getChildren().add(runtimeNode);
                //递归调用
                runtimeNode.getFormula().createBranches_INFUSE(rule, runtimeNode, ((FExists) originFormula).getSubformula(), checker);
            }
        }
    }

    @Override
    public void modifyBranch_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //Delset
        Set<Context> DelSet = checker.getContextPool().getDelSet(this.pattern_id);
        for(Context context : DelSet){
            RemoveBranch_CPCC(rule, curNode, context, false);
        }

        //ModSet
        Set<Context> ModSet = checker.getContextPool().getUpdSet(this.pattern_id);
        for(Context context : ModSet){
            RuntimeNode ModNode = RemoveBranch_CPCC(rule, curNode, context, true);
            if(ModNode != null){
                ModNode.setParent(curNode);
                curNode.getChildren().add(ModNode);
            }
        }

        //AddSet
        Set<Context> AddSet = checker.getContextPool().getAddSet(this.pattern_id);
        if(((FExists)originFormula).isCanConcurrent()){
            List<Future<RuntimeNode>> returnNodes = new ArrayList<>();
            for(Context context : AddSet){
                Future<RuntimeNode> future = ((INFUSE_C) checker).ThreadPool.submit(
                        new INFUSE_C.CreateBranchesTask_INFUSE(rule, curNode.getDepth(),
                                curNode.getVarEnv(), context, originFormula, checker)
                );
                returnNodes.add(future);
            }

            //rest
            List<Future<Void>> voidlist = new ArrayList<>();
            if(((FExists)originFormula).getSubformula().isAffected()){
                for(RuntimeNode child : curNode.getChildren()){
                    Future<Void> future = ((INFUSE_C) checker).ThreadPool.submit(
                            new INFUSE_C.ModifyBranchTask_INFUSE(rule, child, ((FExists)originFormula).getSubformula(), checker)
                    );
                    voidlist.add(future);
                }
            }
            for(Future<Void> future : voidlist){
                try {
                    future.get();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("get Void error");
                    System.exit(1);
                }
            }

            //添加分支
            for(Future<RuntimeNode> future : returnNodes){
                try {
                    RuntimeNode child = future.get();
                    child.setParent(curNode);
                    curNode.getChildren().add(child);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("get returnNode error");
                    System.exit(1);
                }
            }
        }
        else{
            //递归调用Modify
            for(RuntimeNode child : curNode.getChildren()){
                child.getFormula().modifyBranch_INFUSE(rule, child, ((FExists)originFormula).getSubformula(), checker);
            }

            for(Context context : AddSet){
                RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                runtimeNode.setDepth(curNode.getDepth() + 1);
                runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
                runtimeNode.getVarEnv().put(this.var, context);
                runtimeNode.setParent(curNode);
                curNode.getChildren().add(runtimeNode);
                runtimeNode.getFormula().createBranches_INFUSE(rule, runtimeNode, ((FExists)originFormula).getSubformula(), checker);
            }
        }
    }

    @Override
    public boolean truthEvaluationCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        boolean result = false;
        for(RuntimeNode child : curNode.getChildren()){
            boolean tempresult = child.getFormula().truthEvaluationCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker);
            result = result || tempresult;
            //virtual truth
            curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
        }
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return result;
    }

    @Override
    public boolean truthEvaluationPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        //case 1
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else {
            //case 4,5
            if(((FExists)originFormula).getSubformula().isAffected()){
                int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                int ModSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                if(((FExists)originFormula).isCanConcurrent()){
                    boolean result = false;
                    List<Future<Boolean>> truthList = new ArrayList<>();
                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        Future<Boolean> future = ((INFUSE_C) checker).ThreadPool.submit(
                                new INFUSE_C.TruthEvaluationTaskPar_INFUSE(child, ((FExists)originFormula).getSubformula(), checker)
                        );
                        truthList.add(future);
                    }
                    for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        Future<Boolean> future = ((INFUSE_C) checker).ThreadPool.submit(
                                new INFUSE_C.TruthEvaluationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker)
                        );
                        truthList.add(future);
                    }
                    //all
                    for(Future<Boolean> truth : truthList){
                        try {
                            //短路后面get不会执行从而不会阻塞
                            boolean tempresult = truth.get();
                            result = result || tempresult;
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            System.out.println("get truth error");
                            System.exit(1);
                        }
                    }
                    //virtual truth
                    for(RuntimeNode child : curNode.getChildren()){
                        curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                    }
                    curNode.setTruth(result);
                    curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
                    return result;
                }
                else{
                    boolean result = false;
                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        boolean tempresult = child.getFormula().truthEvaluationPar_INFUSE(child, ((FExists)originFormula).getSubformula(), checker);
                        result = result || tempresult;
                        //virtual truth
                        curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                    }
                    for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                        RuntimeNode child = curNode.getChildren().get(i);
                        boolean tempresult = child.getFormula().truthEvaluationCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker);
                        result = result || tempresult;
                        //virtual truth
                        curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                    }
                    curNode.setTruth(result);
                    curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
                    return result;
                }
            }
            else{
                int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
                int ModSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                //case 2
                if(DelSetSize == 0 && ModSetSize == 0){
                    if(((FExists)originFormula).isCanConcurrent()){
                        boolean result = curNode.isTruth();
                        //AddSet
                        List<Future<Boolean>> truthList = new ArrayList<>();
                        for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Future<Boolean> future = ((INFUSE_C) checker).ThreadPool.submit(
                                    new INFUSE_C.TruthEvaluationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker)
                            );
                            truthList.add(future);
                        }
                        //all
                        for(Future<Boolean> truth : truthList){
                            try {
                                //短路后面get不会执行从而不会阻塞
                                boolean tempresult = truth.get();
                                result = result || tempresult;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                System.out.println("get truth error");
                                System.exit(1);
                            }
                        }
                        //virtutal truth
                        for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                        }
                        curNode.setTruth(result);
                        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
                        return result;
                    }
                    else{
                        boolean result = curNode.isTruth();
                        //AddSet
                        for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            boolean tempresult = child.getFormula().truthEvaluationCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker);
                            result = result || tempresult;
                            //virtual truth
                            curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                        }
                        curNode.setTruth(result);
                        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
                        return result;
                    }
                }
                //case 3
                else{
                    if(((FExists)originFormula).isCanConcurrent()){
                        boolean result = false;
                        List<Future<Boolean>> truthList = new ArrayList<>();
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            result = result || child.isTruth();
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Future<Boolean> future = ((INFUSE_C) checker).ThreadPool.submit(
                                    new INFUSE_C.TruthEvaluationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker)
                            );
                            truthList.add(future);
                        }
                        //all
                        for(Future<Boolean> truth : truthList){
                            try {
                                //短路后面get不会执行从而不会阻塞
                                boolean tempresult = truth.get();
                                result = result || tempresult;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                System.out.println("get truth error");
                                System.exit(1);
                            }
                        }
                        //virtual truth
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                        }
                        curNode.setTruth(result);
                        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
                        return result;
                    }
                    else{
                        boolean result = false;
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            boolean tempresult = child.isTruth();
                            result = result || tempresult;
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            boolean tempresult = child.getFormula().truthEvaluationCom_INFUSE(child, ((FExists)originFormula).getSubformula(), checker);
                            result = result || tempresult;
                            //virtual truth
                            curNode.getKidsVT().put(child.getVarEnv().get(this.var), child.getVirtualTruth());
                        }
                        curNode.setTruth(result);
                        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
                        return result;
                    }
                }
            }
        }
    }

    @Override
    public Set<Link> linksGeneration_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                //case 1
                return curNode.getLinks();
            }
            else{
                if(((FExists)originFormula).getSubformula().isAffected()){
                    //case 4,5
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int ModSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    if(((FExists)originFormula).isCanConcurrent()){
                        Map<Integer, Future<Set<Link>>> LSMap= new HashMap<>();
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                    new INFUSE_C.LinksGenerationTaskPar_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                            );
                            LSMap.put(i, future);
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                    new INFUSE_C.LinksGenerationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                            );
                            LSMap.put(i, future);
                        }
                        //all
                        for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                            int index = entry.getKey();
                            RuntimeNode child = curNode.getChildren().get(index);
                            Set<Link> childLink = null;
                            try {
                                childLink = entry.getValue().get();
                            } catch (Exception e) {
                                System.out.println("get links error");
                                System.exit(1);
                            }
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                    else{
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Set<Link> childLink = child.getFormula().linksGeneration_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            Set<Link> childLink = child.getFormula().linksGeneration_ECC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                }
                else{
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
                    int ModSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    //case 2
                    if(DelSetSize == 0 && ModSetSize == 0){
                        if(((FExists)originFormula).isCanConcurrent()){
                            if(curNode.getLinks() != null)
                                result.addAll(curNode.getLinks());
                            //AddSet
                            Map<Integer, Future<Set<Link>>> LSMap = new HashMap<>();
                            for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                        new INFUSE_C.LinksGenerationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                                );
                                LSMap.put(i, future);
                            }
                            //all
                            for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                                int index = entry.getKey();
                                RuntimeNode child = curNode.getChildren().get(index);
                                Set<Link> childLink = null;
                                try {
                                    childLink = entry.getValue().get();
                                } catch (Exception e) {
                                    System.out.println("get links error");
                                    System.exit(1);
                                }
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                if(!child.isTruth()) continue;
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                        else{
                            if(curNode.getLinks() != null)
                                result.addAll(curNode.getLinks());
                            //AddSet
                            for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                Set<Link> childLink = child.getFormula().linksGeneration_ECC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                if(!child.isTruth()) continue;
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                    }
                    //case 3
                    else{
                        if(((FExists)originFormula).isCanConcurrent()){
                            Map<Integer, Future<Set<Link>>> LSMap = new HashMap<>();
                            for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                if(!child.isTruth()) continue;
                                Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                result.addAll(res);
                            }
                            for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                        new INFUSE_C.LinksGenerationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                                );
                                LSMap.put(i, future);
                            }
                            //all
                            for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                                int index = entry.getKey();
                                RuntimeNode child = curNode.getChildren().get(index);
                                Set<Link> childLink = null;
                                try {
                                    childLink = entry.getValue().get();
                                } catch (Exception e) {
                                    System.out.println("get links error");
                                    System.exit(1);
                                }
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                if(!child.isTruth()) continue;
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                        else{
                            for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                if(!child.isTruth()) continue;
                                Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                result.addAll(res);
                            }
                            for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                Set<Link> childLink =  child.getFormula().linksGeneration_ECC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                if(!child.isTruth()) continue;
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                    }

                }
            }
        }
        else if(curNode.isTruth()){
            // case 2: MG && true --> true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    // TODO：look at DESIGN.md
                    assert !((FExists) originFormula).isCanConcurrent();
                    for(RuntimeNode child : curNode.getChildren()){
                        if(!child.isTruth()) continue;
                        Set<Link> childLink = child.getFormula().linksGeneration_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
            else{
                if(((FExists)originFormula).getSubformula().isAffected()){
                    //case 4,5
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int ModSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    if(((FExists)originFormula).isCanConcurrent()){
                        Map<Integer, Future<Set<Link>>> LSMap= new HashMap<>();
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                    new INFUSE_C.LinksGenerationTaskPar_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                            );
                            LSMap.put(i, future);
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                    new INFUSE_C.LinksGenerationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                            );
                            LSMap.put(i, future);
                        }
                        //all
                        for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                            int index = entry.getKey();
                            RuntimeNode child = curNode.getChildren().get(index);
                            Set<Link> childLink = null;
                            try {
                                childLink = entry.getValue().get();
                            } catch (Exception e) {
                                System.out.println("get links error");
                                System.exit(1);
                            }
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                    else{
                        for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Set<Link> childLink = child.getFormula().linksGeneration_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                        for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                            RuntimeNode child = curNode.getChildren().get(i);
                            if(!child.isTruth()) continue;
                            Set<Link> childLink = child.getFormula().linksGeneration_ECC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                }
                else{
                    int AddSetSize = checker.getContextPool().getAddSetSize(this.pattern_id);
                    int DelSetSize = checker.getContextPool().getDelSetSize(this.pattern_id);
                    int ModSetSize = checker.getContextPool().getUpdSetSize(this.pattern_id);
                    //case 2
                    if(DelSetSize == 0 && ModSetSize == 0){
                        if(((FExists)originFormula).isCanConcurrent()){
                            Map<Integer, Future<Set<Link>>> LSMap = new HashMap<>();
                            if(curNode.getLinks() != null){
                                // check whether curNode.links reusable
                                if(prevSubstantialNodes.contains(curNode)){
                                    result.addAll(curNode.getLinks());
                                }
                                else{
                                    //rest
                                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize; ++i){
                                        RuntimeNode child = curNode.getChildren().get(i);
                                        if(!child.isTruth()) continue;
                                        Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                                new INFUSE_C.LinksGenerationTaskPar_INFUSE(child, ((FExists) originFormula).getSubformula(), prevSubstantialNodes, checker)
                                        );
                                        LSMap.put(i, future);
                                    }
                                }
                            }
                            //AddSet
                            for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                if(!child.isTruth()) continue;
                                Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                        new INFUSE_C.LinksGenerationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                                );
                                LSMap.put(i, future);
                            }
                            //all
                            for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                                int index = entry.getKey();
                                RuntimeNode child = curNode.getChildren().get(index);
                                Set<Link> childLink = null;
                                try {
                                    childLink = entry.getValue().get();
                                } catch (Exception e) {
                                    System.out.println("get links error");
                                    System.exit(1);
                                }
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                        else{
                            if(curNode.getLinks() != null){
                                // check whether curNode.links reusable
                                if(prevSubstantialNodes.contains(curNode)){
                                    result.addAll(curNode.getLinks());
                                }
                                else{
                                    // rest
                                    for(int i = 0; i < curNode.getChildren().size() - AddSetSize; ++i){
                                        RuntimeNode child = curNode.getChildren().get(i);
                                        if(!child.isTruth()) continue;
                                        Set<Link> childLink = child.getFormula().linksGeneration_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                        Set<Link> initialSet = new HashSet<>();
                                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                        initialSet.add(initialLink);
                                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                        result.addAll(res);
                                    }
                                }
                            }
                            //AddSet
                            for(int i = curNode.getChildren().size() - AddSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                if(!child.isTruth()) continue;
                                Set<Link> childLink = child.getFormula().linksGeneration_ECC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                    }
                    //case 3
                    else{
                        if(((FExists)originFormula).isCanConcurrent()){
                            Map<Integer, Future<Set<Link>>> LSMap = new HashMap<>();
                            for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                if(!child.isTruth()) continue;
                                // check whether child.links reusable
                                if(prevSubstantialNodes.contains(child)){
                                    Set<Link> initialSet = new HashSet<>();
                                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                    initialSet.add(initialLink);
                                    Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                    result.addAll(res);
                                }
                                else{
                                    Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                            new INFUSE_C.LinksGenerationTaskPar_INFUSE(child, ((FExists) originFormula).getSubformula(), prevSubstantialNodes, checker)
                                    );
                                    LSMap.put(i, future);
                                }
                            }
                            for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                if(!child.isTruth()) continue;
                                Future<Set<Link>> future = ((INFUSE_C) checker).ThreadPool.submit(
                                        new INFUSE_C.LinksGenerationTaskCom_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker)
                                );
                                LSMap.put(i, future);
                            }
                            //all
                            for(Map.Entry<Integer, Future<Set<Link>>> entry : LSMap.entrySet()){
                                int index = entry.getKey();
                                RuntimeNode child = curNode.getChildren().get(index);
                                Set<Link> childLink = null;
                                try {
                                    childLink = entry.getValue().get();
                                } catch (Exception e) {
                                    System.out.println("get links error");
                                    System.exit(1);
                                }
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                        else{
                            for(int i = 0; i < curNode.getChildren().size() - AddSetSize - ModSetSize; ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                if(!child.isTruth()) continue;
                                // check whether child.links reusable
                                if(prevSubstantialNodes.contains(child)){
                                    Set<Link> initialSet = new HashSet<>();
                                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                    initialSet.add(initialLink);
                                    Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                    result.addAll(res);
                                }
                                else{
                                    Set<Link> initialSet = new HashSet<>();
                                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                    initialSet.add(initialLink);
                                    Set<Link> childLink = child.getFormula().linksGeneration_INFUSE(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                    Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                    result.addAll(res);
                                }
                            }
                            for(int i = curNode.getChildren().size() - AddSetSize - ModSetSize; i < curNode.getChildren().size(); ++i){
                                RuntimeNode child = curNode.getChildren().get(i);
                                if(!child.isTruth()) continue;
                                Set<Link> childLink =  child.getFormula().linksGeneration_ECC(child, ((FExists)originFormula).getSubformula(), prevSubstantialNodes, checker);
                                Set<Link> initialSet = new HashSet<>();
                                Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                initialSet.add(initialLink);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                    }
                }
            }
        }
        else{
            // case 3: MG && false --> none
            // do nothing
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }


    /*
        CPCC_BASE
     */

    @Override
    public void modifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(contextChange.getPattern_id().equals(this.pattern_id)){
            //同一个pattern
            if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                RuntimeNode runtimeNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                runtimeNode.setDepth(curNode.getDepth() + 1);
                runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
                runtimeNode.getVarEnv().put(this.var, contextChange.getContext());
                curNode.getChildren().add(runtimeNode);
                //创建下面的分支
                runtimeNode.getFormula().createBranches_ConC(rule_id, runtimeNode, ((FExists) originFormula).getSubformula(), true, checker);
                //因为只有一个change，无需对更小的语法结构进行修改
            }
            else{
                RemoveBranch_PCC(curNode, contextChange.getContext());
            }
        }
        else{
            //不是同一个pattern
            for(RuntimeNode child : curNode.getChildren()){
                child.getFormula().modifyBranch_BASE(rule_id, child, ((FExists)originFormula).getSubformula(), contextChange, checker);
            }
        }
    }

    @Override
    public boolean truthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(!originFormula.isAffected()){
            //no change and affected == false
            return curNode.isTruth();
        }
        else {
            if(contextChange.getPattern_id().equals(this.pattern_id)){
                if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                    boolean result = curNode.isTruth();
                    RuntimeNode addchild = curNode.getChildren().get(
                            curNode.getChildren().size() - 1);
                    boolean res = addchild.getFormula().truthEvaluation_ConC(addchild, ((FExists)originFormula).getSubformula(), true, checker);
                    result = result || res;
                    curNode.setTruth(result);
                    return result;
                }
                else{
                    boolean result = false;
                    for(RuntimeNode child : curNode.getChildren()){
                        result = result || child.isTruth();
                    }
                    curNode.setTruth(result);
                    return result;
                }
            }
            else{
                boolean result = false;
                for(RuntimeNode child : curNode.getChildren()){
                    boolean res = child.getFormula().truthEvaluation_BASE(child, ((FExists)originFormula).getSubformula(), contextChange, checker);
                    result = result || res;
                }
                curNode.setTruth(result);
                return result;
            }
        }
    }

    @Override
    public Set<Link> linksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()) {
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else{
                if(contextChange.getPattern_id().equals(this.pattern_id)){
                    if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                        if(curNode.getLinks() != null)
                            result.addAll(curNode.getLinks());
                        RuntimeNode addchild = curNode.getChildren().get(curNode.getChildren().size() - 1);
                        Set<Link> childLink = addchild.getFormula().linksGeneration_ConC(addchild, ((FExists)originFormula).getSubformula(), true, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, addchild.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        if(addchild.isTruth()){
                            Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                            result.addAll(res);
                        }
                    }
                    else{
                        for(RuntimeNode child : curNode.getChildren()){
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            if(!child.isTruth()) continue;
                            Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                            result.addAll(res);
                        }
                    }
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        Set<Link> childLink =  child.getFormula().linksGeneration_BASE(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        if(!child.isTruth()) continue;
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
        }
        else if(curNode.isTruth()) {
            // case 2: MG && true --> true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        if(!child.isTruth()) continue;
                        Set<Link> childLink =  child.getFormula().linksGeneration_BASE(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
            else{
                if(contextChange.getPattern_id().equals(this.pattern_id)){
                    if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                        if(curNode.getLinks() != null){
                            // check whether curNode.links reusable
                            if(prevSubstantialNodes.contains(curNode)){
                                result.addAll(curNode.getLinks());
                            }
                            else{
                                // except the last one (new add one)
                                for(int index = 0; index < curNode.getChildren().size() - 1; ++index){
                                    RuntimeNode child = curNode.getChildren().get(index);
                                    if(!child.isTruth()) continue;
                                    Set<Link> childLink =  child.getFormula().linksGeneration_BASE(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                                    Set<Link> initialSet = new HashSet<>();
                                    Link initialLink = new Link(Link.Link_Type.SATISFIED);
                                    initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                                    initialSet.add(initialLink);
                                    Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                    result.addAll(res);
                                }
                            }
                        }
                        RuntimeNode addchild = curNode.getChildren().get(curNode.getChildren().size() - 1);
                        if(addchild.isTruth()){
                            Set<Link> childLink = addchild.getFormula().linksGeneration_ConC(addchild, ((FExists)originFormula).getSubformula(), true, prevSubstantialNodes, checker);
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, addchild.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            result.addAll(lgUtils.cartesianSet(initialSet, childLink));
                        }
                    }
                    else{
                        for(RuntimeNode child : curNode.getChildren()){
                            if(!child.isTruth()) continue;
                            Set<Link> initialSet = new HashSet<>();
                            Link initialLink = new Link(Link.Link_Type.SATISFIED);
                            initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                            initialSet.add(initialLink);
                            // check whether child.links reusable
                            if(prevSubstantialNodes.contains(child)){
                                Set<Link> res = lgUtils.cartesianSet(initialSet, child.getLinks());
                                result.addAll(res);
                            }
                            else{
                                Set<Link> childLink = child.getFormula().linksGeneration_BASE(child, ((FExists) originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                                Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                                result.addAll(res);
                            }
                        }
                    }
                }
                else{
                    for(RuntimeNode child : curNode.getChildren()){
                        if(!child.isTruth()) continue;
                        Set<Link> childLink =  child.getFormula().linksGeneration_BASE(child, ((FExists)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
                        Set<Link> initialSet = new HashSet<>();
                        Link initialLink = new Link(Link.Link_Type.SATISFIED);
                        initialLink.AddVA(this.var, child.getVarEnv().get(this.var));
                        initialSet.add(initialLink);
                        Set<Link> res = lgUtils.cartesianSet(initialSet, childLink);
                        result.addAll(res);
                    }
                }
            }
        }
        else {
            // case 3: MG && false --> none
            // do nothing
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }
}
