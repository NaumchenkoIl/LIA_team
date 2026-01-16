package scene_master.reader;

import scene_master.model.Model;
import math.LinealAlgebra.Vector3D;
import scene_master.model.Polygon;
import scene_master.model.TexturePoint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ObjReader {

    public Model readModel(String filename) throws IOException {
        Model model = new Model();

        // Проверка существования файла
        java.io.File file = new java.io.File(filename);
        if (!file.exists()) {
            throw new IOException("Файл не найден: " + filename);
        }

        if (!file.canRead()) {
            throw new IOException("Нет прав на чтение файла: " + filename);
        }

        if (file.length() == 0) {
            throw new IOException("Файл пустой: " + filename);
        }

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
                        case "mtllib": // библиотека материалов
                        case "usemtl": // использование материала
                        case "s":      // сглаживание
                        case "o":      // объект
                        case "g":      // группа
                            // Игнорируем эти директивы, но логируем
                            System.out.println("Директива [" + tokens[0] + "] проигнорирована (строка " + lineNumber + ")");
                            break;
                        default:
                            System.out.println("Неизвестная директива [" + tokens[0] + "] проигнорирована (строка " + lineNumber + ")");
                    }
                } catch (Exception e) {
                    throw new IOException("Ошибка парсинга строки " + lineNumber + ": " + line +
                            "\nПричина: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new IOException("Ошибка чтения файла '" + filename + "': " + e.getMessage(), e);
        }

        // Проверяем валидность модели
        if (model.getVertices().isEmpty()) {
            throw new IOException("Модель не содержит вершин");
        }

        if (model.getPolygons().isEmpty()) {
            System.out.println("Предупреждение: модель не содержит полигонов");
        }

        System.out.println("Модель успешно загружена:");
        System.out.println("  Вершин: " + model.getVertices().size());
        System.out.println("  Текстурных координат: " + model.getTexturePoints().size());
        System.out.println("  Нормалей: " + model.getNormals().size());
        System.out.println("  Полигонов: " + model.getPolygons().size());

        return model;
    }

    private void parseVertex(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Вершина требует минимум 3 координаты (строка " + lineNumber + ")");
        }

        try {
            double x = parseDoubleWithComma(tokens[1]);
            double y = parseDoubleWithComma(tokens[2]);
            double z = parseDoubleWithComma(tokens[3]);

            model.addVertex(new Vector3D((float) x, (float) y, (float) z));
        } catch (NumberFormatException | ParseException e) {
            throw new IllegalArgumentException("Недопустимый формат координат вершины (строка " + lineNumber + ")", e);
        }
    }

    private void parseTexture(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Текстурные координаты требуют минимум 1 значение (строка " + lineNumber + ")");
        }

        try {
            double u = parseDoubleWithComma(tokens[1]);
            double v = tokens.length >= 3 ? parseDoubleWithComma(tokens[2]) : 0.0;

            model.addTexturePoint(new TexturePoint(u, v));
        } catch (NumberFormatException | ParseException e) {
            throw new IllegalArgumentException("Недопустимый формат текстурных координат (строка " + lineNumber + ")", e);
        }
    }

    private void parseNormal(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Нормаль требует 3 координаты (строка " + lineNumber + ")");
        }

        try {
            double x = parseDoubleWithComma(tokens[1]);
            double y = parseDoubleWithComma(tokens[2]);
            double z = parseDoubleWithComma(tokens[3]);

            model.addNormal(new Vector3D((float) x, (float) y, (float) z));
        } catch (NumberFormatException | ParseException e) {
            throw new IllegalArgumentException("Недопустимый формат нормали (строка " + lineNumber + ")", e);
        }
    }

    private void parseFace(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Полигон требует минимум 3 вершины (строка " + lineNumber + ")");
        }

        List<Integer> vertexIndices = new ArrayList<>();
        List<Integer> textureIndices = new ArrayList<>();
        List<Integer> normalIndices = new ArrayList<>();
        List<TexturePoint> texturePoints = model.getTexturePoints();

        for (int i = 1; i < tokens.length; i++) {
            String vertexToken = tokens[i];

            // Разделяем на части
            String[] parts = vertexToken.split("/");

            if (parts.length == 0 || parts[0].isEmpty()) {
                throw new IllegalArgumentException("Недопустимый формат вершины полигона (строка " + lineNumber + ")");
            }

            // Индекс вершины (обязательный)
            int vertexIndex = parseInteger(parts[0]);
            int actualVertexIndex = convertIndex(vertexIndex, model.getVertices().size(), "вершины", lineNumber);
            vertexIndices.add(actualVertexIndex);

            // Индекс текстуры (опциональный)
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                int textureIndex = parseInteger(parts[1]);
                int actualTextureIndex = convertIndex(textureIndex, texturePoints.size(), "текстуры", lineNumber);
                textureIndices.add(actualTextureIndex);
            }

            // Индекс нормали (опциональный)
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                int normalIndex = parseInteger(parts[2]);
                int actualNormalIndex = convertIndex(normalIndex, model.getNormals().size(), "нормали", lineNumber);
                normalIndices.add(actualNormalIndex);
            }
        }

        // Создаем полигон
        Polygon polygon = new Polygon(vertexIndices);

        if (!textureIndices.isEmpty()) {
            polygon.setTextureIndices(textureIndices);
        }

        if (!normalIndices.isEmpty()) {
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
                            " (доступно: 1 до " + totalCount + "), строка " + lineNumber
            );
        }

        return actualIndex;
    }

    /**
     * Парсит число с поддержкой запятой как десятичного разделителя
     */
    private double parseDoubleWithComma(String str) throws NumberFormatException, ParseException {
        try {
            // Сначала пробуем стандартный парсинг (для точек)
            return Double.parseDouble(str);
        } catch (NumberFormatException e1) {
            try {
                // Если не получилось, пробуем заменить запятую на точку
                String normalized = str.replace(',', '.');
                return Double.parseDouble(normalized);
            } catch (NumberFormatException e2) {
                // Если и это не сработало, используем NumberFormat с локалью
                NumberFormat format = NumberFormat.getInstance(Locale.FRANCE); // Используем локаль с запятой
                return format.parse(str).doubleValue();
            }
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