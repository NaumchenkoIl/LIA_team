package scene_master.renderer;

import javafx.scene.paint.Color;
import math.Camera;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;
import scene_master.model.Model3D;

import static org.junit.jupiter.api.Assertions.*;

public class RenderingModesTest {

    @Test
    public void testNoModeUsesSolidColor() {
        Model3D model = new Model3D("test");
        model.setBaseColor(Color.GREEN);

        Camera camera = new Camera(new Vector3D(0, 0, 5), new Vector3D(0, 0, 0));
        SoftwareRenderer renderer = new SoftwareRenderer(null, camera);

        renderer.setUseTexture(false);
        renderer.setUseLighting(false);

        Color result = renderer.calculatePixelColor(model, 0.5, 0.5, null);
        assertEquals(Color.GREEN, result, "Should return solid base color");
    }
}