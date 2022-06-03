package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.NotSupportedException;

import java.util.*;

public abstract class Checker {
    protected RuleHandler ruleHandler;
    protected ContextPool contextPool;
    protected String technique;
    protected Object bfunctions;

    protected Set<Map.Entry<String, Map.Entry<String, Map<String, String>>>> Answers;

    public Checker(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions) {
        this.ruleHandler = ruleHandler;
        this.contextPool = contextPool;
        this.bfunctions = bfunctions;
        this.Answers = new LinkedHashSet<>();
    }

    protected void FormatLinks(String rule_id, Link.Link_Type linkType, Set<Map.Entry<String, Context>> vaSet){
        Map<String, String> vaMap = new HashMap<>();
        for(Map.Entry<String, Context> entry : vaSet){
            vaMap.put(entry.getKey(), entry.getValue().getCtx_id());
        }
        Answers.add(new AbstractMap.SimpleEntry<>(rule_id, new AbstractMap.SimpleEntry<>(linkType == Link.Link_Type.VIOLATED ? "VIOLATED" : "SATISFIED", vaMap)));
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

    public Object getBfunctions() {
        return bfunctions;
    }

    public Set<Map.Entry<String, Map.Entry<String, Map<String, String>>>> getAnswers() {
        return Answers;
    }
}
