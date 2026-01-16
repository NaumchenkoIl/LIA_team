package scene_master.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import math.LinealAlgebra.Vector3D;
import scene_master.calculator.NormalCalculator;
import scene_master.calculator.Triangulator;

public class ModelWrapper {
    private final Model originalModel; // оригинальная модель данных (из ObjReader)
    private final Model3D uiModel; // ui-представление модели (для отображения)
    private final StringProperty name = new SimpleStringProperty(); // имя модели (observable свойство)

    public ModelWrapper(Model model, String name) { // конструктор
        this.originalModel = model; // сохраняем оригинальную модель
        this.name.set(name); // устанавливаем имя
        if (model != null) {
            Triangulator triangulator = new Triangulator();
            NormalCalculator normalCalculator = new NormalCalculator();
            triangulator.triangulateModel(model);
            normalCalculator.calculateNormals(model);
        }
        this.uiModel = convertToUIModel(model); // конвертируем в ui-представление
    }

    private Model3D convertToUIModel(Model model) { // конвертирует Model в Model3D
        Model3D uiModel = new Model3D(name.get()); // создаем ui-модель с именем

        if (model != null) { // если есть данные для конвертации
            for (Vector3D vertex : model.getVertices()) {// конвертируем вершины
                uiModel.getVertices().add(new Vector3D(
                        vertex.getX(),
                        vertex.getY(),
                        vertex.getZ()
                ));            }

            for (TexturePoint tp : model.getTexturePoints()) {// конвертируем текстурные координаты
                uiModel.getTexturePoints().add(tp);
            }

            for (Vector3D normal : model.getNormals()) {// кнвертируем нормали
                uiModel.getNormals().add(new Vector3D(
                        normal.getX(),
                        normal.getY(),
                        normal.getZ()
                ));            }

            for (scene_master.model.Polygon polygon : model.getPolygons()) { // конвертируем полигоны
                int[] indices = polygon.getVertexIndicesArray();
                Polygon uiPolygon = new Polygon(indices);

                if (polygon.hasTexture()) {  // сохраняем текстуры
                    uiPolygon.setTextureIndices(polygon.getTextureIndices());
                }

                if (polygon.hasNormals()) {// сохраняем нормали
                    uiPolygon.setNormalIndices(polygon.getNormalIndices());
                }

                uiModel.getPolygons().add(uiPolygon);
            }
        }

        return uiModel;
    }

    public Model3D getUIModel() {
        return uiModel;
    }

    public Model getOriginalModel() {
        return originalModel;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void updateUIModel() {
        if (originalModel == null) return;

        uiModel.getVertices().clear();
        uiModel.getTexturePoints().clear();
        uiModel.getNormals().clear();
        uiModel.getPolygons().clear();

        for (Vector3D vertex : originalModel.getVertices()) {// вершины
            uiModel.getVertices().add(new Vector3D(
                    vertex.getX(),
                    vertex.getY(),
                    vertex.getZ()
            ));        }

        for (TexturePoint tp : originalModel.getTexturePoints()) { // текстуры
            uiModel.getTexturePoints().add(tp);
        }

        for (Vector3D normal : originalModel.getNormals()) {// нормали
            uiModel.getNormals().add(new Vector3D(
                    normal.getX(),
                    normal.getY(),
                    normal.getZ()
            ));        }

        for (scene_master.model.Polygon polygon : originalModel.getPolygons()) { // полигоны
            int[] indices = polygon.getVertexIndicesArray();
            Polygon uiPolygon = new Polygon(indices);

            if (polygon.hasTexture()) {
                uiPolygon.setTextureIndices(polygon.getTextureIndices());
            }

            if (polygon.hasNormals()) {
                uiPolygon.setNormalIndices(polygon.getNormalIndices());
            }

            uiModel.getPolygons().add(uiPolygon);
        }
    }
}