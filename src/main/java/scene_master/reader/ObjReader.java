package scene_master.reader;

import scene_master.model.Model; // класс модели данных
import scene_master.model.Polygon;
import scene_master.model.Vector3D; // математический вектор
import java.io.BufferedReader; // буферизованное чтение
import java.io.FileReader; // чтение файлов
import java.io.IOException; // исключения ввода-вывода
import java.util.ArrayList;
import java.util.List;

public class ObjReader { //взял из своего 3 таска

    public Model readModel(String filename) throws IOException { // чтение модели из obj файла
        Model model = new Model(); // создаем пустую модель

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) { // открываем файл для чтения
            String line; // текущая строка
            int lineNumber = 0; // номер строки для сообщений об ошибках

            while ((line = reader.readLine()) != null) { // читаем файл построчно
                lineNumber++; // увеличиваем счетчик строк
                line = line.trim(); // удаляем пробелы в начале и конце

                if (line.isEmpty() || line.startsWith("#")) { // если строка пустая или комментарий
                    continue; // пропускаем
                }

                String[] tokens = line.split("\\s+"); // разбиваем строку на токены по пробелам

                try {
                    switch (tokens[0]) { // анализируем первый токен (тип данных)
                        case "v":  // вершина
                            parseVertex(tokens, model, lineNumber); // парсим координаты вершины
                            break;
                        case "f":  // полигон
                            parseFace(tokens, model, lineNumber); // парсим индексы полигона
                            break;
                        case "vt":
                            parseTextureVertex(tokens, model, lineNumber);
                            break;
                        case "vn":
                            parseNormal(tokens, model, lineNumber);
                            break;
                    }
                } catch (Exception e) { // ловим любые исключения при парсинге
                    throw new IOException("Error parsing line " + lineNumber + ": " + line, e); // перебрасываем с контекстом
                }
            }
        }

        return model; // возвращаем заполненную модель
    }

    private void parseVertex(String[] tokens, Model model, int lineNumber) { // парсит строку вершины
        if (tokens.length < 4) { // проверяем количество координат
            throw new IllegalArgumentException("Vertex requires at least 3 coordinates"); // минимум 3 координаты
        }

        double x = parseDouble(tokens[1]); // парсим координату X
        double y = parseDouble(tokens[2]); // парсим координату Y
        double z = parseDouble(tokens[3]); // парсим координату Z

        model.addVertex(new Vector3D(x, y, z)); // добавляем вершину в модель
    }

    private void parseFace(String[] tokens, Model model, int lineNumber) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Face requires at least 3 vertices");
        }

        List<Integer> vertexIndices = new ArrayList<>();
        List<Integer> texIndices = new ArrayList<>();
        List<Integer> normalIndices = new ArrayList<>();

        List<Vector3D> vertices = model.getVertices();
        List<Vector3D> normals = model.getNormals(); // ← добавь геттер getNormals() в Model!
        List<Vector3D> texCoords = model.getTextureVertices(); // ← добавь getTextureVertices() в Model!

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) continue;

            String[] parts = token.split("/");
            int vIdx = parseVertexIndex(parts[0], vertices.size(), lineNumber);
            vertexIndices.add(vIdx);

            int tIdx = -1;
            int nIdx = -1;

            if (parts.length > 1 && !parts[1].isEmpty()) {
                tIdx = parseVertexIndex(parts[1], texCoords != null ? texCoords.size() : 0, lineNumber);
                texIndices.add(tIdx);
            } else {
                texIndices.add(-1); // placeholder
            }

            if (parts.length > 2 && !parts[2].isEmpty()) {
                nIdx = parseVertexIndex(parts[2], normals != null ? normals.size() : 0, lineNumber);
                normalIndices.add(nIdx);
            } else {
                normalIndices.add(-1); // placeholder
            }
        }

        // Убираем placeholder'ы (-1), если UV/нормалей нет вообще
        boolean hasTex = texCoords != null && !texCoords.isEmpty();
        boolean hasNorm = normals != null && !normals.isEmpty();

        if (!hasTex) texIndices.clear();
        if (!hasNorm) normalIndices.clear();

        Polygon polygon = new Polygon(vertexIndices, texIndices, normalIndices);
        model.addPolygon(polygon);
    }

    private void parseTextureVertex(String[] tokens, Model model, int lineNumber) {
        double u = parseDouble(tokens[1]);
        double v = tokens.length > 2 ? parseDouble(tokens[2]) : 0.0;
        model.addTextureVertex(new Vector3D(u, v, 0));
    }

    private void parseNormal(String[] tokens, Model model, int lineNumber) {
        double x = parseDouble(tokens[1]);
        double y = parseDouble(tokens[2]);
        double z = parseDouble(tokens[3]);
        model.addNormal(new Vector3D(x, y, z));
    }

    private int parseVertexIndex(String str, int size, int line) {
        if (str.isEmpty()) return -1;
        int idx = parseInteger(str);
        if (idx == 0) throw new IllegalArgumentException("OBJ indices start from 1");
        if (size == 0) return idx - 1; // allow if list not initialized yet

        int actual;
        if (idx > 0) {
            actual = idx - 1;
        } else {
            actual = size + idx;
        }
        if (actual < 0 || actual >= size) {
            throw new IllegalArgumentException(
                    "Index out of bounds on line " + line + ": " + idx +
                            " (valid: 1 to " + size + ")"
            );
        }
        return actual;
    }

    private double parseDouble(String str) { // парсит строку в double
        try {
            return Double.parseDouble(str); // пытаемся преобразовать
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + str); // ошибка формата числа
        }
    }

    private int parseInteger(String str) { // парсит строку в integer
        try {
            return Integer.parseInt(str); // пытаемся преобразовать
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer format: " + str); // ошибка формата целого
        }
    }
}