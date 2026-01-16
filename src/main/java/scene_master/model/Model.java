package scene_master.model;

import java.util.ArrayList;
import java.util.List;
import math.LinealAlgebra.Vector3D;


public class Model {
    private final List<Vector3D> vertices = new ArrayList();
    private final List<Polygon> polygons = new ArrayList();
    private final List<Vector3D> textureVertices = new ArrayList<>();
    private final List<Vector3D> normals = new ArrayList<>();

    public Model() {
    }

    public void addVertex(Vector3D vertex) {
        this.vertices.add(vertex);
    }

    public void addPolygon(Polygon polygon) {
        this.polygons.add(polygon);
    }

    public List<Vector3D> getVertices() {
        return this.vertices;
    }

    public List<Polygon> getPolygons() {
        return this.polygons;
    }

    public int getVertexCount() {
        return this.vertices.size();
    }

    public int getPolygonCount() {
        return this.polygons.size();
    }

    public void addTextureVertex(Vector3D vt) { textureVertices.add(vt); }
    public void addNormal(Vector3D vn) { normals.add(vn); }

    public List<Vector3D> getTextureVertices() { return textureVertices; }
    public List<Vector3D> getNormals() { return normals; }
}
