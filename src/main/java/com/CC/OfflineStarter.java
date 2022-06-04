package com.CC;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextHandler;
import com.CC.Contexts.ContextHandlerFactory;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.*;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.PatternHandlerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OfflineStarter {
    private RuleHandler ruleHandler;
    private PatternHandler patternHandler;
    private ContextHandler contextHandler;
    private ContextPool contextPool;

    private String dataFile;
    private String ruleFile;
    private String patternFile;
    private String bfuncFile;

    private Scheduler scheduler;
    private Checker checker;

    public OfflineStarter() {}

    public void start(String approach, String ruleFile, String patternFile, String dataFile, String bfuncFile, String type){
        this.dataFile = dataFile;
        this.ruleFile = ruleFile;
        this.patternFile = patternFile;
        this.bfuncFile = bfuncFile;

        this.ruleHandler = new RuleHandler();
        this.patternHandler = new PatternHandlerFactory().getPatternHandler(type);
        this.contextHandler = new ContextHandlerFactory().getContextHandler(type, patternHandler);
        this.contextPool = new ContextPool();

        try {
            buildRulesAndPatterns();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Object bfunctions = null;
        try {
            bfunctions = loadBfuncFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String technique = null;
        String schedule = null;
        if(approach.contains("+")){
            technique = approach.substring(0, approach.indexOf("+"));
            schedule = approach.substring(approach.indexOf("+") + 1);
        }
        else{
            if(approach.equalsIgnoreCase("INFUSE_base")){
                technique = "INFUSE_base";
                schedule = "IMD";
            }
            else if(approach.equalsIgnoreCase("INFUSE")){
                technique = "INFUSE_C";
                schedule = "INFUSE_S";
            }
        }

        assert technique != null;

        switch (technique) {
            case "ECC":
                this.checker = new ECC(this.ruleHandler, this.contextPool, bfunctions);
                break;
            case "ConC":
                this.checker = new ConC(this.ruleHandler, this.contextPool, bfunctions);
                break;
            case "PCC":
                this.checker = new PCC(this.ruleHandler, this.contextPool, bfunctions);
                break;
            case "INFUSE_base":
                this.checker = new BASE(this.ruleHandler, this.contextPool, bfunctions);
                break;
            case "INFUSE_C":
                this.checker = new INFUSE_C(this.ruleHandler, this.contextPool, bfunctions);
                break;
        }

        switch (schedule){
            case "IMD":
                this.scheduler = new IMD(ruleHandler, contextPool, checker);
                break;
            case "GEAS_ori":
                this.scheduler = new GEAS_ori(ruleHandler, contextPool, checker);
                break;
            case "GEAS_opt_s":
                this.scheduler = new GEAS_opt_s(ruleHandler, contextPool, checker);
                break;
            case "GEAS_opt_c":
                this.scheduler = new GEAS_opt_c(ruleHandler, contextPool, checker);
                break;
            case "INFUSE_S":
                this.scheduler = new INFUSE_S(ruleHandler, contextPool, checker);
                break;
        }


        //run
        try {
            runWithOriginData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void buildRulesAndPatterns() throws Exception {
        this.ruleHandler.buildRules(ruleFile);
        this.patternHandler.buildPatterns(patternFile);

        for(Rule rule : ruleHandler.getRuleList()){
            contextPool.PoolInit(rule);
            //S-condition
            rule.DeriveSConditions();
            //DIS
            rule.DeriveRCRESets();
        }

        for(String pattern_id : patternHandler.getPatternMap().keySet()){
            contextPool.ThreeSetsInit(pattern_id);
        }
    }

    private Object loadBfuncFile() throws Exception {
        Path bfuncPath = Paths.get(bfuncFile);
        URLClassLoader classLoader = new URLClassLoader(new URL[]{ bfuncPath.getParent().toFile().toURI().toURL()});
        Class<?> c = classLoader.loadClass(bfuncPath.getFileName().toString().substring(0, bfuncPath.getFileName().toString().length() - 6));
        Constructor<?> constructor = c.getConstructor();
        return constructor.newInstance();
    }


    private void runWithOriginData() throws Exception{
        String line;
        List<ContextChange> changeList = new ArrayList<>();
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        while((line = bufferedReader.readLine()) != null){
            //System.out.println(line);
            this.contextHandler.generateChanges(line, changeList);
            while(!changeList.isEmpty()){
                ContextChange chg = changeList.get(0);
                changeList.remove(0);
                this.scheduler.doSchedule(chg);
            }
        }
        bufferedReader.close();
        inputStreamReader.close();

        this.contextHandler.generateChanges(null, changeList);
        while(!changeList.isEmpty()){
            ContextChange chg = changeList.get(0);
            changeList.remove(0);
            this.scheduler.doSchedule(chg);
        }
        this.scheduler.checkEnds();

        IncOutput();
    }

    private void IncOutput() throws Exception {
        String ansFile = "src/main/resources/example/results.txt";
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(ansFile), StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        for(Map.Entry<String, Map.Entry<String, Map<String,String>>> entry : this.checker.getAnswers()){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(entry.getKey()).append('(');
            stringBuilder.append(entry.getValue().getKey().toLowerCase()).append(",{");
            entry.getValue().getValue().forEach((var, ctx_id) -> {
                stringBuilder.append("(").append(var).append(", ").append(Integer.parseInt(ctx_id.substring(4)) + 1).append("),");
            });
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("})");
            bufferedWriter.write(stringBuilder.toString() + "\n");
            bufferedWriter.flush();
        }
    }
}
