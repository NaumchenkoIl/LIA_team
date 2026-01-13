package scene_master.calculator;

import java.util.Iterator;
import java.util.List;
import scene_master.model.Model;
import scene_master.model.Polygon;
import scene_master.model.Vector3D;

public class NormalCalculator {
    public NormalCalculator() {
    }

    public void calculateNormals(Model model) {
        Iterator var2 = model.getPolygons().iterator();

        while(var2.hasNext()) {
            Polygon polygon = (Polygon)var2.next();
            Vector3D normal = this.calculatePolygonNormal(model, polygon);

            if (normal.getZ() > 0) {
                normal = new Vector3D(-normal.getX(), -normal.getY(), -normal.getZ());
            }

            polygon.setNormal(normal.normalize());
        }

    }

    private Vector3D calculatePolygonNormal(Model model, Polygon polygon) {
        List<Integer> indices = polygon.getVertexIndices();
        if (indices.size() < 3) {
            throw new IllegalArgumentException("Polygon must have at least 3 vertices");
        } else {
            Vector3D v1 = (Vector3D)model.getVertices().get((Integer)indices.get(0));
            Vector3D v2 = (Vector3D)model.getVertices().get((Integer)indices.get(1));
            Vector3D v3 = (Vector3D)model.getVertices().get((Integer)indices.get(2));
            Vector3D s1 = new Vector3D(v2.getX() - v1.getX(), v2.getY() - v1.getY(), v2.getZ() - v1.getZ());
            Vector3D s2 = new Vector3D(v3.getX() - v1.getX(), v3.getY() - v1.getY(), v3.getZ() - v1.getZ());
            return s1.crossProduct(s2);
        }
    }
}

