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

// TestCase2: 针对 T2（处理结果正确性测试），验证不同处理方法的结果正确性
public class TestCase2 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase2";
    
    @AfterEach
    void cleanup() {
        // 测试完成后删除 .class 文件
        deleteClassFiles(RESOURCE_DIR);
    }

    private static void runWithResult(String approach, String resultPath) {
        String resourceDir = "src/test/resources/testcase2";
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
            // 删除旧的输出（如果存在），以免干扰测试（不输出日志）
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
    void T2_INCREMENTAL(){
        String oracle = "src/test/resources/testcase2/results_oracle.txt";
        String incremental = "src/test/resources/testcase2/results_incremental.txt";
        System.out.println("=== 测试增量式泛在数据处理方法结果正确性 ===");
        System.out.println("运行增量方法...");
        runWithResult("PCC+IMD", incremental);
        System.out.println("验证增量方法输出结果...");
        validateResultsEqual(oracle, incremental);
    }

    @Test
    void T2_FUSION(){
        String oracle = "src/test/resources/testcase2/results_oracle.txt";
        String fusion = "src/test/resources/testcase2/results_fusion.txt";
        System.out.println("=== 测试融合式泛在数据处理方法结果正确性 ===");
        System.out.println("运行融合方法...");
        runWithResult("INFUSE", fusion);
        System.out.println("验证融合方法输出结果...");
        validateResultsEqual(oracle, fusion);
    }

    private static void validateResultsEqual(String file1, String file2) {
        try {
            List<String> result1 = FileUtils.readLines(new File(file1), StandardCharsets.UTF_8);
            List<String> result2 = FileUtils.readLines(new File(file2), StandardCharsets.UTF_8);
            
            // 使用稳定排序确保一致性
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
