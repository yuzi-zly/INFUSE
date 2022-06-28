package com.CC.Middleware.Schedulers;


import com.CC.Constraints.RuleHandler;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Checkers.INFUSE_C;
import com.CC.Middleware.Checkers.ConC;


public class IMD extends Scheduler{


    public IMD(RuleHandler ruleHandler, ContextPool contextPool, Checker checker) {
        super(ruleHandler, contextPool, checker);
        this.strategy = "IMD";
    }

    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        this.checker.ctxChangeCheckIMD(contextChange);
    }

    @Override
    public void checkEnds() throws Exception {
        switch (this.checker.getTechnique()) {
            case "ConC":
                ((ConC) this.checker).ThreadPool.shutdown();
                break;
            case "CPCC_NB":
                ((INFUSE_C) this.checker).ThreadPool.shutdown();
                break;
            case "BASE":
                assert this.checker instanceof ConC;
                ((ConC) this.checker).ThreadPool.shutdown();
                break;
        }
    }

    @Override
    public String getOutputInfo(String ruleType) {
       return null;
    }
}
