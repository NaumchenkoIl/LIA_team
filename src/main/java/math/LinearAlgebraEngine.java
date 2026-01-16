package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector2D;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector3D;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector4D;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix.Matrix3x3;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix.Matrix4x4;

public final class LinearAlgebraEngine {

    private LinearAlgebraEngine() {}

    public static Vector2D createVector2D(float x, float y) {
        return new Vector2D(x, y);
    }

    public static Vector3D createVector3D(float x, float y, float z) {
        return new Vector3D(x, y, z);
    }

    public static Vector4D createVector4D(float x, float y, float z, float w) {
        return new Vector4D(x, y, z, w);
    }

    public static Vector4D createVector4DFrom3D(Vector3D vector, float w) {
        return new Vector4D(vector, w);
    }

    public static Matrix3x3 createIdentityMatrix3x3() {
        return Matrix3x3.identity();
    }

    public static Matrix3x3 createZeroMatrix3x3() {
        return Matrix3x3.zero();
    }

    public static Matrix4x4 createIdentityMatrix4x4() {
        return Matrix4x4.identity();
    }

    public static Matrix4x4 createZeroMatrix4x4() {
        return Matrix4x4.zero();
    }

    public static Matrix4x4 createRotationMatrixX(float angle) {
        return Matrix4x4.rotationX(angle);
    }

    public static Matrix4x4 createRotationMatrixY(float angle) {
        return Matrix4x4.rotationY(angle);
    }

    public static Matrix4x4 createRotationMatrixZ(float angle) {
        return Matrix4x4.rotationZ(angle);
    }

    public static Matrix4x4 createScaleMatrix(float sx, float sy, float sz) {
        return Matrix4x4.scale(sx, sy, sz);
    }

    public static Matrix4x4 createTranslationMatrix(float x, float y, float z) {
        return Matrix4x4.translation(x, y, z);
    }

    public static float computeAngleBetweenVectors(Vector3D v1, Vector3D v2) {
        float dotProduct = v1.dot(v2);
        float lengths = v1.length() * v2.length();

        if (Math.abs(lengths) < 1e-12f) {
            throw new ArithmeticException("Cannot compute angle for zero vectors");
        }

        float cosAngle = dotProduct / lengths;
        cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));

        return (float) Math.acos(cosAngle);
    }
}
