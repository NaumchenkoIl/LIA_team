package renderTests;

import math.Camera;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MultiCameraSupportTest {

    @Test
    public void testMultipleCamerasCanBeCreated() {
        List<Camera> cameras = new ArrayList<>();

        cameras.add(new Camera(new Vector3D(0, 0, 5), new Vector3D(0, 0, 0)));
        cameras.add(new Camera(new Vector3D(5, 0, 0), new Vector3D(0, 0, 0)));
        cameras.add(new Camera(new Vector3D(0, 5, 0), new Vector3D(0, 0, 0)));

        assertEquals(3, cameras.size(), "Should support multiple cameras");

        assertNotEquals(cameras.get(0).getPosition(), cameras.get(1).getPosition());
        assertNotEquals(cameras.get(1).getPosition(), cameras.get(2).getPosition());
    }
}