package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.NotSupportedException;

import java.util.List;
import java.util.Set;

public class BASE extends ConC{

    public BASE(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions) {
        super(ruleHandler, contextPool, bfunctions);
        this.technique = "BASE";
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        for(Rule rule : ruleHandler.getRuleList()){
            if(rule.getRelatedPatterns().contains(contextChange.getPattern_id())){
                //apply changes
                contextPool.ApplyChange(rule.getRule_id(), contextChange);
                rule.UpdateAffectedWithOneChange(contextChange, this);
                //modify CCT
                rule.ModifyCCT_BASE(contextChange, this);
                //truth evaluation
                rule.TruthEvaluation_BASE(contextChange, this);
                //links generation
                Set<Link> links = rule.LinksGeneration_BASE(contextChange, this);
                if(links != null){
                    rule.addCriticalSet(links);
                }
                rule.CleanAffected();
                if(links != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                }
                /*
                //modify CCT
                if(rule.isCCTAlready()){
                    rule.ModifyCCT_BASE(contextChange, this);
                    //truth evaluation
                    rule.TruthEvaluation_BASE(contextChange, this);
                    //links generation
                    Set<Link> links = rule.LinksGeneration_BASE(contextChange, this);
                    if(links != null){
                        rule.addCriticalSet(links);
                    }
                    rule.CleanAffected();
                    if(links != null){
                        storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                    }
                }
                //build CCT
                else{
                    //same as ECC
                    rule.BuildCCT_ECCPCC(this);
                    //truth evaluation
                    rule.TruthEvaluation_ECC(this);
                    //links generation
                    Set<Link> links = rule.LinksGeneration_ECC(this);
                    if(links != null){
                        rule.addCriticalSet(links);
                    }
                    rule.CleanAffected();
                    if(links != null){
                        storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                    }
                }

                 */
            }
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) throws NotSupportedException {
        throw new NotSupportedException("not support");
    }

}
