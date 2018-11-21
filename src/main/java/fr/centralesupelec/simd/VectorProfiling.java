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

        @Setup(Level.Trial)
        public void doSetup() {
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
    public long sumRegualar(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.data.length; ++i) {
            sum += state.data[i];
        }
        return sum;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulSIMD(VectorState state) {
        int[] datc = new int[4096];
        for(int i = 0; i < state.data.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.data, i);
            IntVector vb = VectorState.sInt.fromArray(state.datb, i);
            IntVector vc = va.mul(vb);
            vc.intoArray(datc, i);
        }
        return datc;
    }

    @Benchmark
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulRegular(VectorState state) {
        int[] datc = new int[4096];
        for(int i = 0; i < state.data.length; i += VectorState.vecLength) {
            datc[i] = state.data[i] * state.datb[i];
        }
        return datc;
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"--add-modules", "jdk.incubator.vector","-XX:-UseSuperWord"})
    @Warmup(iterations=2, time=2)
    @Measurement(iterations=3, time=2)
    public int[] mulRegularNoSuperWord(VectorState state) {
        int[] datc = new int[4096];
        for(int i = 0; i < state.data.length; i += VectorState.vecLength) {
            datc[i] = state.data[i] * state.datb[i];
        }
        return datc;
    }
}
