package renderTests;

import math.Camera;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;
import scene_master.model.Model3D;
import scene_master.model.Polygon;
import scene_master.renderer.SoftwareRenderer;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RasterizationAndZBufferTest {

    @Test
    public void testZBufferPreventsBackfaceOverlap() {
        Model3D front = createSimpleTriangle(0, 0, -2);
        Model3D back = createSimpleTriangle(0, 0, -5);

        Camera camera = new Camera(new Vector3D(0, 0, 5), new Vector3D(0, 0, 0));
        SoftwareRenderer renderer = new SoftwareRenderer(null, camera);        double[][] zBuffer = new double[100][100];

        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                zBuffer[x][y] = Double.POSITIVE_INFINITY;
            }
        }

        renderPixel(zBuffer, 50, 50, -5.0);
        assertEquals(-5.0, zBuffer[50][50], 0.01);

        renderPixel(zBuffer, 50, 50, -2.0);
        assertEquals(-2.0, zBuffer[50][50], 0.01);

        renderPixel(zBuffer, 50, 50, -6.0);
        assertEquals(-2.0, zBuffer[50][50], 0.01); // не изменилось!
    }

    private void renderPixel(double[][] zBuffer, int x, int y, double depth) {
        if (depth < zBuffer[x][y]) {
            zBuffer[x][y] = depth;
        }
    }

    private Model3D createSimpleTriangle(float x, float y, float z) {
        Model3D model = new Model3D("test");
        model.getVertices().addAll(
                Arrays.asList(
                        new math.LinealAlgebra.Vector3D(x, y, z),
                        new math.LinealAlgebra.Vector3D(x+1, y, z),
                        new math.LinealAlgebra.Vector3D(x, y+1, z)
                )
        );
        model.getPolygons().add(new Polygon(new int[]{0, 1, 2}));
        return model;
    }
}