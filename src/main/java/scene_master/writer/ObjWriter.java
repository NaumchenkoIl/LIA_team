package scene_master.writer;

import math.LinealAlgebra.Vector3D;
import scene_master.model.Model3D;
import scene_master.model.TexturePoint;
import scene_master.model.Polygon;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ObjWriter {

    public void writeModel(Model3D model, String filename) throws IOException {
        writeModel(model, filename, false);
    }

    public void writeModel(Model3D model, String filename, boolean applyTransformations) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            writer.write("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            writer.write("# 3D Редактор - Экспорт модели\n");
            writer.write("# Дата сохранения: " + now.format(formatter) + "\n");
            writer.write("# Название модели: " + model.getName() + "\n");
            writer.write("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            writer.write("# Статистика модели:\n");
            writer.write("# • Вершин: " + model.getVertices().size() + "\n");
            writer.write("# • Текстурных координат: " + model.getTexturePoints().size() + "\n");
            writer.write("# • Нормалей: " + model.getNormals().size() + "\n");
            writer.write("# • Полигонов: " + model.getPolygons().size() + "\n");
            writer.write("# • Трансформации применены: " + (applyTransformations ? "Да" : "Нет") + "\n");

            if (applyTransformations) {
                writer.write("# Трансформации:\n");
                writer.write("# • Перемещение: (" + model.translateXProperty().get() + ", " +
                        model.translateYProperty().get() + ", " + model.translateZProperty().get() + ")\n");
                writer.write("# • Вращение: (" + model.rotateXProperty().get() + "°, " +
                        model.rotateYProperty().get() + "°, " + model.rotateZProperty().get() + "°)\n");
                writer.write("# • Масштаб: (" + model.scaleXProperty().get() + ", " +
                        model.scaleYProperty().get() + ", " + model.scaleZProperty().get() + ")\n");
            }

            writer.write("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n");

            writer.write("# Вершины (v x y z)\n");// зписываем вершины
            List<Vector3D> vertices = model.getVertices();
            for (int i = 0; i < vertices.size(); i++) {
                Vector3D vertex = vertices.get(i);
                double x = vertex.getX();
                double y = vertex.getY();
                double z = vertex.getZ();

                if (applyTransformations) {
                    // здесь будет код применения трансформаций (когда 2-й человек реализует)
                    // пока просто записываем исходные координаты
                    writer.write(String.format("# Вершина %d (оригинальные координаты)\n", i + 1));
                }

                writer.write(String.format("v %.6f %.6f %.6f\n", x, y, z));
            }

            List<TexturePoint> texturePoints = model.getTexturePoints();// записываем текстурные координаты
            if (!texturePoints.isEmpty()) {
                writer.write("\n# Текстурные координаты (vt u v)\n");
                for (int i = 0; i < texturePoints.size(); i++) {
                    TexturePoint tp = texturePoints.get(i);
                    writer.write(String.format("# Текстурная координата %d\n", i + 1));
                    writer.write(String.format("vt %.6f %.6f\n", tp.getU(), tp.getV()));
                }
            }

            List<Vector3D> normals = model.getNormals();// записываем нормали
            if (!normals.isEmpty()) {
                writer.write("\n# Нормали вершин (vn x y z)\n");
                for (int i = 0; i < normals.size(); i++) {
                    Vector3D normal = normals.get(i);
                    writer.write(String.format("# Нормаль %d\n", i + 1));
                    writer.write(String.format("vn %.6f %.6f %.6f\n", normal.getX(), normal.getY(), normal.getZ()));
                }
            }

            writer.write("\n# Полигоны (грани) (f вершина/текстура/нормаль)\n");// записываем полигоны
            List<Polygon> polygons = model.getPolygons();
            for (int i = 0; i < polygons.size(); i++) {
                Polygon polygon = polygons.get(i);
                List<Integer> vertexIndices = polygon.getVertexIndices();
                List<Integer> textureIndices = polygon.getTextureIndices();
                List<Integer> normalIndices = polygon.getNormalIndices();

                writer.write(String.format("# Полигон %d (%d вершин)\n", i + 1, vertexIndices.size()));

                writer.write("f");
                for (int j = 0; j < vertexIndices.size(); j++) {
                    Integer vertexIndex = vertexIndices.get(j) + 1; // OBJ индексы начинаются с 1
                    Integer textureIndex = j < textureIndices.size() ? textureIndices.get(j) + 1 : null;
                    Integer normalIndex = j < normalIndices.size() ? normalIndices.get(j) + 1 : null;

                    writer.write(" " + vertexIndex);

                    if (textureIndex != null || normalIndex != null) {
                        writer.write("/");
                        if (textureIndex != null) {
                            writer.write(textureIndex.toString());
                        }
                        if (normalIndex != null) {
                            writer.write("/" + normalIndex);
                        }
                    }
                }
                writer.write("\n");
            }

            writer.write("# Конец файла\n");
            writer.write("# Всего записано:\n");
            writer.write("# • Вершин: " + vertices.size() + "\n");
            writer.write("# • Текстурных координат: " + texturePoints.size() + "\n");
            writer.write("# • Нормалей: " + normals.size() + "\n");
            writer.write("# • Полигонов: " + polygons.size() + "\n");
        }
    }

    public void writeModelWithOptions(Model3D model, String filename) throws IOException {
        // пока простой вариант - всегда сохраняем без трансформаций
        writeModel(model, filename, false);
    }
}