package fr.centralesupelec.simd;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.Vector.Mask;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@Fork(jvmArgsPrepend = {"--add-modules", "jdk.incubator.vector", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"}, value = 2)
@BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 3, time = 2)
public class VectorOffHeapProfiling {

    private static ByteBuffer allocate(int length) {
        ByteBuffer r = ByteBuffer.allocateDirect(length + VectorState.vecBytes);
        if(r.alignmentOffset(0, VectorState.vecBytes) != 0) {
            r = r.alignedSlice(VectorState.vecBytes);
        }
        r.order(ByteOrder.nativeOrder());
        r.limit(length);
        return r;
    }

    @State(Scope.Thread)
    public static class VectorState {

        @Param({"512", "1024", "2048", "4096", "8192", "16384", "32768", "65536", "131072", "262144", "524288", "1048576", "2097152", "4194304", "8388608", "16777216", "33554432", "67108864"})
        public int ARRAY_LENGTH;
        private static final int ARRAY_BOUND = 12;

        private static final IntVector.IntSpecies<?> sInt = IntVector.preferredSpecies();
        static final int vecBytes = sInt.bitSize() / Byte.SIZE;
        ByteBuffer a; IntBuffer ia;
        ByteBuffer b; IntBuffer ib;
        ByteBuffer c; IntBuffer ic;

        // small values for or/and filtering to prevent branch prediction in benchmarks
        ByteBuffer aSmall; IntBuffer iaSmall;
        ByteBuffer bSmall; IntBuffer ibSmall;
        ByteBuffer cSmall; IntBuffer icSmall;
        ByteBuffer dSmall; IntBuffer idSmall;
        ByteBuffer eSmall; IntBuffer ieSmall;

        int fa;
        int fb;
        int fc;
        int fd;

        @Setup(Level.Trial)
        public final void doSetup() {
            a = allocate(ARRAY_LENGTH * Integer.BYTES);
            b = allocate(ARRAY_LENGTH * Integer.BYTES);
            c = allocate(ARRAY_LENGTH * Integer.BYTES);

            ia = a.asIntBuffer();
            ib = b.asIntBuffer();
            ic = c.asIntBuffer();

            aSmall = allocate(ARRAY_LENGTH * Integer.BYTES);
            bSmall = allocate(ARRAY_LENGTH * Integer.BYTES);
            cSmall = allocate(ARRAY_LENGTH * Integer.BYTES);
            dSmall = allocate(ARRAY_LENGTH * Integer.BYTES);
            eSmall = allocate(ARRAY_LENGTH * Integer.BYTES);

            iaSmall = aSmall.asIntBuffer();
            ibSmall = bSmall.asIntBuffer();
            icSmall = cSmall.asIntBuffer();
            idSmall = dSmall.asIntBuffer();
            ieSmall = eSmall.asIntBuffer();

            Random rnd = new Random();
            for (int i = 0; i < ia.limit(); i++) {
                ia.put(i, rnd.nextInt());
                ib.put(i, rnd.nextInt());
                ic.put(i, rnd.nextInt());
                iaSmall.put(i, rnd.nextInt(ARRAY_BOUND));
                ibSmall.put(i, rnd.nextInt(ARRAY_BOUND));
                icSmall.put(i, rnd.nextInt(ARRAY_BOUND));
                idSmall.put(i, rnd.nextInt(ARRAY_BOUND));
                iaSmall.put(i, rnd.nextInt(ARRAY_BOUND));
            }
            fa = rnd.nextInt(ARRAY_BOUND);
            fb = rnd.nextInt(ARRAY_BOUND);
            fc = rnd.nextInt(ARRAY_BOUND);
            fd = rnd.nextInt(ARRAY_BOUND);
        }
    }

    @Benchmark
    @Fork(jvmArgsAppend = {  "-XX:-UseSuperWord" })
    public final int sumSIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        for (int i = 0; i < state.a.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.a, i);
            vs = vs.add(va);
        }
        return vs.addAll();
    }

    @Benchmark
    public final int sumRegular(VectorState state) {
        int sum = 0;
        for(int i = 0; i < state.ia.limit(); ++i) {
            sum += state.ia.get(i);
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final int sumRegularNoSuperWord(VectorState state) {
        int sum = 0;
        for(int i = 0; i < state.ia.limit(); ++i) {
            sum += state.ia.get(i);
        }
        return sum;
    }

    @Benchmark
    public final ByteBuffer mulSIMD(VectorState state) {
        for(int i = 0; i < state.a.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.a, i);
            IntVector vb = VectorState.sInt.fromByteBuffer(state.b, i);
            IntVector vc = va.mul(vb);
            vc.intoByteBuffer(state.c, i);
        }
        return state.c;
    }

    @Benchmark
    public final IntBuffer mulRegular(VectorState state) {
        for(int i = 0; i < state.ia.limit(); ++i) {
            state.ic.put(i, state.ia.get(i) * state.ib.get(i));
        }
        return state.ic;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final IntBuffer mulRegularNoSuperWord(VectorState state) {
        for(int i = 0; i < state.ia.limit(); ++i) {
            state.ic.put(i, state.ia.get(i) * state.ib.get(i));
        }
        return state.ic;
    }

    @Benchmark
    public final ByteBuffer addSIMD(VectorState state) {
        for (int i = 0; i < state.a.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.a, i);
            IntVector vb = VectorState.sInt.fromByteBuffer(state.b, i);
            IntVector vc = va.add(vb);
            vc.intoByteBuffer(state.c, i);
        }
        return state.c;
    }

    @Benchmark
    public final IntBuffer addRegular(VectorState state) {
        for (int i = 0; i < state.ia.limit(); ++i) {
            state.ic.put(i, state.ia.get(i) + state.ib.get(i));
        }
        return state.ic;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final IntBuffer addRegularNoSuperWord(VectorState state) {
        for (int i = 0; i < state.ia.limit(); ++i) {
            state.ic.put(i, state.ia.get(i) + state.ib.get(i));
        }
        return state.ic;
    }

    @Benchmark
    public final boolean filterSIMD(VectorState state) {
        boolean blackhole = false;
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = va.equal(vfa);
            blackhole ^= m.getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterRegular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            blackhole ^= state.iaSmall.get(i) == state.fa;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final boolean filterRegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            blackhole ^= state.iaSmall.get(i) == state.fa;
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterOr2SIMD(VectorState state) {
        boolean blackhole = false;
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask ma = va.equal(vfa);
            Mask mb = va.equal(vfb);
            blackhole ^= ma.or(mb).getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterOr2Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int a = state.iaSmall.get(i);
            blackhole ^= a == state.fa || a == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final boolean filterOr2RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int a = state.iaSmall.get(i);
            blackhole ^= a == state.fa || a == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterOr4SIMD(VectorState state) {
        boolean blackhole = false;
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = va.equal(vfa);
            m = m.or(va.equal(vfb));
            m = m.or(va.equal(vfc));
            m = m.or(va.equal(vfd));
            blackhole ^= m.getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterOr4Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int a = state.iaSmall.get(i);
            blackhole ^= a == state.fa || a == state.fb || a == state.fc || a == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final boolean filterOr4RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int a = state.iaSmall.get(i);
            blackhole ^= a == state.fa || a == state.fb || a == state.fc || a == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterAnd2SIMD(VectorState state) {
        boolean blackhole = false;
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask ma = va.equal(vfa);
            IntVector vb = VectorState.sInt.fromByteBuffer(state.bSmall, i);
            Mask mb = vb.equal(vfb);
            blackhole ^= ma.and(mb).getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterAnd2Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            blackhole ^= state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final boolean filterAnd2RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            blackhole ^= state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb;
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterAnd4SIMD(VectorState state) {
        boolean blackhole = false;
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector v = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = v.equal(vfa);
            v = VectorState.sInt.fromByteBuffer(state.bSmall, i);
            m = m.and(v.equal(vfb));
            v = VectorState.sInt.fromByteBuffer(state.cSmall, i);
            m = m.and(v.equal(vfc));
            v = VectorState.sInt.fromByteBuffer(state.dSmall, i);
            m = m.and(v.equal(vfd));
            blackhole ^= m.getElement(0);
        }
        return blackhole;
    }

    @Benchmark
    public final boolean filterAnd4Regular(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            blackhole ^= state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb && state.icSmall.get(i) == state.fc && state.idSmall.get(i) == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final boolean filterAnd4RegularNoSuperWord(VectorState state) {
        boolean blackhole = false;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            blackhole ^= state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb && state.icSmall.get(i) == state.fc && state.idSmall.get(i) == state.fd;
        }
        return blackhole;
    }

    @Benchmark
    public final long filterSumSIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = va.equal(vfa);
            vs = VectorState.sInt.fromByteBuffer(state.bSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public final long filterSumRegular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            if(state.iaSmall.get(i) == state.fa) {
                sum += state.ibSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final long filterSumRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            if(state.iaSmall.get(i) == state.fa) {
                sum += state.ibSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    public final long filterSumBranchlessRegular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int n = state.iaSmall.get(i) - state.fa;
            sum += (~(n >> 31) & ~((-n) >> 31)) & state.ibSmall.get(i);
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final long filterSumBranchlessRegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int n = state.iaSmall.get(i) - state.fa;
            sum += (~(n >> 31) & ~((-n) >> 31)) & state.ibSmall.get(i);
        }
        return sum;
    }


    @Benchmark
    public final long filterSumOr2SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = va.equal(vfa);
            m = m.or(va.equal(vfb));
            vs = VectorState.sInt.fromByteBuffer(state.bSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public final long filterSumOr2Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int v = state.iaSmall.get(i);
            if(v == state.fa || v == state.fb) {
                sum += state.ibSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final long filterSumOr2RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int v = state.iaSmall.get(i);
            if(v == state.fa || v == state.fb) {
                sum += state.ibSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    public final long filterSumOr4SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector va = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = va.equal(vfa);
            m = m.or(va.equal(vfb));
            m = m.or(va.equal(vfc));
            m = m.or(va.equal(vfd));
            vs = VectorState.sInt.fromByteBuffer(state.bSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public final long filterSumOr4Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int v = state.iaSmall.get(i);
            if(v == state.fa || v == state.fb || v == state.fc || v == state.fd) {
                sum += state.ibSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final long filterSumOr4RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            int v = state.iaSmall.get(i);
            if(v == state.fa || v == state.fb || v == state.fc || v == state.fd) {
                sum += state.ibSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    public final long filterSumAnd2SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector v = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = v.equal(vfa);
            v = VectorState.sInt.fromByteBuffer(state.bSmall, i);
            m = m.and(v.equal(vfb));
            vs = VectorState.sInt.fromByteBuffer(state.cSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public final long filterSumAnd2Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            if(state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb) {
                sum += state.icSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final long filterSumAnd2RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            if(state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb) {
                sum += state.icSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    public final long filterSumAnd4SIMD(VectorState state) {
        IntVector vs = VectorState.sInt.zero();
        IntVector vfa = VectorState.sInt.broadcast(state.fa);
        IntVector vfb = VectorState.sInt.broadcast(state.fb);
        IntVector vfc = VectorState.sInt.broadcast(state.fc);
        IntVector vfd = VectorState.sInt.broadcast(state.fd);
        for (int i = 0; i < state.aSmall.limit(); i += VectorState.vecBytes) {
            IntVector v = VectorState.sInt.fromByteBuffer(state.aSmall, i);
            Mask m = v.equal(vfa);
            v = VectorState.sInt.fromByteBuffer(state.bSmall, i);
            m = m.and(v.equal(vfb));
            v = VectorState.sInt.fromByteBuffer(state.cSmall, i);
            m = m.and(v.equal(vfc));
            v = VectorState.sInt.fromByteBuffer(state.dSmall, i);
            m = m.and(v.equal(vfd));
            vs = VectorState.sInt.fromByteBuffer(state.eSmall, i, m).add(vs);
        }
        return vs.addAll();
    }

    @Benchmark
    public final long filterSumAnd4Regular(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            if(state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb && state.icSmall.get(i) == state.fc && state.idSmall.get(i) == state.fd) {
                sum += state.ieSmall.get(i);
            }
        }
        return sum;
    }

    @Benchmark
    @Fork(jvmArgsAppend = { "-XX:-UseSuperWord" })
    public final long filterSumAnd4RegularNoSuperWord(VectorState state) {
        long sum = 0;
        for (int i = 0; i < state.iaSmall.limit(); ++i) {
            if(state.iaSmall.get(i) == state.fa && state.ibSmall.get(i) == state.fb && state.icSmall.get(i) == state.fc && state.idSmall.get(i) == state.fd) {
                sum += state.ieSmall.get(i);
            }
        }
        return sum;
    }
}
