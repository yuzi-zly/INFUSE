#!/bin/bash

# ================================================================
# 并行测试执行脚本（基于tmux）
# 功能：使用tmux在真实terminal中并行运行测试（最多2个）
# 使用方法：
#   ./parallel_run_tests.sh              # 运行所有测试
#   ./parallel_run_tests.sh T1           # 运行T1组的所有测试
#   ./parallel_run_tests.sh T1_1 T2_4    # 运行指定的测试
# ================================================================

set -e

# 配置
MAX_PARALLEL=2  # 最大并行数
SESSION_NAME="udpe_tests_$$"  # 使用PID确保唯一性

# 临时目录
TEMP_DIR=$(mktemp -d)
trap "cleanup" EXIT

cleanup() {
    # 仅清理临时目录，保留tmux会话供用户查看
    rm -rf "$TEMP_DIR"
}

# 检查tmux是否安装
check_tmux() {
    if ! command -v tmux &> /dev/null; then
        echo "错误: 未安装tmux"
        echo "请安装tmux:"
        echo "  macOS: brew install tmux"
        echo "  Ubuntu/Debian: sudo apt-get install tmux"
        exit 1
    fi
}

# 任务队列和状态跟踪
declare -a TEST_QUEUE
declare -A TEST_STATUS
declare -A TEST_WINDOW
TOTAL_TESTS=0
COMPLETED_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
NEXT_WINDOW=0

# 创建测试窗口并执行命令
create_and_run_test() {
    local test_name=$1
    local window_index=$NEXT_WINDOW
    NEXT_WINDOW=$((NEXT_WINDOW + 1))
    
    local exit_code_file="$TEMP_DIR/${test_name}.exit"
    
    TEST_WINDOW[$test_name]=$window_index
    TEST_STATUS[$test_name]="RUNNING"
    
    # 创建新窗口
    if [[ $window_index -eq 0 ]]; then
        # 第一个窗口（已存在）
        tmux rename-window -t "$SESSION_NAME:$window_index" "$test_name"
    else
        # 创建新窗口
        tmux new-window -t "$SESSION_NAME" -n "$test_name"
    fi
    
    # 在窗口中直接输入命令（像手动输入一样）
    tmux send-keys -t "$SESSION_NAME:$window_index" "./run_tests.sh test $test_name" C-m
    # 等命令执行完后，保存退出码
    tmux send-keys -t "$SESSION_NAME:$window_index" "echo \$? > '$exit_code_file'" C-m
}

# 检查测试是否完成
check_test_completed() {
    local test_name=$1
    local exit_code_file="$TEMP_DIR/${test_name}.exit"
    
    if [[ -f "$exit_code_file" ]]; then
        local exit_code=$(cat "$exit_code_file")
        
        COMPLETED_TESTS=$((COMPLETED_TESTS + 1))
        
        if [[ $exit_code -eq 0 ]]; then
            TEST_STATUS[$test_name]="PASSED"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            TEST_STATUS[$test_name]="FAILED"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
        
        # 输出测试结果到主terminal（从tmux窗口捕获）
        local window_index=${TEST_WINDOW[$test_name]}
        echo "$ ./run_tests.sh test $test_name"
        tmux capture-pane -t "$SESSION_NAME:$window_index" -p -S -100
        echo ""
        
        # 保留窗口，不关闭
        
        return 0
    fi
    
    return 1
}

# 等待任意测试完成
wait_for_any_completion() {
    while true; do
        for test_name in "${!TEST_STATUS[@]}"; do
            if [[ "${TEST_STATUS[$test_name]}" == "RUNNING" ]]; then
                if check_test_completed "$test_name"; then
                    return 0
                fi
            fi
        done
        sleep 0.5
    done
}

# 获取正在运行的测试数量
get_running_count() {
    local count=0
    for status in "${TEST_STATUS[@]}"; do
        if [[ "$status" == "RUNNING" ]]; then
            count=$((count + 1))
        fi
    done
    echo $count
}

# 解析测试规格参数
parse_test_specs() {
    local specs=()

    for arg in "$@"; do
        if [[ "$arg" =~ ^T([1-3])_([1-6])$ ]]; then
            # 单个测试用例格式 T<group>_<sub>
            local group="${BASH_REMATCH[1]}"
            local sub="${BASH_REMATCH[2]}"
            local test_name="T${group}_${sub}"
            specs+=("${test_name}_Baseline" "${test_name}_Fusion")
        elif [[ "$arg" =~ ^T([1-3])$ ]]; then
            # 整个测试组格式 T<group>
            local group="${BASH_REMATCH[1]}"
            for sub in {1..6}; do
                local test_name="T${group}_${sub}"
                specs+=("${test_name}_Baseline" "${test_name}_Fusion")
            done
        else
            echo "错误: 无效的参数格式: $arg" >&2
            echo "有效格式: T1, T2, T3, T1_1, T2_4, T3_6 等" >&2
            exit 1
        fi
    done

    echo "${specs[@]}"
}

# 打印使用帮助
print_usage() {
    cat << EOF
使用方法:
  $0                    # 运行所有测试
  $0 <test_specs>       # 运行指定的测试

参数格式:
  T<group>              运行整个测试组（例如: T1, T2, T3）
  T<group>_<sub>        运行单个测试用例（例如: T1_1, T2_4, T3_6）

配置:
  最大并行数: $MAX_PARALLEL

示例:
  $0                    # 运行所有测试（最多2个并行）
  $0 T1                 # 运行T1组的所有测试
  $0 T1_1 T2_4          # 运行指定的测试用例

后台运行:
  nohup $0 > parallel_tests.log 2>&1 &
  tail -f parallel_tests.log

注意:
  - 需要安装tmux
  - 测试将在tmux会话中运行，每个测试一个窗口
  - tmux窗口中只显示命令输出，不显示命令本身
  - 主terminal会显示输出摘要
  - tmux会话会保留，可以用 tmux attach 查看详细输出

EOF
}

# 打印测试摘要
print_summary() {
    echo "================================================================"
    echo "测试执行摘要"
    echo "================================================================"
    echo "总测试数:   $TOTAL_TESTS"
    echo "通过测试:   $PASSED_TESTS"
    echo "失败测试:   $FAILED_TESTS"
    
    if [[ $FAILED_TESTS -gt 0 ]]; then
        echo ""
        echo "失败的测试列表:"
        for test_name in "${!TEST_STATUS[@]}"; do
            if [[ "${TEST_STATUS[$test_name]}" == "FAILED" ]]; then
                echo "  ✗ $test_name"
            fi
        done
    fi
    
    echo ""
    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")
        echo "成功率:     ${success_rate}%"
    fi
    echo "================================================================"
    
    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo "🎉 所有测试通过！"
        return 0
    else
        echo "⚠️  部分测试失败"
        return 1
    fi
}

# ================================================================
# 主程序
# ================================================================

# 检查帮助参数
if [[ "$1" = "-h" ]] || [[ "$1" = "--help" ]]; then
    print_usage
    exit 0
fi

# 检查tmux
check_tmux

# 记录开始时间
START_TIME=$(date +%s)

# 确定要运行的测试
if [[ $# -eq 0 ]]; then
    # 没有参数，运行所有测试
    for group in {1..3}; do
        for sub in {1..6}; do
            test_name="T${group}_${sub}"
            TEST_QUEUE+=("${test_name}_Baseline" "${test_name}_Fusion")
        done
    done
else
    # 解析参数
    TEST_QUEUE=($(parse_test_specs "$@"))
fi

TOTAL_TESTS=${#TEST_QUEUE[@]}

echo "开始运行 $TOTAL_TESTS 个测试（最多 $MAX_PARALLEL 个并行）..."
echo ""

# 创建tmux会话（detached模式）
tmux new-session -d -s "$SESSION_NAME"

# 启动初始测试
for ((i=0; i<$MAX_PARALLEL && i<${#TEST_QUEUE[@]}; i++)); do
    test_name=${TEST_QUEUE[$i]}
    create_and_run_test "$test_name"
done

# 移除已启动的测试
if [[ ${#TEST_QUEUE[@]} -ge $MAX_PARALLEL ]]; then
    TEST_QUEUE=("${TEST_QUEUE[@]:$MAX_PARALLEL}")
else
    TEST_QUEUE=()
fi

# 主循环：等待测试完成并启动新测试
while [[ $COMPLETED_TESTS -lt $TOTAL_TESTS ]]; do
    # 等待任意一个测试完成
    wait_for_any_completion
    
    # 如果还有待运行的测试，启动下一个
    if [[ ${#TEST_QUEUE[@]} -gt 0 ]]; then
        test_name=${TEST_QUEUE[0]}
        TEST_QUEUE=("${TEST_QUEUE[@]:1}")
        
        # 等待一小段时间
        sleep 0.5
        
        create_and_run_test "$test_name"
    fi
done

# 记录结束时间
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

echo ""
echo "总执行时间: ${DURATION_MIN}分${DURATION_SEC}秒"
echo ""

# 打印摘要
print_summary

echo ""
echo "================================================================"
echo "查看详细输出:"
echo "================================================================"
echo "tmux会话已保留，可以使用以下命令查看："
echo ""
echo "  # 附加到tmux会话"
echo "  tmux attach-session -t $SESSION_NAME"
echo ""
echo "  # 在tmux中切换窗口"
echo "  Ctrl+b w        - 显示所有窗口列表"
echo "  Ctrl+b n        - 下一个窗口"
echo "  Ctrl+b p        - 上一个窗口"
echo "  Ctrl+b 0-9      - 跳转到指定窗口"
echo ""
echo "  # 退出tmux（不关闭会话）"
echo "  Ctrl+b d        - detach（退出但保留会话）"
echo ""
echo "  # 手动关闭会话（查看完毕后）"
echo "  tmux kill-session -t $SESSION_NAME"
echo "================================================================"
echo ""

# 返回适当的退出码
if [[ $FAILED_TESTS -eq 0 ]]; then
    exit 0
else
    exit 1
fi

