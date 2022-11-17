package com.CC.Constraints.Formulas;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Runtime.LGUtils;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Schedulers.Scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FNot extends Formula{
    private Formula subformula;


    //constructors
    public FNot(){
        this.setFormula_type(Formula_Type.NOT);
        this.subformula = null;
        this.setAffected(false);
    }

    //getter and setter
    public Formula getSubformula() {
        return subformula;
    }

    @Override
    public Formula_Type getFormula_type() {
        return super.getFormula_type();
    }

    public void setSubformula(Formula subformula) {
        this.subformula = subformula;
    }

    @Override
    public void setFormula_type(Formula_Type formula_type) {
        super.setFormula_type(formula_type);
    }

    @Override
    public void output(int offset) {
        for(int i = 0; i < offset; ++i)
            System.out.print(" ");
        System.out.println("not  affected: " + this.isAffected());
        subformula.output(offset + 2);
    }

    @Override
    public Formula FormulaClone() {
        return new FNot();
    }

    //S-condition
    @Override
    public void DeriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        this.subformula.DeriveIncMinusSet(incPlusSet);
    }

    @Override
    public void DeriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        this.subformula.DeriveIncPlusSet(incMinusSet);
    }

    //C-condition
    @Override
    public boolean EvaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, addChange.getContext());
        }

        boolean result;
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        result = runtimeNode.getFormula().EvaluationAndEqualSideEffect(runtimeNode, ((FNot)originFormula).getSubformula(), var, delChange, addChange, canConcurrent, scheduler);
        boolean newTruth = !runtimeNode.isTruth();
        curNode.setOptTruth(curNode.isTruth());
        curNode.setTruth(newTruth);

        return result;
    }

    @Override
    public void sideEffectResolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.setTruth(curNode.isOptTruth());
            curNode.setOptTruth(false);
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, delChange.getContext());
        }
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().sideEffectResolution(runtimeNode, ((FNot)originFormula).getSubformula(), var, delChange, addChange, canConcurrent, scheduler);
    }

    //DIS
    @Override
    public void DeriveRCRESets(boolean from) {
        this.subformula.DeriveRCRESets(!from);
    }

    //PCC
    @Override
    public boolean UpdateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        boolean result = this.subformula.UpdateAffectedWithOneChange(contextChange, checker);
        this.setAffected(result);
        return result;
    }

    //PCCM &&  CPCC
    @Override
    public boolean UpdateAffectedWithChanges(Checker checker) {
        boolean result = this.subformula.UpdateAffectedWithChanges(checker);
        this.setAffected(result);
        return result;
    }
    @Override
    public void CleanAffected() {
        this.setAffected(false);
        this.subformula.CleanAffected();
    }

    //CPCC_NB
    @Override
    public void UpdateCanConcurrent_CPCC_NB(boolean canConcurrent, Rule rule, Checker checker) {
        if(canConcurrent){
            this.subformula.UpdateCanConcurrent_CPCC_NB(true, rule, checker);
        }
    }

    @Override
    public void CleanAffectedAndCanConcurrent() {
        this.setAffected(false);
        this.subformula.CleanAffectedAndCanConcurrent();
    }

    /*
                                        ECC PCC
                                     */
    @Override
    public void CreateBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode = new RuntimeNode(((FNot)originFormula).getSubformula());
        runtimeNode.setDepth(curNode.getDepth() + 1);
        runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode);
        //递归调用
        runtimeNode.getFormula().CreateBranches_ECCPCC(rule_id, runtimeNode, ((FNot) originFormula).getSubformula(), checker);
    }

    /*
        ECC
     */
    @Override
    public boolean TruthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker){
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        boolean result = !runtimeNode.getFormula().TruthEvaluation_ECC(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
        curNode.setTruth(result);
        return  result;
    }

    @Override
    public Set<Link> LinksGeneration_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case: all
        // taint substantial node
        if(checker.isMG()){
            checker.getCurSubstantialNodes().add(runtimeNode);
        }
        // generate links
        Set<Link> ret = runtimeNode.getFormula().LinksGeneration_ECC(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
        Set<Link> result = lgUtils.flipSet(ret);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */

    @Override
    public void ModifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().ModifyBranch_PCC(rule_id, runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
    }

    @Override
    public boolean TruthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            boolean result = runtimeNode.getFormula().TruthEvaluation_PCC(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
            curNode.setTruth(!result);
            return !result;
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> LinksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case
        // taint substantial node
        if(checker.isMG()){
            checker.getCurSubstantialNodes().add(runtimeNode);
        }
        // generate links
        if(originFormula.isAffected()){
            Set<Link> ret = runtimeNode.getFormula().LinksGeneration_PCC(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
            result.addAll(lgUtils.flipSet(ret));
        }
        else{
            if(checker.isMG()){
                // check whether curNode.links reusable
                if(checker.getPrevSubstantialNodes().contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret = runtimeNode.getFormula().LinksGeneration_PCC(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
                    result.addAll(lgUtils.flipSet(ret));
                }
            }
            else{
                return curNode.getLinks();
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        ConC
     */
    @Override
    public void CreateBranches_ConC(String  rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        //分支1
        RuntimeNode runtimeNode = new RuntimeNode(((FNot)originFormula).getSubformula());
        runtimeNode.setDepth(curNode.getDepth() + 1);
        runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode);
        //递归调用
        runtimeNode.getFormula().CreateBranches_ConC(rule_id, runtimeNode, ((FNot) originFormula).getSubformula(), canConcurrent, checker);
    }

    @Override
    public boolean TruthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        boolean result = !runtimeNode.getFormula().TruthEvaluation_ConC(runtimeNode, ((FNot)originFormula).getSubformula(), canConcurrent, checker);
        curNode.setTruth(result);
        return  result;
    }

    @Override
    public Set<Link> LinksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case: all
        // taint substantial node
        if(checker.isMG()){
            checker.getCurSubstantialNodes().add(runtimeNode);
        }
        // generate links
        Set<Link> ret = runtimeNode.getFormula().LinksGeneration_ConC(runtimeNode, ((FNot)originFormula).getSubformula(), canConcurrent, checker);
        Set<Link> result = lgUtils.flipSet(ret);;
        curNode.setLinks(result);
        return curNode.getLinks();
    }


    /*
        PCCM
     */

    @Override
    public void ModifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().ModifyBranch_PCCM(rule_id, runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
    }

    @Override
    public boolean TruthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else{
            boolean result = runtimeNode.getFormula().TruthEvaluation_PCCM(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
            curNode.setTruth(!result);
            return !result;
        }
    }

    @Override
    public Set<Link> LinksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        LGUtils lgUtils = new LGUtils();
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            Set<Link> ret = runtimeNode.getFormula().LinksGeneration_PCCM(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
            Set<Link> result = lgUtils.flipSet(ret);
            curNode.setLinks(result);
            return curNode.getLinks();
        }
        else{
            return curNode.getLinks();
        }
    }

    /*
        CPCC_NB
     */
    @Override
    public void CreateBranches_CPCC_NB(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode = new RuntimeNode(((FNot)originFormula).getSubformula());
        runtimeNode.setDepth(curNode.getDepth() + 1);
        runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode.setParent(curNode);
        curNode.getChildren().add(runtimeNode);
        //递归调用
        runtimeNode.getFormula().CreateBranches_CPCC_NB(rule, runtimeNode, ((FNot) originFormula).getSubformula(), checker);

    }

    @Override
    public void ModifyBranch_CPCC_NB(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().ModifyBranch_CPCC_NB(rule, runtimeNode, ((FNot)originFormula).getSubformula(), checker);

    }

    @Override
    public boolean TruthEvaluationCom_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        boolean result = !runtimeNode.getFormula().TruthEvaluationCom_CPCC_NB(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return  result;
    }

    @Override
    public boolean TruthEvaluationPar_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else{
            boolean result = !runtimeNode.getFormula().TruthEvaluationPar_CPCC_NB(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
    }

    @Override
    public Set<Link> LinksGeneration_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker) {
        LGUtils lgUtils = new LGUtils();
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            Set<Link> ret = runtimeNode.getFormula().LinksGeneration_CPCC_NB(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
            Set<Link> result = lgUtils.flipSet(ret);
            curNode.setLinks(result);
            return curNode.getLinks();
        }
        else{
            return curNode.getLinks();
        }
    }

    /*
        CPCC_BASE
     */

    @Override
    public void ModifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().ModifyBranch_BASE(rule_id, runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
    }

    @Override
    public boolean TruthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            boolean result = runtimeNode.getFormula().TruthEvaluation_BASE(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
            curNode.setTruth(!result);
            return !result;
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> LinksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        LGUtils lgUtils = new LGUtils();
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            Set<Link> ret = runtimeNode.getFormula().LinksGeneration_BASE(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
            Set<Link> result = lgUtils.flipSet(ret);
            curNode.setLinks(result);
            return curNode.getLinks();
        }
        else{
            return curNode.getLinks();
        }
    }
}
