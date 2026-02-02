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

// TestCase15: T15 - 重度负载下基于基础规则的处理效率测试
public class TestCase15 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase15";
    
    @AfterEach
    void cleanup() {
        deleteClassFiles(RESOURCE_DIR);
    }

    private static long runAndMeasureTime(String approach, String resultPath) {
        String resourceDir = "src/test/resources/testcase15";
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
                "-data", resourceDir + "/heavy_workload.txt",
                "-datatype", "rawData",
                "-incs", resultPath
        };

        try {
            File out = new File(resultPath);
            if(out.exists()) out.delete();

            long startTime = System.currentTimeMillis();
            CLIParser.main(args);
            long endTime = System.currentTimeMillis();
            
            return endTime - startTime;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void T15_Baseline(){
        String baselineResult = "src/test/resources/testcase15/results_Baseline.txt";
        System.out.println("=== T15: 重度负载下基于基础规则 - Baseline性能 ===");
        long baselineTime = runAndMeasureTime("ConC+GEAS_ori", baselineResult);
        System.out.println("Baseline方法执行时间: " + baselineTime + " ms");
    }

    @Test
    void T15_Fusion(){
        String fusionResult = "src/test/resources/testcase15/results_Fusion.txt";
        System.out.println("=== T15: 重度负载下基于基础规则 - Fusion性能 ===");
        long fusionTime = runAndMeasureTime("INFUSE", fusionResult);
        System.out.println("Fusion方法执行时间: " + fusionTime + " ms");
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

