//package com.CC.Middleware.Online;
//
//import com.CC.Contexts.Context;
//import com.CC.Contexts.ContextChange;
//import com.CC.Middleware.Schedulers.*;
//
//import java.io.IOException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.SocketException;
//import java.nio.charset.StandardCharsets;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.Callable;
//
//public class CCServer implements Callable<Void> {
//    private Scheduler scheduler;
//    private final String filename;
//    private int counter;
//    private final Date latestDate;
//    private boolean cleaned;
//    private final List<ContextChange> changeBuffer;
//
//    private long totalTime_gen, totalTime_det, oldTime_gen;
//    private int cnt;
//
//    public CCServer(String scheduler_id, String ruleNum, String filename, String technique, String mode, int batchSize) {
//        switch (scheduler_id){
//            case "IMD": this.scheduler = new IMD(ruleNum, filename, technique, mode); break;
//            case "Batch": this.scheduler = new Batch(ruleNum, filename, technique, mode, batchSize); break;
//            case "GEAS-ori": this.scheduler = new GEAS_ori(ruleNum, filename, technique, mode); break;
//            case "GEAS-opt-s": this.scheduler = new GEAS_opt_s(ruleNum, filename, technique, mode); break;
//            case "GEAS-opt-c": this.scheduler = new GEAS_opt_c(ruleNum, filename, technique, mode); break;
//            case "DIS": this.scheduler = new INFUSE_S(ruleNum, filename, technique, mode); break;
//            default: assert false;
//        }
//        this.filename = filename;
//        this.counter = 0;
//        this.latestDate = new Date();
//        this.cleaned = false;
//        this.changeBuffer = new ArrayList<>();
//
//        this.totalTime_gen = 0;
//        this.oldTime_gen = 0;
//        this.totalTime_det = 0;
//        this.cnt = 0;
//    }
//
//    private ContextChange GenNewCtxChange(DatagramSocket datagramSocket){
//        if(!changeBuffer.isEmpty()){
//            ContextChange ret = changeBuffer.get(0);
//            changeBuffer.remove(0);
//            return ret;
//        }
//        assert datagramSocket != null;
//        byte[] data = new byte[100];
//        DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
//        List<ContextChange> changeList = new ArrayList<>();
//        try {
//            totalTime_gen += System.currentTimeMillis() - oldTime_gen;
//            datagramSocket.receive(datagramPacket);
//            oldTime_gen = System.currentTimeMillis();
//            String line = new String(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength(), StandardCharsets.UTF_8);
//
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//
//            Context context = this.scheduler.getContextHandler().BuildContextFromOrigin11(line.trim(), counter++);
//            Date ctxDate = simpleDateFormat.parse(context.getCtx_timestamp());
//
//            this.scheduler.getContextHandler().CleanOverdueContext(ctxDate, changeList);
//            // RegularPattern: 生成当前新addition context change 并更新 activateCtxPQ
//            this.scheduler.getContextHandler().CreateAdditionChangesRegular(context, changeList);
//            // HotAreaPattern: 生成当前新addition context change 并更新 activateCtxPQ
//            this.scheduler.getContextHandler().CreateAdditionChangesHotArea(context, changeList);
//
//            changeBuffer.addAll(changeList);
//            latestDate.setTime(ctxDate.getTime());
//
//            ContextChange ret = changeBuffer.get(0);
//            changeBuffer.remove(0);
//            return ret;
//
//        } catch (IOException e) {
//            // Buffer已经为空，且已经清空过，应该停止
//            if(cleaned){
//                return null;
//            }
//            else{
//                synchronized (System.err){
//                    System.err.println("[Server] datagramSocket receive timeout");
//                }
//                cleaned = true;
//                changeList.clear();
//                latestDate.setTime(latestDate.getTime() + (long)(24*3600));
//                this.scheduler.getContextHandler().CleanOverdueContext(latestDate, changeList);
//                changeBuffer.addAll(changeList);
//                ContextChange ret = changeBuffer.get(0);
//                changeBuffer.remove(0);
//                return ret;
//            }
//        } catch (ParseException e) {
//            synchronized (System.err) {
//                System.err.println("[Server] simpleDateFormat parse error");
//            }
//            e.printStackTrace();
//        } catch (Exception e) {
//            synchronized (System.err) {
//                System.err.println("[Server] ContextHandler generates error");
//            }
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    public Void call() throws Exception {
//        DatagramSocket datagramSocket = null;
//        try {
//            datagramSocket = new DatagramSocket(10086);
//            datagramSocket.setSoTimeout(5000);
//        } catch (SocketException e) {
//            System.err.println("[Server]: Build datagramSocket error");
//            e.printStackTrace();
//        }
//
//        synchronized (System.out){
//            System.out.println("[Server]: " + new Date(System.currentTimeMillis()) + " start checking " + filename);
//        }
//        while(true){
//            try {
//                oldTime_gen = System.currentTimeMillis();
//                ContextChange contextChange = GenNewCtxChange(datagramSocket);
//                totalTime_gen += System.currentTimeMillis() - oldTime_gen;
//                long oldTime_chk = System.currentTimeMillis();
//                if(contextChange == null) break;
//                this.scheduler.doSchedule(contextChange);
//                totalTime_det += System.currentTimeMillis() - oldTime_chk;
//            } catch (Exception e) {
//                System.err.println("[Server] SingleChangeScheduling error");
//                e.printStackTrace();
//            }
//            cnt++;
//
//        }
//        long oldTime_chk = System.currentTimeMillis();
//        this.scheduler.checkEnds();
//        totalTime_det += System.currentTimeMillis() - oldTime_chk;
//
//        this.scheduler.IncOutput();
//        synchronized (System.out){
//            System.out.println("[Server] finish checking.");
//            System.out.println("[Server] totalTime_gen: " + this.totalTime_gen + " ms\ttotalTime_det: " + this.totalTime_det + " ms\n");
//            if(this.scheduler instanceof INFUSE_S){
//                System.out.println("propagateTime: " + ((INFUSE_S)this.scheduler).propagateTime / 1000000L + " ms\tmatchTime: " + ((INFUSE_S)this.scheduler).matchTime / 1000000L + " ms");
//            }
//        }
//        return null;
//    }
//}
