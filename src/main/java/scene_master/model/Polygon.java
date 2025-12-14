package scene_master.model;

import java.util.Arrays;
import java.util.List;

public class Polygon {
    private final List<Integer> vertexIndices;
    private Vector3D normal;

    public Polygon(List<Integer> vertexIndices) {
        this.vertexIndices = List.copyOf(vertexIndices);
    }

    public Polygon(int... indices) {
        this.vertexIndices = List.of((Integer[])Arrays.stream(indices).boxed().toArray((x$0) -> {
            return new Integer[x$0];
        }));
    }

    public void setNormal(Vector3D normal) {
        this.normal = normal;
    }

    public Vector3D getNormal() {
        return this.normal;
    }

    public List<Integer> getVertexIndices() {
        return this.vertexIndices;
    }

    public int[] getVertexIndicesArray() {
        return this.vertexIndices.stream().mapToInt((i) -> {
            return i;
        }).toArray();
    }
}
