package scene_master.model;

public class Vertex {
    public final double x;
    public final double y;
    public final double z;
    public final double u; // Текстурная координата U
    public final double v; // Текстурная координата V

    public Vertex(double x, double y, double z, double u, double v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }

    // Метод для конвертации в Vector3D (без UV)
    public Vector3D toVector3D() {
        return new Vector3D(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f) [%.2f, %.2f]", x, y, z, u, v);
    }
}
