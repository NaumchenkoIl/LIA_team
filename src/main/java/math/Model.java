package math;

import math.LinealAlgebra.Vector3D;

public class Model {
    private final Vector3D[] originalVertices;
    private Vector3D[] currentVertices;

    public Model(Vector3D[] vertices) {
        this.originalVertices = clone(vertices);
        this.currentVertices = clone(vertices);
    }

    public void applyTransform(ModelTransform transform) {
        for (int i = 0; i < currentVertices.length; i++) {
            currentVertices[i] = transform.transformVertex(currentVertices[i]);
        }
    }

    public void resetToOriginal() {
        this.currentVertices = clone(originalVertices);
    }

    public Vector3D[] getCurrentVertices() {
        return clone(currentVertices);
    }

    public Vector3D[] getOriginalVertices() {
        return clone(originalVertices);
    }

    private Vector3D[] clone(Vector3D[] src) {
        Vector3D[] dst = new Vector3D[src.length];
        for (int i = 0; i < src.length; i++) {
            dst[i] = new Vector3D(src[i].getX(), src[i].getY(), src[i].getZ());
        }
        return dst;
    }
}
