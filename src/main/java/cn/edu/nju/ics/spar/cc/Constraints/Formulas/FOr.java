package cn.edu.nju.ics.spar.cc.Constraints.Formulas;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.LGUtils;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.Checker;
import cn.edu.nju.ics.spar.cc.Middleware.Schedulers.Scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FOr extends Formula {
    private final Formula[] subformulas;

    //constructors
    public FOr(){
        this.setFormula_type(Formula_Type.OR);
        this.subformulas = new Formula[2];
        this.setAffected(false);
    }

    public void replaceSubformula(int pos, Formula formula){
        if( pos < 0 || pos >= 2){
            System.err.println("[CCE] position is out of range.");
            System.exit(1);
        }
        this.subformulas[pos] = formula;
    }

    @Override
    public void setFormula_type(Formula.Formula_Type formula_type) {
        super.setFormula_type(formula_type);
    }

    @Override
    public Formula.Formula_Type getFormula_type() {
        return super.getFormula_type();
    }

    public Formula[] getSubformulas() {
        return subformulas;
    }

    @Override
    public void output(int offset) {
        for(int i = 0; i < offset; ++i)
            System.out.print(" ");
        System.out.println("or  affected: " + this.isAffected());
        subformulas[0].output(offset+2);
        subformulas[1].output(offset+2);
    }

    @Override
    public Formula formulaClone() {
        return new FOr();
    }

    //S-condition
    @Override
    public void deriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        this.subformulas[0].deriveIncPlusSet(incPlusSet);
        this.subformulas[1].deriveIncPlusSet(incPlusSet);
    }

    @Override
    public void deriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        this.subformulas[0].deriveIncMinusSet(incMinusSet);
        this.subformulas[1].deriveIncMinusSet(incMinusSet);
    }

    //C-condition
    @Override
    public boolean evaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, addChange.getContext());
        }

        boolean result;
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean tempresult = runtimeNode1.getFormula().evaluationAndEqualSideEffect(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], var, delChange, addChange, canConcurrent, scheduler);
        result = tempresult;

        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        tempresult = runtimeNode2.getFormula().evaluationAndEqualSideEffect(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], var, delChange, addChange, canConcurrent, scheduler);
        result = result && tempresult;

        boolean newTruth = runtimeNode1.isTruth() || runtimeNode2.isTruth();
        curNode.setOptTruth(curNode.isTruth());
        curNode.setTruth(newTruth);

        return result;
    }

    @Override
    public void sideeffectresolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.setTruth(curNode.isOptTruth());
            curNode.setOptTruth(false);
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, delChange.getContext());
        }
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().sideeffectresolution(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], var, delChange, addChange, canConcurrent, scheduler);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().sideeffectresolution(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], var, delChange, addChange, canConcurrent, scheduler);
    }

    //DIS
    @Override
    public void deriveRCRESets(boolean from) {
        this.subformulas[0].deriveRCRESets(from);
        this.subformulas[1].deriveRCRESets(from);
    }

    //PCC
    @Override
    public boolean updateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        boolean result0 = this.subformulas[0].updateAffectedWithOneChange(contextChange, checker);
        boolean result1 = this.subformulas[1].updateAffectedWithOneChange(contextChange, checker);
        result0 = result0 || result1;
        this.setAffected(result0);
        return result0;
    }

    //PCCM && CPCC
    @Override
    public boolean updateAffectedWithChanges(Checker checker) {
        boolean result0 = this.subformulas[0].updateAffectedWithChanges(checker);
        boolean result1 = this.subformulas[1].updateAffectedWithChanges(checker);
        result0 = result0 || result1;
        this.setAffected(result0);
        return result0;
    }
    @Override
    public void cleanAffected() {
        this.setAffected(false);
        this.subformulas[0].cleanAffected();
        this.subformulas[1].cleanAffected();
    }

    //CPCC_NB
    @Override
    public void updateCanConcurrent_INFUSE(boolean canConcurrent, Rule rule, Checker checker) {
        if(canConcurrent){
            this.subformulas[0].updateCanConcurrent_INFUSE(true, rule, checker);
            this.subformulas[1].updateCanConcurrent_INFUSE(true, rule, checker);
        }
    }

    @Override
    public void cleanAffectedAndCanConcurrent() {
        this.setAffected(false);
        this.subformulas[0].cleanAffectedAndCanConcurrent();
        this.subformulas[1].cleanAffectedAndCanConcurrent();
    }

    //MG
    @Override
    public void taintSCCT(RuntimeNode curNode, Formula originFormula, Set<RuntimeNode> substantialNodes) {
        substantialNodes.add(curNode);
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(curNode.isTruth()){
            if(runtimeNode1.isTruth()){
                runtimeNode1.getFormula().taintSCCT(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], substantialNodes);
            }
            if(runtimeNode2.isTruth()){
                runtimeNode2.getFormula().taintSCCT(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], substantialNodes);
            }
        }
        else{
            runtimeNode1.getFormula().taintSCCT(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], substantialNodes);
            runtimeNode2.getFormula().taintSCCT(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], substantialNodes);
        }
    }

    /*
                                            ECC PCC
                                         */
    @Override
    public void createBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FOr)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().createBranches_ECCPCC(rule_id, runtimeNode1, ((FOr) originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FOr) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().createBranches_ECCPCC(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);
    }

    /*
        ECC
     */
    @Override
    public boolean truthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = runtimeNode1.getFormula().truthEvaluation_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().truthEvaluation_ECC(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);
        result = result || tempresult;
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ECC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG() || !curNode.isTruth()){
            // case 1: !MG --> all
            // case 3: MG && false --> all
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else{
                    result = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                }
            }
            else{
                if(runtimeNode2.isTruth()) {
                    runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result = lgUtils.cartesianSet(ret1, ret2);
                }
            }
        }
        else{
            // case 2: MG && true --> true
            if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(ret1);
                result.addAll(ret2);
            }
            else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                result = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
            }
            else{
                result = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
            }
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */

    @Override
    public void modifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_PCC(rule_id, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_PCC(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        //curNode.update == fasle
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluation_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode2.getFormula().truthEvaluation_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret1);
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(ret1);
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret1, runtimeNode2.getLinks()));
                    }
                }
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                        result.addAll(runtimeNode1.getLinks());
                    }
                    else{
                        result.addAll(runtimeNode1.getLinks());
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2, runtimeNode1.getLinks()));
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
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret1);
                        result.addAll(ret2);
                    }
                    else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret1);
                    }
                    else{
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    result.addAll(ret1);
                }
                else{
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if (prevSubstantialNodes.contains(runtimeNode1)) {
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                }
                else{
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert !runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(ret1, ret2));
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if(prevSubstantialNodes.contains(runtimeNode2)){
                    ret2 = runtimeNode2.getLinks();
                }
                else{
                    ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if (prevSubstantialNodes.contains(runtimeNode1)) {
                    ret1 = runtimeNode1.getLinks();
                }
                else{
                    ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        ConC
     */
    @Override
    public void createBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FOr)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().createBranches_ConC(rule_id, runtimeNode1, ((FOr) originFormula).getSubformulas()[0], canConcurrent, checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FOr) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().createBranches_ConC(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], canConcurrent, checker);
    }

    @Override
    public boolean truthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = runtimeNode1.getFormula().truthEvaluation_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().truthEvaluation_ConC(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], canConcurrent, checker);
        result = result || tempresult;
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(!checker.isMG() || !curNode.isTruth()){
            // case 1: !MG --> all
            // case 3: MG && false --> all
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else{
                    result = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    result = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                    result = lgUtils.cartesianSet(ret1, ret2);
                }
            }
        }
        else{
            // case 2: MG && true --> true
            if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                result.addAll(ret1);
                result.addAll(ret2);
            }
            else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                result = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
            }
            else{
                result = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
            }
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCCM
     */

    @Override
    public void modifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_PCCM(rule_id, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_PCCM(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluation_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0],checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode2.getFormula().truthEvaluation_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode1.getFormula().truthEvaluation_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            boolean tempresult = runtimeNode2.getFormula().truthEvaluation_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = result || tempresult;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret1);
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(ret1);
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret1, runtimeNode2.getLinks()));
                    }
                }
            }
            else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                        result.addAll(runtimeNode1.getLinks());
                    }
                    else{
                        result.addAll(runtimeNode1.getLinks());
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2, runtimeNode1.getLinks()));
                    }
                }
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret1);
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(ret1);
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret1, ret2));
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
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(ret1);
                        result.addAll(ret2);
                    }
                    else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        result.addAll(ret1);
                    }
                    else{
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                }
                else{
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
            }
            else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                }
                else{
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                }
                else{
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert !runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(ret1, ret2));
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if (prevSubstantialNodes.contains(runtimeNode2)) {
                    ret2 = runtimeNode2.getLinks();
                }
                else {
                    ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
            else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if(prevSubstantialNodes.contains(runtimeNode1)){
                    ret1 = runtimeNode1.getLinks();
                }
                else {
                    ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                }
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_NB
     */

    @Override
    public void createBranches_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FOr)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode1.setParent(curNode);
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().createBranches_INFUSE(rule, runtimeNode1, ((FOr) originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FOr) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode2.setParent(curNode);
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().createBranches_INFUSE(rule, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);

    }

    @Override
    public void modifyBranch_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_INFUSE(rule, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_INFUSE(rule, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);

    }

    @Override
    public boolean truthEvaluationCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = runtimeNode1.getFormula().truthEvaluationCom_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().truthEvaluationCom_INFUSE(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);
        result = result || tempresult;
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return result;
    }

    @Override
    public boolean truthEvaluationPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluationPar_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
        else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode2.getFormula().truthEvaluationPar_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
        else{
            boolean result = runtimeNode1.getFormula().truthEvaluationPar_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            boolean tempresult = runtimeNode2.getFormula().truthEvaluationPar_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = result || tempresult;
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret1);
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(ret1);
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret1, runtimeNode2.getLinks()));
                    }
                }
            }
            else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                        result.addAll(runtimeNode1.getLinks());
                    }
                    else{
                        result.addAll(runtimeNode1.getLinks());
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2, runtimeNode1.getLinks()));
                    }
                }
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret1);
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(ret1);
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret1, ret2));
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
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(ret1);
                        result.addAll(ret2);
                    }
                    else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        result.addAll(ret1);
                    }
                    else{
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                }
                else{
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
            }
            else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                }
                else{
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(ret1);
                }
                else{
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert !runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(ret1, ret2));
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if (prevSubstantialNodes.contains(runtimeNode2)) {
                    ret2 = runtimeNode2.getLinks();
                }
                else {
                    ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
            else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if(prevSubstantialNodes.contains(runtimeNode1)){
                    ret1 = runtimeNode1.getLinks();
                }
                else {
                    ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                }
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_BASE
     */

    @Override
    public void modifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_BASE(rule_id, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_BASE(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        //curNode.update == fasle
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluation_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode2.getFormula().truthEvaluation_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret1);
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(ret1);
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret1, runtimeNode2.getLinks()));
                    }
                }
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                        result.addAll(runtimeNode1.getLinks());
                    }
                    else{
                        result.addAll(runtimeNode1.getLinks());
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2, runtimeNode1.getLinks()));
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
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret1);
                        result.addAll(ret2);
                    }
                    else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret1);
                    }
                    else{
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    result.addAll(ret1);
                }
                else{
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if (prevSubstantialNodes.contains(runtimeNode1)) {
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                    result.addAll(ret2);
                }
                else if(runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret1);
                }
                else{
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert !runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(ret1, ret2));
                }
            }
            else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if(prevSubstantialNodes.contains(runtimeNode2)){
                    ret2 = runtimeNode2.getLinks();
                }
                else{
                    ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if (prevSubstantialNodes.contains(runtimeNode1)) {
                    ret1 = runtimeNode1.getLinks();
                }
                else{
                    ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(ret1, ret2));
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }
}
