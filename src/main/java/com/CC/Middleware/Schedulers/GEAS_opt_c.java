package com.CC.Middleware.Schedulers;

import com.CC.Constraints.Formulas.Formula;
import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Util.NotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GEAS_opt_c extends GEAS_ori{
    public final ExecutorService ThreadPool;

    public GEAS_opt_c(RuleHandler ruleHandler, ContextPool contextPool, Checker checker) {
        super(ruleHandler, contextPool, checker);
        this.ThreadPool = Executors.newFixedThreadPool(13);
        this.strategy = "GEAS_opt_c";
    }


    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        batchFormAndRefineConcurrent(contextChange);
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(rule.getNewBatch() != null){
                this.checker.ctxChangeCheckBatch(rule, rule.getBatch());
                rule.setBatch(rule.getNewBatch());
                rule.setNewBatch(null);
            }
        }
    }

    private void batchFormAndRefineConcurrent(ContextChange newChange){
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(!rule.getVarPatternMap().containsValue(newChange.getPattern_id()))
                continue;

            if(sConditionMatch(rule, newChange)){
                List<ContextChange> newBatch = new ArrayList<>();
                newBatch.add(newChange);
                rule.setNewBatch(newBatch);
            }
            else{
                ContextChange oriChange = cConditionExamineConcurrent(rule, newChange);
                if(oriChange == null){
                    if(rule.getBatch() != null){
                        rule.getBatch().add(newChange);
                    }
                    else{
                        List<ContextChange> batch = new ArrayList<>();
                        batch.add(newChange);
                        rule.setBatch(batch);
                    }
                }
                else{
                    //此时一定有batch
                    rule.getBatch().remove(oriChange);
                    simpleUpdating(rule, oriChange, newChange);
                }
            }
        }
    }

    private ContextChange cConditionExamineConcurrent(Rule rule, ContextChange newChange){
        if(rule.getBatch() == null)
            return null;
        if(rule.getIncType(newChange).equals("Minus"))
            return null;
        if(newChange.getChange_type() == ContextChange.Change_Type.DELETION){
            Set<Context> pool = contextPool.getPoolSet(rule.getRule_id(), newChange.getPattern_id());
            if(!pool.contains(newChange.getContext()))
                return null;
        }

        for (ContextChange chg : rule.getBatch()){
            if(chg.getChange_type() == newChange.getChange_type()){ // + and -
                continue;
            }
            if(!chg.getPattern_id().equals(newChange.getPattern_id())){ // same pattern
                continue;
            }

            long oldTime = System.nanoTime();
            if(rule.inCriticalSet(chg.getContext().getCtx_id()) || rule.inCriticalSet(newChange.getContext().getCtx_id())){
                continue;
            }

            //examine part2 - sideEffect
            oldTime = System.nanoTime();
            if(isEffectCancellableEvaluatedSideEffectConcurrent(rule, chg, newChange)){
                return chg;
            }
            else{
                sideEffectResolutionConcurrent(rule, chg, newChange);
            }
        }
        return null;
    }

    private void simpleUpdating(Rule rule, ContextChange chg1, ContextChange chg2){
        assert chg1.getPattern_id().equals(chg2.getPattern_id());
        ContextChange delChange = chg1.getChange_type() == ContextChange.Change_Type.DELETION ? chg1 : chg2;
        ContextChange addChange = chg1.getChange_type() == ContextChange.Change_Type.ADDITION ? chg1 : chg2;
        //Context Pool
        Set<Context> Pool = contextPool.getPoolSet(rule.getRule_id(), delChange.getPattern_id());
        assert Pool.contains(delChange.getContext());
        Pool.remove(delChange.getContext());
        assert !Pool.contains(addChange.getContext());
        Pool.add(addChange.getContext());
    }

    private boolean isEffectCancellableEvaluatedSideEffectConcurrent(Rule rule, ContextChange chg1, ContextChange chg2){
        ContextChange delChange = chg1.getChange_type() == ContextChange.Change_Type.DELETION ? chg1 : chg2;
        ContextChange addChange = chg1.getChange_type() == ContextChange.Change_Type.ADDITION ? chg1 : chg2;
        assert !delChange.equals(addChange);
        if(rule.getCCTRoot() == null)
            return false;
        return rule.getCCTRoot().getFormula().evaluationAndEqualSideEffect(rule.getCCTRoot(), rule.getFormula(), null, delChange, addChange, true, this);
    }
    //并发版本EvaluationAndEqualSideEffect
    public static class evaluationAndEqualSideEffectCon implements Callable<Boolean> {
        RuntimeNode curNode;
        Formula formula;
        ContextChange delChange;
        ContextChange addChange;
        Scheduler scheduler;

        public evaluationAndEqualSideEffectCon(RuntimeNode curNode, Formula formula, ContextChange delChange, ContextChange addChange, Scheduler scheduler) {
            this.curNode = curNode;
            this.formula = formula;
            this.delChange = delChange;
            this.addChange = addChange;
            this.scheduler = scheduler;
        }

        @Override
        public Boolean call() {
            return curNode.getFormula().evaluationAndEqualSideEffect(curNode, formula, null, delChange, addChange, false, scheduler);
        }
    }

    private void sideEffectResolutionConcurrent(Rule rule, ContextChange chg1, ContextChange chg2){
        assert chg1.getPattern_id().equals(chg2.getPattern_id());
        ContextChange delChange = chg1.getChange_type() == ContextChange.Change_Type.DELETION ? chg1 : chg2;
        ContextChange addChange = chg1.getChange_type() == ContextChange.Change_Type.ADDITION ? chg1 : chg2;
        rule.getCCTRoot().getFormula().sideeffectresolution(rule.getCCTRoot(), rule.getFormula(), null, delChange, addChange, true, this);
    }
    //并发版本sideEffectResolution
    public static class sideEffectResolutionCon implements Callable<Void>{
        RuntimeNode curNode;
        Formula formula;
        ContextChange delChange;
        ContextChange addChange;
        Scheduler scheduler;

        public sideEffectResolutionCon(RuntimeNode curNode, Formula formula, ContextChange delChange, ContextChange addChange, Scheduler scheduler) {
            this.curNode = curNode;
            this.formula = formula;
            this.delChange = delChange;
            this.addChange = addChange;
            this.scheduler = scheduler;
        }

        @Override
        public Void call() {
            curNode.getFormula().sideeffectresolution(curNode, formula, null, delChange, addChange, false, scheduler);
            return null;
        }
    }

    @Override
    public void checkEnds() throws NotSupportedException {
        super.checkEnds();
        this.ThreadPool.shutdown();
    }

}
