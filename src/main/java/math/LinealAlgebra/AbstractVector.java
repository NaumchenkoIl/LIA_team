package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra;

public abstract class AbstractVector<T extends AbstractVector<T>> implements Vector<T> {

    protected final float[] components;
    protected final int dimensions;

    protected AbstractVector(float[] components) {
        this.components = components.clone();
        this.dimensions = components.length;
    }

    protected abstract T createNew(float[] components);

    @Override
    public T add(T other) {
        checkDimensions(other);
        float[] result = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            result[i] = this.components[i] + other.components[i];
        }
        return createNew(result);
    }

    @Override
    public T subtract(T other) {
        checkDimensions(other);
        float[] result = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            result[i] = this.components[i] - other.components[i];
        }
        return createNew(result);
    }

    @Override
    public T multiply(float scalar) {
        float[] result = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            result[i] = this.components[i] * scalar;
        }
        return createNew(result);
    }

    @Override
    public T divide(float scalar) {
        if (Math.abs(scalar) < 1e-12f) {
            throw new ArithmeticException("Division by zero");
        }
        return multiply(1.0f / scalar);
    }

    @Override
    public float length() {
        float sum = 0;
        for (float component : components) {
            sum += component * component;
        }
        return (float) Math.sqrt(sum);
    }

    @Override
    public T normalize() {
        float len = length();
        if (len < 1e-12f) {
            throw new ArithmeticException("Cannot normalize zero vector");
        }
        return multiply(1.0f / len);
    }

    @Override
    public float dot(T other) {
        checkDimensions(other);
        float result = 0;
        for (int i = 0; i < dimensions; i++) {
            result += this.components[i] * other.components[i];
        }
        return result;
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public float getComponent(int index) {
        if (index < 0 || index >= dimensions) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return components[index];
    }

    protected void checkDimensions(T other) {
        if (this.dimensions != other.dimensions) {
            throw new IllegalArgumentException("Vector dimensions don't match");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AbstractVector<?> other = (AbstractVector<?>) obj;
        if (this.dimensions != other.dimensions) return false;

        for (int i = 0; i < dimensions; i++) {
            if (Math.abs(this.components[i] - other.components[i]) >= 1e-6f) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vector").append(dimensions).append("D(");
        for (int i = 0; i < dimensions; i++) {
            sb.append(String.format("%.3f", components[i]));
            if (i < dimensions - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
