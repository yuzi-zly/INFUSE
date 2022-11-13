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

public class FOr extends Formula {
    private final Formula[] subformulas;

    //constructors
    public FOr(){
        this.setFormula_type(Formula_Type.OR);
        this.subformulas = new Formula[2];
        this.setAffected(false);
    }

    public void replaceSubformula(int pos, Formula formula){
        if( pos < 0 || pos >= 2){
            System.err.println("[CCE] position is out of range.");
            System.exit(1);
        }
        this.subformulas[pos] = formula;
    }

    @Override
    public void setFormula_type(Formula.Formula_Type formula_type) {
        super.setFormula_type(formula_type);
    }

    @Override
    public Formula.Formula_Type getFormula_type() {
        return super.getFormula_type();
    }

    public Formula[] getSubformulas() {
        return subformulas;
    }

    @Override
    public void output(int offset) {
        for(int i = 0; i < offset; ++i)
            System.out.print(" ");
        System.out.println("or  affected: " + this.isAffected());
        subformulas[0].output(offset+2);
        subformulas[1].output(offset+2);
    }

    @Override
    public Formula FormulaClone() {
        return new FOr();
    }

    //S-condition
    @Override
    public void DeriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        this.subformulas[0].DeriveIncPlusSet(incPlusSet);
        this.subformulas[1].DeriveIncPlusSet(incPlusSet);
    }

    @Override
    public void DeriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        this.subformulas[0].DeriveIncMinusSet(incMinusSet);
        this.subformulas[1].DeriveIncMinusSet(incMinusSet);
    }

    //C-condition
    @Override
    public boolean EvaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, addChange.getContext());
        }

        boolean result;
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean tempresult = runtimeNode1.getFormula().EvaluationAndEqualSideEffect(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], var, delChange, addChange, canConcurrent, scheduler);
        result = tempresult;

        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        tempresult = runtimeNode2.getFormula().EvaluationAndEqualSideEffect(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], var, delChange, addChange, canConcurrent, scheduler);
        result = result && tempresult;

        boolean newTruth = runtimeNode1.isTruth() || runtimeNode2.isTruth();
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
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().sideEffectResolution(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], var, delChange, addChange, canConcurrent, scheduler);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().sideEffectResolution(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], var, delChange, addChange, canConcurrent, scheduler);
    }

    //DIS
    @Override
    public void DeriveRCRESets(boolean from) {
        this.subformulas[0].DeriveRCRESets(from);
        this.subformulas[1].DeriveRCRESets(from);
    }

    //PCC
    @Override
    public boolean UpdateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        boolean result0 = this.subformulas[0].UpdateAffectedWithOneChange(contextChange, checker);
        boolean result1 = this.subformulas[1].UpdateAffectedWithOneChange(contextChange, checker);
        result0 = result0 || result1;
        this.setAffected(result0);
        return result0;
    }

    //PCCM && CPCC
    @Override
    public boolean UpdateAffectedWithChanges(Checker checker) {
        boolean result0 = this.subformulas[0].UpdateAffectedWithChanges(checker);
        boolean result1 = this.subformulas[1].UpdateAffectedWithChanges(checker);
        result0 = result0 || result1;
        this.setAffected(result0);
        return result0;
    }
    @Override
    public void CleanAffected() {
        this.setAffected(false);
        this.subformulas[0].CleanAffected();
        this.subformulas[1].CleanAffected();
    }

    //CPCC_NB
    @Override
    public void UpdateCanConcurrent_CPCC_NB(boolean canConcurrent, Rule rule, Checker checker) {
        if(canConcurrent){
            this.subformulas[0].UpdateCanConcurrent_CPCC_NB(true, rule, checker);
            this.subformulas[1].UpdateCanConcurrent_CPCC_NB(true, rule, checker);
        }
    }

    @Override
    public void CleanAffectedAndCanConcurrent() {
        this.setAffected(false);
        this.subformulas[0].CleanAffectedAndCanConcurrent();
        this.subformulas[1].CleanAffectedAndCanConcurrent();
    }

    /*
                                        ECC PCC
                                     */
    @Override
    public void CreateBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FOr)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().CreateBranches_ECCPCC(rule_id, runtimeNode1, ((FOr) originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FOr) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().CreateBranches_ECCPCC(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);
    }

    /*
        ECC
     */
    @Override
    public boolean TruthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = runtimeNode1.getFormula().TruthEvaluation_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().TruthEvaluation_ECC(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);
        result = result || tempresult;
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> LinksGeneration_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        Set<Link> result;
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(runtimeNode1.isTruth()){
            if(runtimeNode2.isTruth()){
                result = new HashSet<>();
                Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
                Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
                result.addAll(ret1);
                result.addAll(ret2);
            }
            else{
                result = runtimeNode1.getFormula().LinksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
                runtimeNode2.getFormula().LinksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            }
        }
        else{
            if(runtimeNode2.isTruth()){
                runtimeNode1.getFormula().LinksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
                result = runtimeNode2.getFormula().LinksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_ECC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
                Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_ECC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
                result = lgUtils.CartesianSet(ret1, ret2);
            }
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */

    @Override
    public void ModifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().ModifyBranch_PCC(rule_id, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().ModifyBranch_PCC(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean TruthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        //curNode.update == fasle
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
            boolean result = runtimeNode1.getFormula().TruthEvaluation_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode2.getFormula().TruthEvaluation_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> LinksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(!originFormula.isAffected()){
            return curNode.getLinks();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
            Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_PCC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret1);
                    result.addAll(runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(ret1);
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(runtimeNode2.getLinks());
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret1, runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
        else{
            Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_PCC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret2);
                    result.addAll(runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(runtimeNode1.getLinks());
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(ret2);
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret2, runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
    }

    /*
        ConC
     */
    @Override
    public void CreateBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FOr)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().CreateBranches_ConC(rule_id, runtimeNode1, ((FOr) originFormula).getSubformulas()[0], canConcurrent, checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FOr) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().CreateBranches_ConC(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], canConcurrent, checker);
    }

    @Override
    public boolean TruthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = runtimeNode1.getFormula().TruthEvaluation_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().TruthEvaluation_ConC(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], canConcurrent, checker);
        result = result || tempresult;
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> LinksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        Set<Link> result;
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(runtimeNode1.isTruth()){
            if(runtimeNode2.isTruth()){
                result = new HashSet<>();
                Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, checker);
                result.addAll(ret1);
                result.addAll(ret2);
            }
            else{
                result = runtimeNode1.getFormula().LinksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, checker);
                runtimeNode2.getFormula().LinksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, checker);
            }
        }
        else{
            if(runtimeNode2.isTruth()){
                runtimeNode1.getFormula().LinksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, checker);
                result = runtimeNode2.getFormula().LinksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, checker);
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_ConC(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], canConcurrent, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_ConC(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], canConcurrent, checker);
                result = lgUtils.CartesianSet(ret1, ret2);
            }
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCCM
     */

    @Override
    public void ModifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().ModifyBranch_PCCM(rule_id, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().ModifyBranch_PCCM(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean TruthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode1.getFormula().TruthEvaluation_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0],checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode2.getFormula().TruthEvaluation_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode1.getFormula().TruthEvaluation_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            boolean tempresult = runtimeNode2.getFormula().TruthEvaluation_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = result || tempresult;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> LinksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(!originFormula.isAffected()){
            return curNode.getLinks();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
            Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret1);
                    result.addAll(runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(ret1);
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(runtimeNode2.getLinks());
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret1, runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
        else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
            Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret2);
                    result.addAll(runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(runtimeNode1.getLinks());
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(ret2);
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret2, runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
        else{
            Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_PCCM(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_PCCM(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret1);
                    result.addAll(ret2);
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(ret1);
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(ret2);
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret1, ret2);
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
    }

    /*
        CPCC_NB
     */

    @Override
    public void CreateBranches_CPCC_NB(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FOr)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode1.setParent(curNode);
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().CreateBranches_CPCC_NB(rule, runtimeNode1, ((FOr) originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FOr) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode2.setParent(curNode);
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().CreateBranches_CPCC_NB(rule, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);

    }

    @Override
    public void ModifyBranch_CPCC_NB(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().ModifyBranch_CPCC_NB(rule, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().ModifyBranch_CPCC_NB(rule, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);

    }

    @Override
    public boolean TruthEvaluationCom_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = runtimeNode1.getFormula().TruthEvaluationCom_CPCC_NB(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().TruthEvaluationCom_CPCC_NB(runtimeNode2, ((FOr) originFormula).getSubformulas()[1], checker);
        result = result || tempresult;
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return result;
    }

    @Override
    public boolean TruthEvaluationPar_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode1.getFormula().TruthEvaluationPar_CPCC_NB(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
        else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode2.getFormula().TruthEvaluationPar_CPCC_NB(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
        else{
            boolean result = runtimeNode1.getFormula().TruthEvaluationPar_CPCC_NB(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            boolean tempresult = runtimeNode2.getFormula().TruthEvaluationPar_CPCC_NB(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            result = result || tempresult;
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
    }

    @Override
    public Set<Link> LinksGeneration_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(!originFormula.isAffected()){
            return curNode.getLinks();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected() && !((FOr)originFormula).getSubformulas()[1].isAffected()){
            Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_CPCC_NB(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret1);
                    result.addAll(runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(ret1);
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(runtimeNode2.getLinks());
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret1, runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
        else if(!((FOr)originFormula).getSubformulas()[0].isAffected() && ((FOr)originFormula).getSubformulas()[1].isAffected()){
            Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_CPCC_NB(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret2);
                    result.addAll(runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(runtimeNode1.getLinks());
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(ret2);
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret2, runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
        else{
            Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_CPCC_NB(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], checker);
            Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_CPCC_NB(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret1);
                    result.addAll(ret2);
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(ret1);
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(ret2);
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret1, ret2);
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
    }

    /*
        CPCC_BASE
     */

    @Override
    public void ModifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().ModifyBranch_BASE(rule_id, runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().ModifyBranch_BASE(rule_id, runtimeNode2, ((FOr) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean TruthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        //curNode.update == fasle
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
            boolean result = runtimeNode1.getFormula().TruthEvaluation_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
            result = result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode2.getFormula().TruthEvaluation_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, checker);
            result = runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> LinksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(!originFormula.isAffected()){
            return curNode.getLinks();
        }
        else if(((FOr)originFormula).getSubformulas()[0].isAffected()){
            Set<Link> ret1 = runtimeNode1.getFormula().LinksGeneration_BASE(runtimeNode1, ((FOr)originFormula).getSubformulas()[0], contextChange, checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret1);
                    result.addAll(runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(ret1);
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(runtimeNode2.getLinks());
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret1, runtimeNode2.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
        else{
            Set<Link> ret2 = runtimeNode2.getFormula().LinksGeneration_BASE(runtimeNode2, ((FOr)originFormula).getSubformulas()[1], contextChange, checker);
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    Set<Link> result = new HashSet<>(ret2);
                    result.addAll(runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
                else{
                    curNode.setLinks(runtimeNode1.getLinks());
                    return curNode.getLinks();
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    curNode.setLinks(ret2);
                    return curNode.getLinks();
                }
                else{
                    Set<Link> result = lgUtils.CartesianSet(ret2, runtimeNode1.getLinks());
                    curNode.setLinks(result);
                    return curNode.getLinks();
                }
            }
        }
    }
}
