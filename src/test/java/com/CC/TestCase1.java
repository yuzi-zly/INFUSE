package com.CC;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

// TestCase1: T1 - 轻量负载下基于基础规则的任务构建正确性测试
public class TestCase1 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase1";
    
    @AfterEach
    void cleanup() {
        deleteClassFiles(RESOURCE_DIR);
    }

    private static void runWithTaskOut(String approach, String taskOutPath) {
        String resourceDir = "src/test/resources/testcase1";
        try {
            ensureFunctionsCompiled(resourceDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile b/m function sources", e);
        }

        String[] args = new String[]{
                "-mode", "offline", "-approach", approach,
                "-rules", resourceDir + "/basic_rules.xml",
                "-bfuncs", resourceDir + "/Bfunction.class",
                "-patterns", resourceDir + "/basic_patterns.xml",
                "-mfuncs", resourceDir + "/Mfunction.class",
                "-data", resourceDir + "/light_workload.txt",
                "-datatype", "rawData",
                "-incs", resourceDir + "/results.txt",
                "-taskOut", taskOutPath
        };

        try {
            File out = new File(taskOutPath);
            if(out.exists()) {
                out.delete();
            }
            CLIParser.main(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void T1_Baseline(){
        String out = "src/test/resources/testcase1/taskout_Baseline.txt";
        String oracle = "src/test/resources/testcase1/oracle_taskout_Baseline.txt";
        System.out.println("=== T1: 轻量负载下基于基础规则 - Baseline任务构建 ===");
        runWithTaskOut("ConC+GEAS_ori", out);
        validateTaskOutAgainstOracle(out, oracle);
    }

    @Test
    void T1_Fusion(){
        String out = "src/test/resources/testcase1/taskout_Fusion.txt";
        String oracle = "src/test/resources/testcase1/oracle_taskout_Fusion.txt";
        System.out.println("=== T1: 轻量负载下基于基础规则 - Fusion任务构建 ===");
        runWithTaskOut("INFUSE", out);
        validateTaskOutAgainstOracle(out, oracle);
    }

    private static void validateTaskOutAgainstOracle(String taskOutPath, String oraclePath) {
        try {
            List<String> oracle = FileUtils.readLines(new File(oraclePath), StandardCharsets.UTF_8);
            List<String> actual = FileUtils.readLines(new File(taskOutPath), StandardCharsets.UTF_8);
            assertTrue(oracle.size() == actual.size(), "Oracle and actual taskOut have different number of lines");
            for (int i = 0; i < oracle.size(); i++) {
                assertTrue(oracle.get(i).equals(actual.get(i)), String.format("Line %d differs\nexpected: %s\nactual:   %s", i, oracle.get(i), actual.get(i)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureFunctionsCompiled(String resourceDir) throws Exception {
        Path resDir = Paths.get(resourceDir).toAbsolutePath();
        Path bJava = resDir.resolve("Bfunction.java");
        Path mJava = resDir.resolve("Mfunction.java");
        
        if (!Files.exists(bJava) || !Files.exists(mJava)) {
            return;
        }

        deleteClassFiles(resourceDir);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            int result = compiler.run(null, null, null, 
                "-d", resDir.toString(),
                bJava.toString(), 
                mJava.toString()
            );
            if (result != 0) {
                throw new RuntimeException("Compilation failed with exit code: " + result);
            }
        } else {
            ProcessBuilder pb = new ProcessBuilder("javac", 
                "-d", resDir.toString(),
                bJava.toString(), 
                mJava.toString()
            );
            pb.directory(resDir.toFile());
            pb.inheritIO();
            Process p = pb.start();
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("javac compilation failed with exit code: " + code);
            }
        }
    }

    private static void deleteClassFiles(String resourceDir) {
        try {
            Path resDir = Paths.get(resourceDir).toAbsolutePath();
            Files.list(resDir)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // 忽略删除失败
                    }
                });
        } catch (IOException e) {
            // 忽略
        }
    }
}
