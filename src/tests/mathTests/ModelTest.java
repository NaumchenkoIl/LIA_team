package mathTests;

import math.LinearAlgebraEngine;
import math.Model;
import math.ModelTransform;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ModelTest {
    @Test
    void testApplyTransformAndReset() {
        var original = new Vector3D[]{ LinearAlgebraEngine.createVector3D(1, 1, 1) };
        var model = new Model(original);

        var transform = new ModelTransform();
        transform.setScale(3, 3, 3);
        model.applyTransform(transform);

        var current = model.getCurrentVertices();
        assertEquals(3.0f, current[0].getX(), 1e-6f);

        model.resetToOriginal();
        current = model.getCurrentVertices();
        assertEquals(1.0f, current[0].getX(), 1e-6f);
    }
}
