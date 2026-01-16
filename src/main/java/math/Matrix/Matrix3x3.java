package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector3D;

public final class Matrix3x3 extends AbstractMatrix<Matrix3x3, Vector3D> {
    public Matrix3x3(float[][] components){
        super(components, 3, 3);
    }
    @Override
    protected Matrix3x3 createNew(float[][] components){
        return new Matrix3x3(components);
    }

    public static Matrix3x3 identity(){
        return new Matrix3x3(new float[][]{
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        });
    }

    public static Matrix3x3 zero(){
        return new Matrix3x3(new float[3][3]);
    }

    @Override
    public Vector3D multiply(Vector3D vector){
        float x = components[0][0] * vector.getX() + components[0][1] * vector.getY() + components[0][2] * vector.getZ();
        float y = components[1][0] * vector.getX() + components[1][1] * vector.getY() + components[1][2] * vector.getZ();
        float z = components[2][0] * vector.getX() + components[2][1] * vector.getY() + components[2][2] * vector.getZ();
        return new Vector3D(x, y, z);
    }
    @Override
    public Matrix3x3 multiply(float scalar){
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = this.components[i][j] * scalar;
            }
        }
        return createNew(result);
    }

    @Override
    public float determinant(){
        float a = components[0][0], b = components[0][1], c = components[0][2];
        float d = components[1][0], e = components[1][1], f = components[1][2];
        float g = components[2][0], h = components[2][1], i = components[2][2];

        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    }
    @Override
    public Matrix3x3 inverse(){
        float det = determinant();
        if (Math.abs(det) < 1e-12f){
            throw  new ArithmeticException("Matrix is singular, cannot invert");
        }
        float a = components[0][0], b = components[0][1], c = components[0][2];
        float d = components[1][0], e = components[1][1], f = components[1][2];
        float g = components[2][0], h = components[2][1], i = components[2][2];

        float invDet = 1.0f / det;
        float[][] result = {
                {(e * i - f * h) * invDet, (c * h - b * i) * invDet, (b * f - c * e) * invDet},
                {(f * g - d * i) * invDet, (a * i - c * g) * invDet, (c * d - a * f) * invDet},
                {(d * h - e * g) * invDet, (b * g - a * h) * invDet, (a * e - b * d) * invDet}
        };
        return new Matrix3x3(result);
    }

    @Override
    public Vector3D solveLinealSystem(Vector3D vector){
        return solveGauss(vector);
    }
    public Vector3D solveGauss(Vector3D b){
        float[][] augmented = new float[3][4];

        for (int i = 0; i < 3; i++) {
            System.arraycopy(components[i], 0, augmented[i], 0, 3);
            switch(i) {
                case 0: augmented[i][3] = b.getX(); break;
                case 1: augmented[i][3] = b.getY(); break;
                case 2: augmented[i][3] = b.getZ(); break;
            }
        }

        for (int i = 0; i < 3; i++) {
            int maxRow = i;
            for (int k = i + 1; k < 3; k++) {
                if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                    maxRow = k;
                }
            }

            float[] temp = augmented[i];
            augmented[i] = augmented[maxRow];
            augmented[maxRow] = temp;

            if (Math.abs(augmented[i][i]) < 1e-12f) {
                throw new ArithmeticException("Matrix is singular, cannot solve system");
            }

            for (int k = i + 1; k < 3; k++) {
                float factor = augmented[k][i] / augmented[i][i];
                for (int j = i; j < 4; j++) {
                    augmented[k][j] -= factor * augmented[i][j];
                }
            }
        }

        float[] solution = new float[3];
        for (int i = 2; i >= 0; i--) {
            solution[i] = augmented[i][3];
            for (int j = i + 1; j < 3; j++) {
                solution[i] -= augmented[i][j] * solution[j];
            }
            solution[i] /= augmented[i][i];
        }

        return new Vector3D(solution[0], solution[1], solution[2]);
    }
}
