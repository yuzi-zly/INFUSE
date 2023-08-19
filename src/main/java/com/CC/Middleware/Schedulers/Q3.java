package com.CC.Middleware.Schedulers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Checkers.ConC;
import com.CC.Middleware.Checkers.INFUSE_C;
import com.CC.Util.NotSupportedException;

import java.util.ArrayList;
import java.util.List;

public class Q3 extends Scheduler{
    public Q3(RuleHandler ruleHandler, ContextPool contextPool, Checker checker) {
        super(ruleHandler, contextPool, checker);
        this.strategy = "Q3_scheduler";
    }

    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        batchForm(contextChange);
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(rule.getNewBatch() != null){
                assert rule.getBatch().size() > 0;
//                bufferedWriter.write(rule.getBatch() + "\n");
//                bufferedWriter.flush();

                this.checker.ctxChangeCheckBatch(rule, rule.getBatch());
                rule.setBatch(rule.getNewBatch());
                rule.setNewBatch(null);
            }
        }
    }

    private void batchForm(ContextChange newChange){
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(!rule.getVarPatternMap().containsValue(newChange.getPattern_id()))
                continue;
            if(rule.getBatch() == null){
                List<ContextChange> batch = new ArrayList<>();
                batch.add(newChange);
                rule.setBatch(batch);
            }
            else{
                ContextChange firstChange = rule.getBatch().get(0);
                if(firstChange.getAtomicGroup() != newChange.getAtomicGroup()){
                    List<ContextChange> newBatch = new ArrayList<>();
                    newBatch.add(newChange);
                    rule.setNewBatch(newBatch);
                }
                else{
                    rule.getBatch().add(newChange);
                }
            }
        }
    }


    @Override
    public void checkEnds() throws Exception {
        cleanUp();
        switch (this.checker.getTechnique()) {
            case "ConC":
                ((ConC) checker).ThreadPool.shutdown();
                break;
            case "INFUSE":
                ((INFUSE_C) checker).ThreadPool.shutdown();
                break;
            case "BASE":
                assert this.checker instanceof ConC;
                ((ConC) checker).ThreadPool.shutdown();
                break;
        }
    }

    protected void cleanUp() throws NotSupportedException {
        //最后一次检测
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(rule.getBatch() != null){
                assert rule.getBatch().size() > 0;
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
