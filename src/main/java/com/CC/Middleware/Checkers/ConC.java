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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConC extends Checker {

    public final ExecutorService ThreadPool;

    public ConC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions, boolean isMG) {
        super(ruleHandler, contextPool, bfunctions, isMG);
        ThreadPool = Executors.newFixedThreadPool(16);
        this.technique = "ConC";
    }

    public static class CreateBranchesTask_ConC implements Callable<RuntimeNode> {
        String rule_id;
        int depth;
        HashMap<String, Context> varEnv;
        Context context;
        Formula originFormula;
        Checker checker;

        public CreateBranchesTask_ConC(String rule_id, int depth,
                                       HashMap<String,Context> varEnv,
                                       Context context, Formula originFormula, Checker checker){
            this.rule_id = rule_id;
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
                returnNode.getFormula().createBranches_ConC(rule_id, returnNode, ((FExists)originFormula).getSubformula(), false, checker);
            }
            else{
                returnNode = new RuntimeNode(((FForall)originFormula).getSubformula());
                returnNode.setDepth(this.depth + 1);
                returnNode.getVarEnv().putAll(this.varEnv);
                returnNode.getVarEnv().put(((FForall)originFormula).getVar(), context);
                returnNode.getFormula().createBranches_ConC(rule_id, returnNode, ((FForall)originFormula).getSubformula(), false, checker);
            }
            return returnNode;
        }
    }

    public static class TruthEvaluationTask_ConC implements Callable<Boolean>{
        RuntimeNode curNode;
        Formula originFormula;
        Checker checker;

        public TruthEvaluationTask_ConC(RuntimeNode curNode, Formula originFormula, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.checker = checker;
        }

        @Override
        public Boolean call() {
            return curNode.getFormula().truthEvaluation_ConC(curNode, originFormula, false, checker);
        }
    }

    public static class LinksGenerationTask_ConC implements Callable<Set<Link>>{
        RuntimeNode curNode;
        Formula originFormula;

        Set<RuntimeNode> prevSubstantialNodes;

        Checker checker;

        public LinksGenerationTask_ConC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker){
            this.curNode = curNode;
            this.originFormula = originFormula;
            this.prevSubstantialNodes = prevSubstantialNodes;
            this.checker = checker;
        }

        @Override
        public Set<Link> call(){
            return curNode.getFormula().linksGeneration_ConC(curNode, originFormula, false, prevSubstantialNodes, checker);
        }
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        // for hospital progress
        if(contextChange.getPattern_id().equals("P_temporal_1") &&
                contextChange.getChange_type() == ContextChange.Change_Type.ADDITION) {
            System.out.println("ConC+IMD Processing: " + contextChange.getContext().getCtx_id());
        }

        //consistency checking
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(rule.getVarPatternMap().containsValue(contextChange.getPattern_id())){
                //apply changes
                contextPool.applyChange(rule.getRule_id(), contextChange);
                //build CCT
                rule.buildCCT_CONC(this);
                //Truth value evaluation
                rule.truthEvaluation_ConC(this);
                //taint SCCT
                Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
                if(this.isMG){
                    this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
                }
                //Links Generation
                Set<Link> links = rule.linksGeneration_ConC(this, prevSubstantialNodes);
                if(links != null){
                    rule.addCriticalSet(links);
                }
                if(links != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                }
            }
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException {
        // for hospital progresss
        List<String> related_ctx_ids = new ArrayList<>();
        for (ContextChange contextChange : batch) {
            if (contextChange.getPattern_id().equals("P_temporal_1") &&
                    contextChange.getChange_type() == ContextChange.Change_Type.ADDITION) {
                related_ctx_ids.add(contextChange.getContext().getCtx_id());
            }
        }
        long max_ctx_id = 0;
        for (String ctx_id : related_ctx_ids) {
            max_ctx_id = Math.max(max_ctx_id, Long.parseLong(ctx_id.split("_")[1]));
        }
        System.out.println("ConC+GEAS Processing: ctx_" + max_ctx_id);

        //rule.intoFile(batch);

        for(ContextChange contextChange : batch){
            contextPool.applyChange(rule.getRule_id(), contextChange);
        }
        rule.buildCCT_CONC(this);
        rule.truthEvaluation_ConC(this);
        //taint SCCT
        Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
        if(this.isMG){
            this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
        }
        Set<Link> links = rule.linksGeneration_ConC(this, prevSubstantialNodes);
        if(links != null){
            rule.addCriticalSet(links);
        }
        if(links != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
        }
    }
}
