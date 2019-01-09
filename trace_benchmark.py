import json
import itertools
from matplotlib import pyplot as plt

if __name__ == "__main__":
    with open("bench_res.json", 'r') as file:
        res_json = file.read()
        res_list = json.loads(res_json)

        bench_results = {}

        for benchmark in res_list:
            if benchmark['benchmark'] not in bench_results:
                bench_results[benchmark['benchmark']] = [], []
            # axis
            bench_results[benchmark['benchmark']][0].append(
                benchmark['params']['array_size'])  # axis
            bench_results[benchmark['benchmark']][1].append(
                benchmark['primaryMetric']['score'])  # ordonee

        bench_results_ratio = {}
        for operation in ['mul', 'sum', 'add']:
            prefix = 'fr.centralesupelec.simd.VectorProfiling.' + operation

            regular_list = list(zip(
                *bench_results[prefix +
                               'RegularNoSuperWord']
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

        colors = itertools.cycle(["r", "b", "g"])
        plt.subplot(2, 1, 1)
        for bench in bench_results_ratio.keys():
            plt.plot(
                bench_results_ratio[bench][0], bench_results_ratio[bench][1], label=bench, color=next(colors))
        plt.legend()
        plt.title("Ration of time per operation RegularNoSuperWord / SIMD")
        plt.xlabel("Int Array Size")
        plt.subplot(2, 1, 2)
        for bench in bench_results_ratio.keys():
            plt.plot(
                bench_results_ratio[bench][0][:7], bench_results_ratio[bench][1][:7], label=bench, color=next(colors))
        plt.legend()
        plt.xlabel("Int Array Size")
        plt.show()
