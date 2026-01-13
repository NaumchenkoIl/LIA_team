package scene_master.writer;

import scene_master.model.Model;
import scene_master.model.Polygon;
import scene_master.model.Vector3D;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ObjWriter {

    public static void write(Model model, String filePath) throws IOException {
        write(model, Path.of(filePath));
    }

    public static void write(Model model, Path outputPath) throws IOException {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }

        ModelValidator.validate(model);
        String content = modelToString(model);
        Files.writeString(outputPath, content);
    }

    public static String modelToString(Model model) {
        return modelToString(model, "Exported by Team CG&Geom");
    }

    public static String modelToString(Model model, String comment) {
        ModelValidator.validate(model);
        StringBuilder sb = new StringBuilder();

        if (comment != null && !comment.trim().isEmpty()) {
            sb.append("# ").append(comment.trim()).append('\n');
        }

        // Вершины
        appendVertices(sb, model.getVertices());

        // Текстурные координаты
        appendTextureVertices(sb, model.getTextureVertices());

        // Нормали
        appendNormals(sb, model.getNormals());

        // Грани
        appendPolygons(sb, model.getPolygons());

        return sb.toString();
    }

    private static void appendVertices(StringBuilder sb, List<Vector3D> vertices) {
        if (vertices == null || vertices.isEmpty()) return;
        for (Vector3D v : vertices) {
            sb.append("v ")
                    .append(formatFloat(v.getX())).append(' ')
                    .append(formatFloat(v.getY())).append(' ')
                    .append(formatFloat(v.getZ())).append('\n');
        }
    }

    private static void appendTextureVertices(StringBuilder sb, List<Vector3D> texCoords) {
        if (texCoords == null || texCoords.isEmpty()) return;
        sb.append('\n');
        for (Vector3D vt : texCoords) {
            sb.append("vt ")
                    .append(formatFloat(vt.getX())).append(' ')
                    .append(formatFloat(vt.getY())).append('\n');
        }
    }

    private static void appendNormals(StringBuilder sb, List<Vector3D> normals) {
        if (normals == null || normals.isEmpty()) return;
        sb.append('\n');
        for (Vector3D vn : normals) {
            sb.append("vn ")
                    .append(formatFloat(vn.getX())).append(' ')
                    .append(formatFloat(vn.getY())).append(' ')
                    .append(formatFloat(vn.getZ())).append('\n');
        }
    }

    private static void appendPolygons(StringBuilder sb, List<Polygon> polygons) {
        if (polygons == null || polygons.isEmpty()) return;
        sb.append('\n');
        for (Polygon p : polygons) {
            sb.append("f");
            List<Integer> vis = p.getVertexIndices();
            List<Integer> vtis = p.getTextureIndices();
            List<Integer> vnis = p.getNormalIndices();

            boolean hasVt = vtis != null && !vtis.isEmpty();
            boolean hasVn = vnis != null && !vnis.isEmpty();

            for (int i = 0; i < vis.size(); i++) {
                sb.append(' ').append(vis.get(i) + 1);

                if (hasVt || hasVn) {
                    sb.append('/');
                    if (hasVt) {
                        int tIdx = vtis.get(i);
                        sb.append(tIdx >= 0 ? tIdx + 1 : "");
                    }
                    if (hasVn) {
                        sb.append('/');
                        int nIdx = vnis.get(i);
                        sb.append(nIdx >= 0 ? nIdx + 1 : "");
                    }
                }
            }
            sb.append('\n');
        }
    }

    private static String formatFloat(double value) {
        if (value == 0.0) value = 0.0;
        String s = String.format(Locale.ROOT, "%.6f", value);
        if (s.contains(".")) {
            s = s.replaceAll("0*$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}