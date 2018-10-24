package fr.centralesupelec.simd;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

@Fork(jvmArgsAppend = {"--add-modules", "jdk.incubator.vector"})
public class VectorProfiling {

    @State(Scope.Thread)
    public static class VectorState {
        private static final IntVector.IntSpecies<?> sInt = IntVector.preferredSpecies();
        private static final int vecLength = sInt.length();
        int[] data = new int[4096];

        @Setup(Level.Trial)
        public void doSetup() {
            for (int i = 0; i < data.length; i++) {
                data[i] = i % 3;
            }
        }
    }

    @Benchmark
    public long simdPreferred(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.data.length; i += VectorState.vecLength) {
            sum += VectorState.sInt.fromArray(state.data, i).addAll();
        }
        return sum;
    }

    @Benchmark
    public long regular(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.data.length; ++i) {
            sum += state.data[i];
        }
        return sum;
    }
}
