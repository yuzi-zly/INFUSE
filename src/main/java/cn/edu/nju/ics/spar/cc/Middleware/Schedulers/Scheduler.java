package cn.edu.nju.ics.spar.cc.Middleware.Schedulers;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Contexts.*;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.*;

public abstract class Scheduler {
    protected String strategy;
    protected RuleHandler ruleHandler;
    protected ContextPool contextPool;
    protected Checker checker;

    public Scheduler(RuleHandler ruleHandler, ContextPool contextPool, Checker checker){
        this.ruleHandler = ruleHandler;
        this.contextPool = contextPool;
        this.checker = checker;
    }

    public abstract void doSchedule(ContextChange contextChange) throws Exception;
    public abstract void checkEnds() throws Exception;
    public abstract String getOutputInfo(String ruleType);

    public Checker getChecker() {
        return checker;
    }
}
