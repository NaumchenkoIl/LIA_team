package scene_master.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Polygon {
    private final List<Integer> vertexIndices;
    private final List<Integer> textureIndices; // Индексы текстурных координат
    private Vector3D normal;

    public Polygon(List<Integer> vertexIndices) {
        this.vertexIndices = List.copyOf(vertexIndices);
        this.textureIndices = new ArrayList<>();
    }

    public Polygon(int... indices) {
        this.vertexIndices = new ArrayList<>();
        for (int index : indices) {
            this.vertexIndices.add(index);
        }
        this.textureIndices = new ArrayList<>();
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

    public Polygon(List<Integer> vertexIndices, List<Integer> textureIndices) {
        this.vertexIndices = List.copyOf(vertexIndices);
        this.textureIndices = new ArrayList<>(textureIndices);
    }

    // Геттер для текстурных индексов
    public List<Integer> getTextureIndices() {
        return textureIndices;
    }

    // Добавить текстурный индекс
    public void addTextureIndex(int index) {
        textureIndices.add(index);
    }
}
