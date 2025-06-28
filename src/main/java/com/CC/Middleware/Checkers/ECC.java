package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ECC extends Checker{

    public ECC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions, boolean isMG) {
        super(ruleHandler, contextPool, bfunctions, isMG);
        this.technique = "ECC";
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        // for hospital progress
        if(contextChange.getPattern_id().equals("P_temporal_1") &&
            contextChange.getChange_type() == ContextChange.Change_Type.ADDITION) {
            System.out.println("ECC+IMD Processing: " + contextChange.getContext().getCtx_id());
        }

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
        // for hospital progresss
        List<String> related_ctx_ids = new ArrayList<>();
        for (ContextChange contextChange : batch) {
            if (contextChange.getPattern_id().equals("P_temporal_1") &&
                contextChange.getChange_type() == ContextChange.Change_Type.ADDITION) {
                related_ctx_ids.add(contextChange.getContext().getCtx_id());
            }
        }
        long max_ctx_id = 0;
        for (String ctx_id : related_ctx_ids) {
            max_ctx_id = Math.max(max_ctx_id, Long.parseLong(ctx_id.split("_")[1]));
        }
        System.out.println("ECC+GEAS Processing: ctx_" + max_ctx_id);

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
}
