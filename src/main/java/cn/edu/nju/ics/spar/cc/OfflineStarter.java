package cn.edu.nju.ics.spar.cc;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Rules.RuleHandler;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Contexts.Context;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Contexts.ContextHandler;
import cn.edu.nju.ics.spar.cc.Contexts.ContextPool;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.*;
import cn.edu.nju.ics.spar.cc.Middleware.Schedulers.*;
import cn.edu.nju.ics.spar.cc.Patterns.PatternHandler;
import cn.edu.nju.ics.spar.cc.Util.Loggable;

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
    private String mfuncFile;

    private String dataFile;
    private String incOutFile;

    private RuleHandler ruleHandler;
    private PatternHandler patternHandler;
    private ContextHandler contextHandler;
    private ContextPool contextPool;


    public OfflineStarter() {}

    public void start(String approach, String ruleFile, String bfuncFile, String patternFile, String mfuncFile, String dataFile, String dataType, boolean isMG, String incOutFile){
        this.ruleFile = ruleFile;
        this.bfuncFile = bfuncFile;
        this.patternFile = patternFile;
        this.mfuncFile = mfuncFile;
        this.dataFile = dataFile;
        this.incOutFile = incOutFile;

        this.ruleHandler = new RuleHandler();
        this.patternHandler = new PatternHandler();
        this.contextHandler = new ContextHandler(patternHandler, dataType);
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
        }

        //check init
        this.checker.checkInit();
        logger.info("Init checking successfully.");

        //run
        try {
            logger.info("Start running......");
            run();
            incsOutput();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void buildRulesAndPatterns() throws Exception {
        this.ruleHandler.buildRules(ruleFile);
        logger.info("Build rules successfully.");
        this.patternHandler.buildPatterns(patternFile, mfuncFile);
        logger.info("Build patterns successfully.");

        for(Rule rule : ruleHandler.getRuleMap().values()){
            contextPool.poolInit(rule);
            //S-condition
            rule.deriveSConditions();
            //DIS
            rule.deriveRCRESets();
        }

        for(String pattern_id : patternHandler.getPatternMap().keySet()){
            contextPool.threeSetsInit(pattern_id);
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

}
