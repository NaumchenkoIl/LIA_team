import scene_master.calculator.Triangulator;
import scene_master.model.Model;
import scene_master.model.Polygon;
import scene_master.model.Vector3D;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriangulatorTest {

    @Test
    void testTriangulateSquare() {
        // Создаем квадрат
        Model model = new Model();
        model.addVertex(new Vector3D(0, 0, 0)); // 0
        model.addVertex(new Vector3D(1, 0, 0)); // 1
        model.addVertex(new Vector3D(1, 1, 0)); // 2
        model.addVertex(new Vector3D(0, 1, 0)); // 3

        // Квадрат (4 вершины)
        Polygon square = new Polygon(Arrays.asList(0, 1, 2, 3));
        model.addPolygon(square);

        Triangulator triangulator = new Triangulator();
        triangulator.triangulateModel(model);

        // После триангуляции должно быть 2 треугольника
        assertEquals(2, model.getPolygons().size());

        // Проверяем, что все полигоны - треугольники
        for (Polygon p : model.getPolygons()) {
            assertEquals(3, p.getVertexIndices().size());
        }
    }

    @Test
    void testTriangulateTriangle() {
        // Треугольник должен остаться треугольником
        Model model = new Model();
        model.addVertex(new Vector3D(0, 0, 0));
        model.addVertex(new Vector3D(1, 0, 0));
        model.addVertex(new Vector3D(0, 1, 0));

        Polygon triangle = new Polygon(Arrays.asList(0, 1, 2));
        model.addPolygon(triangle);

        Triangulator triangulator = new Triangulator();
        triangulator.triangulateModel(model);

        // Должен остаться 1 полигон
        assertEquals(1, model.getPolygons().size());
        assertEquals(3, model.getPolygons().get(0).getVertexIndices().size());
    }

    @Test
    void testPentagonTriangulation() {
        // Пятиугольник
        Model model = new Model();
        model.addVertex(new Vector3D(0, 0, 0));   // 0
        model.addVertex(new Vector3D(2, 0, 0));   // 1
        model.addVertex(new Vector3D(3, 1, 0));   // 2
        model.addVertex(new Vector3D(1, 2, 0));   // 3
        model.addVertex(new Vector3D(-1, 1, 0));  // 4

        Polygon pentagon = new Polygon(Arrays.asList(0, 1, 2, 3, 4));
        model.addPolygon(pentagon);

        Triangulator triangulator = new Triangulator();
        triangulator.triangulateModel(model);

        // Пятиугольник должен быть разбит на 3 треугольника
        assertEquals(3, model.getPolygons().size());

        for (Polygon p : model.getPolygons()) {
            assertEquals(3, p.getVertexIndices().size());
        }
    }

    @Test
    void testConvexPolygonDetection() {
        Model model = new Model();
        model.addVertex(new Vector3D(0, 0, 0));
        model.addVertex(new Vector3D(1, 0, 0));
        model.addVertex(new Vector3D(1, 1, 0));
        model.addVertex(new Vector3D(0, 1, 0));

        Polygon convex = new Polygon(Arrays.asList(0, 1, 2, 3));

        Triangulator triangulator = new Triangulator();
        List<Polygon> triangles = triangulator.triangulatePolygon(model, convex);

        // Выпуклый четырехугольник -> 2 треугольника
        assertEquals(2, triangles.size());
    }

    @Test
    void testEmptyModel() {
        Model model = new Model();
        Triangulator triangulator = new Triangulator();

        // Не должно быть исключений
        assertDoesNotThrow(() -> triangulator.triangulateModel(model));
        assertEquals(0, model.getPolygons().size());
    }

    @Test
    void testNullModel() {
        Triangulator triangulator = new Triangulator();

        // Не должно быть исключений при null
        assertDoesNotThrow(() -> triangulator.triangulateModel(null));
    }
}