package scene_master.model;

public class Vector3D {
    private final double x;
    private final double y;
    private final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D crossProduct(Vector3D other) {
        return new Vector3D(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z, this.x * other.y - this.y * other.x);
    }

    public Vector3D normalize() {
        double length = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (length == 0.0) {
            throw new ArithmeticException("Cannot normalize zero-length vector");
        } else {
            return new Vector3D(this.x / length, this.y / length, this.z / length);
        }
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public Vertex toVertex() {
        return new Vertex(this.x, this.y, this.z);
    }

    public static Vector3D fromVertex(Vertex v) {
        return new Vector3D(v.x, v.y, v.z);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            Vector3D vector = (Vector3D)obj;
            return Math.abs(vector.x - this.x) < 1.0E-10 && Math.abs(vector.y - this.y) < 1.0E-10 && Math.abs(vector.z - this.z) < 1.0E-10;
        } else {
            return false;
        }
    }

    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", this.x, this.y, this.z);
    }
}
