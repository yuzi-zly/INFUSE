package com.CC;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CLIParserTest {

    public static void testTaxi(String md, String ap, String data){
        String [] args = new String[]{
          "-md", md, "-ap", ap,
          "-rf", "src/test/resources/taxi/rules.xml", "-pf", "src/test/resources/taxi/patterns.xml",
          "-df", "src/test/resources/taxi/"+data + ".txt", "-bf", "src/test/resources/taxi/Bfunction.class",
          "-o", "src/test/resources/taxi/" + data + "_cceResult.txt"
        };
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