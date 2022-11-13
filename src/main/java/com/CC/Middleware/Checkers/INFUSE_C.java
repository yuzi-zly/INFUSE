package com.CC.Middleware.Checkers;

import com.CC.Constraints.Formulas.FExists;
import com.CC.Constraints.Formulas.FForall;
import com.CC.Constraints.Formulas.Formula;
import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Util.NotSupportedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class INFUSE_C extends Checker{
    public final ExecutorService ThreadPool;

    public INFUSE_C(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions) {
        super(ruleHandler, contextPool, bfunctions);
        ThreadPool = Executors.newFixedThreadPool(13);
        this.technique = "CPCC_NB";
    }

    public static class CreateBranchesTask_CPCC_NB implements Callable<RuntimeNode> {
        Rule rule;
        int depth;
        HashMap<String, Context> varEnv;
        Context context;
        Formula originFormula;//父结点的formula
        Checker checker;

        public CreateBranchesTask_CPCC_NB(Rule rule, int depth, HashMap<String, Context> varEnv,
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
                returnNode.getFormula().CreateBranches_CPCC_NB(rule, returnNode, ((FExists)originFormula).getSubformula(), checker);
            }
            else{
                returnNode = new RuntimeNode(((FForall)originFormula).getSubformula());
                returnNode.setDepth(this.depth + 1);
                returnNode.getVarEnv().putAll(this.varEnv);
                returnNode.getVarEnv().put(((FForall)originFormula).getVar(), context);
                returnNode.getFormula().CreateBranches_CPCC_NB(rule, returnNode, ((FForall)originFormula).getSubformula(), checker);
            }
            return returnNode;
        }
    }

    public static class ModifyBranchTask_CPCC_NB implements Callable<Void>{
        Rule rule;
        RuntimeNode curNode;
        Formula originFormula;//curNode的formula
        Checker checker;

        public ModifyBranchTask_CPCC_NB(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker){
            this.rule = rule;
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Void call(){
            curNode.getFormula().ModifyBranch_CPCC_NB(rule, curNode, originFormula, checker);
            return null;
        }

    }

    public static class TruthEvaluationTaskCom_CPCC_NB implements Callable<Boolean>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public TruthEvaluationTaskCom_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Boolean call() {
            //must new
            return curNode.getFormula().TruthEvaluationCom_CPCC_NB(curNode, originFormula, checker);
        }
    }

    public static class TruthEvaluationTaskPar_CPCC_NB implements Callable<Boolean>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public TruthEvaluationTaskPar_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Boolean call() {
            //must new
            return curNode.getFormula().TruthEvaluationPar_CPCC_NB(curNode, originFormula, checker);
        }
    }

    public static class LinksGenerationTaskCom_CPCC_NB implements Callable<Set<Link>>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public LinksGenerationTaskCom_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Set<Link> call() {
            return curNode.getFormula().LinksGeneration_ECC(curNode, originFormula, checker);
        }
    }

    public static class LinksGenerationTaskPar_CPCC_NB implements Callable<Set<Link>>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public LinksGenerationTaskPar_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Set<Link> call() {
            return curNode.getFormula().LinksGeneration_CPCC_NB(curNode, originFormula, checker);
        }
    }

    @Override
    public void checkInit() {
        for(Rule rule : this.ruleHandler.getRuleMap().values()){
            rule.BuildCCT_CPCC_NB(this);
            rule.TruthEvaluation_CPCC_NB(this, true);
            rule.LinksGeneration_CPCC_NB(this);
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException {
        //rule.intoFile(batch);

        contextPool.ApplyChanges(rule, batch);
        rule.UpdateAffectedWithChanges(this);
        rule.UpdateCanConcurrent_CPCC_NB(this);

        /*
        if(rule.isCCTAlready()){
            rule.ModifyCCT_CPCC_NB(this);
        }
        else{
            rule.BuildCCT_CPCC_NB(this);
        }

         */
        rule.ModifyCCT_CPCC_NB(this);
        rule.TruthEvaluation_CPCC_NB(this, false);
        Set<Link> links2 = rule.LinksGeneration_CPCC_NB(this);
        if(links2 != null){
            rule.addCriticalSet(links2);
        }
        rule.CleanAffectedAndCanConcurrent();
        if(links2 != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links2);
        }
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        for(Rule rule : ruleHandler.getRuleMap().values()) {
            if (rule.getVarPatternMap().values().contains(contextChange.getPattern_id())) {
                List<ContextChange> batch = new ArrayList<>();
                batch.add(contextChange);

                contextPool.ApplyChanges(rule, batch);
                rule.UpdateAffectedWithChanges(this);
                rule.UpdateCanConcurrent_CPCC_NB(this);

                /*
                if(rule.isCCTAlready()){
                    rule.ModifyCCT_CPCC_NB(this);
                }
                else{
                    rule.BuildCCT_CPCC_NB(this);
                }

                 */
                rule.ModifyCCT_CPCC_NB(this);
                rule.TruthEvaluation_CPCC_NB(this,false);
                Set<Link> links2 = rule.LinksGeneration_CPCC_NB(this);
                if (links2 != null) {
                    rule.addCriticalSet(links2);
                   // rule.oracleCount(links2, contextChange);
                }
                rule.CleanAffectedAndCanConcurrent();
                if(links2 != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links2);
                }
            }
        }
    }
}
