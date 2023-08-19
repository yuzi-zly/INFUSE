package com.CC;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.*;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.*;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.Q3PatternHandler;
import com.CC.Util.Loggable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OfflineStarter implements Loggable {

    private Scheduler scheduler;
    private Checker checker;
    private String ruleFile;
    private String bfuncFile;
    private String patternFile;

    private String dataFile;
    private String incOutFile;

    private String runType;


    private RuleHandler ruleHandler;
    private PatternHandler patternHandler;
    private ContextHandler contextHandler;
    private ContextPool contextPool;


    public OfflineStarter() {}

    public void start(String approach, String ruleFile, String bfuncFile, String patternFile, String dataFile, boolean isMG, String incOutFile, String runType){
        this.ruleFile = ruleFile;
        this.bfuncFile = bfuncFile;
        this.patternFile = patternFile;
        this.dataFile = dataFile;
        this.incOutFile = incOutFile;
        this.runType = runType;

        this.ruleHandler = new RuleHandler();
        // use switch to create specific patternHandler and contextHandler
        // ...
        switch (runType){
            case "highwayQ3" :{
                this.patternHandler = new Q3PatternHandler();
                this.contextHandler = new Q3ContextHandler(patternHandler);
                break;
            }
            default: assert false;
        }

        this.contextPool = new ContextPool();

        try {
            buildRulesAndPatterns();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Object bfuncInstance = null;
        try {
            bfuncInstance = loadBfuncFile();
            logger.info("Load bfunctions successfully.");
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

        if(runType.equals("highwayQ3")){
            technique = "PCC";
            schedule = "Q3";
        }

        logger.debug("Checking technique is " + technique + ", scheduling strategy is " + schedule + ", with MG " + (isMG ? "on" : "off"));
        assert technique != null;

        switch (technique) {
            case "ECC":
                this.checker = new ECC(this.ruleHandler, this.contextPool, bfuncInstance, isMG);
                break;
            case "ConC":
                this.checker = new ConC(this.ruleHandler, this.contextPool, bfuncInstance, isMG);
                break;
            case "PCC":
                this.checker = new PCC(this.ruleHandler, this.contextPool, bfuncInstance, isMG);
                break;
            case "INFUSE_base":
                this.checker = new BASE(this.ruleHandler, this.contextPool, bfuncInstance, isMG);
                break;
            case "INFUSE_C":
                this.checker = new INFUSE_C(this.ruleHandler, this.contextPool, bfuncInstance, isMG);
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
            case "Q3":
                this.scheduler = new Q3(ruleHandler, contextPool, checker);
                break;
        }

        //check init
        this.checker.checkInit();
        logger.info("Init checking successfully.");

        //run
        try {
            logger.info("Start running......");
            run();
            logger.info("Start outputting incs......");
            incsOutput();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void buildRulesAndPatterns() throws Exception {
        this.ruleHandler.buildRules(ruleFile);
        logger.info("Build rules successfully.");
        this.patternHandler.buildPatterns(patternFile);
        logger.info("Build patterns successfully.");

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

    private Object loadBfuncFile() {
        Path bfuncPath = Paths.get(bfuncFile).toAbsolutePath();
        Object bfuncInstance = null;
        try(URLClassLoader classLoader = new URLClassLoader(new URL[]{ bfuncPath.getParent().toFile().toURI().toURL()})){
            Class<?> c = classLoader.loadClass(bfuncPath.getFileName().toString().substring(0, bfuncPath.getFileName().toString().length() - 6));
            Constructor<?> constructor = c.getConstructor();
            bfuncInstance = constructor.newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException | IOException e) {
            throw new RuntimeException(e);
        }
        return bfuncInstance;
    }

    private void run() throws Exception{
        String line;
        try(InputStream inputStream = Files.newInputStream(Paths.get(dataFile))){
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while((line = bufferedReader.readLine()) != null){
                //logger.info(line.trim());
                List<ContextChange> changeList = this.contextHandler.generateChanges(line);
                while(!changeList.isEmpty()){
                    ContextChange chg = changeList.get(0);
                    changeList.remove(0);
                    this.scheduler.doSchedule(chg);
                }
            }
            bufferedReader.close();
            inputStreamReader.close();

            List<ContextChange> changeList = this.contextHandler.generateChanges(null);
            while(!changeList.isEmpty()){
                ContextChange chg = changeList.get(0);
                changeList.remove(0);
                this.scheduler.doSchedule(chg);
            }
            this.scheduler.checkEnds();
        }
    }

    private void incsOutput() throws Exception {
        if(runType.equals("highwayQ3")){
            String dir = "src/test/resources/highwayQ3/answers";
            File dirFile = new File(dir);
            if(!dirFile.exists() && !dirFile.isDirectory()){
                boolean mkdirflag = dirFile.mkdirs();
                System.err.println("mkdirs: " + mkdirflag);
            }
            String ansFile = dir + "/answer_" + patternFile.charAt(patternFile.indexOf("_") + 1) + ".txt";
            System.out.println(ansFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(ansFile), StandardCharsets.UTF_8);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write("oldpassid,passid,stationid,stationtype,time,vehicleid\n");
            bufferedWriter.flush();

            for(List<Map.Entry<Boolean, Set<Link>>> answers : this.checker.getRuleLinksMap().values()){
                //累计每一次的link，分为violated和satisfied(and or implies 两种都可能会有)
                Set<Link> accumVioLinks = new HashSet<>();
                Set<Link> accumSatLinks = new HashSet<>();
                for(Map.Entry<Boolean, Set<Link>> resultEntry : answers){
                    if(resultEntry.getKey()){
                        accumSatLinks.addAll(resultEntry.getValue());
                    }
                    else{
                        accumVioLinks.addAll(resultEntry.getValue());
                    }
                }

                for(Link link : accumVioLinks){
                    Context context1 = null, context2 = null;
                    for(Map.Entry<String, Context> va : link.getVaSet()){
                        if(va.getKey().equals("v1")){
                            context1 = va.getValue();
                        }
                        if(va.getKey().equals("v2")){
                            context2 = va.getValue();
                        }
                    }
                    assert context1 != null && context2 != null;
                    String str = context2.getCtx_fields().get("passId") +
                            "," + context1.getCtx_fields().get("passId") +
                            "," + context1.getCtx_fields().get("stationId") +
                            "," + context1.getCtx_fields().get("flowType") +
                            "," + context1.getCtx_fields().get("timeString") +
                            "," + context1.getCtx_fields().get("vlp") + "-" + context1.getCtx_fields().get("vlpc");
                    bufferedWriter.write(str + "\n");
                    bufferedWriter.flush();
                }
            }
            bufferedWriter.close();
            outputStreamWriter.close();
        }
        else if(runType.equalsIgnoreCase("taxi")){
            OutputStream outputStream = Files.newOutputStream(Paths.get(incOutFile));
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
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

            bufferedWriter.close();
            outputStreamWriter.close();
            outputStream.close();
        }
        else{
            assert false;
        }
    }

}
