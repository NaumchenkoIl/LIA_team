package renderTests;

import org.junit.jupiter.api.Test;
import scene_master.calculator.NormalCalculator;
import scene_master.calculator.Triangulator;
import scene_master.model.Model;
import scene_master.model.Polygon;
import scene_master.reader.ObjReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TriangulationAndNormalsTest {

    @Test
    public void testTriangulationConvertsQuadsToTriangles() {
        String testFilePath = "src/tests/test_cube.obj";

        try {
            ObjReader reader = new ObjReader();
            Model model = reader.readModel(testFilePath);

            int originalPolygons = model.getPolygons().size();

            boolean hasQuads = false;
            for (Polygon p : model.getPolygons()) {
                if (p.getVertexIndices().size() == 4) {
                    hasQuads = true;
                    break;
                }
            }
            assertTrue(hasQuads, "Исходная модель должна содержать квады для теста триангуляции");

            Triangulator triangulator = new Triangulator();
            triangulator.triangulateModel(model);

            for (Polygon p : model.getPolygons()) {
                assertEquals(3, p.getVertexIndices().size(),
                        "All polygons must be triangles after triangulation");
            }

            assertTrue(model.getPolygons().size() > originalPolygons,
                    "Number of polygons should increase after triangulation");

        } catch (IOException e) {
            fail("Не удалось загрузить тестовый файл: " + e.getMessage());
        } catch (Exception e) {
            fail("Ошибка при выполнении теста триангуляции: " + e.getMessage());
        }
    }

    @Test
    public void testNormalRecalculationIgnoresFileNormals() {
        String testFilePath = "src/tests/test_cube.obj";

        try {
            ObjReader reader = new ObjReader();
            Model model = reader.readModel(testFilePath);

            assertNotNull(model, "Модель не должна быть null");
            assertFalse(model.getPolygons().isEmpty(), "Модель должна содержать полигоны");

            List<Polygon> originalPolygons = model.getPolygons();
            Polygon firstPoly = originalPolygons.get(0);

            NormalCalculator calculator = new NormalCalculator();
            calculator.calculateNormals(model);

            Polygon recalculatedPoly = model.getPolygons().get(0);
            assertNotNull(recalculatedPoly.getNormal(), "Normals must be recalculated");

            assertTrue(
                    Math.abs(recalculatedPoly.getNormal().getX()) > 1e-6 ||
                            Math.abs(recalculatedPoly.getNormal().getY()) > 1e-6 ||
                            Math.abs(recalculatedPoly.getNormal().getZ()) > 1e-6,
                    "Recalculated normal must be non-zero"
            );

        } catch (IOException e) {
            fail("Не удалось загрузить тестовый файл: " + e.getMessage());
        } catch (Exception e) {
            fail("Ошибка при выполнении теста нормалей: " + e.getMessage());
        }
    }
}