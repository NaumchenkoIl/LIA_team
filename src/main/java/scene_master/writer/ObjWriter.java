package scene_master.writer;

import scene_master.model.Model3D;
import scene_master.model.Vertex;
import scene_master.model.Polygon;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ObjWriter {

    public void writeModel(Model3D model, String filename) throws IOException {
        writeModel(model, filename, false);
    }

    public void writeModel(Model3D model, String filename, boolean applyTransformations) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("# Модель сохранена из 3D Редактора\n");
            writer.write("# Вершин: " + model.getVertices().size() + "\n");
            writer.write("# Полигонов: " + model.getPolygons().size() + "\n");
            writer.write("# Трансформации применены: " + (applyTransformations ? "Да" : "Нет") + "\n\n");

            writer.write("# Вершины\n");
            List<Vertex> vertices = model.getVertices();

            for (Vertex vertex : vertices) {
                double x = vertex.x;
                double y = vertex.y;
                double z = vertex.z;

                if (applyTransformations) { //  применить трансформации (когда 2-й человек реализует)
                    // здесь будет код применения трансформаций
                }

                writer.write(String.format("v %.6f %.6f %.6f\n", x, y, z));
            }

            writer.write("\n# Полигоны (грани)\n");// записываем полигоны
            List<Polygon> polygons = model.getPolygons();

            for (Polygon polygon : polygons) {
                List<Integer> indices = polygon.getVertexIndices();

                writer.write("f");
                for (Integer index : indices) {
                    writer.write(" " + (index + 1));// в OBJ индексы начинаются с 1 (не с 0)
                }
                writer.write("\n");
            }

            writer.write("\n# Конец файла\n");// записываем комментарий в конце
        }
    }


    public void writeModelWithOptions(Model3D model, String filename) throws IOException {// метод для сохранения с выбором формата через диалог
        // пока простой вариант - всегда сохраняем без трансформаций
        writeModel(model, filename, false);
    }
}