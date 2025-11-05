package cn.edu.nju.ics.spar.cc.Constraints.Formulas;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.Checker;
import cn.edu.nju.ics.spar.cc.Middleware.Schedulers.Scheduler;

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
    public abstract boolean updateAffectedWithOneChange(ContextChange contextChange, Checker checker);
    public abstract void cleanAffected();
    //PCCM && CPCC
    public abstract boolean updateAffectedWithChanges(Checker checker);
    public abstract void updateCanConcurrent_INFUSE(boolean canConcurrent, Rule rule, Checker checker);
    public abstract void cleanAffectedAndCanConcurrent();

    //RuntimeNode创建时使用
    public abstract Formula formulaClone();
    //S-condition
    public abstract void deriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet);
    public abstract void deriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet);
    //C-condition
    public abstract boolean evaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler);
    public abstract void sideeffectresolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler);
    //DIS
    public abstract void deriveRCRESets(boolean from);

    public abstract void taintSCCT(RuntimeNode curNode, Formula originFormula, Set<RuntimeNode> substantialNodes);

    //ECC && PCC
    public abstract void createBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker);
    //ECC
    public abstract boolean truthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract Set<Link> linksGeneration_ECC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker);
    //PCC
    public abstract void modifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract boolean truthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract Set<Link> linksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker);
    //ConC
    public abstract void createBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker);
    public abstract boolean truthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker);
    public abstract Set<Link> linksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, final Set<RuntimeNode> prevSubstantialNodes, Checker checker);
    //PCCM
    public abstract void modifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract boolean truthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract Set<Link> linksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker);

    //INFUSE
    public abstract void createBranches_INFUSE(Rule rule, RuntimeNode curNode , Formula originFormula, Checker checker);
    public abstract void modifyBranch_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract boolean truthEvaluationCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract boolean truthEvaluationPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker);
    public abstract Set<Link> linksGeneration_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker);

    //BASE
    public abstract void modifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract boolean truthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker);
    public abstract Set<Link> linksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker);
}
