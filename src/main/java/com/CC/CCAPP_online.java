//package com.CC;
//
//import com.CC.Middleware.Online.CCClient;
//import com.CC.Middleware.Online.CCServer;
//
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.FutureTask;
//
//public class CCAPP_online {
//    public static void main(String[] args) {
//        FutureTask<Void> clientTask = new FutureTask<>(new CCClient(args[1]));
//        FutureTask<Void> serverTask = new FutureTask<>(new CCServer(args[3], args[0], args[1], args[2], "online", 0));
//        new Thread(serverTask, "Server...").start();
//        new Thread(clientTask, "Client...").start();
//        try {
//            clientTask.get();
//            serverTask.get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
//    }
//}
