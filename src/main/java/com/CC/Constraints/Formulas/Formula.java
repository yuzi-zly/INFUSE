package com.CC.Constraints.Formulas;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Schedulers.Scheduler;

import java.util.Map;
import java.util.Set;

public abstract class Formula {
    public enum Formula_Type {FORALL, EXISTS, AND, OR, IMPLIES, NOT, BFUNC};

    private Formula_Type formula_type;
    private boolean Affected;

    public Formula_Type getFormula_type() {
        return formula_type;
    }
    public boolean isAffected() {
        return Affected;
    }

    public void setAffected(boolean affected) {
        Affected = affected;
    }
    public void setFormula_type(Formula_Type formula_type) {
        this.formula_type = formula_type;
    }

    //为syntax tree输出
    public abstract void output(int offset);
    //PCC
    public abstract boolean UpdateAffectedWithOneChange(ContextChange contextChange, Checker checker);
    public abstract void CleanAffected();
    //PCCM && CPCC
    public abstract boolean UpdateAffectedWithChanges(Checker checker);
    public abstract void UpdateCanConcurrent_CPCC_NB(boolean canConcurrent, Rule rule, Checker checker);
    public abstract void CleanAffectedAndCanConcurrent();

    //RuntimeNode创建时使用
    public abstract Formula FormulaClone();
    //S-condition
    public abstract void DeriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet);
    public abstract void DeriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet);
    //C-condition
    public abstract boolean EvaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler);
    public abstract void sideEffectResolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler);
    //DIS
    public abstract void DeriveRCRESets(boolean from);

    //ECC && PCC
    public abstract void CreateBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker);
    //ECC
    public abstract boolean TruthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract Set<Link> LinksGeneration_ECC(RuntimeNode curNode, Formula originFormula, Checker checker);
    //PCC
    public abstract void ModifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract boolean TruthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract Set<Link> LinksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    //ConC
    public abstract void CreateBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker);
    public abstract boolean TruthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker);
    public abstract Set<Link> LinksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker);
    //PCCM
    public abstract void ModifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract boolean TruthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract Set<Link> LinksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker);

    //CPCC_NB
    public abstract void CreateBranches_CPCC_NB(Rule rule, RuntimeNode curNode ,Formula originFormula, Checker checker);
    public abstract void ModifyBranch_CPCC_NB(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract boolean TruthEvaluationCom_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract boolean TruthEvaluationPar_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract Set<Link> LinksGeneration_CPCC_NB(RuntimeNode curNode, Formula originFormula, Checker checker);

    //BASE
    public abstract void ModifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract boolean TruthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract Set<Link> LinksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
}
