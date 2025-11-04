package com.CC.Middleware.Schedulers;


import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Checkers.INFUSE_C;
import com.CC.Middleware.Checkers.ConC;


public class IMD extends Scheduler{


    public IMD(RuleHandler ruleHandler, ContextPool contextPool, Checker checker, String taskOutFile) {
        super(ruleHandler, contextPool, checker, taskOutFile);
        this.strategy = "IMD";
    }

    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        // TaskInfo: [Strategy] {size}: {changes}
        writeTaskInfo(formatTaskLine(java.util.Collections.singletonList(contextChange)));
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
        closeTaskWriter();
    }

    @Override
    public String getOutputInfo(String ruleType) {
       return null;
    }
}
