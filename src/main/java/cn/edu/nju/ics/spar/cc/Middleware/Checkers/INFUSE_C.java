package cn.edu.nju.ics.spar.cc.Middleware.Checkers;

import cn.edu.nju.ics.spar.cc.Constraints.Formulas.FExists;
import cn.edu.nju.ics.spar.cc.Constraints.Formulas.FForall;
import cn.edu.nju.ics.spar.cc.Constraints.Formulas.Formula;
import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Contexts.Context;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Contexts.ContextPool;
import cn.edu.nju.ics.spar.cc.Util.NotSupportedException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class INFUSE_C extends Checker{
    public final ExecutorService ThreadPool;

    public INFUSE_C(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions, boolean isMG) {
        super(ruleHandler, contextPool, bfunctions, isMG);
        ThreadPool = Executors.newFixedThreadPool(13);
        this.technique = "CPCC_NB";
    }

    public static class CreateBranchesTask_INFUSE implements Callable<RuntimeNode> {
        Rule rule;
        int depth;
        HashMap<String, Context> varEnv;
        Context context;
        Formula originFormula;//父结点的formula
        Checker checker;

        public CreateBranchesTask_INFUSE(Rule rule, int depth, HashMap<String, Context> varEnv,
                                         Context context, Formula originFormula, Checker checker){
            this.rule = rule;
            this.depth = depth;
            this.varEnv = varEnv;
            this.context = context;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public RuntimeNode call() {
            RuntimeNode returnNode;
            if(originFormula.getFormula_type() == Formula.Formula_Type.EXISTS){
                returnNode = new RuntimeNode(((FExists)originFormula).getSubformula());
                returnNode.setDepth(this.depth + 1);
                returnNode.getVarEnv().putAll(this.varEnv);
                returnNode.getVarEnv().put(((FExists)originFormula).getVar(), context);
                returnNode.getFormula().createBranches_INFUSE(rule, returnNode, ((FExists)originFormula).getSubformula(), checker);
            }
            else{
                returnNode = new RuntimeNode(((FForall)originFormula).getSubformula());
                returnNode.setDepth(this.depth + 1);
                returnNode.getVarEnv().putAll(this.varEnv);
                returnNode.getVarEnv().put(((FForall)originFormula).getVar(), context);
                returnNode.getFormula().createBranches_INFUSE(rule, returnNode, ((FForall)originFormula).getSubformula(), checker);
            }
            return returnNode;
        }
    }

    public static class ModifyBranchTask_INFUSE implements Callable<Void>{
        Rule rule;
        RuntimeNode curNode;
        Formula originFormula;//curNode的formula
        Checker checker;

        public ModifyBranchTask_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker){
            this.rule = rule;
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Void call(){
            curNode.getFormula().modifyBranch_INFUSE(rule, curNode, originFormula, checker);
            return null;
        }

    }

    public static class TruthEvaluationTaskCom_INFUSE implements Callable<Boolean>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public TruthEvaluationTaskCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Boolean call() {
            //must new
            return curNode.getFormula().truthEvaluationCom_INFUSE(curNode, originFormula, checker);
        }
    }

    public static class TruthEvaluationTaskPar_INFUSE implements Callable<Boolean>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public TruthEvaluationTaskPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Boolean call() {
            //must new
            return curNode.getFormula().truthEvaluationPar_INFUSE(curNode, originFormula, checker);
        }
    }

    public static class LinksGenerationTaskCom_INFUSE implements Callable<Set<Link>>{
        RuntimeNode curNode;
        Formula originFormula;
        final Set<RuntimeNode> prevSubstantialNodes;
        Checker checker;

        public LinksGenerationTaskCom_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.prevSubstantialNodes = prevSubstantialNodes;
            this.checker = checker;
        }

        @Override
        public Set<Link> call() {
            return curNode.getFormula().linksGeneration_ECC(curNode, originFormula, prevSubstantialNodes, checker);
        }
    }

    public static class LinksGenerationTaskPar_INFUSE implements Callable<Set<Link>>{
        RuntimeNode curNode;
        Formula originFormula;
        final Set<RuntimeNode> prevSubstantialNodes;
        Checker checker;

        public LinksGenerationTaskPar_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.prevSubstantialNodes = prevSubstantialNodes;
            this.checker = checker;
        }

        @Override
        public Set<Link> call() {
            return curNode.getFormula().linksGeneration_INFUSE(curNode, originFormula, prevSubstantialNodes, checker);
        }
    }

    @Override
    public void checkInit() {
        for(Rule rule : this.ruleHandler.getRuleMap().values()){
            rule.buildCCT_INFUSE(this);
            rule.truthEvaluation_INFUSE(this, true);
            //taint SCCT
            Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
            if(this.isMG){
                this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
            }
            rule.linksGeneration_INFUSE(this, prevSubstantialNodes);
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException {
        //rule.intoFile(batch);

        contextPool.applyChanges(rule, batch);
        rule.updateAffectedWithChanges(this);
        rule.updateCanConcurrent_INFUSE(this);

        rule.modifyCCT_INFUSE(this);
        rule.truthEvaluation_INFUSE(this, false);
        //taint SCCT
        Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
        if(this.isMG){
            this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
        }
        Set<Link> links2 = rule.linksGeneration_INFUSE(this, prevSubstantialNodes);
        if(links2 != null){
            rule.addCriticalSet(links2);
        }
        rule.cleanAffectedAndCanConcurrent();
        if(links2 != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links2);
        }
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        for(Rule rule : ruleHandler.getRuleMap().values()) {
            if (rule.getVarPatternMap().containsValue(contextChange.getPattern_id())) {
                List<ContextChange> batch = new ArrayList<>();
                batch.add(contextChange);

                contextPool.applyChanges(rule, batch);
                rule.updateAffectedWithChanges(this);
                rule.updateCanConcurrent_INFUSE(this);

                rule.modifyCCT_INFUSE(this);
                rule.truthEvaluation_INFUSE(this,false);
                //taint SCCT
                Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
                if(this.isMG){
                    this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
                }
                Set<Link> links2 = rule.linksGeneration_INFUSE(this, prevSubstantialNodes);
                if (links2 != null) {
                    rule.addCriticalSet(links2);
                   // rule.oracleCount(links2, contextChange);
                }
                rule.cleanAffectedAndCanConcurrent();
                if(links2 != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links2);
                }
            }
        }
    }
}
