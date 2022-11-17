package com.CC;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CLIParserTest {

    public static void testTaxi(String ap, boolean isMG){
        String[] args = null;
        if(isMG){
            args = new String[]{
                    "-md", "offline", "-ap", ap,
                    "-rf", "src/test/resources/taxi/rules.xml",
                    "-bf", "src/test/resources/taxi/Bfunction.class",
                    "-pf", "src/test/resources/taxi/patterns.xml",
                    "-mf", "src/test/resources/taxi/Mfunction.class",
                    "-df", "src/test/resources/taxi/data_5_0-1_new.txt",
                    "-dt", "rawData",
                    "-mg"
            };
        }
        else{
            args = new String[]{
                    "-md", "offline", "-ap", ap,
                    "-rf", "src/test/resources/taxi/rules.xml",
                    "-bf", "src/test/resources/taxi/Bfunction.class",
                    "-pf", "src/test/resources/taxi/patterns.xml",
                    "-mf", "src/test/resources/taxi/Mfunction.class",
                    "-df", "src/test/resources/taxi/data_5_0-1_new.txt",
                    "-dt", "rawData"
            };
        }
        try {
            CLIParser.main(args);
            //TODO: compare answer
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testTesting(String ap, String path){
        String [] args = new String[]{
                "-t", "-ap", ap,
                "-rf", "src/test/resources/testing/" + path +  "/rules.xml",
                "-cp", "src/test/resources/testing/" + path + "/cp.json",
                "-bf", "src/test/resources/testing/" + path + "/Bfunction.class",
                "-o", "src/test/resources/testing/" + path + "/cceResult.json"
        };
        try {
            CLIParser.main(args);
            //TODO: compare answer
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}