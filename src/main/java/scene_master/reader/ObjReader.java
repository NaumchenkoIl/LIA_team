package scene_master.reader;

import scene_master.model.Model; // класс модели данных
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
                        // игнорируем другие типы данных (нормали, текстуры и тп.)
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

    private void parseFace(String[] tokens, Model model, int lineNumber) { // парсит строку полигона
        if (tokens.length < 4) { // проверяем количество вершин
            throw new IllegalArgumentException("Face requires at least 3 vertices"); // минимум 3 вершины
        }

        List<Integer> vertexIndices = new ArrayList<>(); // список индексов вершин полигона
        List<Vector3D> vertices = model.getVertices(); // список всех вершин модели

        for (int i = 1; i < tokens.length; i++) { // обрабатываем каждый токен вершины (начинаем с 1, т.к. 0 - это "f")
            String vertexToken = tokens[i]; // токен вида "1" или "1/2/3"
            String[] parts = vertexToken.split("/"); // разделяем на вершину/текстуру/нормаль

            int vertexIndex = parseInteger(parts[0]); // парсим индекс вершины (первая часть)
            int actualIndex; // актуальный индекс в списке (0-based)

            if (vertexIndex > 0) { // если индекс положительный
                actualIndex = vertexIndex - 1; // преобразуем в 0-based (индексы в OBJ начинаются с 1)
            } else if (vertexIndex < 0) { // если индекс отрицательный
                actualIndex = vertices.size() + vertexIndex; // вычисляем относительный индекс с конца
            } else {
                throw new IllegalArgumentException("Invalid vertex index: 0"); // индекс 0 недопустим
            }

            if (actualIndex < 0 || actualIndex >= vertices.size()) { // проверяем границы
                throw new IllegalArgumentException( // если индекс вне диапазона
                        "Vertex index out of bounds: " + vertexIndex +
                                " (available vertices: 0 to " + (vertices.size() - 1) + ")"
                );
            }

            vertexIndices.add(actualIndex); // добавляем индекс в список
        }

        model.addPolygon(new scene_master.model.Polygon(vertexIndices)); // создаем и добавляем полигон
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