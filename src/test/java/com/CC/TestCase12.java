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

// TestCase12: T12 - 重度负载下基于复杂约束规则的处理结果正确性测试
public class TestCase12 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase12";
    
    @AfterEach
    void cleanup() {
        deleteClassFiles(RESOURCE_DIR);
    }

    private static void runWithResultsOut(String approach, String resultsPath) {
        String resourceDir = "src/test/resources/testcase12";
        try {
            ensureFunctionsCompiled(resourceDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile b/m function sources", e);
        }

        String[] args = new String[]{
                "-mode", "offline", "-approach", approach,
                "-rules", resourceDir + "/complex_rules.xml",
                "-bfuncs", resourceDir + "/Bfunction.class",
                "-patterns", resourceDir + "/complex_patterns.xml",
                "-mfuncs", resourceDir + "/Mfunction.class",
                "-data", resourceDir + "/heavy_workload.txt",
                "-datatype", "rawData",
                "-incs", resultsPath
        };

        try {
            File out = new File(resultsPath);
            if(out.exists()) {
                out.delete();
            }
            CLIParser.main(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void T12_Baseline(){
        String out = "src/test/resources/testcase12/results_Baseline.txt";
        String oracle = "src/test/resources/testcase12/results_oracle.txt";
        System.out.println("=== T12: 重度负载下基于复杂规则 - Baseline处理结果 ===");
        runWithResultsOut("ConC+GEAS_ori", out);
        validateResultsAgainstOracle(out, oracle);
    }

    @Test
    void T12_Fusion(){
        String out = "src/test/resources/testcase12/results_Fusion.txt";
        String oracle = "src/test/resources/testcase12/results_oracle.txt";
        System.out.println("=== T12: 重度负载下基于复杂规则 - Fusion处理结果 ===");
        runWithResultsOut("INFUSE", out);
        validateResultsAgainstOracle(out, oracle);
    }

    private static void validateResultsAgainstOracle(String resultsPath, String oraclePath) {
        try {
            List<String> oracle = FileUtils.readLines(new File(oraclePath), StandardCharsets.UTF_8);
            List<String> actual = FileUtils.readLines(new File(resultsPath), StandardCharsets.UTF_8);
            assertTrue(oracle.size() == actual.size(), "Oracle and actual results have different number of lines");
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

