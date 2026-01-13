package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.AbstractVector;

public abstract class AbstractMatrix<T extends AbstractMatrix<T, V>, V extends AbstractVector<V>> implements Matrix<T, V> {

    protected final float[][] components;
    protected final int rows;
    protected final int cols;

    protected AbstractMatrix(float[][] components, int rows, int cols) {
        validateMatrix(components, rows, cols);
        this.rows = rows;
        this.cols = cols;
        this.components = deepCopy(components);
    }

    protected abstract T createNew(float[][] data);

    @Override
    public T add(T other) {
        checkDimensions(other);
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = this.components[i][j] + other.components[i][j];
            }
        }
        return createNew(result);
    }

    @Override
    public T subtract(T other) {
        checkDimensions(other);
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = this.components[i][j] - other.components[i][j];
            }
        }
        return createNew(result);
    }

    @Override
    public T multiply(float scalar) {
        float[][] result = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = this.components[i][j] * scalar;
            }
        }
        return createNew(result);
    }

    @Override
    public T multiply(T other) {
        if (this.cols != other.rows) {
            throw new IllegalArgumentException("Matrix dimensions don't match for multiplication");
        }

        float[][] result = new float[this.rows][other.cols];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < other.cols; j++) {
                float sum = 0;
                for (int k = 0; k < this.cols; k++) {
                    sum += this.components[i][k] * other.components[k][j];
                }
                result[i][j] = sum;
            }
        }
        return createNew(result);
    }

    @Override
    public T transpose() {
        float[][] result = new float[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = this.components[i][j];
            }
        }
        return createNew(result);
    }

    @Override
    public int getRows() { return rows; }

    @Override
    public int getColumns() { return cols; }

    @Override
    public float get(int row, int col) {
        checkIndices(row, col);
        return components[row][col];
    }

    protected void checkDimensions(T other) {
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException("Matrix dimensions don't match");
        }
    }

    protected void checkIndices(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IllegalArgumentException("Indices out of bounds");
        }
    }

    protected void validateMatrix(float[][] matrix, int expectedRows, int expectedCols) {
        if (matrix == null || matrix.length != expectedRows) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }
        for (float[] row : matrix) {
            if (row == null || row.length != expectedCols) {
                throw new IllegalArgumentException("Invalid matrix dimensions");
            }
        }
    }

    protected float[][] deepCopy(float[][] original) {
        float[][] copy = new float[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AbstractMatrix<?, ?> other = (AbstractMatrix<?, ?>) obj;
        if (this.rows != other.rows || this.cols != other.cols) return false;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (Math.abs(this.components[i][j] - other.components[i][j]) >= 1e-6f) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix ").append(rows).append("x").append(cols).append(":\n");
        for (int i = 0; i < rows; i++) {
            sb.append("[ ");
            for (int j = 0; j < cols; j++) {
                sb.append(String.format("%8.3f ", components[i][j]));
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

}
