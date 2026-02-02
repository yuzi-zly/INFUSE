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

// TestCase8: T8 - 中度负载下基于基础规则的处理结果正确性测试
public class TestCase8 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase8";
    
    @AfterEach
    void cleanup() {
        deleteClassFiles(RESOURCE_DIR);
    }

    private static void runWithResult(String approach, String resultPath) {
        String resourceDir = "src/test/resources/testcase8";
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
                "-data", resourceDir + "/median_workload.txt",
                "-datatype", "rawData",
                "-incs", resultPath
        };

        try {
            File out = new File(resultPath);
            if(out.exists()) {
                out.delete();
            }
            CLIParser.main(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void T8_Baseline(){
        String oracle = "src/test/resources/testcase8/results_oracle.txt";
        String baseline = "src/test/resources/testcase8/results_Baseline.txt";
        System.out.println("=== T8: 中度负载下基于基础规则 - Baseline处理结果 ===");
        runWithResult("ConC+GEAS_ori", baseline);
        validateResultsEqual(oracle, baseline);
    }

    @Test
    void T8_Fusion(){
        String oracle = "src/test/resources/testcase8/results_oracle.txt";
        String fusion = "src/test/resources/testcase8/results_Fusion.txt";
        System.out.println("=== T8: 中度负载下基于基础规则 - Fusion处理结果 ===");
        runWithResult("INFUSE", fusion);
        validateResultsEqual(oracle, fusion);
    }

    private static void validateResultsEqual(String file1, String file2) {
        try {
            List<String> result1 = FileUtils.readLines(new File(file1), StandardCharsets.UTF_8);
            List<String> result2 = FileUtils.readLines(new File(file2), StandardCharsets.UTF_8);
            
            result1.sort(String::compareTo);
            result2.sort(String::compareTo);
            
            assertTrue(result1.size() == result2.size(), 
                String.format("Results have different number of lines: file1=%d, file2=%d", 
                    result1.size(), result2.size()));
            
            for (int i = 0; i < result1.size(); i++) {
                assertTrue(result1.get(i).equals(result2.get(i)), 
                    String.format("Line %d differs\nfile1: %s\nfile2: %s", i, result1.get(i), result2.get(i)));
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

