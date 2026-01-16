package scene_master.model;

public class TexturePoint {
    private final double u;
    private final double v;

    public TexturePoint(double u, double v) {
        this.u = u;
        this.v = v;
    }

    public double getU() {
        return u;
    }

    public double getV() {
        return v;
    }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f)", u, v);
    }
}