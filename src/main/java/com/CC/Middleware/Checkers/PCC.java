package com.CC.Middleware.Checkers;

import com.CC.Constraints.Formulas.FBfunc;
import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PCC extends Checker{

    // private OutputStream outputStream;
    // private OutputStreamWriter outputStreamWriter;
    // private BufferedWriter bufferedWriter;
    private long bfuncTime;
    private final HashMap<String, List<RuntimeNode>> toEvaluateBfuncNodes;
    private static final int BATCH_SIZE = 10;

    public PCC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions, boolean isMG) {
        super(ruleHandler, contextPool, bfunctions, isMG);
        this.technique = "PCC";
        // try {
        //     this.outputStream = Files.newOutputStream(Paths.get("PCC.txt"));
        //     this.outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        //     this.bufferedWriter = new BufferedWriter(outputStreamWriter);
        // } catch (IOException ex) {
        // }
        this.bfuncTime = 0L;
        this.toEvaluateBfuncNodes = new HashMap<>();
    }

    public void addBfuncTime(long time) {
        this.bfuncTime += time;
    }

    public void closeFiles() {
        System.out.println("bfuncTime: %d ms".formatted(this.bfuncTime));
        // try {
        //     this.bufferedWriter.close();
        //     this.outputStreamWriter.close();
        //     this.outputStream.close();
        // } catch (IOException e) {
        // }
    }

    public void addBfuncNode(String funcName, RuntimeNode bfuncNode) {
        this.toEvaluateBfuncNodes.computeIfAbsent(funcName, k -> new ArrayList<>()).add(bfuncNode);
    }

    public void evaluteBfuncNodes(){
        // 收集所有任务
        List<Runnable> allTasks = new ArrayList<>();
        
        // 使用普通forEach遍历所有函数名和对应的节点列表
        this.toEvaluateBfuncNodes.forEach((funcName, nodes) -> {
            // 计算需要多少批
            int numOfBatches = (int) Math.ceil((double) nodes.size() / BATCH_SIZE);
            
            // 为每个批次创建任务
            for (int i = 0; i < numOfBatches; i++) {
                final int start = i * BATCH_SIZE;
                final int end = Math.min((i + 1) * BATCH_SIZE, nodes.size());
                
                allTasks.add(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // 处理每批中的节点
                    for (int j = start; j < end; j++) {
                        ((FBfunc) nodes.get(j).getFormula()).bfuncCaller(nodes.get(j).getVarEnv(), this);
                    }
                });
            }
        });
        
        // 并行执行所有任务
        allTasks.parallelStream().forEach(Runnable::run);
    }

    @Override
    public void ctxChangeCheckIMD(ContextChange contextChange) {
        //consistency checking
        for(Rule rule : ruleHandler.getRuleMap().values()){
            if(rule.getVarPatternMap().containsValue(contextChange.getPattern_id())){
                //apply changes
                contextPool.applyChange(rule.getRule_id(), contextChange);
                rule.updateAffectedWithOneChange(contextChange, this);

                rule.modifyCCT_PCC(contextChange, this);
                //truth evaluation
                rule.truthEvaluation_PCC(contextChange, this);
                //taint SCCT
                Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
                if(this.isMG){
                    this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
                }
                //links generation
                Set<Link> links = rule.linksGeneration_PCC(contextChange, this, prevSubstantialNodes);
                if(links != null){
                    rule.addCriticalSet(links);
                    //rule.oracleCount(links, contextChange);
                }
                rule.cleanAffected();
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
            contextPool.getAddSet(pattern_id).clear();
            contextPool.getDelSet(pattern_id).clear();
            contextPool.getUpdSet(pattern_id).clear();
        }

        this.toEvaluateBfuncNodes.clear();
        for(ContextChange contextChange : batch){
            contextPool.applyChangeWithSets(rule.getRule_id(), contextChange);
            rule.modifyCCT_PCCM(contextChange, this);
        }
        long startTime = System.currentTimeMillis();
        this.evaluteBfuncNodes();
        long endTime = System.currentTimeMillis();
        this.addBfuncTime(endTime - startTime);
        for(String funcName : this.toEvaluateBfuncNodes.keySet()) {
            System.out.println("%s has %d toEvaluateBfuncNodes".formatted(funcName, this.toEvaluateBfuncNodes.get(funcName).size()));
        }
        System.out.println("PCC bfunc time: %d ms".formatted(endTime - startTime));
        // for (String pattern_id : rule.getVarPatternMap().values()) {
        //     try {
        //         bufferedWriter.write("%s size: %d,%d,%d,%d\n".formatted(
        //             pattern_id, 
        //             contextPool.getPoolSetSize(rule.getRule_id(), pattern_id),
        //             contextPool.getAddSetSize(pattern_id),
        //             contextPool.getUpdSetSize(pattern_id),
        //             contextPool.getDelSetSize(pattern_id)
        //         ));
        //         bufferedWriter.flush();
        //     } catch (IOException e) {
        //         // Handle exception
        //     }
        // }

        rule.updateAffectedWithChanges(this);
        rule.truthEvaluation_PCCM(this);
        //taint SCCT
        Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
        if(this.isMG){
            this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
        }
        Set<Link> links = rule.linksGeneration_PCCM(this, prevSubstantialNodes);
        if(links != null){
            rule.addCriticalSet(links);
        }
        rule.cleanAffected();
        if(links != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
        }
    }

}
