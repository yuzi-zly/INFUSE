package com.CC;

import com.CC.Constraints.Rule;
import com.CC.Constraints.RuleHandler;
import com.CC.Contexts.*;
import com.CC.Middleware.Checkers.*;
import com.CC.Middleware.Schedulers.*;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.PatternHandlerFactory;

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
        private final String dataFile;
        private final String ruleFile;
        private final String patternFile;
        private final String bfuncFile;
        private final RuleHandler ruleHandler;
        private final PatternHandler patternHandler;
        private final ContextHandler contextHandler;
        private final ContextPool contextPool;
        private Scheduler scheduler;
        private Checker checker;

        private final List<ContextChange> changeBuffer;
        private boolean cleaned;

        private long totalTime_gen, totalTime_det, oldTime_gen;

        public CCEServer(String approach, String ruleFile, String patternFile, String dataFile, String bfuncFile, String type) {
            this.changeBuffer = new ArrayList<>();
            this.cleaned = false;
            this.totalTime_gen = 0;
            this.oldTime_gen = 0;
            this.totalTime_det = 0;

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

    public void start(String approach, String ruleFile, String patternFile,  String dataFile, String bfuncFile, String type){
        FutureTask<Void> clientTask = new FutureTask<>(new CCEClient(dataFile));
        FutureTask<Void> serverTask = new FutureTask<>(new CCEServer(approach, ruleFile, patternFile, dataFile, bfuncFile, type));
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
