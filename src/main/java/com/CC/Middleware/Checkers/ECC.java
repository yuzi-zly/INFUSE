package com.CC.Middleware.Checkers;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextPool;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import com.CC.Constraints.Formulas.FBfunc;

public class ECC extends Checker{

    private long bfuncTime;
    private final HashMap<String, List<RuntimeNode>> toEvaluateBfuncNodes;
    private static final int BATCH_SIZE = 10;

    public ECC(RuleHandler ruleHandler, ContextPool contextPool, Object bfunctions, boolean isMG) {
        super(ruleHandler, contextPool, bfunctions, isMG);
        this.technique = "ECC";
        this.bfuncTime = 0L;
        this.toEvaluateBfuncNodes = new HashMap<>();
    }

    public void closeFiles() {
        System.out.println("bfuncTime: %d ms".formatted(this.bfuncTime));
        // No file operations in ECC, so nothing to close
    }

    public void addBfuncTime(long time) {
        this.bfuncTime += time;
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
        for(Rule rule : this.ruleHandler.getRuleMap().values()){
            if (rule.getVarPatternMap().containsValue(contextChange.getPattern_id())){
                //apply change
                contextPool.applyChange(rule.getRule_id(), contextChange);
                //build CCT
                rule.buildCCT_ECCPCC(this);
                //truth value evaluation
                rule.truthEvaluation_ECC(this);
                //taint SCCT
                Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
                if(this.isMG){
                    this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
                }
                //links generation
                Set<Link> links = rule.linksGeneration_ECC(this, prevSubstantialNodes);
                if(links != null){
                    storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
                }
            }
        }
    }

    @Override
    public void ctxChangeCheckBatch(Rule rule, List<ContextChange> batch){
        //apply change
        for(ContextChange contextChange : batch){
            contextPool.applyChange(rule.getRule_id(), contextChange);
        }
        
        this.toEvaluateBfuncNodes.clear();
        //build CCT
        rule.buildCCT_ECCPCC(this);
        long startTime = System.currentTimeMillis();
        this.evaluteBfuncNodes();
        long endTime = System.currentTimeMillis();
        this.addBfuncTime(endTime - startTime);
        for (String funcName : this.toEvaluateBfuncNodes.keySet()) {
            System.out.println("%s has %d toEvaluateBfuncNodes".formatted(funcName, this.toEvaluateBfuncNodes.get(funcName).size()));
        }
        System.out.println("ECC bfunc time: %d ms".formatted(endTime - startTime));

        //truth value evaluation
        rule.truthEvaluation_ECC(this);
        //taint SCCT
        Set<RuntimeNode> prevSubstantialNodes = this.substantialNodes.getOrDefault(rule.getRule_id(),  new HashSet<>());
        if(this.isMG){
            this.substantialNodes.put(rule.getRule_id(), rule.taintSCCT());
        }
        //links generation
        Set<Link> links = rule.linksGeneration_ECC(this, prevSubstantialNodes);
        if(links != null){
            rule.addCriticalSet(links);
        }
        if(links != null){
            storeLink(rule.getRule_id(), rule.getCCTRoot().isTruth(), links);
        }
    }
}
