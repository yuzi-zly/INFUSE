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

// TestCase1: 针对 T1（任务构建正确性测试），验证 -taskOut 功能及输出格式
public class TestCase1 {

    private static final String RESOURCE_DIR = "src/test/resources/testcase1";
    
    @AfterEach
    void cleanup() {
        // 测试完成后删除 .class 文件
        deleteClassFiles(RESOURCE_DIR);
    }

    private static void runWithTaskOut(String approach, String taskOutPath) {
        String resourceDir = "src/test/resources/testcase1";
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
                "-incs", resourceDir + "/result.txt",
                "-taskOut", taskOutPath
        };

        try {
            // 删除旧的输出（如果存在），以免干扰测试
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
    void T1_STATIC(){
        String out = "src/test/resources/testcase1/taskout_static.txt";
        String oracle = "src/test/resources/testcase1/oracle_taskout_static.txt";
        System.out.println("=== 测试静态任务构建方法正确性 ===");
        System.out.println("运行静态任务构建方法...");
        runWithTaskOut("PCC+GEAS_ori", out);
        System.out.println("验证静态任务构建方法输出结果...");
        validateTaskOutAgainstOracle(out, oracle);
    }

    @Test
    void T1_DYNAMIC(){
        String out = "src/test/resources/testcase1/taskout_dynamic.txt";
        String oracle = "src/test/resources/testcase1/oracle_taskout_dynamic.txt";
        System.out.println("=== 测试动态任务构建方法正确性 ===");
        System.out.println("运行动态任务构建方法...");
        runWithTaskOut("INFUSE", out);
        System.out.println("验证动态任务构建方法输出结果...");
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
