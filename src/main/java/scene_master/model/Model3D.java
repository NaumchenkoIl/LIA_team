package scene_master.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
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

    // Текстура и материалы
    private final ObjectProperty<Image> texture = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Color> baseColor = new SimpleObjectProperty<>(Color.LIGHTBLUE);

    // Текстурные координаты (UV)
    private final ObservableList<TextureCoordinate> textureCoords = FXCollections.observableArrayList();
    private List<Vector3D> vertexNormals = new ArrayList<>();

    public Model3D(String name) {
        this.name.set(name);
    }

    // === Геттеры и сеттеры для текстуры ===

    public ObjectProperty<Image> textureProperty() {
        return texture;
    }

    public Image getTexture() {
        return texture.get();
    }

    public void setTexture(Image texture) {
        this.texture.set(texture);
    }

    // === Работа с UV-координатами ===

    public void addTextureCoord(double u, double v) {
        textureCoords.add(new TextureCoordinate(u, v));
    }

    public ObservableList<TextureCoordinate> getTextureCoords() {
        return textureCoords;
    }

    public void clearTextureCoords() {
        textureCoords.clear();
    }

    public double[] getTextureCoordsForPolygonVertex(Polygon polygon, int vertexIndexInPolygon) {
        try {
            List<Integer> indices = polygon.getVertexIndices();
            List<Integer> texIndices = polygon.getTextureIndices();

            // Если есть привязанные UV-индексы
            if (texIndices != null && texIndices.size() > vertexIndexInPolygon) {
                int texIndex = texIndices.get(vertexIndexInPolygon);
                if (texIndex >= 0 && texIndex < this.textureCoords.size()) {
                    TextureCoordinate tc = this.textureCoords.get(texIndex);
                    return new double[]{tc.u, tc.v};
                }
            }

            // Если UV нет, используем координаты вершины как простую проекцию
            if (vertexIndexInPolygon < indices.size()) {
                int vertexIndex = indices.get(vertexIndexInPolygon);
                if (vertexIndex < this.vertices.size()) {
                    Vertex vertex = this.vertices.get(vertexIndex);
                    // Простая проекция: используем X и Y как UV
                    double u = (vertex.x + 1) / 2;  // [-1, 1] -> [0, 1]
                    double v = (vertex.y + 1) / 2;
                    return new double[]{u, v};
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения UV-координат для модели " +
                    this.name.get() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Запасной вариант
        return new double[]{0.5, 0.5};
    }

    // === Вычисление нормалей ===

    public void calculateNormals() {
        if (vertices.isEmpty() || polygons.isEmpty()) return;

        for (Polygon polygon : polygons) {
            List<Integer> indices = polygon.getVertexIndices();
            if (indices.size() < 3) continue;

            Vertex v1 = vertices.get(indices.get(0));
            Vertex v2 = vertices.get(indices.get(1));
            Vertex v3 = vertices.get(indices.get(2));

            // Вектора двух сторон треугольника
            double ax = v2.x - v1.x;
            double ay = v2.y - v1.y;
            double az = v2.z - v1.z;

            double bx = v3.x - v1.x;
            double by = v3.y - v1.y;
            double bz = v3.z - v1.z;

            // Векторное произведение (cross product)
            // Формула: a × b = (ay*bz - az*by, az*bx - ax*bz, ax*by - ay*bx)
            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;

            // ВАЖНОЕ ИСПРАВЛЕНИЕ: Для координат где Z растет ВГЛУБЬ экрана:
            // Если нормаль направлена ОТ камеры (nz > 0), инвертируем её!
            // Это нужно потому что камера смотрит по оси -Z

            // Проверяем: если Z положительный - нормаль направлена ОТ камеры
            if (nz > 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }

            // Нормализация
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            }

            polygon.setNormal(new Vector3D(nx, ny, nz));
        }
    }

    /**
     * Вычисляет сглаженные нормали по вершинам (vertex normals)
     * путём усреднения нормалей всех граней, использующих эту вершину.
     */
    public void calculateVertexNormals() {
        if (vertices.isEmpty() || polygons.isEmpty()) return;

        // 1. Обнуляем все vertex normals
        List<Vector3D> vertexNormals = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            vertexNormals.add(new Vector3D(0, 0, 0));
        }

        // 2. Суммируем face normals для каждой вершины
        for (Polygon polygon : polygons) {
            Vector3D faceNormal = polygon.getNormal();
            if (faceNormal == null) continue;
            for (int idx : polygon.getVertexIndices()) {
                if (idx >= 0 && idx < vertexNormals.size()) {
                    Vector3D current = vertexNormals.get(idx);
                    vertexNormals.set(idx, new Vector3D(
                            current.getX() + faceNormal.getX(),
                            current.getY() + faceNormal.getY(),
                            current.getZ() + faceNormal.getZ()
                    ));
                }
            }
        }

        // 3. Нормализуем
        for (int i = 0; i < vertexNormals.size(); i++) {
            Vector3D n = vertexNormals.get(i);
            double len = Math.sqrt(n.getX()*n.getX() + n.getY()*n.getY() + n.getZ()*n.getZ());
            if (len > 0) {
                vertexNormals.set(i, new Vector3D(n.getX()/len, n.getY()/len, n.getZ()/len));
            }
        }

        // 4. Сохраняем как доп. данные в вершинах (или отдельный список)
        // В данном случае — храним в отдельном списке внутри Model3D
        this.vertexNormals = vertexNormals;
    }

    public void invertNormals() {
        for (Polygon polygon : polygons) {
            Vector3D normal = polygon.getNormal();
            if (normal != null) {
                // Инвертируем нормаль
                polygon.setNormal(new Vector3D(-normal.getX(), -normal.getY(), -normal.getZ()));
            }
        }
        System.out.println("Нормали инвертированы для модели: " + getName());
    }

    // === Остальные геттеры и сеттеры ===

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
        return this.name.get();
    }

    public boolean isVisible() {
        return this.visible.get();
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

    public List<Vector3D> getVertexNormals() {
        return vertexNormals;
    }
}