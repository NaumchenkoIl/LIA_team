package mathTests;

import math.LinearAlgebraEngine;
import math.Matrix.Matrix4x4;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Matrix4x4Tests {

    @Test
    void testTranslation() {
        var t = LinearAlgebraEngine.createTranslationMatrix(1, 2, 3);
        var v = LinearAlgebraEngine.createVector4D(0, 0, 0, 1);
        var result = t.multiply(v);
        assertEquals(LinearAlgebraEngine.createVector4D(1, 2, 3, 1), result);
    }

    @Test
    void testRotationX() {
        var rot = LinearAlgebraEngine.createRotationMatrixX((float) Math.PI / 2); // 90°
        var v = LinearAlgebraEngine.createVector4D(0, 1, 0, 1);
        var result = rot.multiply(v);

        assertEquals(0.0f, result.getX(), 1e-6f);
        assertEquals(0.0f, result.getY(), 1e-6f);
        assertEquals(1.0f, result.getZ(), 1e-6f);
    }

    @Test
    void testPerspectiveProjection() {
        var proj = Matrix4x4.perspective(90, 1, 1, 100);
        var point = LinearAlgebraEngine.createVector4D(0, 0, -2, 1);
        var clip = proj.multiply(point);
        var ndc = clip.perspectiveDivide();
        assertTrue(ndc.getZ() >= -1f && ndc.getZ() <= 1f);
    }

    @Test
    void testSolveLinearSystem4x4() {
        float[][] A = {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
        var matrix = new Matrix4x4(A);
        var b = LinearAlgebraEngine.createVector4D(1, 2, 3, 4);
        var x = matrix.solveLinealSystem(b);

        assertEquals(1, x.getX(), 1e-6f);
        assertEquals(2, x.getY(), 1e-6f);
        assertEquals(3, x.getZ(), 1e-6f);
        assertEquals(4, x.getW(), 1e-6f);
    }

    @Test
    void testSolveNonTrivialSystem() {
        float[][] A = {
                {1, 1, 0, 0},
                {1, -1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
        var matrix = new Matrix4x4(A);
        var b = LinearAlgebraEngine.createVector4D(3, 1, 5, 6);
        var x = matrix.solveLinealSystem(b);

        assertEquals(2, x.getX(), 1e-6f);
        assertEquals(1, x.getY(), 1e-6f);
        assertEquals(5, x.getZ(), 1e-6f);
        assertEquals(6, x.getW(), 1e-6f);
    }

    @Test
    void testMultiplyByZeroMatrix() {
        var zero = Matrix4x4.zero();
        var v = LinearAlgebraEngine.createVector4D(1, 2, 3, 4);
        var result = zero.multiply(v);
        assertEquals(LinearAlgebraEngine.createVector4D(0, 0, 0, 0), result);
    }
}
