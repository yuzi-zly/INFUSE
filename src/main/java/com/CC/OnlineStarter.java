package com.CC;

import com.CC.Constraints.Rules.Rule;
import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Constraints.Runtime.Link;
import com.CC.Contexts.*;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.*;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.PatternHandlerFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;


import java.io.*;
import java.lang.reflect.Constructor;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class OnlineStarter {

    public static final int dataPacketLen = 100;

    static class CCEServer implements Callable<Void> {
        private final String type;
        private final String dataFile;
        private final String ruleFile;
        private final String patternFile;
        private final String bfuncFile;

        private final String outputFile;
        private final RuleHandler ruleHandler;
        private final PatternHandler patternHandler;
        private final ContextHandler contextHandler;
        private final ContextPool contextPool;
        private Scheduler scheduler;
        private Checker checker;

        private final List<ContextChange> changeBuffer;
        private boolean cleaned;

        private long totalTime_gen, totalTime_det, oldTime_gen;

        public CCEServer(String approach, String ruleFile, String patternFile, String dataFile, String bfuncFile, String outputFile, String type) {
            this.type = type;

            this.changeBuffer = new ArrayList<>();
            this.cleaned = false;
            this.totalTime_gen = 0;
            this.oldTime_gen = 0;
            this.totalTime_det = 0;

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

            //check init
            this.checker.checkInit();
        }

        private void buildRulesAndPatterns() throws Exception {
            this.ruleHandler.buildRules(ruleFile);
            this.patternHandler.buildPatterns(patternFile);

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

        private ContextChange getNextChange(DatagramSocket datagramSocket){
            if(!changeBuffer.isEmpty()){
                ContextChange ret = changeBuffer.get(0);
                changeBuffer.remove(0);
                return ret;
            }
            assert datagramSocket != null;
            byte[] data = new byte[dataPacketLen];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            List<ContextChange> changeList = new ArrayList<>();
            try {
                totalTime_gen += System.currentTimeMillis() - oldTime_gen;
                datagramSocket.receive(datagramPacket);
                oldTime_gen = System.currentTimeMillis();
                String line = new String(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength(), StandardCharsets.UTF_8);
                //System.out.println(line);

                this.contextHandler.generateChanges(line.trim(), changeList);
                changeBuffer.addAll(changeList);
                ContextChange ret = changeBuffer.get(0);
                changeBuffer.remove(0);
                return ret;

            } catch (IOException e) {
                // Buffer已经为空，且已经清空过，应该停止
                if(cleaned){
                    return null;
                }
                else{
                    synchronized (System.err){
                        System.err.println("[CCEServer] datagramSocket receive timeout");
                    }
                    try {
                        cleaned = true;
                        changeList.clear();
                        this.contextHandler.generateChanges(null, changeList);
                        changeBuffer.addAll(changeList);
                        ContextChange ret = changeBuffer.get(0);
                        changeBuffer.remove(0);
                        return ret;
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (ParseException e) {
                synchronized (System.err) {
                    System.err.println("[CCEServer] simpleDateFormat parse error");
                }
                e.printStackTrace();
            } catch (Exception e) {
                synchronized (System.err) {
                    System.err.println("[CCEServer] ContextHandler generates error");
                }
                e.printStackTrace();
            }
            return null;
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
            }
            else{
                assert false;
            }
        }
        @Override
        public Void call() throws Exception {
            DatagramSocket datagramSocket = null;
            try {
                datagramSocket = new DatagramSocket(10086);
                datagramSocket.setSoTimeout(5000);
            } catch (SocketException e) {
                System.err.println("[CCEServer]: Build datagramSocket error");
                e.printStackTrace();
            }

            synchronized (System.out){
                System.out.println("[CCEServer]: " + new Date(System.currentTimeMillis()) + " start checking " + dataFile);
            }
            while(true){
                try {
                    oldTime_gen = System.currentTimeMillis();
                    ContextChange contextChange = getNextChange(datagramSocket);
                    totalTime_gen += System.currentTimeMillis() - oldTime_gen;
                    long oldTime_chk = System.currentTimeMillis();
                    if(contextChange == null) break;
                    this.scheduler.doSchedule(contextChange);
                    totalTime_det += System.currentTimeMillis() - oldTime_chk;
                } catch (Exception e) {
                    System.err.println("[CCEServer] SingleChangeScheduling error");
                    e.printStackTrace();
                }

            }
            long oldTime_chk = System.currentTimeMillis();
            this.scheduler.checkEnds();
            totalTime_det += System.currentTimeMillis() - oldTime_chk;

            IncOutput();
            synchronized (System.out){
                System.out.println("[CCEServer] finish checking.");
                System.out.println("[CCEServer] totalTime_gen: " + this.totalTime_gen + " ms\ttotalTime_det: " + this.totalTime_det + " ms\n");
            }
            return null;
        }
    }

    static class CCEClient implements Callable<Void>{
        private final String dataFile;

        public CCEClient(String dataFile) {
            this.dataFile = dataFile;
        }

        @Override
        public Void call() throws Exception {
            //打开数据文件
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(this.dataFile), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(1000);
            datagramSocket.connect(InetAddress.getByName("localhost"), 10086);

            //循环读取change文件，发送数据
            long startTime_fake = -1;
            long startTime_real = -1;
            String line;
            int cnt = 0;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

            synchronized (System.out){
                System.out.println("[CCEClient]: begin at: " + new Date(System.currentTimeMillis()));
            }
            do {
                line = bufferedReader.readLine();
                if(line == null || line.equals("")){
                    break;
                }

                StringTokenizer st = new StringTokenizer(line, ",");
                String time = st.nextToken();
                long curTime_fake = simpleDateFormat.parse(time).getTime();
                long curTime_real = System.currentTimeMillis();

                byte[] data = String.format("%-100s", line).getBytes(StandardCharsets.UTF_8);
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
                if(startTime_fake == -1 && startTime_real == -1){
                    startTime_fake = curTime_fake;
                    startTime_real = curTime_real;
                    datagramSocket.send(datagramPacket);
                }
                else{
                    long deltaTime_real = curTime_real - startTime_real;
                    long deltaTime_fake = curTime_fake - startTime_fake;
                    while(deltaTime_real < deltaTime_fake){
                        deltaTime_real = System.currentTimeMillis() - startTime_real;
                    }
                    datagramSocket.send(datagramPacket);
                }
                cnt++;
                //System.out.println(System.currentTimeMillis() + " " + cnt);
                //if(cnt == 10000) break;
            }while (true);
            datagramSocket.close();
            synchronized (System.out){
                System.out.println("[CCEClient]: " + new Date(System.currentTimeMillis()) + ": write over " + this.dataFile + " with " + cnt + "\n");
            }
            return null;
        }
    }


    public OnlineStarter() {
    }

    public void start(String approach, String ruleFile, String patternFile,  String dataFile, String bfuncFile, String outputFile, String type){
        FutureTask<Void> clientTask = new FutureTask<>(new CCEClient(dataFile));
        FutureTask<Void> serverTask = new FutureTask<>(new CCEServer(approach, ruleFile, patternFile, dataFile, bfuncFile, outputFile, type));
        new Thread(clientTask, "Client...").start();
        new Thread(serverTask, "Server...").start();
        try {
            clientTask.get();
            serverTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
