package scene_master.calculator;

import java.util.Iterator;
import java.util.List;
import scene_master.model.Model;
import scene_master.model.Polygon;
import math.LinealAlgebra.Vector3D;

public class NormalCalculator {
    public NormalCalculator() {
    }

    public void calculateNormals(Model model) {
        Iterator<Polygon> var2 = model.getPolygons().iterator();

        while(var2.hasNext()) {
            Polygon polygon = var2.next();
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
            Vector3D v1 = model.getVertices().get(indices.get(0));
            Vector3D v2 = model.getVertices().get(indices.get(1));
            Vector3D v3 = model.getVertices().get(indices.get(2));

            Vector3D s1 = v2.subtract(v1);
            Vector3D s2 = v3.subtract(v1);

            return s1.cross(s2);
        }
    }
}

