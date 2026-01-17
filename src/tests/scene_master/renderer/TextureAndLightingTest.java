package scene_master.renderer;

import javafx.scene.paint.Color;
import math.Camera;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextureAndLightingTest {

    @Test
    public void testDirectLightingMakesColorBright() {
        Camera camera = new Camera(new Vector3D(0, 0, 5), new Vector3D(0, 0, 0));
        SoftwareRenderer renderer = new SoftwareRenderer(null, camera);

        double[] normal = {0, 0, -1};
        Color baseColor = Color.DARKGRAY;

        renderer.setUseLighting(true);
        Color litColor = renderer.applyLightingToColor(baseColor, normal);

        assertTrue(litColor.getRed() > baseColor.getRed() * 1.5,
                "Direct lighting should significantly brighten the color");
    }
}