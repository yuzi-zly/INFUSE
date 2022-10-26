package com.CC.Constraints;

import com.CC.Constraints.Formulas.Formula;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Middleware.Checkers.Checker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private final Set<String> relatedPatterns;

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

    //counting
    public List<ContextChange> oracleBatch;
    public Set<Link> bufferedLinks;
    public Set<Link> detectedLinks;
    public BufferedWriter bufferedWriter;
    public FileWriter fileWriter;

    public long launchTimeTE, launchTimeLG, traverseTimeTE, traverseTimeLG;

    //constructor
    public Rule(String rule_id){
        this.rule_id = rule_id;
        this.CCTRoot = null;
        this.CCTAlready = false;
        this.relatedPatterns = new HashSet<>();
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

        //统计Batch
        this.oracleBatch = new ArrayList<>();
        this.bufferedLinks = new HashSet<>();
        this.detectedLinks = new HashSet<>();

        this.launchTimeTE = 0;
        this.launchTimeLG = 0;
        this.traverseTimeTE = 0;
        this.traverseTimeLG = 0;
    }

    public void batchInit(String strategy, String dataFile){
        //assets/Data2011/MyData/originalData/data_5/7.txt;
        StringTokenizer st = new StringTokenizer(dataFile,"/");
        st.nextToken(); //assets
        st.nextToken(); //Data2011
        st.nextToken(); //MyData
        st.nextToken(); //originalData
        String dataSeg = st.nextToken();//data_5
        String file = st.nextToken();//7.txt

        try {
            String dir = "assets/Data2011-4-8/batchCount/" + strategy + "/" + dataSeg + "/" + file.substring(0, file.length() - 4);
            File dirFile = new File(dir);
            if(!dirFile.exists() && !dirFile.isDirectory()){
                boolean mkdirflag = dirFile.mkdirs();
                System.err.println("mkdirs: " + mkdirflag);
            }
            fileWriter = new FileWriter(dir + "/" + this.rule_id + ".txt");
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //functional methods
    public void Output(){
        System.out.println("rule id: " + this.rule_id);
        System.out.println("relatedPattern: " + this.relatedPatterns);
        System.out.println("incPlusSet: " + incPlusSet);
        System.out.println("incMinusSet: " + incMinusSet);
        this.formula.output(0);
    }

    //S-condition
    public void DeriveSConditions(){
        this.formula.DeriveIncPlusSet(this.incPlusSet);
        this.formula.DeriveIncMinusSet(this.incMinusSet);
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
    public void DeriveRCRESets(){
        this.formula.DeriveRCRESets(true);
    }


    //counting
    public void oracleCount(Set<Link> newLinks, ContextChange contextChange){
        boolean noEliminate = true;
        for(Link bufferedLink : bufferedLinks){
            if(!newLinks.contains(bufferedLink)){
                //缓存的link不再被检测出来，说明有link消失了，在此处需检测。
                //1. 将缓存的link都标记为被检测
                detectedLinks.addAll(bufferedLinks);
                bufferedLinks.clear();
                //2. 将当前batch保存下来
                try {
                    bufferedWriter.write(oracleBatch.size() + ": " + oracleBatch.toString() + "\n\n");
                    bufferedWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                oracleBatch.clear();
                oracleBatch.add(contextChange);
                //3. 更新缓存的link
                for(Link newLink : newLinks){
                    if(!detectedLinks.contains(newLink)){
                        bufferedLinks.add(newLink);
                    }
                }
                //4. 设置noEliminate
                noEliminate = false;
                break;
            }
        }
        if(noEliminate){
            //没有缓存的link消失，则更新缓存的link
            for(Link newLink : newLinks){
                if(!detectedLinks.contains(newLink)){
                    bufferedLinks.addAll(newLinks);
                }
            }
            oracleBatch.add(contextChange);
        }
    }

    public void oracleClean(){
        try {
            bufferedWriter.write(oracleBatch.size() + ": " + oracleBatch.toString() + "\n\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void intoFile(List<ContextChange> changeList){
        try {
            bufferedWriter.write(changeList.size() + ": " + changeList + "\n\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //PCC
    public void UpdateAffectedWithOneChange(ContextChange contextChange, Checker checker){
        this.formula.UpdateAffectedWithOneChange(contextChange, checker);
    }
    public void CleanAffected(){
        this.formula.CleanAffected();
    }

    //PCCM && CPCC
    public void UpdateAffectedWithChanges(Checker checker){
        this.formula.UpdateAffectedWithChanges(checker);
    }

    //CPCC
    public void UpdateCanConcurrent_CPCC_NB(Checker checker){
        this.formula.UpdateCanConcurrent_CPCC_NB(true, this, checker);
    }

    public void CleanAffectedAndCanConcurrent(){
        this.formula.CleanAffectedAndCanConcurrent();
    }

    //ECC && PCC
    public void BuildCCT_ECCPCC(Checker checker){
        this.CCTRoot = new RuntimeNode(this.formula);
        this.CCTRoot.setDepth(0);
        this.CCTRoot.getFormula().CreateBranches_ECCPCC(this.rule_id, this.CCTRoot, this.formula, checker);
        this.CCTAlready = true;
    }

    public void ModifyCCT_PCC(ContextChange contextChange, Checker checker){
        this.CCTRoot.getFormula().ModifyBranch_PCC(this.rule_id, this.CCTRoot, this.formula, contextChange, checker);
    }

    public void PrintCCT(){
        this.CCTRoot.PrintRuntimeNode(0);
    }

    //ConC
    public void BuildCCT_CONC(Checker checker){
        this.CCTRoot = new RuntimeNode(this.formula);
        this.CCTRoot.setDepth(0);
        this.CCTRoot.getFormula().CreateBranches_ConC(this.rule_id, this.CCTRoot, this.formula, true, checker);
        this.CCTAlready = true;
    }

    //PCCM
    public void ModifyCCT_PCCM(ContextChange contextChange, Checker checker){
        this.CCTRoot.getFormula().ModifyBranch_PCCM(this.rule_id, this.CCTRoot, this.formula, contextChange, checker);
    }

    //CPCC_NB
    public void BuildCCT_CPCC_NB(Checker checker){
        this.CCTRoot = new RuntimeNode(this.formula);
        this.CCTRoot.setDepth(0);
        this.CCTRoot.getFormula().CreateBranches_CPCC_NB(this, this.CCTRoot, this.formula, checker);
        this.CCTAlready = true;
    }

    public void ModifyCCT_CPCC_NB(Checker checker){
        this.CCTRoot.getFormula().ModifyBranch_CPCC_NB(this, this.CCTRoot, this.formula, checker);
    }

    //CPCC_BASE
    public void ModifyCCT_BASE(ContextChange contextChange, Checker checker){
        this.CCTRoot.getFormula().ModifyBranch_BASE(this.rule_id, this.CCTRoot, this.formula, contextChange, checker);
    }

    //ECC check
    public boolean TruthEvaluation_ECC(Checker checker) {
        boolean result = this.CCTRoot.getFormula().TruthEvaluation_ECC(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> LinksGeneration_ECC(Checker checker){
        Set<Link> result = this.CCTRoot.getFormula().LinksGeneration_ECC(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //PCC check
    public boolean TruthEvaluation_PCC(ContextChange contextChange, Checker checker){
        boolean result = this.CCTRoot.getFormula().TruthEvaluation_PCC(this.CCTRoot, this.formula, contextChange, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> LinksGeneration_PCC(ContextChange contextChange, Checker checker){
        Set<Link> result = this.CCTRoot.getFormula().LinksGeneration_PCC(this.CCTRoot, this.formula, contextChange, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //ConC check
    public boolean TruthEvaluation_ConC(Checker checker){
        boolean result = this.CCTRoot.getFormula().TruthEvaluation_ConC(this.CCTRoot, this.formula, true, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> LinksGeneration_ConC(Checker checker){
        Set<Link> result = this.CCTRoot.getFormula().LinksGeneration_ConC(this.CCTRoot, this.formula, true, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //PCCM check
    public boolean TruthEvaluation_PCCM(Checker checker){
        boolean result = this.CCTRoot.getFormula().TruthEvaluation_PCCM(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> LinksGeneration_PCCM(Checker checker){
        Set<Link> result = this.CCTRoot.getFormula().LinksGeneration_PCCM(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }

    //CPCC_NB check
    public boolean TruthEvaluation_CPCC_NB(Checker checker, boolean initial){
        boolean result = false;
        if(initial){
            result = this.CCTRoot.getFormula().TruthEvaluationCom_CPCC_NB(this.CCTRoot, this.formula, checker);
        }
        else{
            result = this.CCTRoot.getFormula().TruthEvaluationPar_CPCC_NB(this.CCTRoot, this.formula, checker);
        }
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> LinksGeneration_CPCC_NB(Checker checker){
        Set<Link> result = this.CCTRoot.getFormula().LinksGeneration_CPCC_NB(this.CCTRoot, this.formula, checker);
        this.CCTRoot.setLinks(result);
        return result;
    }


    //CPCC_BASE check
    public boolean TruthEvaluation_BASE(ContextChange contextChange, Checker checker){
        boolean result = this.CCTRoot.getFormula().TruthEvaluation_BASE(this.CCTRoot, this.formula, contextChange, checker);
        this.CCTRoot.setTruth(result);
        return result;
    }

    public Set<Link> LinksGeneration_BASE(ContextChange contextChange, Checker checker){
        Set<Link> result = this.CCTRoot.getFormula().LinksGeneration_BASE(this.CCTRoot, this.formula, contextChange, checker);
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

    public Set<String> getRelatedPatterns() {
        return relatedPatterns;
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
