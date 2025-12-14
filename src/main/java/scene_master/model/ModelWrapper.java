package scene_master.model;

import javafx.beans.property.SimpleStringProperty; // простое строковое свойство
import javafx.beans.property.StringProperty; // интерфейс строкового свойства

public class ModelWrapper {
    private final Model originalModel; // оригинальная модель данных (из ObjReader)
    private final Model3D uiModel; // ui-представление модели (для отображения)
    private final StringProperty name = new SimpleStringProperty(); // имя модели (observable свойство)

    public ModelWrapper(Model model, String name) { // конструктор
        this.originalModel = model; // сохраняем оригинальную модель
        this.name.set(name); // устанавливаем имя
        this.uiModel = convertToUIModel(model); // конвертируем в ui-представление
    }

    private Model3D convertToUIModel(Model model) { // конвертирует Model в Model3D
        Model3D uiModel = new Model3D(name.get()); // создаем ui-модель с именем

        if (model != null) { // если есть данные для конвертации
            for (Vector3D vertex : model.getVertices()) {// конвертируем вершины
                uiModel.getVertices().add(vertex.toVertex()); // преобразуем Vector3D в Vertex
            }

            for (scene_master.model.Polygon polygon : model.getPolygons()) {// конвертируем полигоны
                int[] indices = polygon.getVertexIndicesArray(); // получаем массив индексов
                uiModel.getPolygons().add(new Polygon(indices)); // создаем новый полигон для UI
            }
        }

        return uiModel; // возвращаем ui-представление
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

    public void updateUIModel() { // обновляет ui-модель (если изменились исходные данные)
        if (originalModel == null) return; // если нет данных - выходим

        uiModel.getVertices().clear(); // очищаем список вершин
        uiModel.getPolygons().clear(); // очищаем список полигонов

        for (Vector3D vertex : originalModel.getVertices()) { // перезаполняем вершины
            uiModel.getVertices().add(vertex.toVertex());
        }

        for (scene_master.model.Polygon polygon : originalModel.getPolygons()) { // перезаполняем полигоны
            int[] indices = polygon.getVertexIndicesArray();
            uiModel.getPolygons().add(new Polygon(indices));
        }
    }
}