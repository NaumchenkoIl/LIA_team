package scene_master.model;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final List<Vector3D> vertices = new ArrayList();
    private final List<Polygon> polygons = new ArrayList();

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
}
