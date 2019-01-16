import json
import sys
import itertools
import math
from matplotlib import pyplot as plt
import matplotlib

if __name__ == "__main__":
    with open(sys.argv[1], 'r') as file:
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
        operations = ['mul', 'sum', 'add', 'filterSum']
        for operation in operations:
            prefix = 'fr.centralesupelec.simd.VectorProfiling.' + operation

            # regular_list = list(zip(
            #     *bench_results[prefix +
            #                    'RegularNoSuperWord']
            # ))

            regular_list = list(zip(
                *bench_results[prefix +
                               'Regular']
            ))

            regular_list.sort(key=lambda res: int(res[0]))

            simd_list = list(zip(
                *bench_results[prefix +
                               'SIMD']
            ))
            simd_list.sort(key=lambda res: int(res[0]))
            ratio_list = [regular_list[i][1] / simd_list[i][1]
                          for i in range(len(simd_list))]

            bench_results_ratio[operation] = [int(x[0])
                                              for x in simd_list], ratio_list

        # log_axis = {}
        # for bench in bench_results_ratio.keys():
        #     log_axis[bench] = [math.log(array_size)
        #                        for array_size in bench_results_ratio[bench][0]]

        colors = itertools.cycle(["r", "b", "g", "violet"])

        plt.figure(num=None, figsize=(16, 6), dpi=80,
                   facecolor='w', edgecolor='k')
        plt.subplot(1, 1, 1)
        plt.xscale("log", basex=2)
        for bench in bench_results_ratio.keys():
            print(bench_results_ratio[bench])
            plt.plot(
                bench_results_ratio[bench][0], bench_results_ratio[bench][1], label=bench, color=next(colors), linestyle='--', marker='o')
        plt.legend()
        plt.title("Ratio of time per operation Regular / SIMD")
        plt.xlabel("Int Array Size")
        plt.savefig("graph-reg-simd", quality=10)
        # plt.show()
