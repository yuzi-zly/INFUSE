package cn.edu.nju.ics.spar.cc.Middleware.Checkers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode.AsyncTruthValue;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Contexts.ContextPool;
import cn.edu.nju.ics.spar.cc.IoC.ServiceContainer;
import cn.edu.nju.ics.spar.cc.Services.LLMService;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.AsyncEvaluationResult;
import cn.edu.nju.ics.spar.cc.Util.InfuseException;
import cn.edu.nju.ics.spar.cc.Util.NotSupportedException;

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
    
    // For async-aware ECC: rule_id -> [(asyncTruthValue1, linkSet1), (asyncTruthValue2, linkSet2)]
    protected final Map<String, List<Map.Entry<AsyncTruthValue, Set<Link>>>> ruleLinksMapAsync;

    public Checker(RuleHandler ruleHandler, ContextPool contextPool, Object bfuncInstance, boolean isMG) {
        this.ruleHandler = ruleHandler;
        this.contextPool = contextPool;
        this.bfuncInstance = bfuncInstance;
        this.isMG = isMG;
        this.substantialNodes = new HashMap<>();
        this.ruleLinksMap = new HashMap<>();
        this.ruleLinksMapAsync = new HashMap<>();
    }

    protected void storeLink(String rule_id, boolean truth, Set<Link> linkSet){
        this.ruleLinksMap.computeIfAbsent(rule_id, k -> new ArrayList<>());
        Objects.requireNonNull(this.ruleLinksMap.computeIfPresent(rule_id, (k, v) -> v)).add(
                new AbstractMap.SimpleEntry<>(truth, linkSet)
        );
    }
    
    // Async-aware version: store links with AsyncTruthValue instead of boolean
    protected void storeLinkAsync(String rule_id, AsyncTruthValue asyncTruth, Set<Link> linkSet){
        this.ruleLinksMapAsync.computeIfAbsent(rule_id, k -> new ArrayList<>());
        Objects.requireNonNull(this.ruleLinksMapAsync.computeIfPresent(rule_id, (k, v) -> v)).add(
                new AbstractMap.SimpleEntry<>(asyncTruth, linkSet)
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
    
    // Async-aware initialization (for async external calls like LLM, database, API)
    public void checkInitAsync() {
        throw new UnsupportedOperationException("Async-aware checking is not supported for " + technique);
    }
    
    public abstract void ctxChangeCheckIMD(ContextChange contextChange);
    public abstract void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException;
    
    // Async-aware context change checking
    public void ctxChangeCheckIMDAsync(ContextChange contextChange) {
        throw new UnsupportedOperationException("Async-aware checking is not supported for " + technique);
    }
    
    public void ctxChangeCheckBatchAsync(Rule rule, List<ContextChange> batch) throws NotSupportedException {
        throw new UnsupportedOperationException("Async-aware checking is not supported for " + technique);
    }


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
    
    // Async-aware version: get links with AsyncTruthValue
    public Map<String, List<Map.Entry<AsyncTruthValue, Set<Link>>>> getRuleLinksMapAsync() {
        return ruleLinksMapAsync;
    }

    public Map<String, Set<RuntimeNode>> getSubstantialNodes() {
        return substantialNodes;
    }

    public boolean isMG() {
        return isMG;
    }
    
    protected void executeAllAsyncIfNeeded(Rule rule, AsyncEvaluationResult evaluationResult) {
        if (!evaluationResult.hasPendingRequests()) {
            return;
        }

        RuntimeNode root = rule.getCCTRoot();

        // Only execute if root status is PENDING_ASYNC
        if (root.getAsyncTruthValue() == AsyncTruthValue.PENDING_ASYNC) {
            LLMService llmService = ServiceContainer.getInstance().getService(LLMService.class);
            if (llmService != null) {
                try {
                    // Clean up redundant async requests
                    llmService.retainAsyncRequests(evaluationResult.getPendingRequestIds());

                    // Execute all async calls and get results
                    Map<String, Boolean> results = llmService.executeAllAsync();

                    // Update pending nodes with results using the mapping from AsyncEvaluationResult
                    for (Map.Entry<String, Boolean> entry : results.entrySet()) {
                        RuntimeNode node = evaluationResult.getPendingNodes().get(entry.getKey());
                        if (node != null) {
                            node.setAsyncTruthValue(entry.getValue() ?
                                AsyncTruthValue.DETERMINED_TRUE :
                                AsyncTruthValue.DETERMINED_FALSE);
                        }
                    }

                    // Propagate truth value updates from leaves to root
                    rule.updateTruthValueAsync();

                } catch (Exception e) {
                    throw new InfuseException("Failed to execute async calls", e);
                } finally {
                    // Clear async queues
                    llmService.clearAsync();
                }
            }
        }
    }
}
