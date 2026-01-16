package math.LinealAlgebra;

import math.ModelTransform;

public class Model {
    private Vector3D[] vertices;
    private Vector3D[] originalVertices; // для сброса

    public Model(Vector3D[] vertices) {
        this.vertices = vertices.clone();
        this.originalVertices = vertices.clone();
    }

    public void applyTransform(ModelTransform transform) {
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = transform.transformVertex(vertices[i]);
        }
    }

    public void resetToOriginal() {
        this.vertices = originalVertices.clone();
    }

    public Vector3D[] getVertices() { return vertices; }
}
