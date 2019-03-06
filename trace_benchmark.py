import json
import sys
import itertools
import math
from matplotlib import pyplot as plt
import matplotlib
import argparse


def keep_common_x(base_list, comparison_list):
    new_base_list = []
    new_comparison_list = []

    comparison_list_x = [i[0] for i in comparison_list]
    base_list_x = [i[0] for i in base_list]

    new_base_list = [(x, y) for x, y in base_list if x in comparison_list_x]
    new_comparison_list = [(x, y)
                           for x, y in comparison_list if x in base_list_x]
    return new_base_list, new_comparison_list


if __name__ == "__main__":

    parser = argparse.ArgumentParser(
        description='Trace graphs for SIMD benchmark from JMH json output.')
    parser.add_argument('json_file', metavar='file', type=str,
                        help='File to read the json from')

    parser.add_argument('--json_file_comparison', metavar='file', type=str,
                        help='Optional file to read from, used for the comparison function',
                        default=False)

    parser.add_argument(
        '--baseSuffix', help='Regular or RegularNoSuperWord', default='RegularNoSuperWord')
    parser.add_argument(
        '--comparisonSuffix', help='SIMD or GroupedSIMD', default='SIMD')

    parser.add_argument(
        '--prefix', help='fr.centralesupelec.simd.VectorProfiling.', default='')

    parser.add_argument(
        '--prefix_comparison', help='fr.centralesupelec.simd.VectorProfiling.', default='')

    parser.add_argument(
        '--functions', help='functions to compare, separated by spaces (such as mul sum add filterSum \n filterSum filterAnd2 filterAnd4 filterOr2 filterOr4 filter', default='mul sum add filterSum', nargs='+')

    args = parser.parse_args()

    res_list = {}

    with open(args.json_file, 'r') as file:
        res_json = file.read()
        res_list = json.loads(res_json)

    bench_results = {}

    for benchmark in res_list:
        if benchmark['benchmark'] not in bench_results:
            bench_results[benchmark['benchmark']] = [], []
        # axis
        bench_results[benchmark['benchmark']][0].append(
            benchmark['params']['ARRAY_LENGTH'])  # axis
        bench_results[benchmark['benchmark']][1].append(
            benchmark['primaryMetric']['score'])  # ordonee

    if args.json_file_comparison:
        bench_results_comparison = {}
        with open(args.json_file_comparison, 'r') as file:
            res_json = file.read()
            res_list = json.loads(res_json)
            for benchmark in res_list:
                if benchmark['benchmark'] not in bench_results_comparison:
                    bench_results_comparison[benchmark['benchmark']] = [], []
                # axis
                bench_results_comparison[benchmark['benchmark']][0].append(
                    benchmark['params']['ARRAY_LENGTH'])  # axis
                bench_results_comparison[benchmark['benchmark']][1].append(
                    benchmark['primaryMetric']['score'])  # ordonee

    bench_results_ratio = {}
    for operation in args.functions:
        prefix = args.prefix + operation

        base_list = list(zip(
            *bench_results[prefix + args.baseSuffix]
        ))

        base_list.sort(key=lambda res: int(res[0]))

        if args.json_file_comparison:
            comparison_list = list(zip(
                *bench_results_comparison[args.prefix_comparison + operation + args.comparisonSuffix]
            ))

        else:
            comparison_list = list(zip(
                *bench_results[prefix + args.comparisonSuffix]
            ))

        comparison_list.sort(key=lambda res: int(res[0]))

        base_list, comparison_list = keep_common_x(base_list, comparison_list)

        ratio_list = [comparison_list[i][1] / base_list[i][1]
                      for i in range(len(comparison_list))]

        bench_results_ratio[operation] = [
            int(x[0]) for x in comparison_list], ratio_list

    colors = itertools.cycle(["r", "b", "g", "violet", "orange", "yellow"])

    plt.figure(num=None, figsize=(16, 6), dpi=80,
               facecolor='w', edgecolor='k')
    plt.subplot(1, 1, 1)
    plt.xscale("log", basex=2)
    for bench in bench_results_ratio.keys():
        plt.plot(
            bench_results_ratio[bench][0], bench_results_ratio[bench][1], label=bench, color=next(colors), linestyle='--', marker='o')
    plt.legend()
    plt.title(
        "Ratio of time per operation {} / {}".format(args.comparisonSuffix, args.baseSuffix))
    plt.xlabel("Int Array Size")
    plt.savefig("graph-{}-{}".format(args.comparisonSuffix,
                                     args.baseSuffix), quality=10)
    plt.show()
