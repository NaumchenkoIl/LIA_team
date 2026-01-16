package math.LinealAlgebra;


public final class Vector4D extends AbstractVector<Vector4D> {

    public Vector4D(float x, float y, float z, float w){
        super(new float[] {x, y, z, w});
    }


    public Vector4D(Vector3D vector, float w){
        this(vector.getX(), vector.getY(), vector.getZ(), w);
    }

    @Override
    protected Vector4D createNew(float[] components){
        return new Vector4D(components[0], components[1], components[2], components[3]);
    }
    public float getX(){ return components[0]; }
    public float getY(){ return components[1]; }
    public float getZ(){ return components[2]; }
    public float getW(){ return components[3]; }


    public Vector3D toVector3D() {
        float w = getW();
        if (Math.abs(w) < 1e-12f) {
            return new Vector3D(getX(), getY(), getZ());
        }
        float invW = 1.0f / w;
        return new Vector3D(
                getX() * invW,
                getY() * invW,
                getZ() * invW
        );
    }

    public Vector4D multiply(float scalar){
        float[] result = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            result[i] = this.components[i] * scalar;
        }
        return createNew(result);
    }

    public float dot(Vector4D other){
        checkDimensions(other);
        float result = 0;
        for (int i = 0; i < dimensions; i++) {
            result += this.components[i] * other.components[i];
        }
        return result;
    }

    public float distance(Vector4D other) {
        float dx = getX() - other.getX();
        float dy = getY() - other.getY();
        float dz = getZ() - other.getZ();
        float dw = getW() - other.getW();
        return (float) Math.sqrt(dx*dx + dy*dy + dz*dz + dw*dw);
    }

    @Override
    public String toString() {
        return String.format("Vector4D(%.3f, %.3f, %.3f, %.3f)", getX(), getY(), getZ(), getW());
    }


    public Vector4D perspectiveDivide() {
        float w = getW();
        if (Math.abs(w) < 1e-12f) {
            return new Vector4D(getX(), getY(), getZ(), 0.0f);
        }
        float invW = 1.0f / w;
        return new Vector4D(
                getX() * invW,
                getY() * invW,
                getZ() * invW,
                1.0f
        );
    }

    @Override
    public float length() {
        return (float) Math.sqrt(getX() * getX() + getY() * getY() + getZ() * getZ());
    }
    @Override
    public Vector4D normalize() {
        float len = length();
        if (len < 1e-12f){
            return new Vector4D(0, 0, 0, getW());
        }
        return new Vector4D(getX() / len, getY() / len, getZ() / len, getW());
    }

    // Добавить полезные методы:
    public boolean isPoint() {
        return Math.abs(getW() - 1.0f) < 1e-6f;
    }

    public boolean isDirection() {
        return Math.abs(getW()) < 1e-6f;
    }

    public Vector4D asPoint() {
        if (isDirection()) {
            throw new ArithmeticException("Cannot convert direction to point");
        }
        if (isPoint()) return this;
        return perspectiveDivide();
    }

    public Vector4D asDirection() {
        return new Vector4D(getX(), getY(), getZ(), 0.0f);
    }
}
