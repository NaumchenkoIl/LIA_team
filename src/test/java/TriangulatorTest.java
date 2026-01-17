import scene_master.calculator.Triangulator;
import scene_master.model.Model;
import scene_master.model.Polygon;
import math.LinealAlgebra.Vector3D;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriangulatorTest {

    @Test
    void testTriangulateSquare() {
        Model model = new Model();// создаем квадрат
        model.addVertex(new Vector3D(0, 0, 0)); // 0
        model.addVertex(new Vector3D(1, 0, 0)); // 1
        model.addVertex(new Vector3D(1, 1, 0)); // 2
        model.addVertex(new Vector3D(0, 1, 0)); // 3

        Polygon square = new Polygon(Arrays.asList(0, 1, 2, 3));// квадрат (4 вершины)
        model.addPolygon(square);

        Triangulator triangulator = new Triangulator();
        triangulator.triangulateModel(model);

        assertEquals(2, model.getPolygons().size());// после триангуляции должно быть 2 треугольника

        for (Polygon p : model.getPolygons()) {// проверяем, что все полигоны - треугольники
            assertEquals(3, p.getVertexIndices().size());
        }
    }

    @Test
    void testTriangulateTriangle() {
        Model model = new Model();// треугольник должен остаться треугольником
        model.addVertex(new Vector3D(0, 0, 0));
        model.addVertex(new Vector3D(1, 0, 0));
        model.addVertex(new Vector3D(0, 1, 0));

        Polygon triangle = new Polygon(Arrays.asList(0, 1, 2));
        model.addPolygon(triangle);

        Triangulator triangulator = new Triangulator();
        triangulator.triangulateModel(model);

        assertEquals(1, model.getPolygons().size());// должен остаться 1 полигон
        assertEquals(3, model.getPolygons().get(0).getVertexIndices().size());
    }

    @Test
    void testPentagonTriangulation() {
        Model model = new Model();
        model.addVertex(new Vector3D(0, 0, 0));   // пятиугольник
        model.addVertex(new Vector3D(2, 0, 0));   // 1
        model.addVertex(new Vector3D(3, 1, 0));   // 2
        model.addVertex(new Vector3D(1, 2, 0));   // 3
        model.addVertex(new Vector3D(-1, 1, 0));  // 4

        Polygon pentagon = new Polygon(Arrays.asList(0, 1, 2, 3, 4));
        model.addPolygon(pentagon);

        Triangulator triangulator = new Triangulator();
        triangulator.triangulateModel(model);

        assertEquals(3, model.getPolygons().size());// пятиугольник должен быть разбит на 3 треугольника

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


        assertEquals(2, triangles.size());// выпуклый четырехугольник -> 2 треугольника
    }

    @Test
    void testEmptyModel() {
        Model model = new Model();
        Triangulator triangulator = new Triangulator();

        assertDoesNotThrow(() -> triangulator.triangulateModel(model));// не должно быть исключений
        assertEquals(0, model.getPolygons().size());
    }

    @Test
    void testNullModel() {
        Triangulator triangulator = new Triangulator();

        assertDoesNotThrow(() -> triangulator.triangulateModel(null));// не должно быть исключений при null
    }
}