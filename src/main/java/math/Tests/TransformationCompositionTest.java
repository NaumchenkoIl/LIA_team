package math.Tests;
import math.LinearAlgebraEngine;
import math.LinealAlgebra.Vector4D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TransformationCompositionTest {

    @Test
    void testRotationPreservesLength() {
        var v = LinearAlgebraEngine.createVector4D(1, 1, 0, 1);
        float originalLength = (float) Math.sqrt(1*1 + 1*1);

        var rotY = LinearAlgebraEngine.createRotationMatrixY((float) Math.PI / 4);
        var result = rotY.multiply(v);
        Vector4D rotated3D = new Vector4D(result.getX(), result.getY(), result.getZ(), 1);
        float newLength = rotated3D.length();

        assertEquals(originalLength, newLength, 1e-6f);
    }

    @Test
    void testInverseTransformationReturnsOriginalPoint() {
        var translate = LinearAlgebraEngine.createTranslationMatrix(5, -2, 3);
        var scale = LinearAlgebraEngine.createScaleMatrix(2, 3, 4);
        var transform = translate.multiply(scale);

        var point = LinearAlgebraEngine.createVector4D(1, 1, 1, 1);
        var transformed = transform.multiply(point);
        var inverse = transform.inverse();
        var restored = inverse.multiply(transformed);

        assertEquals(point.getX(), restored.getX(), 1e-6f);
        assertEquals(point.getY(), restored.getY(), 1e-6f);
        assertEquals(point.getZ(), restored.getZ(), 1e-6f);
        assertEquals(point.getW(), restored.getW(), 1e-6f);
    }

    @Test
    void testCombinedRotationOrderMatters() {
        var v = LinearAlgebraEngine.createVector4D(1, 0, 0, 1);

        var rotX = LinearAlgebraEngine.createRotationMatrixX((float) Math.PI / 2);
        var rotY = LinearAlgebraEngine.createRotationMatrixY((float) Math.PI / 2);
        var result1 = rotY.multiply(rotX.multiply(v));

        var result2 = rotX.multiply(rotY.multiply(v));

        assertFalse(
                Math.abs(result1.getX() - result2.getX()) < 1e-6f &&
                        Math.abs(result1.getY() - result2.getY()) < 1e-6f &&
                        Math.abs(result1.getZ() - result2.getZ()) < 1e-6f
        );
    }
}
