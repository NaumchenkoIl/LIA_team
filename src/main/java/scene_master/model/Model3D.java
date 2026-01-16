package scene_master.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import math.LinealAlgebra.Vector3D;

import java.util.List;


public class Model3D {
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty visible = new SimpleBooleanProperty(true);
    private final ObservableList<Vector3D> vertices = FXCollections.observableArrayList();
    private final ObservableList<TexturePoint> texturePoints = FXCollections.observableArrayList();
    private final ObservableList<Vector3D> normals = FXCollections.observableArrayList();
    private final ObservableList<Polygon> polygons = FXCollections.observableArrayList();
    private final DoubleProperty translateX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty translateY = new SimpleDoubleProperty(0.0);
    private final DoubleProperty translateZ = new SimpleDoubleProperty(0.0);
    private final DoubleProperty rotateX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty rotateY = new SimpleDoubleProperty(0.0);
    private final DoubleProperty rotateZ = new SimpleDoubleProperty(0.0);
    private final DoubleProperty scaleX = new SimpleDoubleProperty(1.0);
    private final DoubleProperty scaleY = new SimpleDoubleProperty(1.0);
    private final DoubleProperty scaleZ = new SimpleDoubleProperty(1.0);
    private final ObjectProperty<Image> texture = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Color> baseColor = new SimpleObjectProperty<>(Color.LIGHTBLUE);
    private final ObservableList<TextureCoordinate> textureCoords = FXCollections.observableArrayList();
    private final DoubleProperty textureScaleU = new SimpleDoubleProperty(1.0);
    private final DoubleProperty textureScaleV = new SimpleDoubleProperty(1.0);

    public Model3D(String name) {
        this.name.set(name);
    }

    public ObjectProperty<Image> textureProperty() { return  texture; }

    public Image getTexture() { return texture.get(); }

    public void setTexture(Image texture) { this.texture.set(texture); }

    public void addTextureCoord(double u, double v) {
        textureCoords.add(new TextureCoordinate(u, v));
    }

    public ObservableList<TextureCoordinate> getTextureCoords() {
        return this.textureCoords;
    }

    public void clearTextureCoords() { textureCoords.clear(); }

    public double[] getTextureCoordsForPolygonVertex(Polygon polygon, int vertexIndexInPolygon) {
        try {
            List<Integer> indices = polygon.getVertexIndices();
            List<Integer> texIndices = polygon.getTextureIndices();

            if (texIndices != null && texIndices.size() > vertexIndexInPolygon) {
                int texIndex = texIndices.get(vertexIndexInPolygon);
                if (texIndex >= 0 && texIndex < textureCoords.size()) {
                    TextureCoordinate tc = textureCoords.get(texIndex);
                    return new double[]{tc.u, tc.v};
                }
            }

            if (vertexIndexInPolygon < indices.size()) {
                int vertexIndex = indices.get(vertexIndexInPolygon);
                if (vertexIndex < vertices.size()) {
                    Vector3D vertex = vertices.get(vertexIndex);
                    double u = (vertex.getX() + 1) / 2;
                    double v = (vertex.getY() + 1) / 2;

                    u *= getTextureScaleU();
                    v *= getTextureScaleV();

                    return new double[]{u, v};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new double[]{0.5, 0.5};
    }

    public ObjectProperty<Color> baseColorProperty() { return baseColor; }
    public Color getBaseColor() { return baseColor.get(); }
    public void setBaseColor(Color color) { baseColor.set(color); }

    public StringProperty nameProperty() {
        return this.name;
    }

    public BooleanProperty visibleProperty() {
        return this.visible;
    }

    public ObservableList<Vector3D> getVertices() {
        return this.vertices;
    }

    public ObservableList<TexturePoint> getTexturePoints() {
        return this.texturePoints;
    }

    public ObservableList<Vector3D> getNormals() {
        return this.normals;
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

    public static class TextureCoordinate {
        public final double u, v;
        public TextureCoordinate(double u, double v){
            this.u = u;
            this.v = v;
        }
    }

    public DoubleProperty textureScaleUProperty() { return textureScaleU; }
    public DoubleProperty textureScaleVProperty() { return textureScaleV; }
    public double getTextureScaleU() { return textureScaleU.get(); }
    public double getTextureScaleV() { return textureScaleV.get(); }
    public void setTextureScaleU(double scale) { textureScaleU.set(scale); }
    public void setTextureScaleV(double scale) { textureScaleV.set(scale); }
}