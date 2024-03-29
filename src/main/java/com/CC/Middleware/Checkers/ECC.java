package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;

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
}
