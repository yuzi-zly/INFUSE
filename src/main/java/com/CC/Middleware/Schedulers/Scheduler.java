package com.CC.Middleware.Schedulers;

import com.CC.Constraints.RuleHandler;
import com.CC.Contexts.*;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.NotSupportedException;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.PatternHandlerFactory;
import org.dom4j.rule.Rule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

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
