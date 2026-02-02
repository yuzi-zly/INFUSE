#!/bin/bash

set -e

# 项目根目录
PROJECT_ROOT=$(cd "$(dirname "$0")" && pwd)

# 编译输出目录
BUILD_DIR="$PROJECT_ROOT/build"
MAIN_DIR="$BUILD_DIR/classes/main"

# Classpath设置
LIBS_CP="$PROJECT_ROOT/libs/*"
MAIN_CP="$MAIN_DIR"
FULL_CP="$LIBS_CP:$MAIN_CP"

# 公共函数：编译测试用的Bfunction和Mfunction
compile_test_functions() {
    local resource_dir="$1"

    # 删除旧的.class文件
    find "$resource_dir" -name "*.class" -delete 2>/dev/null || true

    # 检查源文件是否存在
    if [[ ! -f "$resource_dir/Bfunction.java" ]] || [[ ! -f "$resource_dir/Mfunction.java" ]]; then
        echo "警告: Bfunction.java 或 Mfunction.java 不存在，跳过编译"
        return 0
    fi

    # 编译到资源目录
    javac -cp "$MAIN_CP" -d "$resource_dir" \
        "$resource_dir/Bfunction.java" "$resource_dir/Mfunction.java"

    if [[ $? -ne 0 ]]; then
        echo "错误: 测试函数编译失败"
        return 1
    fi
}

# 公共函数：清理测试函数的.class文件
cleanup_test_functions() {
    local resource_dir="$1"
    find "$resource_dir" -name "*.class" -delete 2>/dev/null || true
}

# 解析时间戳为毫秒 (格式: 2011-04-08 04:00:00:000)
parse_timestamp_to_ms() {
    local timestamp="$1"
    
    # 提取日期时间和毫秒部分
    if [[ "$timestamp" =~ ^([0-9]{4}-[0-9]{2}-[0-9]{2}\ [0-9]{2}:[0-9]{2}:[0-9]{2}):([0-9]{3})$ ]]; then
        local datetime="${BASH_REMATCH[1]}"
        local milliseconds="${BASH_REMATCH[2]}"
        
        # 转换为秒级时间戳
        local seconds
        if date --version &>/dev/null; then
            # GNU date (Linux)
            seconds=$(date -d "$datetime" "+%s" 2>/dev/null || echo "0")
        else
            # BSD date (macOS)
            seconds=$(date -j -f "%Y-%m-%d %H:%M:%S" "$datetime" "+%s" 2>/dev/null || echo "0")
        fi
        
        if [[ "$seconds" != "0" ]]; then
            echo $((seconds * 1000 + 10#$milliseconds))
        else
            echo "0"
        fi
    else
        echo "0"
    fi
}

# 提取行中的时间戳
extract_timestamp() {
    local line="$1"
    if [[ "$line" =~ \"timestamp\":\ *\"([^\"]+)\" ]]; then
        echo "${BASH_REMATCH[1]}"
    fi
}

# 根据测试用例判断负载类型
get_workload_type() {
    local testcase_dir="$1"
    
    # 从目录名提取子编号 (T<group>_<sub>)
    if [[ "$testcase_dir" =~ T[1-3]_([1-6]) ]]; then
        local sub="${BASH_REMATCH[1]}"
        
        case "$sub" in
            1|4)
                echo "轻量负载"
                ;;
            2|5)
                echo "中度负载"
                ;;
            3|6)
                echo "重度负载"
                ;;
            *)
                echo "未知负载"
                ;;
        esac
    else
        echo "未知负载"
    fi
}

# 统计指定测试用例的工作负载数据
analyze_testcase_workload() {
    local testcase_dir="$1"
    local data_file="$PROJECT_ROOT/src/test/resources/$testcase_dir/data.txt"
    
    if [[ ! -f "$data_file" ]]; then
        echo "错误: 数据文件不存在: $data_file"
        return 1
    fi
    
    # 判断负载类型
    local workload_type=$(get_workload_type "$testcase_dir")
    
    echo "================================"
    echo "测试用例: $testcase_dir"
    echo "================================"
    echo "负载类型: $workload_type"
    
    # 统计总行数
    local total_lines=$(wc -l < "$data_file" | tr -d ' ')
    echo "数据条数: $total_lines"
    
    if [[ $total_lines -lt 2 ]]; then
        echo "数据不足，无法计算平均间隔"
        echo ""
        return
    fi
    
    # 读取第一行
    local first_line=$(head -n 1 "$data_file")
    local first_timestamp=$(extract_timestamp "$first_line")
    local first_ts_ms=$(parse_timestamp_to_ms "$first_timestamp")
    
    # 读取最后一行
    local last_line=$(tail -n 1 "$data_file")
    local last_timestamp=$(extract_timestamp "$last_line")
    local last_ts_ms=$(parse_timestamp_to_ms "$last_timestamp")
    
    if [[ "$first_ts_ms" == "0" ]] || [[ "$last_ts_ms" == "0" ]]; then
        echo "错误: 无法解析时间戳"
        echo ""
        return
    fi
    
    # 计算时间跨度
    local time_span_ms=$((last_ts_ms - first_ts_ms))
    
    # 计算平均间隔（保留2位小数）
    local avg_interval=$(awk "BEGIN {printf \"%.2f\", $time_span_ms / ($total_lines - 1)}")
    
    echo "平均时间间隔: $avg_interval ms"
}

# 统计工作负载数据
show_workload_stats() {
    local testcase="$1"
    
    if [[ -n "$testcase" ]]; then
        # 分析指定的测试用例
        analyze_testcase_workload "$testcase"
    else
        # 如果没有指定测试用例，分析所有基础测试用例
        analyze_testcase_workload "T1_1"
        echo ""
        analyze_testcase_workload "T1_2"
        echo ""
        analyze_testcase_workload "T1_3"
    fi
}

# 公共函数：根据组和子编号获取目录名
get_testcase_dir() {
    local group="$1"
    local sub="$2"
    echo "T${group}_${sub}"
}

# 公共函数：根据组和子编号获取测试描述
get_test_description() {
    local group="$1"
    local sub="$2"
    local test_type="$3"  # "task_construction", "result_correctness", "efficiency"
    
    # 确定负载级别和规则复杂度
    local workload=""
    local rule_type=""
    
    # 根据 sub 确定负载和规则类型
    case "$sub" in
        1)
            workload="轻量负载"
            rule_type="基础规则"
            ;;
        2)
            workload="中度负载"
            rule_type="基础规则"
            ;;
        3)
            workload="重度负载"
            rule_type="基础规则"
            ;;
        4)
            workload="轻量负载"
            rule_type="复杂约束规则"
            ;;
        5)
            workload="中度负载"
            rule_type="复杂约束规则"
            ;;
        6)
            workload="重度负载"
            rule_type="复杂约束规则"
            ;;
    esac
    
    # 根据测试类型生成描述
    case "$test_type" in
        "task_construction")
            echo "${workload}下基于${rule_type}的任务构建正确性测试"
            ;;
        "result_correctness")
            echo "${workload}下基于${rule_type}的处理结果正确性测试"
            ;;
        "efficiency")
            echo "${workload}下基于${rule_type}的处理效率测试"
            ;;
    esac
}

# 公共函数：验证两个文件内容是否一致
validate_files_equal() {
    local file1="$1"
    local file2="$2"
    local test_type="$3"  # "task_construction" 或 "result_correctness"

    if [[ ! -f "$file1" ]]; then
        echo "错误: 文件不存在 $file1"
        return 1
    fi

    if [[ ! -f "$file2" ]]; then
        echo "错误: Oracle文件不存在 $file2"
        echo "提示: 请先运行测试生成输出文件，然后将其复制为oracle文件"
        return 1
    fi

    # 使用diff命令比较文件
    if diff -q "$file1" "$file2" >/dev/null 2>&1; then
        if [[ "$test_type" == "task_construction" ]]; then
            echo "✓ 任务构建结果验证成功"
        elif [[ "$test_type" == "result_correctness" ]]; then
            echo "✓ 处理结果验证成功"
        else
            echo "✓ 验证成功"
        fi
        return 0
    else
        if [[ "$test_type" == "task_construction" ]]; then
            echo "✗ 任务构建结果验证失败: 文件内容不一致"
        elif [[ "$test_type" == "result_correctness" ]]; then
            echo "✗ 处理结果验证失败: 文件内容不一致"
        else
            echo "✗ 验证失败: 文件内容不一致"
        fi
        echo "差异详情:"
        diff "$file1" "$file2" | head -20
        return 1
    fi
}

# 通用运行函数：任务构建测试（T1_1-T1_6）
run_task_construction_test() {
    local group="$1"
    local sub="$2"
    local method="$3"  # Baseline 或 Fusion
    local approach=""
    
    if [[ "$method" == "Baseline" ]]; then
        approach="ConC+GEAS_ori"
    else
        approach="INFUSE"
    fi

    local test_desc=$(get_test_description "$group" "$sub" "task_construction")
    echo "=== 测试T${group}_${sub}_${method}: ${test_desc} ==="

    local testcase_dir=$(get_testcase_dir "$group" "$sub")
    local resource_dir="$PROJECT_ROOT/src/test/resources/${testcase_dir}"
    local incs_file="$resource_dir/results.txt"
    local taskout_file="$resource_dir/taskout_${method}.txt"
    local oracle_file="$resource_dir/oracle_taskout_${method}.txt"

    # 确定规则和模式文件
    local rules_file patterns_file data_file
    # if [[ -f "$resource_dir/basic_rules.xml" ]]; then
    #     rules_file="$resource_dir/basic_rules.xml"
    #     patterns_file="$resource_dir/basic_patterns.xml"
    # else
    #     rules_file="$resource_dir/complex_rules.xml"
    #     patterns_file="$resource_dir/complex_patterns.xml"
    # fi
    rules_file="$resource_dir/rules.xml"
    patterns_file="$resource_dir/patterns.xml"
    
    # 确定数据文件
    # if [[ -f "$resource_dir/light_workload.txt" ]]; then
    #     data_file="$resource_dir/light_workload.txt"
    # elif [[ -f "$resource_dir/median_workload.txt" ]]; then
    #     data_file="$resource_dir/median_workload.txt"
    # else
    #     data_file="$resource_dir/heavy_workload.txt"
    # fi
    data_file="$resource_dir/data.txt"

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 删除旧输出文件
    [[ -f "$taskout_file" ]] && rm -f "$taskout_file"
    [[ -f "$incs_file" ]] && rm -f "$incs_file"

    # 运行测试
    if [[ "$sub" == 6 ]]; then
        # 对于复杂规则重度负载，增加JVM内存设置
        java -Xmx50G -Xms30G -cp "$FULL_CP" com.CC.CLIParser \
            -mode offline \
            -approach "$approach" \
            -rules "$rules_file" \
            -bfuncs "$resource_dir/Bfunction.class" \
            -patterns "$patterns_file" \
            -mfuncs "$resource_dir/Mfunction.class" \
            -data "$data_file" \
            -datatype rawData \
            -incs "$incs_file" \
            -taskOut "$taskout_file"
    else
        java -cp "$FULL_CP" com.CC.CLIParser \
            -mode offline \
            -approach "$approach" \
            -rules "$rules_file" \
            -bfuncs "$resource_dir/Bfunction.class" \
            -patterns "$patterns_file" \
            -mfuncs "$resource_dir/Mfunction.class" \
            -data "$data_file" \
            -datatype rawData \
            -incs "$incs_file" \
            -taskOut "$taskout_file"
    fi

    if [[ $? -ne 0 ]]; then
        echo "错误: UDPE程序执行失败"
        cleanup_test_functions "$resource_dir"
        return 1
    fi

    # 验证输出结果
    if [[ -f "$oracle_file" ]]; then
        validate_files_equal "$taskout_file" "$oracle_file" "task_construction" || {
            cleanup_test_functions "$resource_dir"
            return 1
        }
    else
        echo "警告: Oracle文件不存在，跳过验证"
        echo "生成的输出文件: $taskout_file"
        echo "请检查输出并将其复制为: $oracle_file"
    fi

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T${group}_${sub}_${method} 测试通过 ===\033[0m"
}

# 通用运行函数：处理结果测试（T2_1-T2_6）
run_result_correctness_test() {
    local group="$1"
    local sub="$2"
    local method="$3"  # Baseline 或 Fusion
    local approach=""
    
    if [[ "$method" == "Baseline" ]]; then
        approach="ConC+GEAS_ori"
    else
        approach="INFUSE"
    fi

    local test_desc=$(get_test_description "$group" "$sub" "result_correctness")
    echo "=== 测试T${group}_${sub}_${method}: ${test_desc} ==="

    local testcase_dir=$(get_testcase_dir "$group" "$sub")
    local resource_dir="$PROJECT_ROOT/src/test/resources/${testcase_dir}"
    local results_file="$resource_dir/results_${method}.txt"
    local oracle_file="$resource_dir/results_oracle.txt"

    # 确定规则和模式文件
    local rules_file patterns_file data_file
    # if [[ -f "$resource_dir/basic_rules.xml" ]]; then
    #     rules_file="$resource_dir/basic_rules.xml"
    #     patterns_file="$resource_dir/basic_patterns.xml"
    # else
    #     rules_file="$resource_dir/complex_rules.xml"
    #     patterns_file="$resource_dir/complex_patterns.xml"
    # fi
    rules_file="$resource_dir/rules.xml"
    patterns_file="$resource_dir/patterns.xml"
    
    # 确定数据文件
    # if [[ -f "$resource_dir/light_workload.txt" ]]; then
    #     data_file="$resource_dir/light_workload.txt"
    # elif [[ -f "$resource_dir/median_workload.txt" ]]; then
    #     data_file="$resource_dir/median_workload.txt"
    # else
    #     data_file="$resource_dir/heavy_workload.txt"
    # fi
    data_file="$resource_dir/data.txt"

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 删除旧输出文件
    [[ -f "$results_file" ]] && rm -f "$results_file"

    # 运行测试
    if [[ "$sub" == 6 ]]; then
        # 对于复杂规则重度负载，增加JVM内存设置
        java -Xmx50G -Xms30G -cp "$FULL_CP" com.CC.CLIParser \
            -mode offline \
            -approach "$approach" \
            -rules "$rules_file" \
            -bfuncs "$resource_dir/Bfunction.class" \
            -patterns "$patterns_file" \
            -mfuncs "$resource_dir/Mfunction.class" \
            -data "$data_file" \
            -datatype rawData \
            -incs "$results_file"
    else
        java -cp "$FULL_CP" com.CC.CLIParser \
            -mode offline \
            -approach "$approach" \
            -rules "$rules_file" \
            -bfuncs "$resource_dir/Bfunction.class" \
            -patterns "$patterns_file" \
            -mfuncs "$resource_dir/Mfunction.class" \
            -data "$data_file" \
            -datatype rawData \
            -incs "$results_file"  
    fi

    if [[ $? -ne 0 ]]; then
        echo "错误: UDPE程序执行失败"
        cleanup_test_functions "$resource_dir"
        return 1
    fi

    # 验证输出结果（排序后比较）
    if [[ -f "$oracle_file" ]]; then
        sort "$oracle_file" > "${oracle_file}.sorted"
        sort "$results_file" > "${results_file}.sorted"
        validate_files_equal "${results_file}.sorted" "${oracle_file}.sorted" "result_correctness" || {
            rm -f "${oracle_file}.sorted" "${results_file}.sorted"
            cleanup_test_functions "$resource_dir"
            return 1
        }
        rm -f "${oracle_file}.sorted" "${results_file}.sorted"
    else
        echo "警告: Oracle文件不存在，跳过验证"
        echo "生成的输出文件: $results_file"
        echo "请检查输出并将其复制为: $oracle_file"
    fi

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T${group}_${sub}_${method} 测试通过 ===\033[0m"
}

# 通用运行函数：效率测试（T3_1-T3_6）
run_efficiency_test() {
    local group="$1"
    local sub="$2"
    local method="$3"  # Baseline 或 Fusion
    local approach=""
    
    if [[ "$method" == "Baseline" ]]; then
        approach="ConC+GEAS_ori"
    else
        approach="INFUSE"
    fi

    local test_desc=$(get_test_description "$group" "$sub" "efficiency")
    echo "=== 测试T${group}_${sub}_${method}: ${test_desc} ==="

    local testcase_dir=$(get_testcase_dir "$group" "$sub")
    local resource_dir="$PROJECT_ROOT/src/test/resources/${testcase_dir}"
    local results_file="$resource_dir/results_${method}.txt"

    # 确定规则和模式文件
    local rules_file patterns_file data_file
    # if [[ -f "$resource_dir/basic_rules.xml" ]]; then
    #     rules_file="$resource_dir/basic_rules.xml"
    #     patterns_file="$resource_dir/basic_patterns.xml"
    # else
    #     rules_file="$resource_dir/complex_rules.xml"
    #     patterns_file="$resource_dir/complex_patterns.xml"
    # fi
    rules_file="$resource_dir/rules.xml"
    patterns_file="$resource_dir/patterns.xml"
    
    # 确定数据文件
    # if [[ -f "$resource_dir/light_workload.txt" ]]; then
    #     data_file="$resource_dir/light_workload.txt"
    # elif [[ -f "$resource_dir/median_workload.txt" ]]; then
    #     data_file="$resource_dir/median_workload.txt"
    # else
    #     data_file="$resource_dir/heavy_workload.txt"
    # fi
    data_file="$resource_dir/data.txt"

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 删除旧输出文件
    [[ -f "$results_file" ]] && rm -f "$results_file"

    # 记录开始时间
    local start_time_display=$(date '+%Y-%m-%d %H:%M:%S')
    local start_time=$(date +%s%N)  # 纳秒级时间戳
    echo "开始执行时间: $start_time_display"
    
    # 运行测试
    if [[ "$sub" == 6 ]]; then
        # 对于复杂规则重度负载，增加JVM内存设置
        java -Xmx50G -Xms30G -cp "$FULL_CP" com.CC.CLIParser \
            -mode offline \
            -approach "$approach" \
            -rules "$rules_file" \
            -bfuncs "$resource_dir/Bfunction.class" \
            -patterns "$patterns_file" \
            -mfuncs "$resource_dir/Mfunction.class" \
            -data "$data_file" \
            -datatype rawData \
            -incs "$results_file"
    else
        java -cp "$FULL_CP" com.CC.CLIParser \
                -mode offline \
                -approach "$approach" \
                -rules "$rules_file" \
                -bfuncs "$resource_dir/Bfunction.class" \
                -patterns "$patterns_file" \
                -mfuncs "$resource_dir/Mfunction.class" \
                -data "$data_file" \
                -datatype rawData \
                -incs "$results_file"
    fi

    # 记录结束时间
    local end_time=$(date +%s%N)
    local end_time_display=$(date '+%Y-%m-%d %H:%M:%S')
    local execution_time=$(( (end_time - start_time) / 1000000 ))
    
    # 输出结束时间和执行时间
    echo "执行结束时间: $end_time_display"
    echo "检测执行时间: ${execution_time} ms"

    if [[ $? -ne 0 ]]; then
        echo "错误: UDPE程序执行失败"
        cleanup_test_functions "$resource_dir"
        return 1
    fi

    # echo "执行完成，用时: $execution_time ms"

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T${group}_${sub}_${method} 执行完成 (用时 ${execution_time} ms) ===\033[0m"
}

# 编译代码
run_compile() {
    echo "=== 编译项目代码 ==="

    # 初始化编译环境
    echo "初始化编译环境..."
    rm -rf "$BUILD_DIR"
    mkdir -p "$MAIN_DIR"

    echo "编译主代码..."
    # 编译主代码 - 递归查找所有Java文件
    find "$PROJECT_ROOT/src/main/java" -name "*.java" -print0 | xargs -0 javac \
      -cp "$LIBS_CP" \
      -d "$MAIN_DIR" \
      -encoding UTF-8

    if [[ $? -ne 0 ]]; then
        echo -e "\033[91m=== 主代码编译失败 ===\033[0m"
        exit 1
    fi

    echo -e "\033[92m=== 编译完成 ===\033[0m"
}

# 检查编译文件是否存在
check_compiled() {
    local main_classes=$(find "$MAIN_DIR" -name "*.class" 2>/dev/null | wc -l)

    if [[ $main_classes -eq 0 ]]; then
        echo "错误: 项目代码尚未编译，请先运行: $0 compile"
        exit 1
    fi
}

# 显示使用帮助
show_help() {
    echo "用法: $0 {compile|test <case_name>|stats [testcase]|clean|help}"
    echo ""
    echo "命令选项:"
    echo "  compile                     编译项目代码"
    echo "  test <case_name>            运行指定测试用例"
    echo "  stats [testcase]            统计工作负载数据（可选指定测试用例目录，如 T1_1）"
    echo "  clean                       清理编译生成的文件"
    echo "  help                        显示此帮助信息"
    echo ""
    echo "测试用例 (基于测试计划 1.2.2 Extended):"
    echo ""
    echo "【任务构建正确性测试 (T1组)】"
    echo "  T1_1_Baseline / T1_1_Fusion     轻量负载 + 基础规则"
    echo "  T1_2_Baseline / T1_2_Fusion     中度负载 + 基础规则"
    echo "  T1_3_Baseline / T1_3_Fusion     重度负载 + 基础规则"
    echo "  T1_4_Baseline / T1_4_Fusion     轻量负载 + 复杂规则"
    echo "  T1_5_Baseline / T1_5_Fusion     中度负载 + 复杂规则"
    echo "  T1_6_Baseline / T1_6_Fusion     重度负载 + 复杂规则"
    echo ""
    echo "【处理结果正确性测试 (T2组)】"
    echo "  T2_1_Baseline / T2_1_Fusion     轻量负载 + 基础规则"
    echo "  T2_2_Baseline / T2_2_Fusion     中度负载 + 基础规则"
    echo "  T2_3_Baseline / T2_3_Fusion     重度负载 + 基础规则"
    echo "  T2_4_Baseline / T2_4_Fusion     轻量负载 + 复杂规则"
    echo "  T2_5_Baseline / T2_5_Fusion     中度负载 + 复杂规则"
    echo "  T2_6_Baseline / T2_6_Fusion     重度负载 + 复杂规则"
    echo ""
    echo "【处理效率测试 (T3组)】"
    echo "  T3_1_Baseline / T3_1_Fusion     轻量负载 + 基础规则"
    echo "  T3_2_Baseline / T3_2_Fusion     中度负载 + 基础规则"
    echo "  T3_3_Baseline / T3_3_Fusion     重度负载 + 基础规则"
    echo "  T3_4_Baseline / T3_4_Fusion     轻量负载 + 复杂规则"
    echo "  T3_5_Baseline / T3_5_Fusion     中度负载 + 复杂规则"
    echo "  T3_6_Baseline / T3_6_Fusion     重度负载 + 复杂规则"
    echo ""
    echo "示例:"
    echo "  $0 compile                     # 编译项目代码"
    echo "  $0 test T1_1_Baseline          # 运行T1_1 Baseline测试"
    echo "  $0 test T2_4_Fusion            # 运行T2_4 Fusion测试"
    echo "  $0 stats                       # 统计所有基础负载数据"
    echo "  $0 stats T1_1                  # 统计T1_1测试用例的负载数据"
    echo "  $0 clean                       # 清理编译文件"
}

# 清理编译生成的文件
run_clean() {
    echo "=== 清理编译生成的文件 ==="

    # 清理主编译目录
    if [[ -d "$BUILD_DIR" ]]; then
        echo "删除编译目录: $BUILD_DIR"
        rm -rf "$BUILD_DIR"
    fi

    # 清理测试资源目录中的.class文件
    for test_dir in "$PROJECT_ROOT/src/test/resources/T"*; do
        if [[ -d "$test_dir" ]]; then
            echo "清理测试目录: $test_dir"
            find "$test_dir" -name "*.class" -delete 2>/dev/null || true
        fi
    done

    # 清理测试输出文件（保留oracle基准文件）
    echo "清理测试输出文件..."
    find "$PROJECT_ROOT/src/test/resources" -name "results.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "results_Baseline.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "results_Fusion.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "taskout_Baseline.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "taskout_Fusion.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "*.sorted" -delete 2>/dev/null || true

    echo -e "\033[92m=== 清理完成 ===\033[0m"
}

# 主执行逻辑
main() {
    local command="$1"
    local test_name="$2"

    case "$command" in
        "help"|"--help"|"-h"|"")
            show_help
            return 0
            ;;
        "compile")
            run_compile
            return 0
            ;;
        "clean")
            run_clean
            return 0
            ;;
        "test")
            # 检查是否提供了测试用例名
            if [[ -z "$test_name" ]]; then
                echo "错误: 请指定测试用例名称"
                echo ""
                show_help
                exit 1
            fi

            # 对于测试命令，先检查编译状态
            check_compiled

            # 解析测试名称（新格式：T<group>_<sub>_<method>）
            if [[ "$test_name" =~ ^T([1-3])_([1-6])_(Baseline|Fusion)$ ]]; then
                local group="${BASH_REMATCH[1]}"
                local sub="${BASH_REMATCH[2]}"
                local method="${BASH_REMATCH[3]}"
                
                # 根据 group 判断测试类型
                case "$group" in
                    1)
                        # 任务构建测试 (T1_1 到 T1_6)
                        run_task_construction_test "$group" "$sub" "$method"
                        ;;
                    2)
                        # 处理结果测试 (T2_1 到 T2_6)
                        run_result_correctness_test "$group" "$sub" "$method"
                        ;;
                    3)
                        # 效率测试 (T3_1 到 T3_6)
                        run_efficiency_test "$group" "$sub" "$method"
                        ;;
                esac
            else
                echo "错误: 无效的测试用例名称 '$test_name'"
                echo ""
                echo "正确格式: T<group>_<sub>_<method>"
                echo "  group: 1-3 (1=任务构建, 2=处理正确性, 3=效率测试)"
                echo "  sub: 1-6 (对应6种负载和规则组合)"
                echo "  method: Baseline 或 Fusion"
                echo ""
                echo "示例: T1_1_Baseline, T2_4_Fusion, T3_6_Baseline"
                exit 1
            fi

            if [[ $? -ne 0 ]]; then
                echo -e "\033[91m=== 测试执行失败 ===\033[0m"
                exit 1
            fi
            ;;
        stats)
            # 统计工作负载数据
            local testcase="$2"
            show_workload_stats "$testcase"
            ;;
        *)
            echo "错误: 未知的命令 '$command'"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"

