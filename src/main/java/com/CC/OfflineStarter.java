package com.CC;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.Context;
import com.CC.Contexts.ContextChange;
import com.CC.Contexts.ContextHandler;
import com.CC.Contexts.ContextPool;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.*;
import com.CC.Patterns.PatternHandler;
import com.CC.Util.Loggable;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OfflineStarter implements Loggable {

    private Scheduler scheduler;
    private Checker checker;
    private String ruleFile;
    private String bfuncFile;
    private String patternFile;
    private String mfuncFile;

    private String dataFile;
    private String incOutFile;
    private String dataOutFile;

    private String type;


    private RuleHandler ruleHandler;
    private PatternHandler patternHandler;
    private ContextHandler contextHandler;
    private ContextPool contextPool;


    public OfflineStarter() {}

    public void start(String approach, String ruleFile, String bfuncFile, String patternFile, String mfuncFile, String dataFile, String incOutFile, String dataOutFile, String type){
        this.ruleFile = ruleFile;
        this.bfuncFile = bfuncFile;
        this.patternFile = patternFile;
        this.mfuncFile = mfuncFile;
        this.dataFile = dataFile;
        this.incOutFile = incOutFile;
        this.dataOutFile =dataOutFile;
        this.type = type;

        this.ruleHandler = new RuleHandler();
        this.patternHandler = new PatternHandler();
        this.contextHandler = new ContextHandler(patternHandler);
        this.contextPool = new ContextPool();

        try {
            buildRulesAndPatterns();
            logger.info("Build rules and patterns successfully.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Object bfuncInstance = null;
        try {
            bfuncInstance = loadBfuncFile();
            logger.info("Load bfunc file successfully.");
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
        logger.info("Checking technique is " + technique + ", scheduling strategy is " + schedule);

        assert technique != null;

        switch (technique) {
            case "ECC":
                this.checker = new ECC(this.ruleHandler, this.contextPool, bfuncInstance);
                break;
            case "ConC":
                this.checker = new ConC(this.ruleHandler, this.contextPool, bfuncInstance);
                break;
            case "PCC":
                this.checker = new PCC(this.ruleHandler, this.contextPool, bfuncInstance);
                break;
            case "INFUSE_base":
                this.checker = new BASE(this.ruleHandler, this.contextPool, bfuncInstance);
                break;
            case "INFUSE_C":
                this.checker = new INFUSE_C(this.ruleHandler, this.contextPool, bfuncInstance);
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

        //check init
        this.checker.checkInit();
        logger.info("Init checking successfully.");

        //run
        try {
            logger.info("Start running......");
            run();
            if(type.equals("test")){
                testRunEnd(bfuncInstance);
            }
            IncOutput();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void buildRulesAndPatterns() throws Exception {
        this.ruleHandler.buildRules(ruleFile);
        this.patternHandler.buildPatterns(patternFile, mfuncFile);

        for(Rule rule : ruleHandler.getRuleMap().values()){
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
        Path bfuncPath = Paths.get(bfuncFile).toAbsolutePath();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{ bfuncPath.getParent().toFile().toURI().toURL()});
        Class<?> c = classLoader.loadClass(bfuncPath.getFileName().toString().substring(0, bfuncPath.getFileName().toString().length() - 6));
        Constructor<?> constructor = c.getConstructor();
        return constructor.newInstance();
    }

    private void run() throws Exception{
        String line;
        List<ContextChange> changeList = new ArrayList<>();
        InputStreamReader inputStreamReader = new InputStreamReader(Files.newInputStream(Paths.get(dataFile)), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String dataType =  bufferedReader.readLine().trim();
        this.contextHandler.setDataType(dataType);

        while((line = bufferedReader.readLine()) != null){
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
    }

    private void testRunEnd(Object bfuncInstance) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        bfuncInstance.getClass().getMethod("end").invoke(bfuncInstance);
    }

    private void IncOutput() throws Exception {
        if(type.equalsIgnoreCase("taxi")){
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            //对每个rule遍历
            for(Map.Entry<String, List<Map.Entry<Boolean, Set<Link>>>> entry : this.checker.getRuleLinksMap().entrySet()){
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(entry.getKey()).append('(');
                //累计每一次的link，氛围violated和satisfied(and or implies 两种都可能会有)
                Set<Link> accumVioLinks = new HashSet<>();
                Set<Link> accumSatLinks = new HashSet<>();
                for(Map.Entry<Boolean, Set<Link>> resultEntry : entry.getValue()){
                    if(resultEntry.getKey()){
                        accumSatLinks.addAll(resultEntry.getValue());
                    }
                    else{
                        accumVioLinks.addAll(resultEntry.getValue());
                    }
                }
                for(Link link : accumVioLinks){
                    Link.Link_Type linkType = Link.Link_Type.VIOLATED;
                    StringBuilder tmpBuilder = new StringBuilder(stringBuilder);
                    tmpBuilder.append(linkType.name()).append(",{");
                    //对当前每个link的变量赋值遍历
                    for(Map.Entry<String, Context> va : link.getVaSet()){
                        tmpBuilder.append("(").append(va.getKey()).append(",").append(Integer.parseInt(va.getValue().getCtx_id().substring(4)) + 1).append("),");
                    }
                    tmpBuilder.deleteCharAt(tmpBuilder.length() - 1);
                    tmpBuilder.append("})");
                    bufferedWriter.write(tmpBuilder.toString() + "\n");
                    bufferedWriter.flush();
                }
                for(Link link : accumSatLinks){
                    Link.Link_Type linkType = Link.Link_Type.SATISFIED;
                    StringBuilder tmpBuilder = new StringBuilder(stringBuilder);
                    tmpBuilder.append(linkType.name()).append(",{");
                    //对当前每个link的变量赋值遍历
                    for(Map.Entry<String, Context> va : link.getVaSet()){
                        tmpBuilder.append("(").append(va.getKey()).append(",").append(Integer.parseInt(va.getValue().getCtx_id().substring(4)) + 1).append("),");
                    }
                    tmpBuilder.deleteCharAt(tmpBuilder.length() - 1);
                    tmpBuilder.append("})");
                    bufferedWriter.write(tmpBuilder.toString() + "\n");
                    bufferedWriter.flush();
                }
            }
        }
        else if (type.equalsIgnoreCase("test")){
            JSONObject root = new JSONObject();
            Map<String, List<Map.Entry<Boolean, Set<Link>>>> ruleLinksMap = this.checker.getRuleLinksMap();
            for(Rule rule : this.ruleHandler.getRuleMap().values()){
                String rule_id = rule.getRule_id();
                if(ruleLinksMap.containsKey(rule_id)){
                    JSONObject ruleJsonObj = new JSONObject();
                    Map.Entry<Boolean, Set<Link>> latestResult = ruleLinksMap.get(rule_id).get(ruleLinksMap.get(rule_id).size() - 1);
                    ruleJsonObj.put("truth", latestResult.getKey());
                    JSONArray linksJsonArray = new JSONArray();
                    //links foreach
                    for(Link link : latestResult.getValue()){
                        JSONArray linkJsonArray = new JSONArray();
                        //vaSet foreach
                        for(Map.Entry<String, Context> vaEntry : link.getVaSet()){
                            JSONObject vaJsonObj = new JSONObject();
                            //set var
                            vaJsonObj.put("var", vaEntry.getKey());
                            //set value
                            Context context = vaEntry.getValue();
                            JSONObject valueJsonObj = new JSONObject();
                            valueJsonObj.put("ctx_id", context.getCtx_id());
                            JSONObject fieldsJsonObj = new JSONObject();
                            //context fields foreach
                            for(String fieldName : context.getCtx_fields().keySet()){
                                fieldsJsonObj.put(fieldName, context.getCtx_fields().get(fieldName));
                            }
                            valueJsonObj.put("fields", fieldsJsonObj);
                            vaJsonObj.put("value", valueJsonObj);
                            //store vaNode
                            linkJsonArray.add(vaJsonObj);
                        }
                        //store linkNode
                        linksJsonArray.add(linkJsonArray);
                    }
                    //store linksNode
                    ruleJsonObj.put("links", linksJsonArray);
                    //store ruleNode
                    root.put(rule_id, ruleJsonObj);
                }
                else{
                    JSONObject ruleJsonObj = new JSONObject();
                    ruleJsonObj.put("truth", rule.getCCTRoot().isTruth());
                    ruleJsonObj.put("links", new JSONArray());
                    root.put(rule_id, ruleJsonObj);
                }
            }

            JSONWriter jsonWriter = new JSONWriter(new FileWriter(outputFile));
            jsonWriter.writeObject(root);
            jsonWriter.close();
        }
        else{
            assert false;
        }
    }
}
