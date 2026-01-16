package math.Tests;

import math.LinearAlgebraEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Vector3DTests {

    @Test
    void testAdd() {
        var a = LinearAlgebraEngine.createVector3D(1, 2, 3);
        var b = LinearAlgebraEngine.createVector3D(4, 5, 6);
        var result = a.add(b);
        assertEquals(5, result.getX(), 1e-6f);
        assertEquals(7, result.getY(), 1e-6f);
        assertEquals(9, result.getZ(), 1e-6f);
    }

    @Test
    void testCrossProduct() {
        var x = LinearAlgebraEngine.createVector3D(1, 0, 0);
        var y = LinearAlgebraEngine.createVector3D(0, 1, 0);
        var z = x.cross(y);
        assertEquals(LinearAlgebraEngine.createVector3D(0, 0, 1), z);
    }

    @Test
    void testNormalizeZeroVector() {
        var zero = LinearAlgebraEngine.createVector3D(0, 0, 0);
        assertThrows(ArithmeticException.class, zero::normalize);
    }

    @Test
    void testLength() {
        var v = LinearAlgebraEngine.createVector3D(3, 4, 0);
        assertEquals(5.0f, v.length(), 1e-6f);
    }
}
