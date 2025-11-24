package cn.edu.nju.ics.spar.cc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class LLMTest {

    private static final String RESOURCE_DIR = "src/test/resources/llmTest";

    @AfterEach
    void cleanup() {
        // 测试完成后删除 .class 文件和结果文件
        deleteClassFiles(RESOURCE_DIR);
        //deleteResultFiles(RESOURCE_DIR);
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

        // 获取当前类路径
        String classpath = System.getProperty("java.class.path");

        // 直接用 javac 编译到当前目录
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            // 使用系统 Java 编译器，输出到当前目录
            int result = compiler.run(null, null, null,
                "-cp", classpath,
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
                "-cp", classpath,
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

    /**
     * 删除测试资源目录中的结果文件
     */
    private static void deleteResultFiles(String resourceDir) {
        try {
            Path resDir = Paths.get(resourceDir).toAbsolutePath();
            Files.list(resDir)
                .filter(p -> p.getFileName().toString().startsWith("result"))
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

    public static void testDriver(String ap, boolean isMG, String resultPath) {
        String resourceDir = "src/test/resources/llmTest";
        try {
            // 在运行前确保 Bfunction/Mfunction 已经被编译到对应的 class 文件中
            ensureFunctionsCompiled(resourceDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile b/m function sources", e);
        }

        String[] args = null;
        if(isMG) {
            args = new String[]{
                    "-mode", "offline", "-approach", ap,
                    "-rules", resourceDir + "/rules.xml",
                    "-bfuncs", resourceDir + "/Bfunction.class",
                    "-patterns", resourceDir + "/patterns.xml",
                    "-mfuncs", resourceDir + "/Mfunction.class",
                    "-data", resourceDir + "/data.txt",
                    "-datatype", "rawData",
                    "-mg",
                    "-incs", resultPath
            };
        }
        else {
            args = new String[]{
                    "-mode", "offline", "-approach", ap,
                    "-rules", resourceDir + "/rules.xml",
                    "-bfuncs", resourceDir + "/Bfunction.class",
                    "-patterns", resourceDir + "/patterns.xml",
                    "-mfuncs", resourceDir + "/Mfunction.class",
                    "-data", resourceDir + "/data.txt",
                    "-datatype", "rawData",
                    "-incs", resultPath
            };
        }
        try {
            CLIParser.main(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void ECC_IMD_CG_Test() {
        String resultPath = "src/test/resources/llmTest/result_ecc_imd_cg.txt";
        System.out.println("=== 测试 ECC+IMD+CG 方法 ===");
        System.out.println("运行 ECC+IMD+CG 方法...");
        LLMTest.testDriver("ECC+IMD", false, resultPath);
        System.out.println("ECC+IMD+CG 方法测试完成");
    }

    @Test
    void PCC_IMD_CG_Test() {
        String resultPath = "src/test/resources/llmTest/result_pcc_imd_cg.txt";
        System.out.println("=== 测试 PCC+IMD+CG 方法 ===");
        System.out.println("运行 PCC+IMD+CG 方法...");
        LLMTest.testDriver("PCC+IMD", false, resultPath);
        System.out.println("PCC+IMD+CG 方法测试完成");
    }
}