package cn.edu.nju.ics.spar.cc.Middleware.Schedulers;


import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Contexts.ContextPool;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.Checker;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.INFUSE_C;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.ConC;


public class IMD extends Scheduler{
    private final boolean isAsyncMode;

    public IMD(RuleHandler ruleHandler, ContextPool contextPool, Checker checker, boolean isAsyncMode) {
        super(ruleHandler, contextPool, checker);
        this.strategy = "IMD";
        this.isAsyncMode = isAsyncMode;
    }

    @Override
    public void doSchedule(ContextChange contextChange) throws Exception {
        if (isAsyncMode) {
            this.checker.ctxChangeCheckIMDAsync(contextChange);
        } else {
            this.checker.ctxChangeCheckIMD(contextChange);
        }
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
