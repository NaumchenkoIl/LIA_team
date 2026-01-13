package scene_master.model;

import javafx.beans.property.SimpleStringProperty; // простое строковое свойство
import javafx.beans.property.StringProperty; // интерфейс строкового свойства
import scene_master.calculator.NormalCalculator;
import scene_master.calculator.Triangulator;

public class ModelWrapper {
    private final Model originalModel; // оригинальная модель данных (из ObjReader)
    private final Model3D uiModel; // ui-представление модели (для отображения)
    private final StringProperty name = new SimpleStringProperty(); // имя модели (observable свойство)
    private final Triangulator triangulator = new Triangulator();
    private final NormalCalculator normalCalculator = new NormalCalculator();

    public ModelWrapper(Model model, String name) { // конструктор
        this.originalModel = model; // сохраняем оригинальную модель
        this.name.set(name); // устанавливаем имя
        // Триангулируем и вычисляем нормали перед созданием UI модели
        if (model != null) {
            triangulator.triangulateModel(model);
            normalCalculator.calculateNormals(model);
        }
        this.uiModel = convertToUIModel(model); // конвертируем в ui-представление

        if (uiModel != null) {
            forceFixUIModelNormals();
        }
    }

    private void forceFixUIModelNormals() {
        System.out.println("=== ПРИНУДИТЕЛЬНЫЙ ФИКС НОРМАЛЕЙ UI МОДЕЛИ ===");

        // 1. Пересчитываем нормали (используем наш исправленный метод)
        uiModel.calculateNormals();

        // 2. Проверяем все нормали
        int wrongNormals = 0;
        for (Polygon polygon : uiModel.getPolygons()) {
            Vector3D normal = polygon.getNormal();
            if (normal != null && normal.getZ() > 0) {
                wrongNormals++;
                // Экстренная инверсия
                polygon.setNormal(new Vector3D(-normal.getX(), -normal.getY(), -normal.getZ()));
            }
        }

        if (wrongNormals > 0) {
            System.out.println("Исправлено " + wrongNormals + " нормалей с Z > 0");
        }

        // 3. Создаем vertex normals для сглаживания
        uiModel.calculateVertexNormals();
    }

    private Model3D convertToUIModel(Model model) {
        Model3D uiModel = new Model3D(name.get());

        if (model != null) {
            // Конвертируем вершины
            for (Vector3D vertex : model.getVertices()) {
                uiModel.getVertices().add(vertex.toVertex());
            }

            // Конвертируем полигоны (теперь все треугольники)
            for (Polygon polygon : model.getPolygons()) {
                int[] indices = polygon.getVertexIndicesArray();
                Polygon uiPolygon = new Polygon(indices);

                // Копируем нормаль если есть
                if (polygon.getNormal() != null) {
                    uiPolygon.setNormal(polygon.getNormal());
                }

                uiModel.getPolygons().add(uiPolygon);
            }
        }

        return uiModel;
    }

    public Model3D getUIModel() { // ui-модели
        return uiModel; // возвращаем ui-представление
    }

    public Model getOriginalModel() { //оригинальные модели
        return originalModel; // возвращаем данные
    }

    public StringProperty nameProperty() { // observable свойства имени
        return name; // возвращаем свойство (можно привязывать к UI)
    }

    public void updateUIModel() {
        if (originalModel == null) return;

        // Обновляем оригинальную модель
        originalModel.getVertices().clear();
        originalModel.getPolygons().clear();

        // TODO: Нужно заполнить originalModel из uiModel если были изменения

        // Пересчитываем триангуляцию и нормали
        triangulator.triangulateModel(originalModel);
        normalCalculator.calculateNormals(originalModel);

        // Обновляем UI модель
        uiModel.getVertices().clear();
        uiModel.getPolygons().clear();

        for (Vector3D vertex : originalModel.getVertices()) {
            uiModel.getVertices().add(vertex.toVertex());
        }

        for (Polygon polygon : originalModel.getPolygons()) {
            int[] indices = polygon.getVertexIndicesArray();
            Polygon uiPolygon = new Polygon(indices);

            if (polygon.getNormal() != null) {
                uiPolygon.setNormal(polygon.getNormal());
            }

            uiModel.getPolygons().add(uiPolygon);
        }
    }
}