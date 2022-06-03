package com.CC.Middleware.Schedulers;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.Checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GEAS_opt_s extends GEAS_ori{


    public GEAS_opt_s(RuleHandler ruleHandler, ContextPool contextPool, Checker checker) {
        super(ruleHandler, contextPool, checker);
        this.strategy = "GEAS_opt_s";
    }

    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        Batch_FormAndRefine_Serial(contextChange);
        for(Rule rule : ruleHandler.getRuleList()){
            if(rule.getNewBatch() != null){
                this.checker.CtxChangeCheckBatch(rule, rule.getBatch());
                rule.setBatch(rule.getNewBatch());
                rule.setNewBatch(null);
            }
        }
    }

    private void Batch_FormAndRefine_Serial(ContextChange newChange){
        for(Rule rule : ruleHandler.getRuleList()){
            if(!rule.getRelatedPatterns().contains(newChange.getPattern_id()))
                continue;

            if(S_Condition_Match(rule, newChange)){
                List<ContextChange> newBatch = new ArrayList<>();
                newBatch.add(newChange);
                rule.setNewBatch(newBatch);
            }
            else{
                ContextChange oriChange = C_Condition_Examine_Serial(rule, newChange);
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

    private ContextChange C_Condition_Examine_Serial(Rule rule, ContextChange newChange){
        if(rule.getBatch() == null)
            return null;
        if(rule.getIncType(newChange).equals("Minus"))
            return null;
        if(newChange.getChange_type() == ContextChange.Change_Type.DELETION){
            Set<Context> pool = contextPool.GetPoolSet(rule.getRule_id(), newChange.getPattern_id());
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

            if(rule.inCriticalSet(chg.getContext().getCtx_id()) || rule.inCriticalSet(newChange.getContext().getCtx_id())){
                continue;
            }
            //examine part2 - sideEffect
            if(isEffectCancellableEvaluated_sideEffect_Serial(rule, chg, newChange)){
                return chg;
            }
            else{
                sideEffectResolution_Serial(rule, chg, newChange);
            }
        }
        return null;
    }

    private void simpleUpdating(Rule rule, ContextChange chg1, ContextChange chg2){
        assert chg1.getPattern_id().equals(chg2.getPattern_id());
        ContextChange delChange = chg1.getChange_type() == ContextChange.Change_Type.DELETION ? chg1 : chg2;
        ContextChange addChange = chg1.getChange_type() == ContextChange.Change_Type.ADDITION ? chg1 : chg2;
        //Context Pool
        Set<Context> Pool = contextPool.GetPoolSet(rule.getRule_id(), delChange.getPattern_id());
        assert Pool.contains(delChange.getContext());
        Pool.remove(delChange.getContext());
        assert !Pool.contains(addChange.getContext());
        Pool.add(addChange.getContext());
    }

    private boolean isEffectCancellableEvaluated_sideEffect_Serial(Rule rule, ContextChange chg1, ContextChange chg2){
        ContextChange delChange = chg1.getChange_type() == ContextChange.Change_Type.DELETION ? chg1 : chg2;
        ContextChange addChange = chg1.getChange_type() == ContextChange.Change_Type.ADDITION ? chg1 : chg2;
        assert !delChange.equals(addChange);
        if(rule.getCCTRoot() == null)
            return false;
        return rule.getCCTRoot().getFormula().EvaluationAndEqualSideEffect(rule.getCCTRoot(), rule.getFormula(), null, delChange, addChange, false, this);
    }

    private void sideEffectResolution_Serial(Rule rule, ContextChange chg1, ContextChange chg2){
        assert chg1.getPattern_id().equals(chg2.getPattern_id());
        ContextChange delChange = chg1.getChange_type() == ContextChange.Change_Type.DELETION ? chg1 : chg2;
        ContextChange addChange = chg1.getChange_type() == ContextChange.Change_Type.ADDITION ? chg1 : chg2;
        rule.getCCTRoot().getFormula().sideEffectResolution(rule.getCCTRoot(), rule.getFormula(), null, delChange, addChange, false, this);
    }

}
