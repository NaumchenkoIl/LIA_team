package scene_master.model;

import java.util.ArrayList;
import java.util.List;
import math.LinealAlgebra.Vector3D;


public class Model {
    private final List<Vector3D> vertices = new ArrayList<>();
    private final List<TexturePoint> texturePoints = new ArrayList<>();
    private final List<Vector3D> normals = new ArrayList<>();
    private final List<Polygon> polygons = new ArrayList<>();

    public Model() {
    }

    public void addVertex(Vector3D vertex) {
        this.vertices.add(vertex);
    }

    public void addTexturePoint(TexturePoint texturePoint) {
        this.texturePoints.add(texturePoint);
    }

    public void addNormal(Vector3D normal) {
        this.normals.add(normal);
    }

    public void addPolygon(Polygon polygon) {
        this.polygons.add(polygon);
    }

    public List<Vector3D> getVertices() {
        return this.vertices;
    }

    public List<TexturePoint> getTexturePoints() {
        return this.texturePoints;
    }

    public List<Vector3D> getNormals() {
        return this.normals;
    }

    public List<Polygon> getPolygons() {
        return this.polygons;
    }

    public int getVertexCount() {
        return this.vertices.size();
    }

    public int getTexturePointCount() {
        return this.texturePoints.size();
    }

    public int getNormalCount() {
        return this.normals.size();
    }

    public int getPolygonCount() {
        return this.polygons.size();
    }
}