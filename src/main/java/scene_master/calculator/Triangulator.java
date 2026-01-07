package scene_master.calculator;

import scene_master.model.Model;
import scene_master.model.Polygon;
import scene_master.model.Vector3D;
import java.util.ArrayList;
import java.util.List;

public class Triangulator {

    public Triangulator() {
    }

    /**
     * Триангулирует все полигоны в модели
     */
    public void triangulateModel(Model model) {
        if (model == null) return;

        List<Polygon> originalPolygons = new ArrayList<>(model.getPolygons());
        model.getPolygons().clear();

        for (Polygon polygon : originalPolygons) {
            List<Polygon> triangles = triangulatePolygon(model, polygon);
            model.getPolygons().addAll(triangles);
        }
    }

    /**
     * Триангулирует один полигон на треугольники
     * Использует простой веерный алгоритм (ear clipping для выпуклых полигонов)
     * Для невыпуклых - более сложный алгоритм
     */
    public List<Polygon> triangulatePolygon(Model model, Polygon polygon) {
        List<Integer> indices = polygon.getVertexIndices();
        List<Polygon> triangles = new ArrayList<>();

        // Если полигон уже треугольник
        if (indices.size() == 3) {
            triangles.add(polygon);
            return triangles;
        }

        // Проверяем, является ли полигон выпуклым
        if (isConvexPolygon(model, polygon)) {
            // Для выпуклых полигонов используем веерный алгоритм
            return fanTriangulation(indices);
        } else {
            // Для невыпуклых используем алгоритм ear clipping
            return earClippingTriangulation(model, polygon);
        }
    }

    /**
     * Веерная триангуляция для выпуклых полигонов
     */
    private List<Polygon> fanTriangulation(List<Integer> indices) {
        List<Polygon> triangles = new ArrayList<>();
        int firstIndex = indices.get(0);

        for (int i = 1; i < indices.size() - 1; i++) {
            List<Integer> triangleIndices = new ArrayList<>();
            triangleIndices.add(firstIndex);
            triangleIndices.add(indices.get(i));
            triangleIndices.add(indices.get(i + 1));
            triangles.add(new Polygon(triangleIndices));
        }

        return triangles;
    }

    /**
     * Алгоритм Ear Clipping для невыпуклых полигонов
     */
    private List<Polygon> earClippingTriangulation(Model model, Polygon polygon) {
        List<Integer> indices = new ArrayList<>(polygon.getVertexIndices());
        List<Polygon> triangles = new ArrayList<>();
        List<Vector3D> vertices = model.getVertices();

        // Проверяем ориентацию полигона (по часовой или против)
        if (!isPolygonClockwise(model, polygon)) {
            // Если против часовой, разворачиваем
            List<Integer> reversed = new ArrayList<>();
            for (int i = indices.size() - 1; i >= 0; i--) {
                reversed.add(indices.get(i));
            }
            indices = reversed;
        }

        int n = indices.size();
        int[] V = new int[n];
        for (int i = 0; i < n; i++) {
            V[i] = indices.get(i);
        }

        // Алгоритм Ear Clipping
        while (n > 3) {
            boolean earFound = false;

            for (int i = 0; i < n; i++) {
                int prev = V[(i + n - 1) % n];
                int curr = V[i];
                int next = V[(i + 1) % n];

                if (isEar(prev, curr, next, vertices, V, n)) {
                    // Нашли ухо - создаем треугольник
                    triangles.add(new Polygon(prev, curr, next));

                    // Удаляем вершину curr из многоугольника
                    for (int j = i; j < n - 1; j++) {
                        V[j] = V[j + 1];
                    }
                    n--;
                    earFound = true;
                    break;
                }
            }

            if (!earFound) {
                // Если не нашли ухо, используем веерную триангуляцию как запасной вариант
                System.err.println("Ear clipping failed, using fan triangulation as fallback");
                return fanTriangulation(indices);
            }
        }

        // Последний треугольник
        triangles.add(new Polygon(V[0], V[1], V[2]));

        return triangles;
    }

    /**
     * Проверяет, является ли треугольник ухом
     */
    private boolean isEar(int prev, int curr, int next,
                          List<Vector3D> vertices, int[] polygon, int n) {
        Vector3D a = vertices.get(prev);
        Vector3D b = vertices.get(curr);
        Vector3D c = vertices.get(next);

        // Проверяем, что треугольник выпуклый
        if (!isConvex(a, b, c)) {
            return false;
        }

        // Проверяем, что внутри треугольника нет других вершин полигона
        for (int i = 0; i < n; i++) {
            int vertexIndex = polygon[i];
            if (vertexIndex == prev || vertexIndex == curr || vertexIndex == next) {
                continue;
            }

            Vector3D p = vertices.get(vertexIndex);
            if (isPointInTriangle(p, a, b, c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Проверяет, лежит ли точка внутри треугольника
     */
    private boolean isPointInTriangle(Vector3D p, Vector3D a, Vector3D b, Vector3D c) {
        // Используем барицентрические координаты
        Vector3D v0 = subtract(b, a);
        Vector3D v1 = subtract(c, a);
        Vector3D v2 = subtract(p, a);

        double dot00 = dot(v0, v0);
        double dot01 = dot(v0, v1);
        double dot11 = dot(v1, v1);
        double dot20 = dot(v2, v0);
        double dot21 = dot(v2, v1);

        double denom = dot00 * dot11 - dot01 * dot01;
        double u = (dot11 * dot20 - dot01 * dot21) / denom;
        double v = (dot00 * dot21 - dot01 * dot20) / denom;

        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    /**
     * Проверяет, является ли треугольник выпуклым (вершины в порядке обхода)
     */
    private boolean isConvex(Vector3D a, Vector3D b, Vector3D c) {
        Vector3D ab = subtract(b, a);
        Vector3D bc = subtract(c, b);
        Vector3D cross = ab.crossProduct(bc);

        // Для 2D проверки используем только Z-компоненту
        // В 3D нужно спроецировать на плоскость полигона, но для простоты используем Z
        return cross.getZ() >= 0;
    }

    /**
     * Проверяет, является ли полигон выпуклым
     */
    private boolean isConvexPolygon(Model model, Polygon polygon) {
        List<Integer> indices = polygon.getVertexIndices();
        List<Vector3D> vertices = model.getVertices();

        if (indices.size() < 4) return true; // Треугольник всегда выпуклый

        boolean sign = false;
        boolean signSet = false;

        for (int i = 0; i < indices.size(); i++) {
            Vector3D a = vertices.get(indices.get(i));
            Vector3D b = vertices.get(indices.get((i + 1) % indices.size()));
            Vector3D c = vertices.get(indices.get((i + 2) % indices.size()));

            Vector3D ab = subtract(b, a);
            Vector3D bc = subtract(c, b);
            Vector3D cross = ab.crossProduct(bc);

            double crossZ = cross.getZ();

            if (!signSet) {
                sign = crossZ >= 0;
                signSet = true;
            } else if ((crossZ >= 0) != sign) {
                return false; // Нашли разный знак - полигон невыпуклый
            }
        }

        return true;
    }

    /**
     * Проверяет ориентацию полигона (по часовой стрелке или против)
     * Возвращает true, если по часовой
     */
    private boolean isPolygonClockwise(Model model, Polygon polygon) {
        List<Integer> indices = polygon.getVertexIndices();
        List<Vector3D> vertices = model.getVertices();

        // Вычисляем площадь полигона (знак показывает ориентацию)
        double area = 0;

        for (int i = 0; i < indices.size(); i++) {
            Vector3D current = vertices.get(indices.get(i));
            Vector3D next = vertices.get(indices.get((i + 1) % indices.size()));

            area += (current.getX() * next.getY()) - (next.getX() * current.getY());
        }

        // Если area > 0 - против часовой, если < 0 - по часовой
        return area < 0;
    }

    /**
     * Вспомогательные математические операции
     */
    private Vector3D subtract(Vector3D a, Vector3D b) {
        return new Vector3D(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    private double dot(Vector3D a, Vector3D b) {
        return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
    }
}