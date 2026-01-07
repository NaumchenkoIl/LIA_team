package scene_master.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import scene_master.calculator.NormalCalculator;

import java.util.List;

public class Model3D {
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty visible = new SimpleBooleanProperty(true);
    private final ObservableList<Vertex> vertices = FXCollections.observableArrayList();
    private final ObservableList<Polygon> polygons = FXCollections.observableArrayList();

    // Трансформации
    private final DoubleProperty translateX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty translateY = new SimpleDoubleProperty(0.0);
    private final DoubleProperty translateZ = new SimpleDoubleProperty(0.0);
    private final DoubleProperty rotateX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty rotateY = new SimpleDoubleProperty(0.0);
    private final DoubleProperty rotateZ = new SimpleDoubleProperty(0.0);
    private final DoubleProperty scaleX = new SimpleDoubleProperty(1.0);
    private final DoubleProperty scaleY = new SimpleDoubleProperty(1.0);
    private final DoubleProperty scaleZ = new SimpleDoubleProperty(1.0);
    // Список текстурных координат (отдельно от вершин для совместимости с OBJ)
    private final ObservableList<TextureCoordinate> textureCoords = FXCollections.observableArrayList();
    // Текстура и материалы
    private final ObjectProperty<Image> texture = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Color> baseColor = new SimpleObjectProperty<>(Color.LIGHTBLUE);

    // Координаты текстуры для каждой вершины
    private final ObservableList<TextureCoordinate> textureCoordinates = FXCollections.observableArrayList();

    public Model3D(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return this.name;
    }

    public BooleanProperty visibleProperty() {
        return this.visible;
    }

    public ObservableList<Vertex> getVertices() {
        return this.vertices;
    }

    public ObservableList<Polygon> getPolygons() {
        return this.polygons;
    }

    public DoubleProperty translateXProperty() {
        return this.translateX;
    }

    public DoubleProperty translateYProperty() {
        return this.translateY;
    }

    public DoubleProperty translateZProperty() {
        return this.translateZ;
    }

    public DoubleProperty rotateXProperty() {
        return this.rotateX;
    }

    public DoubleProperty rotateYProperty() {
        return this.rotateY;
    }

    public DoubleProperty rotateZProperty() {
        return this.rotateZ;
    }

    public DoubleProperty scaleXProperty() {
        return this.scaleX;
    }

    public DoubleProperty scaleYProperty() {
        return this.scaleY;
    }

    public DoubleProperty scaleZProperty() {
        return this.scaleZ;
    }

    public String getName() {
        return (String)this.name.get();
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public ObjectProperty<Image> textureProperty() {
        return texture;
    }

    public Image getTexture() {
        return texture.get();
    }

    public void setTexture(Image texture) {
        this.texture.set(texture);
    }

    public ObjectProperty<Color> baseColorProperty() {
        return baseColor;
    }

    public Color getBaseColor() {
        return baseColor.get();
    }

    public void setBaseColor(Color color) {
        baseColor.set(color);
    }

    public ObservableList<TextureCoordinate> getTextureCoordinates() {
        return textureCoordinates;
    }

    public void clearTextureCoordinates() {
        textureCoordinates.clear();
    }

    public void addTextureCoordinate(double u, double v) {
        textureCoordinates.add(new TextureCoordinate(u, v));
    }

    // Внутренний класс для координат текстуры
    public static class TextureCoordinate {
        public final double u;
        public final double v;

        public TextureCoordinate(double u, double v) {
            this.u = u;
            this.v = v;
        }

        @Override
        public String toString() {
            return String.format("(%.4f, %.4f)", u, v);
        }
    }

    public void calculateNormals() {
        if (vertices.isEmpty() || polygons.isEmpty()) return;

        // Для каждого полигона вычисляем нормаль напрямую
        for (Polygon polygon : polygons) {
            List<Integer> indices = polygon.getVertexIndices();
            if (indices.size() < 3) continue;

            // Берем три первые вершины полигона (все полигоны теперь треугольники)
            Vertex v1 = vertices.get(indices.get(0));
            Vertex v2 = vertices.get(indices.get(1));
            Vertex v3 = vertices.get(indices.get(2));

            // Векторы сторон треугольника
            double ax = v2.x - v1.x;
            double ay = v2.y - v1.y;
            double az = v2.z - v1.z;

            double bx = v3.x - v1.x;
            double by = v3.y - v1.y;
            double bz = v3.z - v1.z;

            // Векторное произведение (нормаль)
            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;

            // Нормализация
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            }

            // Сохраняем нормаль
            polygon.setNormal(new scene_master.model.Vector3D(nx, ny, nz));
        }
    }

    // Метод для добавления текстурной координаты
    public void addTextureCoord(double u, double v) {
        textureCoords.add(new TextureCoordinate(u, v));
    }

    public ObservableList<TextureCoordinate> getTextureCoords() {
        return textureCoords;
    }

    public void clearTextureCoords() {
        textureCoords.clear();
    }

    // Получить UV для вершины полигона
    public double[] getTextureCoordsForPolygonVertex(Polygon polygon, int vertexIndexInPolygon) {
        List<Integer> indices = polygon.getVertexIndices();
        List<Integer> texIndices = polygon.getTextureIndices();

        if (texIndices != null && texIndices.size() > vertexIndexInPolygon) {
            int texIndex = texIndices.get(vertexIndexInPolygon);
            if (texIndex >= 0 && texIndex < textureCoords.size()) {
                TextureCoordinate tc = textureCoords.get(texIndex);
                return new double[]{tc.u, tc.v};
            }
        }

        // Возвращаем координаты по умолчанию
        Vertex v = getVertices().get(indices.get(vertexIndexInPolygon));
        return new double[]{v.u, v.v};
    }
}

