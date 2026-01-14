package scene_master.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Model3D {
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty visible = new SimpleBooleanProperty(true);
    private final ObservableList<Vertex> vertices = FXCollections.observableArrayList();
    private final ObservableList<TexturePoint> texturePoints = FXCollections.observableArrayList();
    private final ObservableList<Vertex> normals = FXCollections.observableArrayList();
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

    public ObservableList<TexturePoint> getTexturePoints() {
        return this.texturePoints;
    }

    public ObservableList<Vertex> getNormals() {
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
}