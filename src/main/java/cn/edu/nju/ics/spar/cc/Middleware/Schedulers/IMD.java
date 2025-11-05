package cn.edu.nju.ics.spar.cc.Middleware.Schedulers;


import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Contexts.ContextPool;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.Checker;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.INFUSE_C;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.ConC;


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
