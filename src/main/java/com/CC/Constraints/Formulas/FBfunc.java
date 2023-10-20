package com.CC.Constraints.Formulas;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Middleware.Checkers.Checker;
import com.CC.Middleware.Schedulers.Scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class FBfunc extends Formula {

    private String func = null;  // Function name
    private HashMap<String, String> params = new HashMap<>();

    //constructor
    public FBfunc(String _func) {
        this.setFormula_type(Formula_Type.BFUNC);
        this.func = _func;
        this.setAffected(false);
    }

    public void addParam(String pos, String var) {
        assert params.get(pos) == null;
        params.put(pos, var);
    }

    // getter and setter
    @Override
    public Formula_Type getFormula_type() {
        return super.getFormula_type();
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public String getFunc() {
        return func;
    }

    @Override
    public void setFormula_type(Formula_Type formula_type) {
        super.setFormula_type(formula_type);
    }

    public void setFunc(String func) {
        this.func = func;
    }

    public void setParams(HashMap<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "FBfunc{" +
                "func=" + func +
                '}';
    }

    @Override
    public void output(int offset) {
        for(int i = 0; i < offset; ++i)
            System.out.print(" ");
        System.out.println("bfunc: " + this.func + "  affected: " + this.isAffected());
        this.params.forEach((k,v) -> {
            for(int i = 0; i < offset+2; ++i)
                System.out.print(" ");
            System.out.println("pos: " + k + " var: " + v);
        });
    }

    @Override
    public Formula formulaClone() {
        FBfunc fBfunc = new FBfunc(this.func);
        HashMap<String, String> tmpParams = new HashMap<>(this.params);
        fBfunc.setParams(tmpParams);
        return fBfunc;
    }

    //S-condition
    @Override
    public void deriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        //Do nothing
    }

    @Override
    public void deriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        //Do nothing
    }

    //C-condition
    @Override
    public boolean evaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var == null){
            return true;
        }
        curNode.getVarEnv().remove(var);
        curNode.getVarEnv().put(var, addChange.getContext());

        boolean newTruth = false;
        newTruth = bfuncCaller(curNode.getVarEnv(), scheduler.getChecker());
        curNode.setOptTruth(curNode.isTruth());
        curNode.setTruth(newTruth);
        return true;
    }

    @Override
    public void sideeffectresolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.setTruth(curNode.isOptTruth());
            curNode.setOptTruth(false);
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, delChange.getContext());
        }
    }

    //DIS
    @Override
    public void deriveRCRESets(boolean from) {

    }

    //PCC
    @Override
    public boolean updateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        return false;
    }

    //PCCM && CPCC
    @Override
    public boolean updateAffectedWithChanges(Checker checker) {
        return false;
    }
    @Override
    public void cleanAffected() {
        this.setAffected(false);
    }

    //CPCC_NB
    @Override
    public void updateCanConcurrent_INFUSE(boolean canConcurrent, Rule rule, Checker checker) {

    }

    @Override
    public void cleanAffectedAndCanConcurrent() {
        this.setAffected(false);
    }

    //MG
    @Override
    public void taintSCCT(RuntimeNode curNode, Formula originFormula, Set<RuntimeNode> substantialNodes) {
        substantialNodes.add(curNode);
    }

    /*
                                            ECC PCC
                                         */
    @Override
    public void createBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //do nothing
    }

    /*
        ECC
     */
    @Override
    public boolean truthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        boolean result = false;
        result = bfuncCaller(curNode.getVarEnv(), checker);
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ECC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>(1);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */

    @Override
    public void modifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //do nothing
    }

    @Override
    public boolean truthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(originFormula.isAffected()){
            this.truthEvaluation_ECC(curNode, originFormula, checker);
            return curNode.isTruth();
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> linksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>(1);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        ConC
     */
    @Override
    public void createBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        //do nothing
    }

    @Override
    public boolean truthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        boolean result = false;
        result = bfuncCaller(curNode.getVarEnv(), checker);
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>(1);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCCM
     */

    @Override
    public void modifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {

    }

    @Override
    public boolean truthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        if(originFormula.isAffected()){
            this.truthEvaluation_ECC(curNode, originFormula, checker);
            return curNode.isTruth();
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> linksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>(1);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_NB
     */

    @Override
    public void createBranches_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {

    }

    @Override
    public void modifyBranch_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {

    }

    @Override
    public boolean truthEvaluationCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        boolean result = false;
        result = bfuncCaller(curNode.getVarEnv(), checker);
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return result;
    }

    @Override
    public boolean truthEvaluationPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        if(originFormula.isAffected()){
            this.truthEvaluationCom_INFUSE(curNode, originFormula, checker);
            curNode.setVirtualTruth(curNode.isTruth() ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return curNode.isTruth();
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> linksGeneration_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>(1);
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_BASE
     */

    @Override
    public void modifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //do nothing
    }

    @Override
    public boolean truthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        if(originFormula.isAffected()){
            this.truthEvaluation_ECC(curNode, originFormula, checker);
            return curNode.isTruth();
        }
        else{
            return curNode.isTruth();
        }
    }

    @Override
    public Set<Link> linksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>(1);
        curNode.setLinks(result);
        return curNode.getLinks();
    }


    public boolean bfuncCaller(HashMap<String, Context> varEnv, Checker checker){
        Map<String, Map<String, String>> vcMap = new HashMap<>();
        for(String pos : params.keySet()){
            HashMap<String, String> ctxInfos = new HashMap<>();
            Context context = varEnv.get(params.get(pos));
            ctxInfos.put("ctx_id", context.getCtx_id());
            for(String attriName : context.getCtx_fields().keySet()){
                ctxInfos.put(attriName, context.getCtx_fields().get(attriName));
            }
            vcMap.put(params.get(pos), ctxInfos);
        }

        boolean result = false;
        try {
            Object bfuncInstance = checker.getBfuncInstance();
            Method m = bfuncInstance.getClass().getMethod("bfunc", String.class, Class.forName("java.util.Map"));
            result = (boolean) m.invoke(bfuncInstance,func, vcMap);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}

