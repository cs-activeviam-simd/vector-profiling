package fr.centralesupelec.simd;

import jdk.incubator.vector.*;
import jdk.incubator.vector.Vector.*;
import org.openjdk.jmh.annotations.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(jvmArgsAppend = {"--add-modules", "jdk.incubator.vector"}, value = 2)
@BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 3, time = 2)
public class VectorProfiling {

    @State(Scope.Thread)
    public static class VectorState {

        @Param({"512", "1024", "2048", "4096", "8192", "16384", "32768", "65536", "131072", "262144", "524288", "1048576", "2097152", "4194304", "8388608", "16777216", "33554432", "67108864"})
        public int ARRAY_LENGTH;
        private static final int ARRAY_BOUND = 12;

        private static final IntVector.IntSpecies<?> sInt = IntVector.preferredSpecies();
        private static final int vecLength = sInt.length();
        public static final int bitSize = sInt.bitSize();
        int[] a;
        int[] b;
        int[] c;

        // small values for or/and filtering to prevent branch prediction in benchmarks
        int[] aSmall;
        int[] bSmall;
        int[] cSmall;
        int[] dSmall;

        int fa;
        int fb;
        int fc;
        int fd;

        @Setup(Level.Trial)
        public void doSetup() {
            a = new int[ARRAY_LENGTH];
            b = new int[ARRAY_LENGTH];
            c = new int[ARRAY_LENGTH];
            aSmall = new int[ARRAY_LENGTH];
            bSmall = new int[ARRAY_LENGTH];
            cSmall = new int[ARRAY_LENGTH];
            dSmall = new int[ARRAY_LENGTH];

            Random rnd = new Random();
            for (int i = 0; i < a.length; i++) {
                a[i] = rnd.nextInt();
                b[i] = rnd.nextInt();
                c[i] = rnd.nextInt();
                aSmall[i] = rnd.nextInt(ARRAY_BOUND);
                bSmall[i] = rnd.nextInt(ARRAY_BOUND);
                cSmall[i] = rnd.nextInt(ARRAY_BOUND);
                dSmall[i] = rnd.nextInt(ARRAY_BOUND);
            }
            fa = rnd.nextInt(ARRAY_BOUND);
            fb = rnd.nextInt(ARRAY_BOUND);
            fc = rnd.nextInt(ARRAY_BOUND);
            fd = rnd.nextInt(ARRAY_BOUND);
        }
    }

    @Benchmark
    public long sumSIMD(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.a.length; i += VectorState.vecLength) {
            sum += VectorState.sInt.fromArray(state.a, i).addAll();
        }
        return sum;
    }

    @Benchmark
    public long sumRegular(VectorState state) {
        long sum = 0;
        for(int i = 0; i < state.a.length; ++i) {
            sum += state.a[i];
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public long sumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.a.length; ++i) {
            sum += state.a[i];
        }
        return sum;
    }

    @Benchmark
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
    public int[] mulRegular(VectorState state) {
        for(int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] * state.b[i];
        }
        return state.c;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public int[] mulRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] * state.b[i];
        }
        return state.c;
    }

    @Benchmark
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
    public int[] addRegular(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] + state.b[i];
        }
        return state.c;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public int[] addRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] + state.b[i];
        }
        return state.c;
    }

    @Benchmark
    public boolean filterSIMD(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(state.fa);
            blackhole ^= m.getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterRegular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public boolean filterRegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa;
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterOr2SIMD(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask ma = va.equal(state.fa);
            Mask mb = va.equal(state.fb);
            blackhole ^= ma.or(mb).getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterOr2Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int a = state.aSmall[i];
            blackhole ^= a == state.fa || a == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public boolean filterOr2RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int a = state.aSmall[i];
            blackhole ^= a == state.fa || a == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterOr4SIMD(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(state.fa);
            m = m.or(va.equal(state.fb));
            m = m.or(va.equal(state.fc));
            m = m.or(va.equal(state.fd));
            blackhole ^= m.getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterOr4Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int a = state.aSmall[i];
            blackhole ^= a == state.fa || a == state.fb || a == state.fc || a == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public boolean filterOr4RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int a = state.aSmall[i];
            blackhole ^= a == state.fa || a == state.fb || a == state.fc || a == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterAnd2SIMD(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask ma = va.equal(state.fa);

            IntVector vb = VectorState.sInt.fromArray(state.bSmall, i);
            Mask mb = vb.equal(state.fb);
            blackhole ^= ma.and(mb).getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterAnd2Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa && state.bSmall[i] == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public boolean filterAnd2RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa && state.bSmall[i] == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterAnd4SIMD(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector v = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = v.equal(state.fa);
            v = VectorState.sInt.fromArray(state.bSmall, i);
            m = m.and(v.equal(state.fb));
            v = VectorState.sInt.fromArray(state.cSmall, i);
            m = m.and(v.equal(state.fc));
            v = VectorState.sInt.fromArray(state.dSmall, i);
            m = m.and(v.equal(state.fd));
            blackhole ^= m.getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public boolean filterAnd4Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa && state.bSmall[i] == state.fb && state.cSmall[i] == state.fc && state.dSmall[i] == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public boolean filterAnd4RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa && state.bSmall[i] == state.fb && state.cSmall[i] == state.fc && state.dSmall[i] == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    public long filterSumSIMD(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.lessThanEq(state.fa);
            sum += VectorState.sInt.fromArray(state.bSmall, i).addAll(m);
        }
        return sum;
    }

    @Benchmark
    public long filterSumRegular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] <= state.fa) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public long filterSumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] <= state.fa) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    public long filterSumBranchlessRegular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int n = state.aSmall[i] - state.fa;
            sum += (~(n >> 31) & ~((-n) >> 31)) & state.bSmall[i];
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "--add-modules", "jdk.incubator.vector", "-XX:-UseSuperWord" })
    public long filterSumBranchlessRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int n = state.aSmall[i] - state.fa;
            sum += (~(n >> 31) & ~((-n) >> 31)) & state.bSmall[i];
        }
        return sum;
    }
}
