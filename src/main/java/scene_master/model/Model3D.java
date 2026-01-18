package scene_master.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import math.LinealAlgebra.Vector3D;

import java.util.ArrayList;
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
    private List<Vector3D> vertexNormals = new ArrayList<>();

    public Model3D(String name) {
        this.name.set(name);
    }

    public ObjectProperty<Image> textureProperty() { return texture; }
    public Image getTexture() { return texture.get(); }
    public void setTexture(Image texture) { this.texture.set(texture); }

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

            double u = 0.5, v = 0.5;

            if (texIndices != null && texIndices.size() > vertexIndexInPolygon) {
                int texIndex = texIndices.get(vertexIndexInPolygon);
                if (texIndex >= 0 && texIndex < textureCoords.size()) {
                    TextureCoordinate tc = textureCoords.get(texIndex);
                    u = tc.u;
                    v = tc.v;
                }
            } else if (vertexIndexInPolygon < indices.size()) {
                int vertexIndex = indices.get(vertexIndexInPolygon);
                if (vertexIndex < vertices.size()) {
                    Vector3D vertex = vertices.get(vertexIndex);
                    u = (vertex.getX() + 1) / 2;
                    v = (vertex.getY() + 1) / 2;
                }
            }

            u *= getTextureScaleU();
            v *= getTextureScaleV();

            return new double[]{u, v};
        } catch (Exception e) {
            e.printStackTrace();
            return new double[]{0.5, 0.5};
        }
    }

    public void generateUVFromGeometry() {
        generateUVFromGeometry(false);  // По умолчанию per-face.
    }

    public void generateUVFromGeometry(boolean globalNormalize) {
        textureCoords.clear();

        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = Double.MIN_VALUE;
        if (globalNormalize) {
            for (Vector3D vertex : vertices) {
                minX = Math.min(minX, vertex.getX());
                maxX = Math.max(maxX, vertex.getX());
                minY = Math.min(minY, vertex.getY());
                maxY = Math.max(maxY, vertex.getY());
                minZ = Math.min(minZ, vertex.getZ());
                maxZ = Math.max(maxZ, vertex.getZ());
            }
        }

        int texIndexOffset = 0;
        for (Polygon polygon : polygons) {
            List<Integer> indices = polygon.getVertexIndices();
            if (indices.size() < 3) continue;

            Vector3D v1 = vertices.get(indices.get(0));
            Vector3D v2 = vertices.get(indices.get(1));
            Vector3D v3 = vertices.get(indices.get(2));

            double nx = Math.abs(polygon.getNormal().getX());
            double ny = Math.abs(polygon.getNormal().getY());
            double nz = Math.abs(polygon.getNormal().getZ());

            String plane = "XY";
            if (nz > nx && nz > ny) plane = "XY";
            else if (ny > nx && ny > nz) plane = "XZ";
            else if (nx > ny && nx > nz) plane = "YZ";

            for (int i = 0; i < indices.size(); i++) {
                Vector3D vertex = vertices.get(indices.get(i));
                double u = 0, v = 0;

                switch (plane) {
                    case "XY":
                        u = vertex.getX();
                        v = vertex.getY();
                        break;
                    case "XZ":
                        u = vertex.getX();
                        v = vertex.getZ();
                        break;
                    case "YZ":
                        u = vertex.getY();
                        v = vertex.getZ();
                        break;
                }

                if (globalNormalize) {
                    switch (plane) {
                        case "XY":
                            u = (u - minX) / (maxX - minX);
                            v = (v - minY) / (maxY - minY);
                            break;
                        case "XZ":
                            u = (u - minX) / (maxX - minX);
                            v = (v - minZ) / (maxZ - minZ);
                            break;
                        case "YZ":
                            u = (u - minY) / (maxY - minY);
                            v = (v - minZ) / (maxZ - minZ);
                            break;
                    }
                } else {
                    u = (u + 1) / 2;
                    v = (v + 1) / 2;
                }

                addTextureCoord(u, v);
            }

            polygon.setTextureIndices(new ArrayList<>());
            for (int i = 0; i < indices.size(); i++) {
                polygon.addTextureIndex(texIndexOffset + i);
            }
            texIndexOffset += indices.size();
        }
    }

    public ObjectProperty<Color> baseColorProperty() { return baseColor; }
    public Color getBaseColor() { return baseColor.get(); }
    public void setBaseColor(Color color) { baseColor.set(color); }

    public StringProperty nameProperty() { return name; }
    public BooleanProperty visibleProperty() { return visible; }
    public ObservableList<Vector3D> getVertices() { return vertices; }
    public ObservableList<TexturePoint> getTexturePoints() { return texturePoints; }
    public ObservableList<Vector3D> getNormals() { return normals; }
    public ObservableList<Polygon> getPolygons() { return polygons; }

    public DoubleProperty translateXProperty() { return translateX; }
    public DoubleProperty translateYProperty() { return translateY; }
    public DoubleProperty translateZProperty() { return translateZ; }
    public DoubleProperty rotateXProperty() { return rotateX; }
    public DoubleProperty rotateYProperty() { return rotateY; }
    public DoubleProperty rotateZProperty() { return rotateZ; }
    public DoubleProperty scaleXProperty() { return scaleX; }
    public DoubleProperty scaleYProperty() { return scaleY; }
    public DoubleProperty scaleZProperty() { return scaleZ; }

    public String getName() { return name.get(); }
    public boolean isVisible() { return visible.get(); }

    public DoubleProperty textureScaleUProperty() { return textureScaleU; }
    public DoubleProperty textureScaleVProperty() { return textureScaleV; }
    public double getTextureScaleU() { return textureScaleU.get(); }
    public double getTextureScaleV() { return textureScaleV.get(); }
    public void setTextureScaleU(double scale) { textureScaleU.set(scale); }
    public void setTextureScaleV(double scale) { textureScaleV.set(scale); }

    public void calculateVertexNormals() {
        vertexNormals.clear();
        for (int i = 0; i < vertices.size(); i++) {
            vertexNormals.add(new Vector3D(0, 0, 0));
        }

        for (Polygon polygon : polygons) {
            Vector3D faceNormal = polygon.getNormal();
            if (faceNormal != null) {
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
        }

        for (int i = 0; i < vertexNormals.size(); i++) {
            Vector3D n = vertexNormals.get(i);
            double len = Math.sqrt(n.getX()*n.getX() + n.getY()*n.getY() + n.getZ()*n.getZ());
            if (len > 0) {
                vertexNormals.set(i, new Vector3D(
                        (float) (n.getX()/len),
                        (float) (n.getY()/len),
                        (float) (n.getZ()/len)
                ));
            }
        }
    }

    public List<Vector3D> getVertexNormals() {
        return vertexNormals;
    }

    public static class TextureCoordinate {
        public final double u, v;
        public TextureCoordinate(double u, double v) {
            this.u = u;
            this.v = v;
        }
    }
}