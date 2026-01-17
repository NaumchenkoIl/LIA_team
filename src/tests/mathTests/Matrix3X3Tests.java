package mathTests;

import math.Matrix.Matrix3x3;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Matrix3X3Tests {

    @Test
    void testIdentityMatrix() {
        var id = Matrix3x3.identity();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == j) {
                    assertEquals(1.0f, id.get(i, j), 1e-6f);
                } else {
                    assertEquals(0.0f, id.get(i, j), 1e-6f);
                }
            }
        }
    }

    @Test
    void testMultiplyByVector() {
        var m = Matrix3x3.identity();
        var v = new Vector3D(1, 2, 3);
        var result = m.multiply(v);
        assertEquals(v, result);
    }

    @Test
    void testDeterminantOfIdentity() {
        var id = Matrix3x3.identity();
        assertEquals(1.0f, id.determinant(), 1e-6f);
    }

    @Test
    void testInverseOfIdentity() {
        var id = Matrix3x3.identity();
        var inv = id.inverse();
        assertEquals(id, inv);
    }

    @Test
    void testSingularMatrixCannotBeInverted() {
        float[][] singular = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        var m = new Matrix3x3(singular);
        assertEquals(0.0f, m.determinant(), 1e-6f);
        assertThrows(ArithmeticException.class, m::inverse);
    }

    @Test
    void testSolveLinearSystem() {
        float[][] A = {
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        };
        var matrix = new Matrix3x3(A);
        var b = new Vector3D(1, 2, 3);
        var x = matrix.solveLinealSystem(b);
        assertEquals(1, x.getX(), 1e-6f);
        assertEquals(2, x.getY(), 1e-6f);
        assertEquals(3, x.getZ(), 1e-6f);
    }
}
