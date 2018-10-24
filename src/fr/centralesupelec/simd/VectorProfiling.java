package fr.centralesupelec.simd;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Shapes;

public class VectorProfiling {
    private static final FloatVector.FloatSpecies<Shapes.S256Bit> sFloat = FloatVector.species(Shapes.S_256_BIT);

    public static void main(String[] args) {
        // for now, let's just make a simple program using vectors that compiles
        float[] a = {1.0f, 2.5f, 3.0f, 4.2f, 5.3f, 4.2f, 3.2f, 1.1f};
        float[] b = {-1.0f, 4.5f, Float.NaN, 0, 1, Float.MAX_VALUE, 41.1f, 4.2f};
        float[] r = new float[8];
        sFloat.fromArray(a, 0).add(sFloat.fromArray(b, 0)).intoArray(r, 0);
        System.out.println("--- RESULT ---");
        for(float n : r) {
            System.out.print(n);
            System.out.print(" ");
        }
        System.out.println();
        System.out.println("--------------");
    }
}
