package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;

import java.util.List;
import java.util.Set;

public class PCC extends Checker{

    public PCC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions) {
        super(ruleHandler, contextPool, bfunctions);
        this.technique = "PCC";
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        //consistency checking
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(rule.getVarPatternMap().values().contains(contextChange.getPattern_id())){
                //apply changes
                contextPool.ApplyChange(rule.getRule_id(), contextChange);
                rule.UpdateAffectedWithOneChange(contextChange, this);
                /*
                //modify CCT
                if(rule.isCCTAlready()){
                    rule.ModifyCCT_PCC(contextChange, this);
                    //truth evaluation
                    rule.TruthEvaluation_PCC(contextChange, this);
                    //links generation
                    Set<Link> links = rule.LinksGeneration_PCC(contextChange, this);
                    if(links != null){
                        rule.addCriticalSet(links);
                        //rule.oracleCount(links, contextChange);
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
                        //rule.oracleCount(links, contextChange);
                    }
                    rule.CleanAffected();
                    if(links != null){
                        storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                    }
                }
                 */

                rule.ModifyCCT_PCC(contextChange, this);
                //truth evaluation
                rule.TruthEvaluation_PCC(contextChange, this);
                //links generation
                Set<Link> links = rule.LinksGeneration_PCC(contextChange, this);
                if(links != null){
                    rule.addCriticalSet(links);
                    //rule.oracleCount(links, contextChange);
                }
                rule.CleanAffected();
                if(links != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                }
            }
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch) {
        //rule.intoFile(batch);
        //clean
        for(String pattern_id : rule.getVarPatternMap().values()){
            contextPool.GetAddSet(pattern_id).clear();
            contextPool.GetDelSet(pattern_id).clear();
            contextPool.GetUpdSet(pattern_id).clear();
        }
        for(ContextChange contextChange : batch){
            contextPool.ApplyChangeWithSets(rule.getRule_id(), contextChange);
            /*
            if(rule.isCCTAlready()){
                rule.ModifyCCT_PCCM(contextChange, this);
            }
            else{
                rule.BuildCCT_ECCPCC(this);
            }

             */
            rule.ModifyCCT_PCCM(contextChange, this);
        }
        rule.UpdateAffectedWithChanges(this);
        rule.TruthEvaluation_PCCM(this);
        Set<Link> links = rule.LinksGeneration_PCCM(this);
        if(links != null){
            rule.addCriticalSet(links);
        }
        rule.CleanAffected();
        if(links != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
        }
    }

}
