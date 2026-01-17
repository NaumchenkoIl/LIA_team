package mathTests;

import math.LinearAlgebraEngine;
import math.ModelTransform;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ModelTransformTest {

    @Test
    void testModelMatrixAppliesScaleThenRotateThenTranslate() {
        var transform = new ModelTransform();
        transform.setScale(2, 2, 2);
        transform.setRotationDeg(0, 90, 0); // поворот вокруг Y на 90°
        transform.setTranslation(0, 0, 5);

        var vertex = LinearAlgebraEngine.createVector3D(1, 0, 0);
        var transformed = transform.transformVertex(vertex);

        assertEquals(0.0f, transformed.getX(), 1e-6f);
        assertEquals(0.0f, transformed.getY(), 1e-6f);
        assertEquals(3.0f, transformed.getZ(), 1e-6f);
    }

    @Test
    void testRotation360DegreesReturnsOriginal() {
        var v = LinearAlgebraEngine.createVector3D(1, 2, 3);
        var transform = new ModelTransform();
        transform.setRotationDeg(0, 360, 0);
        var result = transform.transformVertex(v);
        assertEquals(v.getX(), result.getX(), 1e-6f);
        assertEquals(v.getY(), result.getY(), 1e-6f);
        assertEquals(v.getZ(), result.getZ(), 1e-6f);
    }
}
