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
    public Formula formulaClone() {
        return new FNot();
    }

    //S-condition
    @Override
    public void deriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        this.subformula.deriveIncMinusSet(incPlusSet);
    }

    @Override
    public void deriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        this.subformula.deriveIncPlusSet(incMinusSet);
    }

    //C-condition
    @Override
    public boolean evaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, addChange.getContext());
        }

        boolean result;
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        result = runtimeNode.getFormula().evaluationAndEqualSideEffect(runtimeNode, ((FNot)originFormula).getSubformula(), var, delChange, addChange, canConcurrent, scheduler);
        boolean newTruth = !runtimeNode.isTruth();
        curNode.setOptTruth(curNode.isTruth());
        curNode.setTruth(newTruth);

        return result;
    }

    @Override
    public void sideeffectresolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.setTruth(curNode.isOptTruth());
            curNode.setOptTruth(false);
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, delChange.getContext());
        }
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().sideeffectresolution(runtimeNode, ((FNot)originFormula).getSubformula(), var, delChange, addChange, canConcurrent, scheduler);
    }

    //DIS
    @Override
    public void deriveRCRESets(boolean from) {
        this.subformula.deriveRCRESets(!from);
    }

    //PCC
    @Override
    public boolean updateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        boolean result = this.subformula.updateAffectedWithOneChange(contextChange, checker);
        this.setAffected(result);
        return result;
    }

    //PCCM &&  CPCC
    @Override
    public boolean updateAffectedWithChanges(Checker checker) {
        boolean result = this.subformula.updateAffectedWithChanges(checker);
        this.setAffected(result);
        return result;
    }
    @Override
    public void cleanAffected() {
        this.setAffected(false);
        this.subformula.cleanAffected();
    }

    //CPCC_NB
    @Override
    public void updateCanConcurrent_INFUSE(boolean canConcurrent, Rule rule, Checker checker) {
        if(canConcurrent){
            this.subformula.updateCanConcurrent_INFUSE(true, rule, checker);
        }
    }

    @Override
    public void cleanAffectedAndCanConcurrent() {
        this.setAffected(false);
        this.subformula.cleanAffectedAndCanConcurrent();
    }

    //MG
    @Override
    public void taintSCCT(RuntimeNode curNode, Formula originFormula, Set<RuntimeNode> substantialNodes) {
        substantialNodes.add(curNode);
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().taintSCCT(runtimeNode, ((FNot) originFormula).getSubformula(), substantialNodes);
    }

    /*
                                            ECC PCC
                                         */
    @Override
    public void createBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode = new RuntimeNode(((FNot)originFormula).getSubformula());
        runtimeNode.setDepth(curNode.getDepth() + 1);
        runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode);
        //递归调用
        runtimeNode.getFormula().createBranches_ECCPCC(rule_id, runtimeNode, ((FNot) originFormula).getSubformula(), checker);
    }

    /*
        ECC
     */
    @Override
    public boolean truthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker){
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        boolean result = !runtimeNode.getFormula().truthEvaluation_ECC(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
        curNode.setTruth(result);
        return  result;
    }

    @Override
    public Set<Link> linksGeneration_ECC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case: all
        Set<Link> ret = runtimeNode.getFormula().linksGeneration_ECC(runtimeNode, ((FNot)originFormula).getSubformula(), prevSubstantialNodes, checker);
        Set<Link> result = lgUtils.flipSet(ret);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */

    @Override
    public void modifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().modifyBranch_PCC(rule_id, runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            boolean result = runtimeNode.getFormula().truthEvaluation_PCC(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
            curNode.setTruth(!result);
            return !result;
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> linksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case
        if(originFormula.isAffected()){
            Set<Link> ret = runtimeNode.getFormula().linksGeneration_PCC(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
            result.addAll(lgUtils.flipSet(ret));
        }
        else{
            if(checker.isMG()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret = runtimeNode.getFormula().linksGeneration_PCC(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
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
    public void createBranches_ConC(String  rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        //分支1
        RuntimeNode runtimeNode = new RuntimeNode(((FNot)originFormula).getSubformula());
        runtimeNode.setDepth(curNode.getDepth() + 1);
        runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode);
        //递归调用
        runtimeNode.getFormula().createBranches_ConC(rule_id, runtimeNode, ((FNot) originFormula).getSubformula(), canConcurrent, checker);
    }

    @Override
    public boolean truthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        boolean result = !runtimeNode.getFormula().truthEvaluation_ConC(runtimeNode, ((FNot)originFormula).getSubformula(), canConcurrent, checker);
        curNode.setTruth(result);
        return  result;
    }

    @Override
    public Set<Link> linksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case: all
        Set<Link> ret = runtimeNode.getFormula().linksGeneration_ConC(runtimeNode, ((FNot)originFormula).getSubformula(), canConcurrent, prevSubstantialNodes, checker);
        Set<Link> result = lgUtils.flipSet(ret);;
        curNode.setLinks(result);
        return curNode.getLinks();
    }


    /*
        PCCM
     */

    @Override
    public void modifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().modifyBranch_PCCM(rule_id, runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else{
            boolean result = runtimeNode.getFormula().truthEvaluation_PCCM(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
            curNode.setTruth(!result);
            return !result;
        }
    }

    @Override
    public Set<Link> linksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case
        if(originFormula.isAffected()){
            Set<Link> ret = runtimeNode.getFormula().linksGeneration_PCCM(runtimeNode, ((FNot)originFormula).getSubformula(), prevSubstantialNodes, checker);
            result.addAll(lgUtils.flipSet(ret));
        }
        else{
            if(checker.isMG()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret = runtimeNode.getFormula().linksGeneration_PCCM(runtimeNode, ((FNot)originFormula).getSubformula(), prevSubstantialNodes, checker);
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
        CPCC_NB
     */
    @Override
    public void createBranches_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode = new RuntimeNode(((FNot)originFormula).getSubformula());
        runtimeNode.setDepth(curNode.getDepth() + 1);
        runtimeNode.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode.setParent(curNode);
        curNode.getChildren().add(runtimeNode);
        //递归调用
        runtimeNode.getFormula().createBranches_INFUSE(rule, runtimeNode, ((FNot) originFormula).getSubformula(), checker);

    }

    @Override
    public void modifyBranch_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().modifyBranch_INFUSE(rule, runtimeNode, ((FNot)originFormula).getSubformula(), checker);

    }

    @Override
    public boolean truthEvaluationCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        boolean result = !runtimeNode.getFormula().truthEvaluationCom_INFUSE(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return  result;
    }

    @Override
    public boolean truthEvaluationPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else{
            boolean result = !runtimeNode.getFormula().truthEvaluationPar_INFUSE(runtimeNode, ((FNot)originFormula).getSubformula(), checker);
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case
        if(originFormula.isAffected()){
            Set<Link> ret = runtimeNode.getFormula().linksGeneration_INFUSE(runtimeNode, ((FNot)originFormula).getSubformula(), prevSubstantialNodes, checker);
            result.addAll(lgUtils.flipSet(ret));
        }
        else{
            if(checker.isMG()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret = runtimeNode.getFormula().linksGeneration_INFUSE(runtimeNode, ((FNot)originFormula).getSubformula(), prevSubstantialNodes, checker);
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
        CPCC_BASE
     */

    @Override
    public void modifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        runtimeNode.getFormula().modifyBranch_BASE(rule_id, runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(originFormula.isAffected()){
            RuntimeNode runtimeNode = curNode.getChildren().get(0);
            boolean result = runtimeNode.getFormula().truthEvaluation_BASE(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, checker);
            curNode.setTruth(!result);
            return !result;
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> linksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode = curNode.getChildren().get(0);
        LGUtils lgUtils = new LGUtils();
        // only one case
        if(originFormula.isAffected()){
            Set<Link> ret = runtimeNode.getFormula().linksGeneration_BASE(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
            result.addAll(lgUtils.flipSet(ret));
        }
        else{
            if(checker.isMG()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret = runtimeNode.getFormula().linksGeneration_BASE(runtimeNode, ((FNot)originFormula).getSubformula(), contextChange, prevSubstantialNodes, checker);
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
}
