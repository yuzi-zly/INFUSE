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

// TestCase3: 针对 T3（处理效率测试），测量不同处理方法的性能表现
public class TestCase3 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase3";
    
    @AfterEach
    void cleanup() {
        // 测试完成后删除 .class 文件
        deleteClassFiles(RESOURCE_DIR);
    }

    private static long runAndMeasureTime(String approach, String resultPath) {
        String resourceDir = "src/test/resources/testcase3";
        try {
            // 在运行前确保 Bfunction/Mfunction 已经被编译到对应的 class 文件中
            ensureFunctionsCompiled(resourceDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile b/m function sources", e);
        }

        String[] args = new String[]{
                "-mode", "offline", "-approach", approach,
                "-rules", resourceDir + "/rules.xml",
                "-bfuncs", resourceDir + "/Bfunction.class",
                "-patterns", resourceDir + "/patterns.xml",
                "-mfuncs", resourceDir + "/Mfunction.class",
                "-data", resourceDir + "/data.txt",
                "-datatype", "rawData",
                "-incs", resultPath
        };

        try {
            // 删除旧的输出（如果存在），以免干扰测试
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
    void T3_PERFORMANCE_INCREMENTAL(){
        String baselineResult = "src/test/resources/testcase3/result_baseline.txt";
        String incrementalResult = "src/test/resources/testcase3/result_incremental.txt";
        
        System.out.println("=== 测试增量式泛在数据处理方法性能 ===");
        
        // 运行基准方法
        System.out.println("运行基准方法...");
        long baselineTime = runAndMeasureTime("ECC+IMD", baselineResult);
        System.out.println("基准方法执行时间: " + baselineTime + " ms");
        
        // 运行增量方法
        System.out.println("运行增量方法...");
        long incrementalTime = runAndMeasureTime("PCC+IMD", incrementalResult);
        System.out.println("增量方法执行时间: " + incrementalTime + " ms");
        
        // 计算性能提升
        double improvement = (double)(baselineTime - incrementalTime) / baselineTime;
        System.out.println("性能提升: " + String.format("%.2f%%", improvement * 100));
        System.out.println("时间减少: " + (baselineTime - incrementalTime) + " ms");
        System.out.println();
    }

    @Test
    void T3_PERFORMANCE_FUSION(){
        String baselineResult = "src/test/resources/testcase3/result_baseline.txt";
        String fusionResult = "src/test/resources/testcase3/result_fusion.txt";
        
        System.out.println("=== 测试融合式泛在数据处理方法性能 ===");
        
        // 运行基准方法
        System.out.println("运行基准方法...");
        long baselineTime = runAndMeasureTime("ECC+IMD", baselineResult);
        System.out.println("基准方法执行时间: " + baselineTime + " ms");
        
        // 运行融合方法
        System.out.println("运行融合方法...");
        long fusionTime = runAndMeasureTime("INFUSE", fusionResult);
        System.out.println("融合方法执行时间: " + fusionTime + " ms");
        
        // 计算性能提升
        double improvement = (double)(baselineTime - fusionTime) / baselineTime;
        System.out.println("性能提升: " + String.format("%.2f%%", improvement * 100));
        System.out.println("时间减少: " + (baselineTime - fusionTime) + " ms");
        System.out.println();
    }

    /**
     * 确保 Bfunction.java 和 Mfunction.java 被编译成 .class 文件
     * 每次都强制重新编译
     */
    private static void ensureFunctionsCompiled(String resourceDir) throws Exception {
    
        Path resDir = Paths.get(resourceDir).toAbsolutePath();
        Path bJava = resDir.resolve("Bfunction.java");
        Path mJava = resDir.resolve("Mfunction.java");
        
        if (!Files.exists(bJava) || !Files.exists(mJava)) {
            // 没有源文件，跳过编译
            return;
        }

        // 先删除旧的 .class 文件
        deleteClassFiles(resourceDir);

        // 直接用 javac 编译到当前目录
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            // 使用系统 Java 编译器，输出到当前目录
            int result = compiler.run(null, null, null, 
                "-d", resDir.toString(),
                bJava.toString(), 
                mJava.toString()
            );
            if (result != 0) {
                throw new RuntimeException("Compilation failed with exit code: " + result);
            }
        } else {
            // 回退到外部 javac 命令
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

    /**
     * 删除测试资源目录中的 .class 文件
     */
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
