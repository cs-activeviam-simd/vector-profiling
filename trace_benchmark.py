import json
import sys
import itertools
import math
from matplotlib import pyplot as plt
import matplotlib
import argparse


if __name__ == "__main__":

    parser = argparse.ArgumentParser(
        description='Trace graphs for SIMD benchmark from JMH json output.')
    parser.add_argument('json_file', metavar='file', type=str,
                        help='File to read the json from')
    parser.add_argument(
        '--baseSuffix', help='Regular or RegularNoSuperWord', default='RegularNoSuperWord')
    parser.add_argument(
        '--comparisonSuffix', help='SIMD or GroupedSIMD', default='SIMD')

    parser.add_argument(
        '--functions', help='functions to compare, separated by spaces (such as mul sum add filterSum \n filterSum filterAnd2 filterAnd4 filterOr2 filterOr4 filter', default='mul sum add filterSum', nargs='+')

    args = parser.parse_args()

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

        bench_results_ratio = {}
        # operations = ['mul', 'sum', 'add', 'filterSum']
        operations = ['filterSum', 'filterAnd2',
                      'filterAnd4', 'filterOr2', 'filterOr4', 'filter']
        # operations = ['filterSum']
        for operation in args.functions:
            prefix = 'fr.centralesupelec.simd.VectorProfiling.' + operation

            regular_list = list(zip(
                *bench_results[prefix + args.baseSuffix]
            ))

            regular_list.sort(key=lambda res: int(res[0]))

            simd_list = list(zip(
                *bench_results[prefix + args.comparisonSuffix]
            ))
            simd_list.sort(key=lambda res: int(res[0]))

            ratio_list = [regular_list[i][1] / simd_list[i][1]
                          for i in range(len(simd_list))]

            bench_results_ratio[operation] = [
                int(x[0]) for x in simd_list], ratio_list

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
