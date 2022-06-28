package com.CC.Middleware.Schedulers;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Checkers.INFUSE_C;
import com.CC.Middleware.Checkers.ConC;
import com.CC.Middleware.NotSupportedException;

import java.util.*;

public class GEAS_ori extends Scheduler{

    public GEAS_ori(RuleHandler ruleHandler, ContextPool contextPool, Checker checker) {
        super(ruleHandler, contextPool, checker);
        this.strategy = "GEAS_ori";
    }

    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        Batch_Form(contextChange);
        for(Rule rule : ruleHandler.getRuleList()){
            if(rule.getNewBatch() != null){
                this.checker.ctxChangeCheckBatch(rule, rule.getBatch());
                rule.setBatch(rule.getNewBatch());
                rule.setNewBatch(null);
            }
        }
    }

    private void Batch_Form(ContextChange newChange){
        for(Rule rule : ruleHandler.getRuleList()){
            if(!rule.getRelatedPatterns().contains(newChange.getPattern_id()))
                continue;
            if(S_Condition_Match(rule, newChange)){
                List<ContextChange> newBatch = new ArrayList<>();
                newBatch.add(newChange);
                rule.setNewBatch(newBatch);
            }
            else{
                if(rule.getBatch() != null){
                    rule.getBatch().add(newChange);
                }
                else{
                    List<ContextChange> batch = new ArrayList<>();
                    batch.add(newChange);
                    rule.setBatch(batch);
                }
            }
        }
    }

    protected boolean S_Condition_Match(Rule rule, ContextChange newChange) {
        boolean retflag = false;
        if(rule.getBatch() == null)
            return false;
        else{
            for(ContextChange change : rule.getBatch()){
                assert !rule.getIncType(change).equals("NotThisRule");
                assert !rule.getIncType(newChange).equals("NotThisRule");
                if(rule.getIncType(change).equals("Plus")){
                    if(rule.getIncType(newChange).equals("Minus")){
                        retflag = true;
                        break;
                    }
                }
            }
        }
        return retflag;
    }

    public void checkEnds() throws NotSupportedException {
        CleanUp();
        switch (this.checker.getTechnique()) {
            case "ConC":
                ((ConC) checker).ThreadPool.shutdown();
                break;
            case "CPCC_NB":
                ((INFUSE_C) checker).ThreadPool.shutdown();
                break;
            case "BASE":
                assert this.checker instanceof ConC;
                ((ConC) checker).ThreadPool.shutdown();
                break;
        }
    }

    protected void CleanUp() throws NotSupportedException {
        //最后一次检测
        for(Rule rule : ruleHandler.getRuleList()){
            if(rule.getBatch() != null){
                this.checker.ctxChangeCheckBatch(rule, rule.getBatch());
                rule.setBatch(null);
            }
        }
    }

    @Override
    public String getOutputInfo(String ruleType) {
        return null;
    }


}

