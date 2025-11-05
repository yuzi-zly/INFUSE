#!/bin/bash

# INFUSE项目不依赖Gradle的测试执行脚本
# 严格按照测试用例(TestCase1, TestCase2, TestCase3)的逻辑执行

set -e

# 项目根目录
PROJECT_ROOT=$(cd "$(dirname "$0")" && pwd)

# 编译输出目录
BUILD_DIR="$PROJECT_ROOT/build"
MAIN_DIR="$BUILD_DIR/classes/main"
TEST_DIR="$BUILD_DIR/classes/test"

# Classpath设置
LIBS_CP="$PROJECT_ROOT/libs/*"
MAIN_CP="$MAIN_DIR"
TEST_CP="$TEST_DIR"
FULL_CP="$LIBS_CP:$MAIN_CP:$TEST_CP"

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

# 公共函数：验证两个文件内容是否一致
validate_files_equal() {
    local file1="$1"
    local file2="$2"

    if [[ ! -f "$file1" ]]; then
        echo "错误: 文件不存在 $file1"
        return 1
    fi

    if [[ ! -f "$file2" ]]; then
        echo "错误: 文件不存在 $file2"
        return 1
    fi

    # 使用diff命令比较文件
    if diff -q "$file1" "$file2" >/dev/null 2>&1; then
        echo "验证通过: 文件内容一致"
        return 0
    else
        echo "验证失败: 文件内容不一致"
        echo "差异详情:"
        diff "$file1" "$file2" | head -20
        return 1
    fi
}

# 公共函数：运行INFUSE程序
run() {
    local approach="$1"
    local resource_dir="$2"
    local output_file="$3"
    local extra_args=("${@:4}")

    # 删除旧输出文件
    [[ -f "$output_file" ]] && rm -f "$output_file"

    # 构建命令参数
    local args=(
        "-mode" "offline"
        "-approach" "$approach"
        "-rules" "$resource_dir/rules.xml"
        "-bfuncs" "$resource_dir/Bfunction.class"
        "-patterns" "$resource_dir/patterns.xml"
        "-mfuncs" "$resource_dir/Mfunction.class"
        "-data" "$resource_dir/data.txt"
        "-datatype" "rawData"
        "-incs" "$resource_dir/result.txt"
        "${extra_args[@]}"
    )

    
    # 运行程序
    java -cp "$FULL_CP" com.CC.CLIParser "${args[@]}"

    if [[ $? -ne 0 ]]; then
        echo "错误: INFUSE程序执行失败"
        return 1
    fi
}

# TestCase1: T1_STATIC - 静态任务构建方法正确性测试
run_T1_STATIC() {
    echo "=== 测试T1_STATIC: 静态任务构建方法正确性 ==="

    local resource_dir="$PROJECT_ROOT/src/test/resources/testcase1"
    local output_file="$resource_dir/taskout_static.txt"
    local oracle_file="$resource_dir/oracle_taskout_static.txt"

    # 检查必要文件
    if [[ ! -f "$oracle_file" ]]; then
        echo "错误: Oracle文件不存在 $oracle_file"
        return 1
    fi

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 运行静态任务构建方法 (PCC+GEAS_ori)
    echo "运行静态任务构建方法..."
    run "PCC+GEAS_ori" "$resource_dir" "$output_file" "-taskOut" "$output_file" || return 1

    # 验证输出结果
    echo "验证静态任务构建方法输出结果..."
    validate_files_equal "$output_file" "$oracle_file" || return 1

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T1_STATIC 测试通过 ===\033[0m"
}

# TestCase1: T1_DYNAMIC - 动态任务构建方法正确性测试
run_T1_DYNAMIC() {
    echo "=== 测试T1_DYNAMIC: 动态任务构建方法正确性 ==="

    local resource_dir="$PROJECT_ROOT/src/test/resources/testcase1"
    local output_file="$resource_dir/taskout_dynamic.txt"
    local oracle_file="$resource_dir/oracle_taskout_dynamic.txt"

    # 检查必要文件
    if [[ ! -f "$oracle_file" ]]; then
        echo "错误: Oracle文件不存在 $oracle_file"
        return 1
    fi

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 运行动态任务构建方法 (INFUSE)
    echo "运行动态任务构建方法..."
    run "INFUSE" "$resource_dir" "$output_file" "-taskOut" "$output_file" || return 1

    # 验证输出结果
    echo "验证动态任务构建方法输出结果..."
    validate_files_equal "$output_file" "$oracle_file" || return 1

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T1_DYNAMIC 测试通过 ===\033[0m"
}

# TestCase2: T2_INCREMENTAL - 增量式泛在数据处理方法结果正确性测试
run_T2_INCREMENTAL() {
    echo "=== 测试T2_INCREMENTAL: 增量式泛在数据处理方法结果正确性 ==="

    local resource_dir="$PROJECT_ROOT/src/test/resources/testcase2"
    local output_file="$resource_dir/results_incremental.txt"
    local oracle_file="$resource_dir/results_oracle.txt"

    # 检查必要文件
    if [[ ! -f "$oracle_file" ]]; then
        echo "错误: Oracle文件不存在 $oracle_file"
        return 1
    fi

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 运行增量式方法 (PCC+IMD)
    echo "运行增量方法..."
    run "PCC+IMD" "$resource_dir" "$output_file" || return 1

    # 验证输出结果 (注意：输出文件应该是result.txt，但验证时使用results_incremental.txt)
    if [[ -f "$resource_dir/result.txt" ]]; then
        mv "$resource_dir/result.txt" "$output_file"
    fi

    echo "验证增量方法输出结果..."
    # 按照测试用例逻辑，先排序再比较
    sort "$oracle_file" > "${oracle_file}.sorted"
    sort "$output_file" > "${output_file}.sorted"
    validate_files_equal "${output_file}.sorted" "${oracle_file}.sorted" || return 1
    rm -f "${oracle_file}.sorted" "${output_file}.sorted"

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T2_INCREMENTAL 测试通过 ===\033[0m"
}

# TestCase2: T2_FUSION - 融合式泛在数据处理方法结果正确性测试
run_T2_FUSION() {
    echo "=== 测试T2_FUSION: 融合式泛在数据处理方法结果正确性 ==="

    local resource_dir="$PROJECT_ROOT/src/test/resources/testcase2"
    local output_file="$resource_dir/results_fusion.txt"
    local oracle_file="$resource_dir/results_oracle.txt"

    # 检查必要文件
    if [[ ! -f "$oracle_file" ]]; then
        echo "错误: Oracle文件不存在 $oracle_file"
        return 1
    fi

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 运行融合式方法 (INFUSE)
    echo "运行融合方法..."
    run "INFUSE" "$resource_dir" "$output_file" || return 1

    # 验证输出结果
    if [[ -f "$resource_dir/result.txt" ]]; then
        mv "$resource_dir/result.txt" "$output_file"
    fi

    echo "验证融合方法输出结果..."
    # 按照测试用例逻辑，先排序再比较
    sort "$oracle_file" > "${oracle_file}.sorted"
    sort "$output_file" > "${output_file}.sorted"
    validate_files_equal "${output_file}.sorted" "${oracle_file}.sorted" || return 1
    rm -f "${oracle_file}.sorted" "${output_file}.sorted"

    # 清理
    cleanup_test_functions "$resource_dir"

    echo -e "\033[92m=== T2_FUSION 测试通过 ===\033[0m"
}

# TestCase3性能测试 - 运行并测量执行时间
run_with_timing() {
    local approach="$1"
    local resource_dir="$2"
    local output_file="$3"

    # 删除旧输出文件
    [[ -f "$output_file" ]] && rm -f "$output_file"

    # 构建命令参数
    local args=(
        "-mode" "offline"
        "-approach" "$approach"
        "-rules" "$resource_dir/rules.xml"
        "-bfuncs" "$resource_dir/Bfunction.class"
        "-patterns" "$resource_dir/patterns.xml"
        "-mfuncs" "$resource_dir/Mfunction.class"
        "-data" "$resource_dir/data.txt"
        "-datatype" "rawData"
        "-incs" "$resource_dir/result.txt"
    )

    
    # 计时执行 - 使用nanosecond避免overflow
    local start_time=$(date +%s%N)  # 纳秒级时间戳
    java -cp "$FULL_CP" com.CC.CLIParser "${args[@]}"
    local end_time=$(date +%s%N)

    # 计算毫秒级执行时间
    local execution_time=$(( (end_time - start_time) / 1000000 ))

    if [[ $? -ne 0 ]]; then
        echo "错误: INFUSE程序执行失败"
        return 1
    fi

    # 重命名输出文件
    if [[ -f "$resource_dir/result.txt" ]]; then
        mv "$resource_dir/result.txt" "$output_file"
    fi

    echo "$execution_time"
}

# T3_PERFORMANCE_INCREMENTAL - 增量式泛在数据处理方法性能测试
run_T3_PERFORMANCE_INCREMENTAL() {
    echo "=== 测试T3_PERFORMANCE_INCREMENTAL: 增量式泛在数据处理方法性能 ==="

    local resource_dir="$PROJECT_ROOT/src/test/resources/testcase3"
    local baseline_result="$resource_dir/result_baseline.txt"
    local incremental_result="$resource_dir/result_incremental.txt"

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 运行基准方法 (ECC+IMD)
    echo "运行基准方法..."
    local baseline_time=$(run_with_timing "ECC+IMD" "$resource_dir" "$baseline_result")
    echo "基准方法执行时间: $baseline_time ms"

    # 运行增量方法 (PCC+IMD)
    echo "运行增量方法..."
    local incremental_time=$(run_with_timing "PCC+IMD" "$resource_dir" "$incremental_result")
    echo "增量方法执行时间: $incremental_time ms"

    # 计算性能提升
    local time_diff=$((baseline_time - incremental_time))
    local improvement=$(awk "BEGIN {printf \"%.4f\", ($baseline_time - $incremental_time) * 100 / $baseline_time}")

    echo "性能提升: $(printf "%.2f" "$improvement")%"
    echo "时间减少: $time_diff ms"

    # 验证性能提升是否达到50%的目标
    local improvement_threshold=50
    # 使用awk进行安全的小数比较
    local meets_threshold=$(awk "BEGIN {print ($improvement >= $improvement_threshold) ? 1 : 0}")

    if [[ $meets_threshold -eq 1 ]]; then
        echo "性能测试通过: 提升了 $(printf "%.2f" "$improvement")% (目标≥50%)"
        # 清理
        cleanup_test_functions "$resource_dir"
        echo -e "\033[92m=== T3_PERFORMANCE_INCREMENTAL 测试通过 ===\033[0m"
    else
        echo "性能测试未达标: 仅提升了 $(printf "%.2f" "$improvement")% (目标≥50%)"
        echo -e "\033[91m=== T3_PERFORMANCE_INCREMENTAL 测试失败 ===\033[0m"
        # 清理
        cleanup_test_functions "$resource_dir"
        return 1
    fi
}

# T3_PERFORMANCE_FUSION - 融合式泛在数据处理方法性能测试
run_T3_PERFORMANCE_FUSION() {
    echo "=== 测试T3_PERFORMANCE_FUSION: 融合式泛在数据处理方法性能 ==="

    local resource_dir="$PROJECT_ROOT/src/test/resources/testcase3"
    local baseline_result="$resource_dir/result_baseline.txt"
    local fusion_result="$resource_dir/result_fusion.txt"

    # 编译测试函数
    compile_test_functions "$resource_dir" || return 1

    # 运行基准方法 (ECC+IMD)
    echo "运行基准方法..."
    local baseline_time=$(run_with_timing "ECC+IMD" "$resource_dir" "$baseline_result")
    echo "基准方法执行时间: $baseline_time ms"

    # 运行融合方法 (INFUSE)
    echo "运行融合方法..."
    local fusion_time=$(run_with_timing "INFUSE" "$resource_dir" "$fusion_result")
    echo "融合方法执行时间: $fusion_time ms"

    # 计算性能提升
    local time_diff=$((baseline_time - fusion_time))
    local improvement=$(awk "BEGIN {printf \"%.4f\", ($baseline_time - $fusion_time) * 100 / $baseline_time}")

    echo "性能提升: $(printf "%.2f" "$improvement")%"
    echo "时间减少: $time_diff ms"

    # 验证性能提升是否达到50%的目标
    local improvement_threshold=50
    # 使用awk进行安全的小数比较
    local meets_threshold=$(awk "BEGIN {print ($improvement >= $improvement_threshold) ? 1 : 0}")

    if [[ $meets_threshold -eq 1 ]]; then
        echo "性能测试通过: 提升了 $(printf "%.2f" "$improvement")% (目标≥50%)"
        # 清理
        cleanup_test_functions "$resource_dir"
        echo -e "\033[92m=== T3_PERFORMANCE_FUSION 测试通过 ===\033[0m"
    else
        echo "性能测试未达标: 仅提升了 $(printf "%.2f" "$improvement")% (目标≥50%)"
        echo -e "\033[91m=== T3_PERFORMANCE_FUSION 测试失败 ===\033[0m"
        # 清理
        cleanup_test_functions "$resource_dir"
        return 1
    fi
}

# 编译代码
run_compile() {
    echo "=== 编译项目代码 ==="

    # 初始化编译环境
    echo "初始化编译环境..."
    rm -rf "$BUILD_DIR"
    mkdir -p "$MAIN_DIR" "$TEST_DIR"

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

    echo "编译测试代码..."
    # 编译测试代码
    find "$PROJECT_ROOT/src/test/java" -name "*.java" -print0 | xargs -0 javac \
      -cp "$FULL_CP" \
      -d "$TEST_DIR" \
      -encoding UTF-8

    if [[ $? -ne 0 ]]; then
        echo -e "\033[91m=== 测试代码编译失败 ===\033[0m"
        exit 1
    fi

    echo -e "=== 编译完成 ==="
}

# 检查编译文件是否存在
check_compiled() {
    local main_classes=$(find "$MAIN_DIR" -name "*.class" 2>/dev/null | wc -l)
    local test_classes=$(find "$TEST_DIR" -name "*.class" 2>/dev/null | wc -l)

    if [[ $main_classes -eq 0 ]] || [[ $test_classes -eq 0 ]]; then
        echo "错误: 项目代码尚未编译，请先运行: $0 compile"
        exit 1
    fi
}

# 显示使用帮助
show_help() {
    echo "用法: $0 {compile|test <case_name>|clean|help}"
    echo ""
    echo "命令选项:"
    echo "  compile                     编译项目代码"
    echo "  test <case_name>            运行指定测试用例"
    echo "  clean                        清理编译生成的文件"
    echo "  help                         显示此帮助信息"
    echo ""
    echo "测试用例:"
    echo "  T1_STATIC                    静态任务构建方法正确性测试"
    echo "  T1_DYNAMIC                   动态任务构建方法正确性测试"
    echo "  T2_INCREMENTAL               增量式泛在数据处理方法结果正确性测试"
    echo "  T2_FUSION                    融合式泛在数据处理方法结果正确性测试"
    echo "  T3_PERFORMANCE_INCREMENTAL   增量式性能测试"
    echo "  T3_PERFORMANCE_FUSION        融合式性能测试"
    echo ""
    echo "示例:"
    echo "  $0 compile                   # 编译项目代码"
    echo "  $0 test T1_STATIC            # 运行T1静态测试"
    echo "  $0 test T2_INCREMENTAL       # 运行T2增量测试"
    echo "  $0 clean                     # 清理编译文件"
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
    for test_dir in "$PROJECT_ROOT/src/test/resources/testcase"*; do
        if [[ -d "$test_dir" ]]; then
            echo "清理测试目录: $test_dir"
            find "$test_dir" -name "*.class" -delete 2>/dev/null || true
        fi
    done

    # 清理测试输出文件
    echo "清理测试输出文件..."
    # 只删除程序生成的临时输出文件，保留oracle基准文件
    find "$PROJECT_ROOT/src/test/resources" -name "result.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "result_baseline.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "result_incremental.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "result_fusion.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "taskout*.txt" -delete 2>/dev/null || true
    find "$PROJECT_ROOT/src/test/resources" -name "*.sorted" -delete 2>/dev/null || true

    echo "=== 清理完成 ==="
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
                echo "可用测试用例:"
                echo "  T1_STATIC, T1_DYNAMIC, T2_INCREMENTAL, T2_FUSION"
                echo "  T3_PERFORMANCE_INCREMENTAL, T3_PERFORMANCE_FUSION"
                echo ""
                echo "示例: $0 test T1_STATIC"
                exit 1
            fi

            # 对于测试命令，先检查编译状态
            check_compiled

            case "$test_name" in
                "T1_STATIC")
                    run_T1_STATIC
                    ;;
                "T1_DYNAMIC")
                    run_T1_DYNAMIC
                    ;;
                "T2_INCREMENTAL")
                    run_T2_INCREMENTAL
                    ;;
                "T2_FUSION")
                    run_T2_FUSION
                    ;;
                "T3_PERFORMANCE_INCREMENTAL")
                    run_T3_PERFORMANCE_INCREMENTAL
                    ;;
                "T3_PERFORMANCE_FUSION")
                    run_T3_PERFORMANCE_FUSION
                    ;;
                *)
                    echo "错误: 未知的测试用例 '$test_name'"
                    echo ""
                    echo "可用测试用例:"
                    echo "  T1_STATIC, T1_DYNAMIC, T2_INCREMENTAL, T2_FUSION"
                    echo "  T3_PERFORMANCE_INCREMENTAL, T3_PERFORMANCE_FUSION"
                    exit 1
                    ;;
            esac

            if [[ $? -ne 0 ]]; then
                echo -e "\033[91m=== 测试执行失败 ===\033[0m"
                exit 1
            fi
            ;;
        *)
            echo "错误: 未知的命令 '$command'"
            echo ""
            echo "可用命令: compile, test, clean, help"
            echo ""
            echo "示例:"
            echo "  $0 compile"
            echo "  $0 test T1_STATIC"
            echo "  $0 clean"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"