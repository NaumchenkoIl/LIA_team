package math.Matrix;

import math.LinealAlgebra.Vector3D;
import math.LinealAlgebra.Vector4D;

public final class Matrix4x4 extends AbstractMatrix<Matrix4x4, Vector4D> {
    public Matrix4x4(float[][] components){
        super(components, 4, 4);
    }
    @Override
    protected Matrix4x4 createNew(float[][] components){
        return new Matrix4x4(components);
    }
    public static Matrix4x4 identity() {
        return new Matrix4x4(new float[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });
    }

    public static Matrix4x4 zero() {
        return new Matrix4x4(new float[4][4]);
    }

    public static Matrix4x4 translation(float x, float y, float z) {
        return new Matrix4x4(new float[][]{
                {1, 0, 0, x},
                {0, 1, 0, y},
                {0, 0, 1, z},
                {0, 0, 0, 1}
        });
    }

    @Override
    public Matrix4x4 multiply(float scalar){
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = this.components[i][j] * scalar;
            }
        }
        return createNew(result);
    }

    @Override
    public Vector4D multiply(Vector4D vector) {
        float x = components[0][0] * vector.getX() + components[0][1] * vector.getY() + components[0][2] * vector.getZ() + components[0][3] * vector.getW();
        float y = components[1][0] * vector.getX() + components[1][1] * vector.getY() + components[1][2] * vector.getZ() + components[1][3] * vector.getW();
        float z = components[2][0] * vector.getX() + components[2][1] * vector.getY() + components[2][2] * vector.getZ() + components[2][3] * vector.getW();
        float w = components[3][0] * vector.getX() + components[3][1] * vector.getY() + components[3][2] * vector.getZ() + components[3][3] * vector.getW();
        return new Vector4D(x, y, z, w);
    }


    @Override
    public float determinant() {
        float det = 0;
        for (int j = 0; j < 4; j++) {
            det += components[0][j] * cofactor(0, j);
        }
        return det;
    }

    @Override
    public Matrix4x4 inverse() {
        float det = determinant();
        if (Math.abs(det) < 1e-12f) {
            throw new ArithmeticException("Matrix is singular, cannot invert");
        }

        float[][] result = new float[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[j][i] = cofactor(i, j) / det;
            }
        }
        return new Matrix4x4(result);
    }

    @Override
    public Vector4D solveLinealSystem(Vector4D vector) {
        return solveGauss(vector);
    }

    private Vector4D solveGauss(Vector4D b) {
        float[][] augmented = new float[4][5];

        for (int i = 0; i < 4; i++) {
            System.arraycopy(components[i], 0, augmented[i], 0, 4);
            switch(i) {
                case 0: augmented[i][4] = b.getX(); break;
                case 1: augmented[i][4] = b.getY(); break;
                case 2: augmented[i][4] = b.getZ(); break;
                case 3: augmented[i][4] = b.getW(); break;
            }
        }

        for (int i = 0; i < 4; i++) {
            int maxRow = i;
            for (int k = i + 1; k < 4; k++) {
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

            for (int k = i + 1; k < 4; k++) {
                float factor = augmented[k][i] / augmented[i][i];
                for (int j = i; j < 5; j++) {
                    augmented[k][j] -= factor * augmented[i][j];
                }
            }
        }

        float[] solution = new float[4];
        for (int i = 3; i >= 0; i--) {
            solution[i] = augmented[i][4];
            for (int j = i + 1; j < 4; j++) {
                solution[i] -= augmented[i][j] * solution[j];
            }
            solution[i] /= augmented[i][i];
        }

        return new Vector4D(solution[0], solution[1], solution[2], solution[3]);
    }

    private float minor(int row, int col) {
        float[][] minorMatrix = new float[3][3];
        int minorRow = 0;

        for (int i = 0; i < 4; i++) {
            if (i == row) continue;
            int minorCol = 0;
            for (int j = 0; j < 4; j++) {
                if (j == col) continue;
                minorMatrix[minorRow][minorCol] = components[i][j];
                minorCol++;
            }
            minorRow++;
        }

        Matrix3x3 minor = new Matrix3x3(minorMatrix);
        return minor.determinant();
    }

    private float cofactor(int row, int col) {
        float minor = minor(row, col);
        return ((row + col) % 2 == 0) ? minor : -minor;
    }

    public static Matrix4x4 scale(float sx, float sy, float sz) {
        return new Matrix4x4(new float[][]{
                {sx,  0f,  0f, 0f},
                {0f,  sy,  0f, 0f},
                {0f,  0f,  sz, 0f},
                {0f,  0f,  0f, 1f}
        });
    }

    public static Matrix4x4 rotationX(float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);

        return new Matrix4x4(new float[][]{
                {1f,  0f,   0f,  0f},
                {0f,  cos, sin, 0f},
                {0f,  -sin,  cos, 0f},
                {0f,  0f,   0f,  1f}
        });
    }
    public static Matrix4x4 rotationY(float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);

        return new Matrix4x4(new float[][]{
                {cos,  0f,  sin, 0f},
                {0f,   1f,  0f,  0f},
                {-sin, 0f,  cos, 0f},
                {0f,   0f,  0f,  1f}
        });
    }

    public static Matrix4x4 rotationZ(float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);

        return new Matrix4x4(new float[][]{
                {cos, sin, 0f, 0f},
                {-sin,  cos, 0f, 0f},
                {0f,   0f,  1f, 0f},
                {0f,   0f,  0f, 1f}
        });
    }

    public static Matrix4x4 rotationXDeg(float angleDeg) {
        return rotationX((float) Math.toRadians(angleDeg));
    }

    public static Matrix4x4 rotationYDeg(float angleDeg) {
        return rotationY((float) Math.toRadians(angleDeg));
    }

    public static Matrix4x4 rotationZDeg(float angleDeg) {
        return rotationZ((float) Math.toRadians(angleDeg));
    }

    // камера
    public static Matrix4x4 lookAt(Vector3D eye, Vector3D target, Vector3D up) {

        // направление взгляда (z-ось камеры)
        Vector3D zAxis = target.subtract(eye).normalize();

        // правая ось (x-ось камеры) = up × z
        Vector3D xAxis = up.cross(zAxis).normalize();

        // вектор "вверх" (y-ось камеры) = z × x
        Vector3D yAxis = zAxis.cross(xAxis);

        return new Matrix4x4(new float[][]{
                {xAxis.getX(), xAxis.getY(), xAxis.getZ(), -xAxis.dot(eye)},
                {yAxis.getX(), yAxis.getY(), yAxis.getZ(), -yAxis.dot(eye)},
                {zAxis.getX(), zAxis.getY(), zAxis.getZ(), -zAxis.dot(eye)},
                {0f, 0f, 0f, 1f}
        });
    }

    // матрица проекции
    public static Matrix4x4 perspective(float fovDegrees, float aspectRatio, float near, float far) {

        float fovRad = (float) Math.toRadians(fovDegrees);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);
        float rangeInv = 1.0f / (near - far);

        return new Matrix4x4(new float[][]{
                {f / aspectRatio, 0f, 0f, 0f},
                {0f, f, 0f, 0f},
                {0f, 0f, (near + far) * rangeInv, 2 * near * far * rangeInv},
                {0f, 0f, -1f, 0f}
        });
    }

}
