#!/bin/bash

# ================================================================
# 串行运行所有测试用例的负载分析命令（基于tmux）
# 功能：使用tmux在terminal中逐个运行stats命令
# 使用方法：./run_all_stats.sh
# ================================================================

set -e

# 配置
SESSION_NAME="udpe_stats"
WINDOW_NAME="stats"

# 检查tmux是否安装
if ! command -v tmux &> /dev/null; then
    echo "错误: 未安装tmux"
    echo "请安装tmux:"
    echo "  macOS: brew install tmux"
    echo "  Ubuntu/Debian: sudo apt-get install tmux"
    exit 1
fi

# 检查会话是否已存在，如果存在则先删除
tmux has-session -t "$SESSION_NAME" 2>/dev/null && tmux kill-session -t "$SESSION_NAME"

# 创建tmux会话
tmux new-session -d -s "$SESSION_NAME" -n "$WINDOW_NAME"

# 进入项目目录
tmux send-keys -t "$SESSION_NAME:$WINDOW_NAME" "cd $(pwd)" C-m

# 短暂延迟
sleep 0.5

# 循环发送stats命令到tmux窗口
for group in 1 2 3; do
    for sub in {1..6}; do
        testcase="T${group}_${sub}"
        
        # 发送命令到tmux窗口（模拟人工输入）
        tmux send-keys -t "$SESSION_NAME:$WINDOW_NAME" "./run_tests.sh stats $testcase" C-m
        
        # 等待命令执行完成
        sleep 1.5
    done
done

# 最后附加到会话，让用户看到结果
tmux attach -t "$SESSION_NAME"
