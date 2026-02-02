#!/bin/bash

# UDPE项目自动化打包脚本
# 将src、libs、run_tests.sh、parallel_run.sh以及测试文档打包成UDPE_test.zip
#
# 用法:
#   ./package_udpe.sh                      # 默认不包含JUnit测试类
#   ./package_udpe.sh --include-junit-tests # 包含JUnit测试类

set -e

# 配置参数
INCLUDE_JUNIT_TESTS=false

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查文件是否存在
check_file() {
    if [ ! -f "$1" ]; then
        print_error "文件不存在: $1"
        return 1
    fi
}

# 检查目录是否存在
check_dir() {
    if [ ! -d "$1" ]; then
        print_error "目录不存在: $1"
        return 1
    fi
}

# 清理旧的打包文件
cleanup_old() {
    print_info "清理旧的打包文件..."

    # 删除旧的UDPE目录
    if [ -d "UDPE" ]; then
        rm -rf UDPE
        print_info "已删除旧的UDPE目录"
    fi

    # 删除旧的zip文件
    if [ -f "UDPE_test.zip" ]; then
        rm -f UDPE_test.zip
        print_info "已删除旧的UDPE_test.zip文件"
    fi

    print_success "清理完成"
}

# 创建目录结构
create_structure() {
    print_info "创建UDPE目录结构..."

    mkdir -p UDPE
    print_success "UDPE目录创建完成"
}

# 复制必要文件
copy_files() {
    print_info "复制项目文件..."

    # 复制src目录
    if [ -d "src" ]; then
        cp -r src UDPE/
        print_success "✓ src目录复制完成"
        
        # 如果不包含JUnit测试，则删除test/java目录
        if [ "$INCLUDE_JUNIT_TESTS" = false ]; then
            if [ -d "UDPE/src/test/java" ]; then
                print_info "移除JUnit测试类目录 (src/test/java)..."
                rm -rf UDPE/src/test/java
                print_success "✓ 已移除 src/test/java 目录"
            fi
        else
            print_info "✓ 保留JUnit测试类文件"
        fi
        
        # 删除 src/test/assets 目录（不需要打包）
        if [ -d "UDPE/src/test/assets" ]; then
            print_info "移除测试资源源文件 (src/test/assets)..."
            rm -rf UDPE/src/test/assets
            print_success "✓ 已移除 src/test/assets 目录"
        fi
    else
        print_error "src目录不存在"
        return 1
    fi

    # 复制libs目录
    if [ -d "libs" ]; then
        cp -r libs UDPE/
        print_success "✓ libs目录复制完成"
    else
        print_error "libs目录不存在"
        return 1
    fi

    # 复制run_tests.sh脚本
    if [ -f "run_tests.sh" ]; then
        cp run_tests.sh UDPE/
        # 确保脚本有执行权限
        chmod +x UDPE/run_tests.sh
        print_success "✓ run_tests.sh脚本复制完成"
    else
        print_error "run_tests.sh脚本不存在"
        return 1
    fi

    # 复制parallel_run.sh脚本
    if [ -f "parallel_run.sh" ]; then
        cp parallel_run.sh UDPE/
        # 确保脚本有执行权限
        chmod +x UDPE/parallel_run.sh
        print_success "✓ parallel_run.sh脚本复制完成"
    else
        print_warning "parallel_run.sh脚本不存在，跳过"
    fi

    print_success "所有项目文件复制完成"
}

# 验证复制结果
verify_package() {
    print_info "验证打包内容..."

    local errors=0

    # 检查UDPE目录结构
    if [ ! -d "UDPE/src" ]; then
        print_error "UDPE/src目录缺失"
        ((errors++))
    fi

    if [ ! -d "UDPE/libs" ]; then
        print_error "UDPE/libs目录缺失"
        ((errors++))
    fi

    if [ ! -f "UDPE/run_tests.sh" ]; then
        print_error "UDPE/run_tests.sh文件缺失"
        ((errors++))
    fi

    if [ ! -f "UDPE/parallel_run.sh" ]; then
        print_warning "UDPE/parallel_run.sh文件缺失（可选）"
    fi

    if [ $errors -eq 0 ]; then
        print_success "打包内容验证通过"
        return 0
    else
        print_error "发现 $errors 个错误"
        return 1
    fi
}

# 创建压缩包
create_zip() {
    print_info "创建UDPE_test.zip压缩包..."

    # 仅打包UDPE目录（包含代码和测试脚本）
    local zip_items="UDPE"

    # 创建zip文件
    zip -r UDPE_test.zip $zip_items

    if [ -f "UDPE_test.zip" ]; then
        local size=$(ls -lh UDPE_test.zip | awk '{print $5}')
        print_success "压缩包创建完成: UDPE_test.zip (大小: $size)"
        return 0
    else
        print_error "压缩包创建失败"
        return 1
    fi
}

# 显示打包结果
show_result() {
    print_info "打包结果详情:"
    echo "----------------------------------------"
    
    # 显示打包配置
    echo "📋 打包配置:"
    if [ "$INCLUDE_JUNIT_TESTS" = true ]; then
        echo "  - JUnit测试类: ✓ 已包含"
    else
        echo "  - JUnit测试类: ✗ 已排除"
    fi
    echo "  - 测试资源文件: ✓ 已包含 (src/test/resources)"
    echo "  - 测试源文件: ✗ 已排除 (src/test/assets)"
    echo "  - 测试脚本: run_tests.sh"
    if [ -f "UDPE/parallel_run.sh" ]; then
        echo "  - 并行测试脚本: parallel_run.sh ✓"
    fi
    echo ""

    if [ -d "UDPE" ]; then
        echo "📁 UDPE目录内容:"
        tree UDPE -L 3 2>/dev/null || find UDPE -type d | head -10
        echo ""
    fi

    if [ -f "UDPE_test.zip" ]; then
        echo "📦 压缩包信息:"
        ls -lh UDPE_test.zip
        echo ""

        echo "📋 压缩包内容:"
        unzip -l UDPE_test.zip | head -20
        echo "        ..."
        echo ""
    fi

    echo "✅ 打包完成！文件已保存为: UDPE_test.zip"
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --include-junit-tests    包含JUnit测试类文件 (TestCase*.java)"
    echo "  --help, -h               显示此帮助信息"
    echo ""
    echo "说明:"
    echo "  默认情况下，打包时不包含JUnit测试类文件。"
    echo "  如果需要包含这些文件，请使用 --include-junit-tests 参数。"
    echo "  自动打包 run_tests.sh 和 parallel_run.sh 脚本（如果存在）。"
    echo ""
    echo "示例:"
    echo "  $0                          # 默认打包（不包含JUnit测试）"
    echo "  $0 --include-junit-tests    # 打包并包含JUnit测试类"
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --include-junit-tests)
                INCLUDE_JUNIT_TESTS=true
                print_info "将包含JUnit测试类文件"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                print_error "未知参数: $1"
                echo ""
                show_help
                exit 1
                ;;
        esac
    done
}

# 主函数
main() {
    # 解析命令行参数
    parse_args "$@"
    
    echo "======================================"
    echo "    UDPE项目自动化打包脚本"
    echo "======================================"
    echo ""

    # 记录开始时间
    local start_time=$(date +%s)

    # 执行打包流程
    cleanup_old
    create_structure
    copy_files
    verify_package
    create_zip
    show_result

    # 计算耗时
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    echo "======================================"
    print_success "打包完成！总耗时: ${duration}秒"
    echo "======================================"
}

# 错误处理
trap 'print_error "脚本执行过程中发生错误，退出码: $?"' ERR

# 运行主函数
main "$@"