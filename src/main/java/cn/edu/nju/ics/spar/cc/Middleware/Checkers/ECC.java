package cn.edu.nju.ics.spar.cc.Middleware.Checkers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Contexts.ContextPool;

public class ECC extends Checker{

    public ECC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions, boolean isMG) {
        super(ruleHandler, contextPool, bfunctions, isMG);
        this.technique = "ECC";
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        //consistency checking
        for(Rule rule : this.ruleHandler.getRuleMap().values()){
            if (rule.getVarPatternMap().containsValue(contextChange.getPattern_id())){
                //apply change
                contextPool.applyChange(rule.getRule_id(), contextChange);
                //build CCT
                rule.buildCCT_ECCPCC(this);
                //truth value evaluation
                rule.truthEvaluation_ECC(this);
                //taint SCCT
                Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
                if(this.isMG){
                    this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
                }
                //links generation
                Set<Link> links = rule.linksGeneration_ECC(this, prevSubstantialNodes);
                if(links != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                }
            }
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch){
        //apply change
        for(ContextChange contextChange : batch){
            contextPool.applyChange(rule.getRule_id(), contextChange);
        }
        //build CCT
        rule.buildCCT_ECCPCC(this);
        //truth value evaluation
        rule.truthEvaluation_ECC(this);
        //taint SCCT
        Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
        if(this.isMG){
            this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
        }
        //links generation
        Set<Link> links = rule.linksGeneration_ECC(this, prevSubstantialNodes);
        if(links != null){
            rule.addCriticalSet(links);
        }
        if(links != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
        }
    }
    
    // ========== Async-aware methods (for async external calls) ==========
    
    @Override
    public void checkInitAsync() {
        for(Rule rule : ruleHandler.getRuleMap().values()){
            rule.buildCCT_ECCPCC(this);
            
            // Phase 1: Async truth evaluation (replaces truthEvaluation_ECC)
            rule.truthEvaluationAsync_ECC(this);
            
            // Phase 2: Execute pending async calls if root status is PENDING_ASYNC
            // After execution, updateTruthValueAsync is called to propagate updates
            executeAllAsyncIfNeeded(rule);
            
            // Phase 3: Links generation (no MG support)
            Set<Link> links = rule.linksGenerationAsync_ECC(this);
            if(links != null){
                storeLinkAsync(rule.getRule_id(), rule.getCCTRoot().getAsyncTruthValue(), links);
            }
        }
    }
    
    @Override
    public void ctxChangeCheckIMDAsync(ContextChange contextChange) {
        for(Rule rule : this.ruleHandler.getRuleMap().values()){
            if (rule.getVarPatternMap().containsValue(contextChange.getPattern_id())){
                // Apply change
                contextPool.applyChange(rule.getRule_id(), contextChange);
                
                // Build CCT
                rule.buildCCT_ECCPCC(this);
                
                // Phase 1: Async truth evaluation
                rule.truthEvaluationAsync_ECC(this);
                
                // Phase 2: Execute pending async calls and update truth values
                executeAllAsyncIfNeeded(rule);
                
                // Phase 3: Links generation (no MG support)
                Set<Link> links = rule.linksGenerationAsync_ECC(this);
                if(links != null){
                    storeLinkAsync(rule.getRule_id(), rule.getCCTRoot().getAsyncTruthValue(), links);
                }
            }
        }
    }
    
    @Override
    public void ctxChangeCheckBatchAsync(Rule rule, List<ContextChange> batch) {
        // Apply changes
        for(ContextChange contextChange : batch){
            contextPool.applyChange(rule.getRule_id(), contextChange);
        }
        
        // Build CCT
        rule.buildCCT_ECCPCC(this);
        
        // Phase 1: Async truth evaluation
        rule.truthEvaluationAsync_ECC(this);
        
        // Phase 2: Execute pending async calls and update truth values
        executeAllAsyncIfNeeded(rule);
        
        // Phase 3: Links generation (no MG support)
        Set<Link> links = rule.linksGenerationAsync_ECC(this);
        if(links != null){
            rule.addCriticalSet(links);
        }
        if(links != null){
            storeLinkAsync(rule.getRule_id(), rule.getCCTRoot().getAsyncTruthValue(), links);
        }
    }
}
