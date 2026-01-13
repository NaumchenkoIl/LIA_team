package scene_master.writer;

import scene_master.model.Model;
import scene_master.model.Polygon;
import scene_master.model.Vector3D;

import java.util.List;

public final class ModelValidator {

    private ModelValidator() {}

    public static void validate(Model model) {
        if (model == null) {
            throw new ObjWriterException("Model cannot be null");
        }

        List<Vector3D> vertices = model.getVertices();
        List<Polygon> polygons = model.getPolygons();

        if (vertices == null) {
            throw new ObjWriterException("Vertices list cannot be null");
        }
        if (vertices.isEmpty()) {
            throw new ObjWriterException("Model must have at least one vertex");
        }

        if (polygons == null) {
            throw new ObjWriterException("Polygons list cannot be null");
        }
        if (polygons.isEmpty()) {
            throw new ObjWriterException("Model must have at least one polygon");
        }

        // Валидация вершин
        for (int i = 0; i < vertices.size(); i++) {
            Vector3D v = vertices.get(i);
            if (v == null) {
                throw new ObjWriterException("Vertex " + i + " is null");
            }
            validateFloat(v.getX(), "x", i);
            validateFloat(v.getY(), "y", i);
            validateFloat(v.getZ(), "z", i);
        }

        // Валидация полигонов
        for (int i = 0; i < polygons.size(); i++) {
            Polygon p = polygons.get(i);
            if (p == null) {
                throw new ObjWriterException("Polygon " + i + " is null");
            }

            List<Integer> vis = p.getVertexIndices();
            if (vis == null || vis.isEmpty() || vis.size() < 3) {
                throw new ObjWriterException("Polygon " + i + " must have ≥3 vertices");
            }

            for (int idx : vis) {
                if (idx < 0 || idx >= vertices.size()) {
                    throw new ObjWriterException(
                            "Polygon " + i + ": invalid vertex index " + idx +
                                    " (valid: 0–" + (vertices.size() - 1) + ")");
                }
            }

            // Валидация UV (если есть)
            List<Integer> vtis = p.getTextureIndices();
            if (vtis != null && !vtis.isEmpty()) {
                if (vtis.size() != vis.size()) {
                    throw new ObjWriterException(
                            "Polygon " + i + ": vertex and texture index counts differ");
                }
                // UV-координаты хранятся в Model3D, а не в Model → не можем проверить границы
                // Пропускаем проверку индексов UV
            }
        }
    }

    private static void validateFloat(double f, String coord, int i) {
        if (Double.isNaN(f)) {
            throw new ObjWriterException("Vertex " + i + " has NaN in " + coord);
        }
        if (Double.isInfinite(f)) {
            throw new ObjWriterException("Vertex " + i + " has Infinity in " + coord);
        }
    }
}