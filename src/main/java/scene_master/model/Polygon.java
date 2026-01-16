package scene_master.model;

import java.util.List;
import java.util.ArrayList;

public class Polygon {
    private final List<Integer> vertexIndices;
    private List<Integer> textureIndices;
    private List<Integer> normalIndices;
    private Vector3D normal;

    public Polygon(List<Integer> vertexIndices) {
        this.vertexIndices = List.copyOf(vertexIndices);
        this.textureIndices = new ArrayList<>();
        this.normalIndices = new ArrayList<>();
    }

    public Polygon(int... indices) {
        this.vertexIndices = new ArrayList<>();
        for (int index : indices) {
            this.vertexIndices.add(index);
        }
        this.textureIndices = new ArrayList<>();
        this.normalIndices = new ArrayList<>();
    }

    public void setNormal(Vector3D normal) {
        this.normal = normal;
    }

    public void setTextureIndices(List<Integer> textureIndices) {
        this.textureIndices = new ArrayList<>(textureIndices);
    }

    public void setNormalIndices(List<Integer> normalIndices) {
        this.normalIndices = new ArrayList<>(normalIndices);
    }

    public Vector3D getNormal() {
        return this.normal;
    }

    public List<Integer> getTextureIndices() {
        return this.textureIndices;
    }

    public List<Integer> getNormalIndices() {
        return this.normalIndices;
    }

    public List<Integer> getVertexIndices() {
        return this.vertexIndices;
    }

    public int[] getVertexIndicesArray() {
        return this.vertexIndices.stream().mapToInt((i) -> {
            return i;
        }).toArray();
    }

    public boolean hasTexture() {
        return !textureIndices.isEmpty();
    }

    public boolean hasNormals() {
        return !normalIndices.isEmpty();
    }
}