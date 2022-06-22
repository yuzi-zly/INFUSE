package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.NotSupportedException;

import java.util.*;

public abstract class Checker {
    protected RuleHandler ruleHandler;
    protected ContextPool contextPool;
    protected String technique;
    protected Object bfuncInstance;

    // rule_id -> [linkSet1, linkSet2]
    protected Map<String, List<Set<Link>>> ruleLinksMap;

    public Checker(RuleHandler ruleHandler, ContextPool contextPool, Object bfuncInstance) {
        this.ruleHandler = ruleHandler;
        this.contextPool = contextPool;
        this.bfuncInstance = bfuncInstance;
        this.ruleLinksMap = new HashMap<>();
    }

    protected void storeLink(String rule_id, Set<Link> linkSet){
        this.ruleLinksMap.computeIfAbsent(rule_id, k -> new ArrayList<>());
        Objects.requireNonNull(this.ruleLinksMap.computeIfPresent(rule_id, (k, v) -> v)).add(linkSet);
    }

    public abstract void CtxChangeCheckIMD(ContextChange contextChange);
    public abstract void CtxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException;


    //getter
    public RuleHandler getRuleHandler() {
        return ruleHandler;
    }

    public ContextPool getContextPool() {
        return contextPool;
    }
    public String getTechnique() {
        return technique;
    }

    public Object getBfuncInstance() {
        return bfuncInstance;
    }

    public Map<String, List<Set<Link>>> getRuleLinksMap() {
        return ruleLinksMap;
    }
}
