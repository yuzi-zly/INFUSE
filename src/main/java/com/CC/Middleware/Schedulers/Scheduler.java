package com.CC.Middleware.Schedulers;

import com.CC.Constraints.Rules.RuleHandler;
import com.CC.Contexts.*;
import com.CC.Middleware.Checkers.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class Scheduler {
    protected String strategy;
    protected RuleHandler ruleHandler;
    protected ContextPool contextPool;
    protected Checker checker;

    protected String taskOutFile;
    protected BufferedWriter taskWriter;

    public Scheduler(RuleHandler ruleHandler, ContextPool contextPool, Checker checker, String taskOutFile) {
        this.ruleHandler = ruleHandler;
        this.contextPool = contextPool;
        this.checker = checker;
        this.taskOutFile = taskOutFile;

        // 如果指定了 taskOutFile，创建对应的 writer
        if (taskOutFile != null && !taskOutFile.isEmpty()) {
            try {
                Path outPath = Paths.get(taskOutFile);
                if (outPath.getParent() != null) {
                    Files.createDirectories(outPath.getParent());
                }
                // 直接使用 newBufferedWriter，关闭 BufferedWriter 即会级联关闭底层流
                this.taskWriter = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create task output file: " + taskOutFile, e);
            }
        }
    }

    protected String formatTaskLine(List<ContextChange> changes) {
        int size = (changes == null) ? 0 : changes.size();
        StringBuilder sb = new StringBuilder();
        if (changes != null && !changes.isEmpty()) {
            for (int i = 0; i < changes.size(); i++) {
                sb.append(changes.get(i).toString());
                if (i < changes.size() - 1) sb.append(", ");
            }
        }
        return String.format("[%s] %d: %s", this.strategy, size, sb.toString());
    }

    protected void writeTaskInfo(String info) {
        if (taskWriter != null) {
            try {
                taskWriter.write(info + "\n");
                taskWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write task info", e);
            }
        }
    }

    protected void closeTaskWriter() {
        if (taskWriter != null) {
            try {
                taskWriter.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close task writer", e);
            }
        }
    }

    public abstract void doSchedule(ContextChange contextChange) throws Exception;
    public abstract void checkEnds() throws Exception;
    public abstract String getOutputInfo(String ruleType);

    public Checker getChecker() {
        return checker;
    }
}
