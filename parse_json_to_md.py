import json
import sys
import argparse

parser = argparse.ArgumentParser(
    description='Parse JSON to md array format')
parser.add_argument('--json_file_512', metavar='file', type=str,
                    help='File to read the json from', default=None)
parser.add_argument('--json_file_256', metavar='file', type=str,
                    help='File to read the json from', default=None)

# ARRAY_LENGTH > Benchmark SUFF > 512 & 256
# Array per length

args = parser.parse_args()


def humanbytes(B):
    'Return the given bytes as a human friendly KB, MB, GB, or TB string'
    B = float(B)
    KB = float(1024)
    MB = float(KB ** 2)  # 1,048,576
    GB = float(KB ** 3)  # 1,073,741,824
    TB = float(KB ** 4)  # 1,099,511,627,776

    if B < KB:
        return '{0} {1}'.format(B, 'Bytes' if 0 == B > 1 else 'Byte')
    elif KB <= B < MB:
        return '{0:.2f} KB'.format(B/KB)
    elif MB <= B < GB:
        return '{0:.2f} MB'.format(B/MB)
    elif GB <= B < TB:
        return '{0:.2f} GB'.format(B/GB)
    elif TB <= B:
        return '{0:.2f} TB'.format(B/TB)


def convert_dict(benchmark_file_512, benchmark_file_256):
    benchmark_list_512 = json.loads(
        benchmark_file_512.read()) if benchmark_file_512 else []
    benchmark_list_256 = json.loads(
        benchmark_file_256.read()) if benchmark_file_256 else []

    bench_results = {}

    for benchmark in benchmark_list_512:
        suffix = benchmark['benchmark'].split('.')[-1]
        array_length = benchmark['params']['ARRAY_LENGTH']
        bench_results[array_length] = bench_results.get(array_length, {})
        bench_results[array_length][suffix] = bench_results[array_length].get(
            suffix, {})
        bench_results[array_length][suffix] = benchmark['primaryMetric']['score']

    for benchmark in benchmark_list_256:
        suffix = benchmark['benchmark'].split('.')[-1]
        array_length = benchmark['params']['ARRAY_LENGTH']
        previous = bench_results[array_length][suffix]
        bench_results[array_length][suffix] = previous, benchmark['primaryMetric']['score']

    return bench_results


file_512 = open(args.json_file_512, 'r') if args.json_file_512 else None
file_256 = open(args.json_file_256, 'r') if args.json_file_256 else None

bench_results = convert_dict(file_512, file_256)

if file_512 and file_256:
    for arr_length in sorted(bench_results.keys(), key=lambda length: int(length)):
        print("## {}".format(humanbytes(int(arr_length)*4)))
        print("| Benchmark | ns/op(SIMD512) | ns/op(SIMD256) |")
        print("| -------- | -------- | -------- |")

        for bench in sorted(bench_results[arr_length].keys()):
            print("| {} | {:.2f} | {:.2f} |".format(
                bench, *bench_results[arr_length][bench]))
else:
    for arr_length in sorted(bench_results.keys(), key=lambda length: int(length)):
        print("## {}".format(humanbytes(int(arr_length)*4)))
        print("| Benchmark | ns/op({}) |".format("SIMD512" if args.json_file_512 else "SIMD256"))
        print("| -------- | -------- |")

        for bench in sorted(bench_results[arr_length].keys()):
            print("| {} | {:.2f} |".format(
                bench, bench_results[arr_length][bench]))
