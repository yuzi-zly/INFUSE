package com.CC;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

// TestCase18: T18 - 重度负载下基于复杂约束规则的处理效率测试
public class TestCase18 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase18";
    
    @AfterEach
    void cleanup() {
        deleteClassFiles(RESOURCE_DIR);
    }

    private static long runAndMeasure(String approach, String resultsPath) {
        String resourceDir = "src/test/resources/testcase18";
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
            long start = System.currentTimeMillis();
            CLIParser.main(args);
            long end = System.currentTimeMillis();
            return end - start;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void T18_Baseline(){
        String out = "src/test/resources/testcase18/results_Baseline.txt";
        System.out.println("=== T18: 重度负载下基于复杂规则 - Baseline效率测试 ===");
        long time = runAndMeasure("ConC+GEAS_ori", out);
        System.out.println("Baseline execution time: " + time + " ms");
        assertTrue(time > 0, "Execution time should be positive");
    }

    @Test
    void T18_Fusion(){
        String out = "src/test/resources/testcase18/results_Fusion.txt";
        System.out.println("=== T18: 重度负载下基于复杂规则 - Fusion效率测试 ===");
        long time = runAndMeasure("INFUSE", out);
        System.out.println("Fusion execution time: " + time + " ms");
        assertTrue(time > 0, "Execution time should be positive");
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

