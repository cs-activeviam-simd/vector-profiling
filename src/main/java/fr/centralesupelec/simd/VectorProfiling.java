package fr.centralesupelec.simd;

import jdk.incubator.vector.*;
import jdk.incubator.vector.Vector.*;
import org.openjdk.jmh.annotations.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** @noinspection unchecked*/
@Fork(jvmArgsPrepend = {"--add-modules", "jdk.incubator.vector", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"}, value = 2)
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
        int[] eSmall;

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
            eSmall = new int[ARRAY_LENGTH];

            Random rnd = new Random();
            for (int i = 0; i < a.length; i++) {
                a[i] = rnd.nextInt();
                b[i] = rnd.nextInt();
                c[i] = rnd.nextInt();
                aSmall[i] = rnd.nextInt(ARRAY_BOUND);
                bSmall[i] = rnd.nextInt(ARRAY_BOUND);
                cSmall[i] = rnd.nextInt(ARRAY_BOUND);
                dSmall[i] = rnd.nextInt(ARRAY_BOUND);
                eSmall[i] = rnd.nextInt(ARRAY_BOUND);
            }
            fa = rnd.nextInt(ARRAY_BOUND);
            fb = rnd.nextInt(ARRAY_BOUND);
            fc = rnd.nextInt(ARRAY_BOUND);
            fd = rnd.nextInt(ARRAY_BOUND);
        }
    }

    @Benchmark
    @Fork(jvmArgsAppend = {  "-XX:-UseSuperWord" })
    public long sumSIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        for (int i = 0; i < state.a.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.a, i);
            vs = vs.add(va);
        }
        return vs.addAll();
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public int[] addRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.a.length; ++i) {
            state.c[i] = state.a[i] + state.b[i];
        }
        return state.c;
    }

    @Benchmark
    public boolean filterSIMD(VectorState state) {
        boolean blackhole = false;
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(vfa);
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
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
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask ma = va.equal(vfa);
            Mask mb = va.equal(vfb);
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
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
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(vfa);
            m = m.or(va.equal(vfb));
            m = m.or(va.equal(vfc));
            m = m.or(va.equal(vfd));
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
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
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask ma = va.equal(vfa);
            IntVector vb = VectorState.sInt.fromArray(state.bSmall, i);
            Mask mb = vb.equal(vfb);
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
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
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector v = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = v.equal(vfa);
            v = VectorState.sInt.fromArray(state.bSmall, i);
            m = m.and(v.equal(vfb));
            v = VectorState.sInt.fromArray(state.cSmall, i);
            m = m.and(v.equal(vfc));
            v = VectorState.sInt.fromArray(state.dSmall, i);
            m = m.and(v.equal(vfd));
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public boolean filterAnd4RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.aSmall.length; ++i) {
            blackhole ^= state.aSmall[i] == state.fa && state.bSmall[i] == state.fb && state.cSmall[i] == state.fc && state.dSmall[i] == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    public long filterSumSIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(vfa);
            vs = VectorState.sInt.fromArray(state.bSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public long filterSumRegular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] == state.fa) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public long filterSumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] == state.fa) {
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
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public long filterSumBranchlessRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int n = state.aSmall[i] - state.fa;
            sum += (~(n >> 31) & ~((-n) >> 31)) & state.bSmall[i];
        }
        return sum;
    }


    @Benchmark
    public long filterSumOr2SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(vfa);
            m = m.or(va.equal(vfb));
            vs = VectorState.sInt.fromArray(state.bSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public long filterSumOr2Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int v = state.aSmall[i];
            if(v == state.fa || v == state.fb) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public long filterSumOr2RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int v = state.aSmall[i];
            if(v == state.fa || v == state.fb) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    public long filterSumOr4SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector va = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = va.equal(vfa);
            m = m.or(va.equal(vfb));
            m = m.or(va.equal(vfc));
            m = m.or(va.equal(vfd));
            vs = VectorState.sInt.fromArray(state.bSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public long filterSumOr4Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int v = state.aSmall[i];
            if(v == state.fa || v == state.fb || v == state.fc || v == state.fd) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public long filterSumOr4RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            int v = state.aSmall[i];
            if(v == state.fa || v == state.fb || v == state.fc || v == state.fd) {
                sum += state.bSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    public long filterSumAnd2SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector v = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = v.equal(vfa);
            v = VectorState.sInt.fromArray(state.bSmall, i);
            m = m.and(v.equal(vfb));
            vs = VectorState.sInt.fromArray(state.cSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public long filterSumAnd2Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] == state.fa && state.bSmall[i] == state.fb) {
                sum += state.cSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public long filterSumAnd2RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] == state.fa && state.bSmall[i] == state.fb) {
                sum += state.cSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    public long filterSumAnd4SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.length; i += VectorState.vecLength) {
            IntVector v = VectorState.sInt.fromArray(state.aSmall, i);
            Mask m = v.equal(vfa);
            v = VectorState.sInt.fromArray(state.bSmall, i);
            m = m.and(v.equal(vfb));
            v = VectorState.sInt.fromArray(state.cSmall, i);
            m = m.and(v.equal(vfc));
            v = VectorState.sInt.fromArray(state.dSmall, i);
            m = m.and(v.equal(vfd));
            vs = VectorState.sInt.fromArray(state.eSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public long filterSumAnd4Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] == state.fa && state.bSmall[i] == state.fb && state.cSmall[i] == state.fc && state.dSmall[i] == state.fd) {
                sum += state.eSmall[i];
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public long filterSumAnd4RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.aSmall.length; ++i) {
            if(state.aSmall[i] == state.fa && state.bSmall[i] == state.fb && state.cSmall[i] == state.fc && state.dSmall[i] == state.fd) {
                sum += state.eSmall[i];
            }
        }
        return sum;
    }

}
