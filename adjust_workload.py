#!/usr/bin/env python3
"""
负载数据调整脚本
根据数据条数和平均时间间隔要求，从原始负载文件中智能挑选数据

用法示例：
    python3 adjust_workload.py -i src/test/light_workload.txt -o src/test/light_workload_adjusted.txt --max-lines 50000 --min-interval 50
"""

import json
import argparse
from pathlib import Path
from datetime import datetime
from typing import List, Tuple


def parse_timestamp(timestamp_str: str) -> datetime:
    """解析时间戳字符串为 datetime 对象"""
    return datetime.strptime(timestamp_str, "%Y-%m-%d %H:%M:%S:%f")


def calculate_interval_ms(ts1: datetime, ts2: datetime) -> float:
    """计算两个时间戳之间的间隔（毫秒）"""
    delta = ts2 - ts1
    return delta.total_seconds() * 1000


def analyze_workload(input_file: str) -> Tuple[int, float, datetime, datetime]:
    """
    分析负载文件的基本信息
    
    Returns:
        (总行数, 平均间隔ms, 开始时间, 结束时间)
    """
    print(f"正在分析文件: {input_file}")
    
    total_lines = 0
    first_timestamp = None
    last_timestamp = None
    
    with open(input_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            try:
                data = json.loads(line)
                timestamp = parse_timestamp(data['timestamp'])
                
                if first_timestamp is None:
                    first_timestamp = timestamp
                last_timestamp = timestamp
                total_lines += 1
                
            except Exception as e:
                print(f"警告: 解析行失败 - {e}")
                continue
    
    # 计算平均间隔
    if total_lines > 1 and first_timestamp and last_timestamp:
        total_time_ms = calculate_interval_ms(first_timestamp, last_timestamp)
        avg_interval = total_time_ms / (total_lines - 1)
    else:
        avg_interval = 0
    
    return total_lines, avg_interval, first_timestamp, last_timestamp


def adjust_workload(input_file: str, output_file: str, max_lines: int, min_avg_interval: float):
    """
    调整负载数据
    
    Args:
        input_file: 输入文件路径
        output_file: 输出文件路径
        max_lines: 最大数据条数
        min_avg_interval: 最小平均间隔（毫秒）
    """
    print("=" * 70)
    print("负载数据调整工具")
    print("=" * 70)
    print()
    
    # 分析原始文件
    total_lines, current_avg_interval, start_time, end_time = analyze_workload(input_file)
    
    print(f"原始文件分析结果:")
    print(f"  文件路径: {input_file}")
    print(f"  数据条数: {total_lines:,} 条")
    print(f"  平均间隔: {current_avg_interval:.2f} ms")
    print(f"  开始时间: {start_time}")
    print(f"  结束时间: {end_time}")
    print()
    
    # 计算采样策略
    if total_lines <= max_lines and current_avg_interval >= min_avg_interval:
        print("✓ 原始文件已满足要求，无需调整")
        print(f"  - 数据条数 {total_lines} <= {max_lines}")
        print(f"  - 平均间隔 {current_avg_interval:.2f} >= {min_avg_interval}")
        return
    
    print(f"调整目标:")
    print(f"  最大数据条数: {max_lines:,} 条")
    print(f"  最小平均间隔: {min_avg_interval:.2f} ms")
    print()
    
    # 计算需要的采样率
    # 基于数据条数的采样率
    sampling_rate_by_count = max_lines / total_lines
    
    # 基于时间间隔的采样率
    sampling_rate_by_interval = current_avg_interval / min_avg_interval if min_avg_interval > 0 else 1.0
    
    # 选择更严格的采样率（更小的值）
    sampling_rate = min(sampling_rate_by_count, sampling_rate_by_interval, 1.0)
    
    print(f"采样策略:")
    print(f"  基于数据条数的采样率: {sampling_rate_by_count:.4f}")
    print(f"  基于时间间隔的采样率: {sampling_rate_by_interval:.4f}")
    print(f"  最终采样率: {sampling_rate:.4f}")
    print(f"  预计输出: {int(total_lines * sampling_rate):,} 条")
    print()
    
    # 执行采样
    print(f"正在处理数据...")
    selected_count = 0
    sampling_step = 1.0 / sampling_rate
    next_index = 0.0
    
    selected_lines = []
    
    with open(input_file, 'r', encoding='utf-8') as f:
        current_index = 0
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            # 判断是否选择当前行
            if current_index >= int(next_index):
                selected_lines.append(line)
                selected_count += 1
                next_index += sampling_step
                
                if selected_count % 10000 == 0:
                    print(f"  已处理 {current_index:,} 行，已选择 {selected_count:,} 行")
                
                # 如果已达到最大行数，停止
                if selected_count >= max_lines:
                    break
            
            current_index += 1
    
    print(f"  处理完成，共选择 {selected_count:,} 行")
    print()
    
    # 写入输出文件
    print(f"正在写入输出文件: {output_file}")
    with open(output_file, 'w', encoding='utf-8') as f:
        for line in selected_lines:
            f.write(line + '\n')
    
    print(f"✓ 输出文件已创建")
    print()
    
    # 分析输出文件
    print("验证输出文件...")
    output_total, output_avg_interval, output_start, output_end = analyze_workload(output_file)
    
    print(f"输出文件分析结果:")
    print(f"  文件路径: {output_file}")
    print(f"  数据条数: {output_total:,} 条")
    print(f"  平均间隔: {output_avg_interval:.2f} ms")
    print(f"  开始时间: {output_start}")
    print(f"  结束时间: {output_end}")
    print()
    
    # 检查是否满足要求
    print("要求检查:")
    count_ok = output_total <= max_lines
    interval_ok = output_avg_interval >= min_avg_interval
    
    print(f"  ✓ 数据条数: {output_total:,} {'<=' if count_ok else '>'} {max_lines:,} {'✓' if count_ok else '✗'}")
    print(f"  ✓ 平均间隔: {output_avg_interval:.2f} {'>=' if interval_ok else '<'} {min_avg_interval:.2f} {'✓' if interval_ok else '✗'}")
    print()
    
    if count_ok and interval_ok:
        print("✓ 所有要求均已满足！")
    else:
        print("⚠ 警告: 部分要求未满足，可能需要调整参数")
    
    print()
    print("=" * 70)
    print("调整完成！")
    print("=" * 70)


def main():
    parser = argparse.ArgumentParser(
        description='负载数据调整工具 - 根据数据条数和平均时间间隔要求调整负载数据',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 调整轻量负载：数据条数 < 50000，平均间隔 > 50ms
  python3 adjust_workload.py -i src/test/light_workload.txt -o src/test/light_workload_adjusted.txt --max-lines 50000 --min-interval 50
  
  # 仅分析文件（不生成输出）
  python3 adjust_workload.py -i src/test/light_workload.txt --analyze-only
  
  # 调整中量负载：数据条数 < 100000，平均间隔 > 30ms
  python3 adjust_workload.py -i src/test/median_workload.txt -o src/test/median_workload_adjusted.txt --max-lines 100000 --min-interval 30
        """
    )
    
    parser.add_argument(
        '-i', '--input',
        required=True,
        help='输入负载文件路径'
    )
    
    parser.add_argument(
        '-o', '--output',
        help='输出负载文件路径（默认：在输入文件名后添加 _adjusted）'
    )
    
    parser.add_argument(
        '--max-lines',
        type=int,
        help='最大数据条数'
    )
    
    parser.add_argument(
        '--min-interval',
        type=float,
        help='最小平均间隔（毫秒）'
    )
    
    parser.add_argument(
        '--analyze-only',
        action='store_true',
        help='仅分析文件，不生成输出'
    )
    
    args = parser.parse_args()
    
    # 检查输入文件
    input_path = Path(args.input)
    if not input_path.exists():
        print(f"错误: 输入文件不存在: {args.input}")
        return 1
    
    # 仅分析模式
    if args.analyze_only:
        print("=" * 70)
        print("负载数据分析工具（仅分析模式）")
        print("=" * 70)
        print()
        total_lines, avg_interval, start_time, end_time = analyze_workload(args.input)
        print()
        print(f"分析结果:")
        print(f"  文件路径: {args.input}")
        print(f"  数据条数: {total_lines:,} 条")
        print(f"  平均间隔: {avg_interval:.2f} ms")
        print(f"  开始时间: {start_time}")
        print(f"  结束时间: {end_time}")
        total_time_ms = calculate_interval_ms(start_time, end_time)
        print(f"  总时长: {total_time_ms / 1000:.2f} 秒")
        print()
        return 0
    
    # 检查必需参数
    if args.max_lines is None and args.min_interval is None:
        print("错误: 必须指定 --max-lines 和/或 --min-interval")
        return 1
    
    # 设置默认值
    max_lines = args.max_lines if args.max_lines else float('inf')
    min_interval = args.min_interval if args.min_interval else 0
    
    # 确定输出文件路径
    if args.output:
        output_path = args.output
    else:
        # 默认在输入文件名后添加 _adjusted
        output_path = str(input_path.parent / (input_path.stem + '_adjusted' + input_path.suffix))
    
    # 检查输出文件是否与输入文件相同
    if Path(output_path).resolve() == input_path.resolve():
        print("错误: 输出文件不能与输入文件相同（为保护原始文件）")
        return 1
    
    # 执行调整
    adjust_workload(args.input, output_path, max_lines, min_interval)
    
    return 0


if __name__ == "__main__":
    exit(main())
