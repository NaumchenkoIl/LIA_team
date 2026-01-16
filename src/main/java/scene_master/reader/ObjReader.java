package scene_master.reader;

import scene_master.model.Model;
import math.LinealAlgebra.Vector3D;
import scene_master.model.TexturePoint;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjReader {

    public Model readModel(String filename) throws IOException {
        Model model = new Model();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split("\\s+");

                try {
                    switch (tokens[0]) {
                        case "v":  // вершина (x, y, z[, w])
                            parseVertex(tokens, model, lineNumber);
                            break;
                        case "vt": // текстурные координаты (u, v[, w])
                            parseTexture(tokens, model, lineNumber);
                            break;
                        case "vn": // нормали (x, y, z)
                            parseNormal(tokens, model, lineNumber);
                            break;
                        case "f":  // полигон
                            parseFace(tokens, model, lineNumber);
                            break;
                    }// другие директивы пока игнорируем
                } catch (Exception e) {
                    throw new IOException("Ошибка парсинга строки " + lineNumber + ": " + line, e);
                }
            }
        }

        return model;
    }

    private void parseVertex(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Вершина требует минимум 3 координаты");
        }

        double x = parseDouble(tokens[1]);
        double y = parseDouble(tokens[2]);
        double z = parseDouble(tokens[3]);

        model.addVertex(new Vector3D((float) x, (float) y, (float) z));
    }

    private void parseTexture(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Текстурные координаты требуют минимум 1 значение");
        }

        double u = parseDouble(tokens[1]);
        double v = tokens.length >= 3 ? parseDouble(tokens[2]) : 0.0;

        model.addTexturePoint(new TexturePoint(u, v));
    }

    private void parseNormal(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Нормаль требует 3 координаты");
        }

        double x = parseDouble(tokens[1]);
        double y = parseDouble(tokens[2]);
        double z = parseDouble(tokens[3]);

        model.addNormal(new Vector3D((float) x, (float) y, (float) z));
    }

    private void parseFace(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Полигон требует минимум 3 вершины");
        }

        List<Integer> vertexIndices = new ArrayList<>();
        List<Integer> textureIndices = new ArrayList<>();
        List<Integer> normalIndices = new ArrayList<>();

        for (int i = 1; i < tokens.length; i++) {
            String vertexToken = tokens[i];
            String[] parts = vertexToken.split("/");

            int vertexIndex = parseInteger(parts[0]);// индекс вершины (обязательный)
            vertexIndices.add(convertIndex(vertexIndex, model.getVertices().size(), "вершины", lineNumber));

            if (parts.length >= 2 && !parts[1].isEmpty()) {// индекс текстуры (опциональный)
                int textureIndex = parseInteger(parts[1]);
                textureIndices.add(convertIndex(textureIndex, model.getTexturePoints().size(), "текстуры", lineNumber));
            }

            if (parts.length >= 3 && !parts[2].isEmpty()) {// индекс нормали (опциональный)
                int normalIndex = parseInteger(parts[2]);
                normalIndices.add(convertIndex(normalIndex, model.getNormals().size(), "нормали", lineNumber));
            }
        }

        scene_master.model.Polygon polygon = new scene_master.model.Polygon(vertexIndices);

        if (!textureIndices.isEmpty()) {// сохраняем текстуры, если они есть
            polygon.setTextureIndices(textureIndices);
        }

        if (!normalIndices.isEmpty()) {// сохраняем нормали, если они есть
            polygon.setNormalIndices(normalIndices);
        }

        model.addPolygon(polygon);
    }

    private int convertIndex(int index, int totalCount, String type, int lineNumber) {
        int actualIndex;

        if (index > 0) {
            actualIndex = index - 1;
        } else if (index < 0) {
            actualIndex = totalCount + index;
        } else {
            throw new IllegalArgumentException("Недопустимый индекс " + type + ": 0 (строка " + lineNumber + ")");
        }

        if (actualIndex < 0 || actualIndex >= totalCount) {
            throw new IllegalArgumentException(
                    "Индекс " + type + " вне диапазона: " + index +
                            " (доступно: 0 до " + (totalCount - 1) + "), строка " + lineNumber
            );
        }

        return actualIndex;
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Недопустимый числовой формат: " + str);
        }
    }

    private int parseInteger(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Недопустимый целочисленный формат: " + str);
        }
    }
}