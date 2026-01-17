package mathTests;

import math.LinearAlgebraEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Vector2DTests {

    @Test
    void testAdd() {
        var a = LinearAlgebraEngine.createVector2D(1, 2);
        var b = LinearAlgebraEngine.createVector2D(3, 4);
        var result = a.add(b);
        assertEquals(4, result.getX(), 1e-6f);
        assertEquals(6, result.getY(), 1e-6f);
    }

    @Test
    void testSubtract() {
        var a = LinearAlgebraEngine.createVector2D(5, 5);
        var b = LinearAlgebraEngine.createVector2D(2, 3);
        var result = a.subtract(b);
        assertEquals(3, result.getX(), 1e-6f);
        assertEquals(2, result.getY(), 1e-6f);
    }

    @Test
    void testMultiplyScalar() {
        var v = LinearAlgebraEngine.createVector2D(2, 3);
        var result = v.multiply(2.0f);
        assertEquals(4, result.getX(), 1e-6f);
        assertEquals(6, result.getY(), 1e-6f);
    }

    @Test
    void testLength() {
        var v = LinearAlgebraEngine.createVector2D(3, 4);
        assertEquals(5.0f, v.length(), 1e-6f);
    }

    @Test
    void testNormalize() {
        var v = LinearAlgebraEngine.createVector2D(3, 4);
        var n = v.normalize();
        assertEquals(1.0f, n.length(), 1e-6f);
    }

    @Test
    void testNormalizeZeroVector() {
        var zero = LinearAlgebraEngine.createVector2D(0, 0);
        assertThrows(ArithmeticException.class, zero::normalize);
    }

    @Test
    void testNormalizeNearZeroVector() {
        var v = LinearAlgebraEngine.createVector3D(1e-13f, 1e-13f, 1e-13f);
        assertThrows(ArithmeticException.class, v::normalize);
    }

    @Test
    void testDotProduct() {
        var a = LinearAlgebraEngine.createVector2D(1, 0);
        var b = LinearAlgebraEngine.createVector2D(0, 1);
        assertEquals(0.0f, a.dot(b), 1e-6f);
    }

    @Test
    void testCrossProduct2D() {
        var a = LinearAlgebraEngine.createVector2D(1, 0);
        var b = LinearAlgebraEngine.createVector2D(0, 1);
        // В 2D cross — это скаляр: ax*by - ay*bx
        assertEquals(1.0f, a.cross(b), 1e-6f);
    }

    @Test
    void testDistance() {
        var a = LinearAlgebraEngine.createVector2D(0, 0);
        var b = LinearAlgebraEngine.createVector2D(3, 4);
        assertEquals(5.0f, a.distance(b), 1e-6f);
    }
}
