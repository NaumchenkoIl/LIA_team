package math.Tests;

import math.Camera;
import math.LinealAlgebra.Vector3D;
import math.LinealAlgebra.Vector4D;
import math.LinearAlgebraEngine;
import math.Matrix.Matrix4x4;
import math.ModelTransform;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RenderingPipelineTest {

    @Test
    void testFullTransformationFromLocalToScreen() {

        Vector3D local = LinearAlgebraEngine.createVector3D(1, 0, 0);

        ModelTransform modelT = new ModelTransform();
        modelT.setRotationDeg(0, 90, 0);
        modelT.setTranslation(0, 0, 5);
        Matrix4x4 modelMatrix = modelT.getModelMatrix();

        Camera cam = new Camera(
                LinearAlgebraEngine.createVector3D(0, 0, 10),
                LinearAlgebraEngine.createVector3D(0, 0, 0)
        );
        Matrix4x4 viewMatrix = cam.getViewMatrix();
        Matrix4x4 projMatrix = cam.getProjectionMatrix();

        Vector4D world = modelMatrix.multiply(LinearAlgebraEngine.createVector4DFrom3D(local, 1));
        Vector4D view = viewMatrix.multiply(world);
        Vector4D clip = projMatrix.multiply(view);
        Vector4D ndc = clip.perspectiveDivide();

        assertTrue(ndc.getX() >= -1f && ndc.getX() <= 1f);
        assertTrue(ndc.getY() >= -1f && ndc.getY() <= 1f);
        assertTrue(ndc.getZ() >= -1f && ndc.getZ() <= 1f);
        assertEquals(1.0f, ndc.getW(), 1e-6f);

        int w = 800, h = 600;
        float screenX = (ndc.getX() + 1) * w / 2f;
        float screenY = (1 - ndc.getY()) * h / 2f;

        assertTrue(screenX >= 0 && screenX <= w);
        assertTrue(screenY >= 0 && screenY <= h);
    }

    @Test
    void testPointBehindCameraHasNegativeW() {
        var proj = Matrix4x4.perspective(90, 1, 1, 100);
        var point = LinearAlgebraEngine.createVector4D(0, 0, 1, 1); // за камерой!
        var clip = proj.multiply(point);
        assertTrue(clip.getW() < 0);
    }
}
