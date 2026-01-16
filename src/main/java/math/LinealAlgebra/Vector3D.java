package math.LinealAlgebra;

public final class Vector3D extends AbstractVector<Vector3D> {
    public Vector3D(float x, float y, float z){
        super(new float[]{x, y, z});
    }

    @Override
    protected Vector3D createNew(float[] components){
        return new Vector3D(components[0], components[1], components[2]);
    }

    public float getX() { return components[0]; };
    public float getY() { return components[1]; };
    public float getZ() { return components[2]; };

    public Vector3D cross(Vector3D other){
        return new Vector3D(
                getY() * other.getZ() - getZ() * other.getY(),
                getZ() * other.getX() - getX() * other.getZ(),
                getX() * other.getY() - getY() * other.getX()
        );
    }
    public float dot(Vector3D other){
        checkDimensions(other);
        float result = 0;
        for (int i = 0; i < dimensions; i++) {
            result += this.components[i] * other.components[i];
        }
        return result;
    }

    public Vector3D multiply(float scalar){
        float[] result = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            result[i] = this.components[i] * scalar;
        }
        return createNew(result);
    }

    public float distance(Vector3D other) {
        float dx = getX() - other.getX();
        float dy = getY() - other.getY();
        float dz = getZ() - other.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }


    @Override
    public String toString() {
        return String.format("Vector3D(%.3f, %.3f, %.3f)", getX(), getY(), getZ());
    }
}
