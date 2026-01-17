package mathTests;

import math.LinearAlgebraEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class Vector4DTests {

    @Test
    void testConstructorFromComponents() {
        var v = LinearAlgebraEngine.createVector4D(1, 2, 3, 4);
        assertEquals(1, v.getX(), 1e-6f);
        assertEquals(2, v.getY(), 1e-6f);
        assertEquals(3, v.getZ(), 1e-6f);
        assertEquals(4, v.getW(), 1e-6f);
    }

    @Test
    void testConstructorFromVector3D() {
        var v3 = LinearAlgebraEngine.createVector3D(1, 2, 3);
        var v4 = LinearAlgebraEngine.createVector4DFrom3D(v3, 1);
        assertEquals(1, v4.getX(), 1e-6f);
        assertEquals(2, v4.getY(), 1e-6f);
        assertEquals(3, v4.getZ(), 1e-6f);
        assertEquals(1, v4.getW(), 1e-6f);
    }

    @Test
    void testToVector3DWithW1() {
        var v4 = LinearAlgebraEngine.createVector4D(2, 4, 6, 2);
        var v3 = v4.toVector3D(); // (2/2, 4/2, 6/2) = (1,2,3)
        assertEquals(1, v3.getX(), 1e-6f);
        assertEquals(2, v3.getY(), 1e-6f);
        assertEquals(3, v3.getZ(), 1e-6f);
    }

    @Test
    void testToVector3DWithW0() {
        var v4 = LinearAlgebraEngine.createVector4D(1, 2, 3, 0);
        var v3 = v4.toVector3D(); // w ≈ 0 → не делим
        assertEquals(1, v3.getX(), 1e-6f);
        assertEquals(2, v3.getY(), 1e-6f);
        assertEquals(3, v3.getZ(), 1e-6f);
    }

    @Test
    void testPerspectiveDivide() {
        var v4 = LinearAlgebraEngine.createVector4D(2, 4, 6, 2);
        var divided = v4.perspectiveDivide();
        assertEquals(1, divided.getX(), 1e-6f);
        assertEquals(2, divided.getY(), 1e-6f);
        assertEquals(3, divided.getZ(), 1e-6f);
        assertEquals(1, divided.getW(), 1e-6f);
    }

    @Test
    void testIsPointAndIsDirection() {
        var point = LinearAlgebraEngine.createVector4D(1, 2, 3, 1);
        var dir = LinearAlgebraEngine.createVector4D(1, 2, 3, 0);

        assertTrue(point.isPoint());
        assertFalse(point.isDirection());

        assertTrue(dir.isDirection());
        assertFalse(dir.isPoint());
    }

    @Test
    void testAsPointFromDirection() {
        var dir = LinearAlgebraEngine.createVector4D(1, 2, 3, 0);
        assertThrows(ArithmeticException.class, dir::asPoint);
    }

    @Test
    void testAsPointFromPoint() {
        var p = LinearAlgebraEngine.createVector4D(2, 4, 6, 2);
        var asPoint = p.asPoint();
        assertEquals(1, asPoint.getX(), 1e-6f);
        assertEquals(2, asPoint.getY(), 1e-6f);
        assertEquals(3, asPoint.getZ(), 1e-6f);
        assertEquals(1, asPoint.getW(), 1e-6f);
    }

    @Test
    void testLengthIgnoresW() {
        var v = LinearAlgebraEngine.createVector4D(3, 4, 0, 100);
        assertEquals(5.0f, v.length(), 1e-6f); // sqrt(3²+4²)
    }

    @Test
    void testNormalizeIgnoresW() {
        var v = LinearAlgebraEngine.createVector4D(3, 4, 0, 100);
        var n = v.normalize();
        assertEquals(0.6f, n.getX(), 1e-6f);
        assertEquals(0.8f, n.getY(), 1e-6f);
        assertEquals(0.0f, n.getZ(), 1e-6f);
        assertEquals(100, n.getW(), 1e-6f); // W сохраняется!
    }
}
