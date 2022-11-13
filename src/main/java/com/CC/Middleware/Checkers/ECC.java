package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;

import java.util.List;
import java.util.Set;

public class ECC extends Checker{

    public ECC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions) {
        super(ruleHandler, contextPool, bfunctions);
        this.technique = "ECC";
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        //consistency checking
        for(Rule rule : this.ruleHandler.getRuleMap().values()){
            if (rule.getVarPatternMap().values().contains(contextChange.getPattern_id())){
                //apply change
                contextPool.ApplyChange(rule.getRule_id(), contextChange);
                //build CCT
                rule.BuildCCT_ECCPCC(this);
                //truth value evaluation
                rule.TruthEvaluation_ECC(this);
                //links generation
                Set<Link> links = rule.LinksGeneration_ECC(this);
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
            contextPool.ApplyChange(rule.getRule_id(), contextChange);
        }
        //build CCT
        rule.BuildCCT_ECCPCC(this);
        //truth value evaluation
        rule.TruthEvaluation_ECC(this);
        //links generation
        Set<Link> links = rule.LinksGeneration_ECC(this);
        if(links != null){
            rule.addCriticalSet(links);
        }
        if(links != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
        }
    }
}
