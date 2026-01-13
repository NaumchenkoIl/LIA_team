package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector3D;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector4D;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix.Matrix4x4;

public class ModelTransform {
    private Vector3D translation = new Vector3D(0, 0, 0);
    private Vector3D rotationDeg = new Vector3D(0, 0, 0);
    private Vector3D scale = new Vector3D(1, 1, 1);

    public void setTranslation(float x, float y, float z) {
        this.translation = new Vector3D(x, y, z);
    }

    public void setRotationDeg(float rx, float ry, float rz) {
        this.rotationDeg = new Vector3D(rx, ry, rz);
    }

    public void setScale(float sx, float sy, float sz) {
        this.scale = new Vector3D(sx, sy, sz);
    }

    // получение итоговой модельной матрицы
    public Matrix4x4 getModelMatrix() {
        Matrix4x4 T = Matrix4x4.translation(translation.getX(), translation.getY(), translation.getZ());
        Matrix4x4 Rx = Matrix4x4.rotationXDeg(rotationDeg.getX());
        Matrix4x4 Ry = Matrix4x4.rotationYDeg(rotationDeg.getY());
        Matrix4x4 Rz = Matrix4x4.rotationZDeg(rotationDeg.getZ());
        Matrix4x4 S = Matrix4x4.scale(scale.getX(), scale.getY(), scale.getZ());

        return T.multiply(Rz.multiply(Ry.multiply(Rx.multiply(S))));
    }

    // применение трансформации к одной вершине
    public Vector3D transformVertex(Vector3D vertex) {
        Vector4D local = new Vector4D(vertex, 1.0f);
        Vector4D world = getModelMatrix().multiply(local);
        return world.toVector3D();
    }
}
