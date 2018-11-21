package fr.centralesupelec.simd;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;
import java.util.Random;

@Fork(jvmArgsAppend = {"--add-modules", "jdk.incubator.vector"})
public class VectorProfiling {

    @State(Scope.Thread)
    public static class VectorState {
        private static final IntVector.IntSpecies<?> sInt = IntVector.preferredSpecies();
        private static final int vecLength = sInt.length();
        public static final int bitSize = sInt.bitSize();
        int[] data = new int[4096];
        int[] datb = new int[4096];
        int[] datc = new int[4096];

        @Setup(Level.Trial)
        public void doSetup() {
            System.out.println(sInt.bitSize());
            Random rnd = new Random();
            for (int i = 0; i < data.length; i++) {
                data[i] = rnd.nextInt();
                datb[i] = rnd.nextInt();
            }
        }
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public long sumSIMD(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.data.length; i += VectorState.vecLength) {
            sum += VectorState.sInt.fromArray(state.data, i).addAll();
        }
        return sum;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public long sumRegular(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.data.length; ++i) {
            sum += state.data[i];
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public long sumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.data.length; ++i) {
            sum += state.data[i];
        }
        return sum;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulSIMD(VectorState state) {
        for(int i = 0; i < state.data.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.data, i);
            IntVector vb = VectorState.sInt.fromArray(state.datb, i);
            IntVector vc = va.mul(vb);
            vc.intoArray(state.datc, i);
        }
        return state.datc;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulRegular(VectorState state) {
        for(int i = 0; i < state.data.length; ++i) {
            state.datc[i] = state.data[i] * state.datb[i];
        }
        return state.datc;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] mulRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.data.length; ++i) {
            state.datc[i] = state.data[i] * state.datb[i];
        }
        return state.datc;
    }

    @Benchmark
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] addSIMD(VectorState state) {
        for (int i = 0; i < state.data.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.data, i);
            IntVector vb = VectorState.sInt.fromArray(state.datb, i);
            IntVector vc = va.add(vb);
            vc.intoArray(state.datc, i);
        }
        return state.datc;
    }

    @Benchmark
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] addRegular(VectorState state) {
        for (int i = 0; i < state.data.length; ++i) {
            state.datc[i] = state.data[i] + state.datb[i];
        }
        return state.datc;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    @Warmup(iterations = 2, time = 2)
    @Measurement(iterations = 3, time = 2)
    public int[] addRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.data.length; ++i) {
            state.datc[i] = state.data[i] + state.datb[i];
        }
        return state.datc;
    }
}
