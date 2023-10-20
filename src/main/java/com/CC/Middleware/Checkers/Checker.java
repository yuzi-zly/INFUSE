package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Util.NotSupportedException;

import java.util.*;

public abstract class Checker {
    protected RuleHandler ruleHandler;
    protected ContextPool contextPool;
    protected String technique;
    protected Object bfuncInstance;
    // for MG
    protected boolean isMG;
    protected final Map<String, Set<RuntimeNode>> substantialNodes;

    // rule_id -> [(truthValue1, linkSet1), (truthValue2,linkSet2)]
    protected final Map<String, List<Map.Entry<Boolean, Set<Link>>>> ruleLinksMap;

    public Checker(RuleHandler ruleHandler, ContextPool contextPool, Object bfuncInstance, boolean isMG) {
        this.ruleHandler = ruleHandler;
        this.contextPool = contextPool;
        this.bfuncInstance = bfuncInstance;
        this.isMG = isMG;
        this.substantialNodes = new HashMap<>();
        this.ruleLinksMap = new HashMap<>();
    }

    protected void storeLink(String rule_id, boolean truth, Set<Link> linkSet){
        this.ruleLinksMap.computeIfAbsent(rule_id, k -> new ArrayList<>());
        Objects.requireNonNull(this.ruleLinksMap.computeIfPresent(rule_id, (k, v) -> v)).add(
                new AbstractMap.SimpleEntry<>(truth, linkSet)
        );
    }

    public void checkInit(){
        for(Rule rule : ruleHandler.getRuleMap().values()){
            rule.buildCCT_ECCPCC(this);
            rule.truthEvaluation_ECC(this);
            Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
            if(this.isMG){
                this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
            }
            rule.linksGeneration_ECC(this, prevSubstantialNodes);
        }
    }
    public abstract void ctxChangeCheckIMD(ContextChange contextChange);
    public abstract void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException;


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

    public Map<String, List<Map.Entry<Boolean, Set<Link>>>> getRuleLinksMap() {
        return ruleLinksMap;
    }

    public Map<String, Set<RuntimeNode>> getSubstantialNodes() {
        return substantialNodes;
    }

    public boolean isMG() {
        return isMG;
    }
}
