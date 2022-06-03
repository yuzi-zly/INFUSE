//package com.CC.Middleware.Online;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.StringTokenizer;
//import java.util.concurrent.Callable;
//
//public class CCClient implements Callable<Void> {
//    private final String filename;
//
//    public CCClient(String filename){
//        this.filename = filename;
//    }
//
//    @Override
//    public Void call() throws Exception {
//        //打开数据文件
//        FileReader fileReader = new FileReader(this.filename);
//        BufferedReader bufferedReader = new BufferedReader(fileReader);
//
//        DatagramSocket datagramSocket = new DatagramSocket();
//        datagramSocket.setSoTimeout(1000);
//        datagramSocket.connect(InetAddress.getByName("localhost"), 10086);
//
//        //循环读取change文件，发送数据
//        long startTime_fake = -1;
//        long startTime_real = -1;
//        String line;
//        int cnt = 0;
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//
//        synchronized (System.out){
//            System.out.println("[Client]: begin at: " + new Date(System.currentTimeMillis()));
//        }
//        do {
//            line = bufferedReader.readLine();
//            if(line == null || line.equals("")){
//                break;
//            }
//
//            StringTokenizer st = new StringTokenizer(line, ",");
//            String time = st.nextToken();
//            long curTime_fake = simpleDateFormat.parse(time).getTime();
//            long curTime_real = System.currentTimeMillis();
//
//            byte[] data = String.format("%-100s", line).getBytes(StandardCharsets.UTF_8);
//            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
//            if(startTime_fake == -1 && startTime_real == -1){
//                startTime_fake = curTime_fake;
//                startTime_real = curTime_real;
//                datagramSocket.send(datagramPacket);
//            }
//            else{
//                long deltaTime_real = curTime_real - startTime_real;
//                long deltaTime_fake = curTime_fake - startTime_fake;
//                while(deltaTime_real < deltaTime_fake){
//                    deltaTime_real = System.currentTimeMillis() - startTime_real;
//                }
//                datagramSocket.send(datagramPacket);
//            }
//            cnt++;
//            //System.out.println(System.currentTimeMillis() + " " + cnt);
//            //if(cnt == 10000) break;
//        }while (true);
//        datagramSocket.close();
//        synchronized (System.out){
//            System.out.println("[Client]: " + new Date(System.currentTimeMillis()) + ": write over " + this.filename + " with " + cnt + "\n");
//        }
//        return null;
//    }
//}
