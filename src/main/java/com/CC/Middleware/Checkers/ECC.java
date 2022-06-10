package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
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
    public void CtxChangeCheckIMD(ContextChange contextChange) {
        //consistency checking
        for(Rule rule : this.ruleHandler.getRuleList()){
            if (rule.getRelatedPatterns().contains(contextChange.getPattern_id())){
                //apply change
                contextPool.ApplyChange(rule.getRule_id(), contextChange);
                //build CCT
                rule.BuildCCT_ECCPCC(this);
                //truth value evaluation
                rule.TruthEvaluation_ECC(this);
                //links generation
                Set<Link> links = rule.LinksGeneration_ECC(this);
                if(links != null){
                    for(Link link : links){
                        storeLink(rule.getRule_id(), link);
                    }
                }
            }
        }
    }

    @Override
    public void CtxChangeCheckBatch(Rule rule, List<ContextChange> batch){
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
            for(Link link : links){
                storeLink(rule.getRule_id(), link);
            }
        }
    }
}
