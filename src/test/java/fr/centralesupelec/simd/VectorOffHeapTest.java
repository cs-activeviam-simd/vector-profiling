package fr.centralesupelec.simd;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static fr.centralesupelec.simd.VectorOffHeapProfiling.VectorState;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorOffHeapTest {

    private static ByteBuffer clone(IntBuffer buffer) {
        ByteBuffer br = ByteBuffer.allocateDirect(buffer.capacity() * Integer.BYTES);
        br.order(ByteOrder.nativeOrder());
        IntBuffer r = br.asIntBuffer();
        r.put(buffer);
        buffer.position(0);
        r.position(0);
        return br;
    }

    private static void assertBufferEquals(ByteBuffer b1, ByteBuffer b2, String message) {
        assertEquals(b1.remaining(), b2.remaining(), message + ": remaining mismatch");
        assertEquals(-1, b1.mismatch(b2), message);
    }

    private static void assertBufferEquals(IntBuffer b1, ByteBuffer b2, String message) {
        ByteBuffer br = ByteBuffer.allocateDirect(b1.capacity() * Integer.BYTES);
        br.order(ByteOrder.nativeOrder());
        IntBuffer r = br.asIntBuffer();
        r.put(b1);
        b1.position(0);
        r.position(0);
        assertBufferEquals(br, b2, message);
    }

    private static VectorState state = new VectorState();
    private static VectorOffHeapProfiling v = new VectorOffHeapProfiling();
    
    @BeforeAll
    static void setupState() {
        state.ARRAY_LENGTH = 16384;
        state.doSetup();
    }

    @Test
    void bufferImpl() {
        assertEquals("java.nio.DirectByteBuffer", state.a.getClass().getCanonicalName(), "unexpected state.a implementation");
        assertEquals("java.nio.DirectByteBuffer", state.b.getClass().getCanonicalName(), "unexpected state.b implementation");
        assertEquals("java.nio.DirectByteBuffer", state.c.getClass().getCanonicalName(), "unexpected state.c implementation");
        assertEquals("java.nio.DirectIntBufferU", state.ia.getClass().getCanonicalName(), "unexpected state.ia implementation");
        assertEquals("java.nio.DirectIntBufferU", state.ib.getClass().getCanonicalName(), "unexpected state.ib implementation");
        assertEquals("java.nio.DirectIntBufferU", state.ic.getClass().getCanonicalName(), "unexpected state.ic implementation");

        assertEquals("java.nio.DirectByteBuffer", state.aSmall.getClass().getCanonicalName(), "unexpected state.aSmall implementation");
        assertEquals("java.nio.DirectByteBuffer", state.bSmall.getClass().getCanonicalName(), "unexpected state.bSmall implementation");
        assertEquals("java.nio.DirectByteBuffer", state.cSmall.getClass().getCanonicalName(), "unexpected state.cSmall implementation");
        assertEquals("java.nio.DirectByteBuffer", state.dSmall.getClass().getCanonicalName(), "unexpected state.dSmall implementation");
        assertEquals("java.nio.DirectByteBuffer", state.eSmall.getClass().getCanonicalName(), "unexpected state.eSmall implementation");
        assertEquals("java.nio.DirectIntBufferU", state.iaSmall.getClass().getCanonicalName(), "unexpected state.iaSmall implementation");
        assertEquals("java.nio.DirectIntBufferU", state.ibSmall.getClass().getCanonicalName(), "unexpected state.ibSmall implementation");
        assertEquals("java.nio.DirectIntBufferU", state.icSmall.getClass().getCanonicalName(), "unexpected state.icSmall implementation");
        assertEquals("java.nio.DirectIntBufferU", state.idSmall.getClass().getCanonicalName(), "unexpected state.idSmall implementation");
        assertEquals("java.nio.DirectIntBufferU", state.ieSmall.getClass().getCanonicalName(), "unexpected state.ieSmall implementation");

        assertEquals(0, state.a.alignmentOffset(0, VectorState.vecBytes), "unaligned state.a");
        assertEquals(0, state.b.alignmentOffset(0, VectorState.vecBytes), "unaligned state.a");
        assertEquals(0, state.c.alignmentOffset(0, VectorState.vecBytes), "unaligned state.a");

        assertEquals(0, state.aSmall.alignmentOffset(0, VectorState.vecBytes), "unaligned state.aSmall");
        assertEquals(0, state.bSmall.alignmentOffset(0, VectorState.vecBytes), "unaligned state.bSmall");
        assertEquals(0, state.cSmall.alignmentOffset(0, VectorState.vecBytes), "unaligned state.cSmall");
        assertEquals(0, state.dSmall.alignmentOffset(0, VectorState.vecBytes), "unaligned state.dSmall");
        assertEquals(0, state.eSmall.alignmentOffset(0, VectorState.vecBytes), "unaligned state.eSmall");
    }

    @Test
    void sum() {
        long r = v.sumRegular(state);
        assertEquals(v.sumRegularNoSuperWord(state), r, "sumRegular/sumRegularNoSuperWord mismatch");
        assertEquals(v.sumSIMD(state), r, "sumRegular/sumSIMD mismatch");
    }

    @Test
    void mul() {
        ByteBuffer r = clone(v.mulRegular(state));
        assertBufferEquals(v.mulRegularNoSuperWord(state), r, "mulRegular/mulRegularNoSuperWord mismatch");
        assertBufferEquals(v.mulSIMD(state), r, "mulRegular/mulSIMD mismatch");
    }

    @Test
    void add() {
        ByteBuffer r = clone(v.addRegular(state));
        assertBufferEquals(v.addRegularNoSuperWord(state), r, "addRegular/addRegularNoSuperWord mismatch");
        assertBufferEquals(v.addSIMD(state), r, "addRegular/addSIMD mismatch");
    }

    @Test
    @Disabled
    void filter() {
        boolean r = v.filterRegular(state);
        assertEquals(v.filterRegularNoSuperWord(state), r, "filterRegular/filterRegularNoSuperWord mismatch");
        assertEquals(v.filterSIMD(state), r, "filterRegular/filterSIMD mismatch");
    }

    @Test
    @Disabled
    void filterOr2() {
        boolean r = v.filterOr2Regular(state);
        assertEquals(v.filterOr2RegularNoSuperWord(state), r, "filterOr2Regular/filterOr2RegularNoSuperWord mismatch");
        assertEquals(v.filterOr2SIMD(state), r, "filterOr2Regular/filterOr2SIMD mismatch");
    }
    
    @Test
    @Disabled
    void filterOr4() {
        boolean r = v.filterOr4Regular(state);
        assertEquals(v.filterOr4RegularNoSuperWord(state), r, "filterOr4Regular/filterOr4RegularNoSuperWord mismatch");
        assertEquals(v.filterOr4SIMD(state), r, "filterOr4Regular/filterOr4SIMD mismatch");
    }
    
    @Test
    @Disabled
    void filterAnd2() {
        boolean r = v.filterAnd2Regular(state);
        assertEquals(v.filterAnd2RegularNoSuperWord(state), r, "filterAnd2Regular/filterAnd2RegularNoSuperWord mismatch");
        assertEquals(v.filterAnd2SIMD(state), r, "filterAnd2Regular/filterAnd2SIMD mismatch");
    }
    
    @Test
    @Disabled
    void filterAnd4() {
        boolean r = v.filterAnd4Regular(state);
        assertEquals(v.filterAnd4RegularNoSuperWord(state), r, "filterAnd4Regular/filterAnd4RegularNoSuperWord mismatch");
        assertEquals(v.filterAnd4SIMD(state), r, "filterAnd4Regular/filterAnd4SIMD mismatch");
    }
    
    @Test
    void filterSum() {
        long r = v.filterSumRegular(state);
        assertEquals(v.filterSumRegularNoSuperWord(state), r, "filterSumRegular/filterSumRegularNoSuperWord mismatch");
        assertEquals(v.filterSumSIMD(state), r, "filterSumRegular/filterSumSIMD mismatch");
        assertEquals(v.filterSumBranchlessRegular(state), r, "filterSumRegular/filterSumBranchlessRegular mismatch");
        assertEquals(v.filterSumBranchlessRegularNoSuperWord(state), r, "filterSumRegular/filterSumBranchlessRegularNoSuperWord mismatch");
    }

    @Test
    void filterSumOr2() {
        long r = v.filterSumOr2Regular(state);
        assertEquals(v.filterSumOr2RegularNoSuperWord(state), r, "filterSumOr2Regular/filterSumOr2RegularNoSuperWord mismatch");
        assertEquals(v.filterSumOr2SIMD(state), r, "filterSumOr2Regular/filterSumOr2SIMD mismatch");
    }

    @Test
    void filterSumOr4() {
        long r = v.filterSumOr4Regular(state);
        assertEquals(v.filterSumOr4RegularNoSuperWord(state), r, "filterSumOr4Regular/filterSumOr4RegularNoSuperWord mismatch");
        assertEquals(v.filterSumOr4SIMD(state), r, "filterSumOr4Regular/filterSumOr4SIMD mismatch");
    }

    @Test
    void filterSumAnd2() {
        long r = v.filterSumAnd2Regular(state);
        assertEquals(v.filterSumAnd2RegularNoSuperWord(state), r, "filterSumAnd2Regular/filterSumAnd2RegularNoSuperWord mismatch");
        assertEquals(v.filterSumAnd2SIMD(state), r, "filterSumAnd2Regular/filterSumAnd2SIMD mismatch");
    }

    @Test
    void filterSumAnd4() {
        long r = v.filterSumAnd4Regular(state);
        assertEquals(v.filterSumAnd4RegularNoSuperWord(state), r, "filterSumAnd4Regular/filterSumAnd4RegularNoSuperWord mismatch");
        assertEquals(v.filterSumAnd4SIMD(state), r, "filterSumAnd4Regular/filterSumAnd4SIMD mismatch");
    }
}
