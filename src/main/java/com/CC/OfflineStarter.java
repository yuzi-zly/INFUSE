package com.CC;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.*;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.*;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.PatternHandlerFactory;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OfflineStarter {
    private RuleHandler ruleHandler;
    private PatternHandler patternHandler;
    private ContextHandler contextHandler;
    private ContextPool contextPool;

    private String dataFile;
    private String ruleFile;
    private String patternFile;
    private String bfuncFile;

    private String outputFile;

    private Scheduler scheduler;
    private Checker checker;

    private String type;

    public OfflineStarter() {}

    public void start(String approach, String ruleFile, String patternFile, String dataFile, String bfuncFile, String outputFile, String type){
        this.type = type;

        this.dataFile = dataFile;
        this.ruleFile = ruleFile;
        this.patternFile = patternFile;
        this.bfuncFile = bfuncFile;
        this.outputFile = outputFile;

        this.ruleHandler = new RuleHandler();
        this.patternHandler = new PatternHandlerFactory().getPatternHandler(type);
        this.contextHandler = new ContextHandlerFactory().getContextHandler(type, patternHandler);
        this.contextPool = new ContextPool();

        try {
            buildRulesAndPatterns();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Object bfuncInstance = null;
        try {
            bfuncInstance = loadBfuncFile();
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

        //run
        try {
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
        Path bfuncPath = Paths.get(bfuncFile).toAbsolutePath();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{ bfuncPath.getParent().toFile().toURI().toURL()});
        Class<?> c = classLoader.loadClass(bfuncPath.getFileName().toString().substring(0, bfuncPath.getFileName().toString().length() - 6));
        Constructor<?> constructor = c.getConstructor();
        return constructor.newInstance();
    }

    private void run() throws Exception{
        String line;
        List<ContextChange> changeList = new ArrayList<>();
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
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
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();

            Map<String, List<Map.Entry<Boolean, Set<Link>>>> ruleLinksMap = this.checker.getRuleLinksMap();
            for(Rule rule : this.ruleHandler.getRuleList()){
                String rule_id = rule.getRule_id();
                if(ruleLinksMap.containsKey(rule_id)){
                    ObjectNode ruleNode = mapper.createObjectNode();
                    Map.Entry<Boolean, Set<Link>> latestResult = ruleLinksMap.get(rule_id).get(ruleLinksMap.get(rule_id).size() - 1);
                    ruleNode.put("truth", latestResult.getKey());
                    ArrayNode linksNode = mapper.createArrayNode();
                    //links foreach
                    for(Link link : latestResult.getValue()){
                        ArrayNode linkNode = mapper.createArrayNode();
                        //vaSet foreach
                        for(Map.Entry<String, Context> vaEntry : link.getVaSet()){
                            ObjectNode vaNode = mapper.createObjectNode();
                            //set var
                            vaNode.put("var", vaEntry.getKey());
                            //set value
                            Context context = vaEntry.getValue();
                            ObjectNode valueNode = mapper.createObjectNode();
                            valueNode.put("ctx_id", context.getCtx_id());
                            ObjectNode fieldsNode = mapper.createObjectNode();
                            //context fields foreach
                            for(String fieldName : context.getCtx_fields().keySet()){
                                fieldsNode.put(fieldName, context.getCtx_fields().get(fieldName));
                            }
                            valueNode.set("fields", fieldsNode);
                            vaNode.set("value", valueNode);
                            //store vaNode
                            linkNode.add(vaNode);
                        }
                        //store linkNode
                        linksNode.add(linkNode);
                    }
                    //store linksNode
                    ruleNode.set("links", linksNode);
                    //store ruleNode
                    root.set(rule_id, ruleNode);
                }
                else{
                    ObjectNode ruleNode = mapper.createObjectNode();
                    ruleNode.put("truth", rule.getCCTRoot().isTruth());
                    ArrayNode linksNode = mapper.createArrayNode();
                    ruleNode.set("links", linksNode);
                    root.set(rule_id, ruleNode);
                }
            }
            ObjectWriter objectWriter = mapper.writer(new DefaultPrettyPrinter());
            objectWriter.writeValue(new File(outputFile), root);
        }
        else{
            assert false;
        }
    }
}
