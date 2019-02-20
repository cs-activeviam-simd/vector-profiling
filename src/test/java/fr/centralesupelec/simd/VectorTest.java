package fr.centralesupelec.simd;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static fr.centralesupelec.simd.VectorProfiling.VectorState;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorTest {
    
    private static VectorState state = new VectorState();
    private static VectorProfiling v = new VectorProfiling();
    
    @BeforeAll
    static void setupState() {
        state.ARRAY_LENGTH = 16384;
        state.doSetup();
    }

    @Test
    void sum() {
        long r = v.sumRegular(state);
        assertEquals(v.sumRegularNoSuperWord(state), r, "sumRegular/sumRegularNoSuperWord mismatch");
        assertEquals(v.sumSIMD(state), r, "sumRegular/sumSIMD mismatch");
    }

    @Test
    void mul() {
        int[] r = v.mulRegular(state);
        r = Arrays.copyOf(r, r.length); // clone before contents are overwritten
        assertArrayEquals(v.mulRegularNoSuperWord(state), r, "mulRegular/mulRegularNoSuperWord mismatch");
        assertArrayEquals(v.mulSIMD(state), r, "mulRegular/mulSIMD mismatch");
    }

    @Test
    void add() {
        int[] r = v.addRegular(state);
        r = Arrays.copyOf(r, r.length); // clone before contents are overwritten
        assertArrayEquals(v.addRegularNoSuperWord(state), r, "addRegular/addRegularNoSuperWord mismatch");
        assertArrayEquals(v.addSIMD(state), r, "addRegular/addSIMD mismatch");
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
