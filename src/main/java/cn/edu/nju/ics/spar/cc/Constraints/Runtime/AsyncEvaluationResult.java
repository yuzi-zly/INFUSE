package cn.edu.nju.ics.spar.cc.Constraints.Runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode.AsyncTruthValue;

/**
 * 异步评估结果类，包含真值和待处理的异步请求映射
 */
public class AsyncEvaluationResult {
    private final AsyncTruthValue truthValue;
    private final Map<String, RuntimeNode> pendingNodes;  // requestId -> RuntimeNode映射

    public AsyncEvaluationResult(AsyncTruthValue truthValue, Map<String, RuntimeNode> pendingNodes) {
        this.truthValue = truthValue;
        this.pendingNodes = pendingNodes != null ?
            new HashMap<>(pendingNodes) : new HashMap<>();
    }

    public AsyncEvaluationResult(AsyncTruthValue truthValue) {
        this(truthValue, Collections.emptyMap());
    }

    public AsyncTruthValue getTruthValue() {
        return truthValue;
    }

    public Map<String, RuntimeNode> getPendingNodes() {
        return new HashMap<>(pendingNodes);
    }

    public Set<String> getPendingRequestIds() {
        return new HashSet<>(pendingNodes.keySet());
    }

    public boolean hasPendingRequests() {
        return !pendingNodes.isEmpty();
    }

    // 静态工厂方法
    public static AsyncEvaluationResult determinedTrue() {
        return new AsyncEvaluationResult(AsyncTruthValue.DETERMINED_TRUE);
    }

    public static AsyncEvaluationResult determinedFalse() {
        return new AsyncEvaluationResult(AsyncTruthValue.DETERMINED_FALSE);
    }

    public static AsyncEvaluationResult pending(String requestId, RuntimeNode node) {
        Map<String, RuntimeNode> pendingMap = new HashMap<>();
        pendingMap.put(requestId, node);
        return new AsyncEvaluationResult(AsyncTruthValue.PENDING_ASYNC, pendingMap);
    }

    public static AsyncEvaluationResult pending(Map<String, RuntimeNode> pendingNodes) {
        return new AsyncEvaluationResult(AsyncTruthValue.PENDING_ASYNC, pendingNodes);
    }

    // 合并两个结果（用于逻辑组合）
    public static AsyncEvaluationResult combine(AsyncEvaluationResult... results) {
        Map<String, RuntimeNode> allPendingNodes = new HashMap<>();
        AsyncTruthValue combinedTruth = AsyncTruthValue.DETERMINED_TRUE;

        for (AsyncEvaluationResult result : results) {
            if (result != null) {
                allPendingNodes.putAll(result.getPendingNodes());

                // 根据优先级确定最终真值
                if (result.getTruthValue() == AsyncTruthValue.PENDING_ASYNC) {
                    combinedTruth = AsyncTruthValue.PENDING_ASYNC;
                } else if (result.getTruthValue() == AsyncTruthValue.DETERMINED_FALSE
                           && combinedTruth != AsyncTruthValue.PENDING_ASYNC) {
                    combinedTruth = AsyncTruthValue.DETERMINED_FALSE;
                }
            }
        }

        return new AsyncEvaluationResult(combinedTruth, allPendingNodes);
    }

    @Override
    public String toString() {
        return "AsyncEvaluationResult{" +
                "truthValue=" + truthValue +
                ", pendingRequestIds=" + getPendingRequestIds() +
                '}';
    }
}