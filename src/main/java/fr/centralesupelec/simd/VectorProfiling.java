package fr.centralesupelec.simd;

import jdk.incubator.vector.*;
import jdk.incubator.vector.Vector.*;
import org.openjdk.jmh.annotations.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(jvmArgsAppend = {"--add-modules", "jdk.incubator.vector"}, value = 2)
@BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
public class VectorProfiling {

    @State(Scope.Thread)
    public static class VectorState {

        @Param({"512", "1024", "2048", "4096", "8192", "16384", "32768", "65536", "131072", "262144", "524288", "1048576", "2097152", "4194304", "8388608", "16777216", "33554432", "67108864"})
        public int ARRAY_LENGTH;

        private static final IntVector.IntSpecies<?> sInt = IntVector.preferredSpecies();
        private static final int vecLength = sInt.length();
        public static final int bitSize = sInt.bitSize();
        int[] a;
        int[] b;
        int[] c;
        int filter;

        @Setup(Level.Trial)
        public void doSetup() {
            a = new int[ARRAY_LENGTH];
            b = new int[ARRAY_LENGTH];
            c = new int[ARRAY_LENGTH];

            Random rnd = new Random();
            for (int i = 0; i < a.length; i++) {
                a[i] = rnd.nextInt();
                b[i] = rnd.nextInt();
            }
            filter = Integer.MAX_VALUE / 2 + rnd.nextInt(1000000) - 500000;
        }
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public long sumSIMD(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.a.length; i += VectorState.vecLength) {
            sum += VectorState.sInt.fromArray(state.a, i).addAll();
        }
        return sum;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public long sumRegular(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.a.length; ++i) {
            sum += state.a[i];
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public long sumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.a.length; ++i) {
            sum += state.a[i];
        }
        return sum;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulSIMD(VectorState state) {
        for(int i = 0; i < state.a.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.a, i);
            IntVector vb = VectorState.sInt.fromArray(state.b, i);
            IntVector vc = va.mul(vb);
            vc.intoArray(state.c, i);
        }
        return state.c;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulRegular(VectorState state) {
        for(int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] * state.b[i];
        }
        return state.c;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] mulRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] * state.b[i];
        }
        return state.c;
    }

    @Benchmark
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] addSIMD(VectorState state) {
        for (int i = 0; i < state.a.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.a, i);
            IntVector vb = VectorState.sInt.fromArray(state.b, i);
            IntVector vc = va.add(vb);
            vc.intoArray(state.c, i);
        }
        return state.c;
    }

    @Benchmark
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] addRegular(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] + state.b[i];
        }
        return state.c;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] addRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] + state.b[i];
        }
        return state.c;
    }

    @Benchmark
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public long filterSumSIMD(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.a.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.a, i);
            Mask mask = va.lessThanEq(state.filter);
            sum += VectorState.sInt.fromArray(state.b, i).addAll(mask);
        }
        return sum;
    }

    @Benchmark
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public long filterSumRegular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.a.length; ++i) {
            if(state.a[i] <= state.filter) {
                sum += state.b[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public long filterSumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.a.length; ++i) {
            if(state.a[i] <= state.filter) {
                sum += state.b[i];
            }
        }
        return sum;
    }
}
