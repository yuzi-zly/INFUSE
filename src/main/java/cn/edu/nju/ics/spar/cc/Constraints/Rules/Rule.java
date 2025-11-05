package cn.edu.nju.ics.spar.cc.Constraints.Rules;

import cn.edu.nju.ics.spar.cc.Constraints.Formulas.Formula;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Contexts.Context;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.Checker;

import java.util.*;

public class Rule {
    private String rule_id;
    //syntax construction
    private Formula formula;
    //Runtime node
    private RuntimeNode CCTRoot;
    //build or modify CCT
    private boolean CCTAlready;
    // Related patterns
    private final Map<String,String> varPatternMap;

    //for CPCC_NB
    //pat to maxUnderDepth
    private final Map<String, Integer> patToDepth;
    private final TreeMap<Integer, String> depthToPat;

    //for DIS
    private boolean riskAlready;
    private final Map<String, Formula> patToFormula;
    private final Map<String, Set<RuntimeNode>> patToRuntimeNode;

    //GEAS S-condition
    //Set of ContextChange ignoring the context: <change_type, pattern_id>
    private final Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet;
    private final Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet;
    //bath and Newbatch
    private List<ContextChange> batch;
    private List<ContextChange> newBatch;

    //GEAS C-condition
    private final Set<String> criticalSet;


    //constructor
    public Rule(String rule_id){
        this.rule_id = rule_id;
        this.CCTRoot = null;
        this.CCTAlready = false;
        this.varPatternMap = new HashMap<>();
        //GEAS
        this.incMinusSet = new HashSet<>();
        this.incPlusSet = new HashSet<>();
        this.criticalSet = new HashSet<>();
        this.batch = null;
        this.newBatch = null;
        //DIS
        this.riskAlready = false;
        this.patToFormula = new HashMap<>();
        this.patToRuntimeNode = new HashMap<>();
        this.patToDepth = new HashMap<>();
        this.depthToPat = new TreeMap<>();
    }

    //functional methods
    public void output(){
        System.out.println("rule id: " + this.rule_id);
        System.out.println("varPatternMap: " + this.varPatternMap);
        System.out.println("incPlusSet: " + incPlusSet);
        System.out.println("incMinusSet: " + incMinusSet);
        this.formula.output(0);
    }

    //S-condition
    public void deriveSConditions(){
        this.formula.deriveIncPlusSet(this.incPlusSet);
        this.formula.deriveIncMinusSet(this.incMinusSet);
    }

    public String getIncType(ContextChange contextChange) {
        Map.Entry<ContextChange.Change_Type, String> entry= new AbstractMap.SimpleEntry<>(contextChange.getChange_type(), contextChange.getPattern_id());
        if(this.incPlusSet.contains(entry)){
            return "Plus";
        }
        else if(this.incMinusSet.contains(entry)){
            return "Minus";
        }
        else
            return "NotThisRule";
    }

    //C-condition
    public void addCriticalSet(Set<Link> links){
        this.criticalSet.clear();
        for(Link link : links){
            for(Map.Entry<String, Context> entry : link.getVaSet()){
                this.criticalSet.add(entry.getValue().getCtx_id());
            }
        }
    }

    public boolean inCriticalSet(String ctx_id){
        return this.criticalSet.contains(ctx_id);
    }

    //DIS
    public void deriveRCRESets(){
        this.formula.deriveRCRESets(true);
    }

    //PCC
    public void updateAffectedWithOneChange(ContextChange contextChange, Checker checker){
        this.formula.updateAffectedWithOneChange(contextChange, checker);
    }
    public void cleanAffected(){
        this.formula.cleanAffected();
    }

    //PCCM && CPCC
    public void updateAffectedWithChanges(Checker checker){
        this.formula.updateAffectedWithChanges(checker);
    }

    //CPCC
    public void updateCanConcurrent_INFUSE(Checker checker){
        this.formula.updateCanConcurrent_INFUSE(true, this, checker);
    }

    public void cleanAffectedAndCanConcurrent(){
        this.formula.cleanAffectedAndCanConcurrent();
    }

    //MG
    public Set<RuntimeNode> taintSCCT(){
        Set<RuntimeNode> curSubstantialNodes = new HashSet<>();
        this.CCTRoot.getFormula().taintSCCT(this.CCTRoot, this.formula, curSubstantialNodes);
        return curSubstantialNodes;
    }

    //ECC && PCC
    public void buildCCT_ECCPCC(Checker checker){
        this.CCTRoot = new RuntimeNode(this.formula);
        this.CCTRoot.setDepth(0);
        this.CCTRoot.getFormula().createBranches_ECCPCC(this.rule_id, this.CCTRoot, this.formula, checker);
        this.CCTAlready = true;
    }

    public void modifyCCT_PCC(ContextChange contextChange, Checker checker){
        this.CCTRoot.getFormula().modifyBranch_PCC(this.rule_id, this.CCTRoot, this.formula, contextChange, checker);
    }


    //ConC
    public void buildCCT_CONC(Checker checker){
        this.CCTRoot = new RuntimeNode(this.formula);
        this.CCTRoot.setDepth(0);
        this.CCTRoot.getFormula().createBranches_ConC(this.rule_id, this.CCTRoot, this.formula, true, checker);
        this.CCTAlready = true;
    }

    //PCCM
    public void modifyCCT_PCCM(ContextChange contextChange, Checker checker){
        this.CCTRoot.getFormula().modifyBranch_PCCM(this.rule_id, this.CCTRoot, this.formula, contextChange, checker);
    }

    //CPCC_NB
    public void buildCCT_INFUSE(Checker checker){
        this.CCTRoot = new RuntimeNode(this.formula);
        this.CCTRoot.setDepth(0);
        this.CCTRoot.getFormula().createBranches_INFUSE(this, this.CCTRoot, this.formula, checker);
        this.CCTAlready = true;
    }

    public void modifyCCT_INFUSE(Checker checker){
        this.CCTRoot.getFormula().modifyBranch_INFUSE(this, this.CCTRoot, this.formula, checker);
    }

    //CPCC_BASE
    public void modifyCCT_BASE(ContextChange contextChange, Checker checker){
        this.CCTRoot.getFormula().modifyBranch_BASE(this.rule_id, this.CCTRoot, this.formula, contextChange, checker);
    }

    //ECC check
    public boolean truthEvaluation_ECC(Checker checker) {
        boolean result = this.CCTRoot.getFormula().truthEvaluation_ECC(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> linksGeneration_ECC(Checker checker, final Set<RuntimeNode> prevSubstantialNodes){
        Set<Link> result = this.CCTRoot.getFormula().linksGeneration_ECC(this.CCTRoot, this.formula, prevSubstantialNodes, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //PCC check
    public boolean truthEvaluation_PCC(ContextChange contextChange, Checker checker){
        boolean result = this.CCTRoot.getFormula().truthEvaluation_PCC(this.CCTRoot, this.formula, contextChange, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> linksGeneration_PCC(ContextChange contextChange, Checker checker, final Set<RuntimeNode> prevSubstantialNodes){
        Set<Link> result = this.CCTRoot.getFormula().linksGeneration_PCC(this.CCTRoot, this.formula, contextChange, prevSubstantialNodes, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //ConC check
    public boolean truthEvaluation_ConC(Checker checker){
        boolean result = this.CCTRoot.getFormula().truthEvaluation_ConC(this.CCTRoot, this.formula, true, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> linksGeneration_ConC(Checker checker, final Set<RuntimeNode> prevSubstantialNodes){
        Set<Link> result = this.CCTRoot.getFormula().linksGeneration_ConC(this.CCTRoot, this.formula, true, prevSubstantialNodes, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //PCCM check
    public boolean truthEvaluation_PCCM(Checker checker){
        boolean result = this.CCTRoot.getFormula().truthEvaluation_PCCM(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> linksGeneration_PCCM(Checker checker, final Set<RuntimeNode> prevSubstantialNodes){
        Set<Link> result = this.CCTRoot.getFormula().linksGeneration_PCCM(this.CCTRoot, this.formula, prevSubstantialNodes, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //CPCC_NB check
    public boolean truthEvaluation_INFUSE(Checker checker, boolean initial){
        boolean result = false;
        if(initial){
            result = this.CCTRoot.getFormula().truthEvaluationCom_INFUSE(this.CCTRoot, this.formula, checker);
        }
        else{
            result = this.CCTRoot.getFormula().truthEvaluationPar_INFUSE(this.CCTRoot, this.formula, checker);
        }
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> linksGeneration_INFUSE(Checker checker, final Set<RuntimeNode> prevSubstantialNodes){
        Set<Link> result = this.CCTRoot.getFormula().linksGeneration_INFUSE(this.CCTRoot, this.formula, prevSubstantialNodes, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }


    //CPCC_BASE check
    public boolean truthEvaluation_BASE(ContextChange contextChange, Checker checker){
        boolean result = this.CCTRoot.getFormula().truthEvaluation_BASE(this.CCTRoot, this.formula, contextChange, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> linksGeneration_BASE(ContextChange contextChange, Checker checker, final Set<RuntimeNode> prevSubstantialNodes){
        Set<Link> result = this.CCTRoot.getFormula().linksGeneration_BASE(this.CCTRoot, this.formula, contextChange, prevSubstantialNodes, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //getter
    public boolean isRiskAlready() {
        return riskAlready;
    }

    public Map<String, Set<RuntimeNode>> getPatToRuntimeNode() {
        return patToRuntimeNode;
    }

    public Map<String, Formula> getPatToFormula() {
        return patToFormula;
    }

    public Formula getFormula() {
        return formula;
    }

    public String getRule_id() {
        return rule_id;
    }

    public Map<String, String> getVarPatternMap() {
        return varPatternMap;
    }

    public List<ContextChange> getBatch() {
        return batch;
    }

    public List<ContextChange> getNewBatch() {
        return newBatch;
    }

    public Set<Map.Entry<ContextChange.Change_Type, String>> getIncMinusSet() {
        return incMinusSet;
    }

    public Set<Map.Entry<ContextChange.Change_Type, String>> getIncPlusSet() {
        return incPlusSet;
    }

    public Set<String> getCriticalSet() {
        return criticalSet;
    }

    public boolean isCCTAlready() {
        return CCTAlready;
    }

    public RuntimeNode getRuntimeNode() {
        return CCTRoot;
    }

    public RuntimeNode getCCTRoot() {
        return CCTRoot;
    }

    public Map<String, Integer> getPatToDepth() {
        return patToDepth;
    }

    public TreeMap<Integer, String> getDepthToPat() {
        return depthToPat;
    }

    //setter
    public void setRiskAlready(boolean riskAlready) {
        this.riskAlready = riskAlready;
    }

    public void setFormula(Formula formula) {
        this.formula = formula;
    }

    public void setRule_id(String rule_id) {
        this.rule_id = rule_id;
    }

    public void setBatch(List<ContextChange> batch) {
        this.batch = batch;
    }

    public void setNewBatch(List<ContextChange> newBatch) {
        this.newBatch = newBatch;
    }

    public void setCCTAlready(boolean CCTAlready) {
        this.CCTAlready = CCTAlready;
    }

    public void setRuntimeNode(RuntimeNode runtimeNode) {
        this.CCTRoot = runtimeNode;
    }

    public void setCCTRoot(RuntimeNode CCTRoot) {
        this.CCTRoot = CCTRoot;
    }
}
