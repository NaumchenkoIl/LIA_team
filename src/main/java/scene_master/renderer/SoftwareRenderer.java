package scene_master.renderer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.KeyCode;
import math.Camera;
import math.CameraInputAdapter;
import math.LinealAlgebra.Vector4D;
import math.Matrix.Matrix4x4;
import math.ModelTransform;
import scene_master.model.Model3D;
import scene_master.model.Polygon;
import math.LinealAlgebra.Vector3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.*;

public class SoftwareRenderer {
    private Canvas canvas;
    private GraphicsContext gc;
    private int width;
    private int height;

    private double[][] zBuffer;

    private boolean renderWireframe = false;
    private boolean showVertices = false;
    private boolean useTexture = false;
    private boolean useLighting = false;

    private TextureManager textureManager = TextureManager.getInstance();

    private double ambientLight = 0.3;
    private double diffuseIntensity = 0.7;

    private Color backgroundColor = Color.rgb(30, 30, 46);
    private Color vertexColor = Color.YELLOW;
    private Color wireframeColor = Color.RED;

    private int debugTriangleCount = 0;
    private long lastRenderTime = 0;
    private static final long MIN_RENDER_INTERVAL = 16;

    private Camera camera;
    private CameraInputAdapter cameraInputAdapter;
    private WritableImage buffer;
    private PixelWriter pixelWriter;

    public SoftwareRenderer(Canvas canvas, Camera camera) {
        this.canvas = canvas;
        this.camera = camera;
        this.cameraInputAdapter = new CameraInputAdapter(camera);
        this.width = 0;
        this.height = 0;
        this.zBuffer = null;
        this.buffer = null;
        this.pixelWriter = null;
    }

    private void initZBuffer() {
        zBuffer = new double[width][height];
        clearZBuffer();
    }

    public void clearZBuffer() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                zBuffer[x][y] = Double.POSITIVE_INFINITY;
            }
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        initZBuffer();
        buffer = new WritableImage(width, height);
        pixelWriter = buffer.getPixelWriter();
    }

    // Геттеры/сеттеры
    public void setRenderWireframe(boolean renderWireframe) { this.renderWireframe = renderWireframe; }
    public void setShowVertices(boolean showVertices) { this.showVertices = showVertices; }
    public void setUseTexture(boolean useTexture) { this.useTexture = useTexture; }
    public void setUseLighting(boolean useLighting) { this.useLighting = useLighting; }
    public void setVertexColor(Color color) { this.vertexColor = color; }
    public void setWireframeColor(Color color) { this.wireframeColor = color; }
    public void setBackgroundColor(Color color) { this.backgroundColor = color; }
    public void setAmbientLight(double ambient) { this.ambientLight = Math.max(0, Math.min(1, ambient)); }
    public void setDiffuseIntensity(double diffuse) { this.diffuseIntensity = Math.max(0, Math.min(1, diffuse)); }

    /**
     * Очистка экрана и Z-буфера
     */
    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelWriter.setColor(x, y, backgroundColor);
            }
        }
        clearZBuffer();
    }

    /**
     * Рендеринг сцены со всеми моделями
     */
    public void renderScene(List<Model3D> models) {
        int currentWidth = (int) canvas.getWidth();
        int currentHeight = (int) canvas.getHeight();

        if (currentWidth != width || currentHeight != height) {
            this.width = currentWidth;
            this.height = currentHeight;

            if (width <= 0) width = 1;
            if (height <= 0) height = 1;

            initZBuffer();
            buffer = new WritableImage(width, height);
            pixelWriter = buffer.getPixelWriter();
        }

        this.gc = canvas.getGraphicsContext2D();
        if (gc == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRenderTime < MIN_RENDER_INTERVAL) {
            return;
        }
        lastRenderTime = currentTime;

        camera.setAspectRatio((float) width / height);
        clear();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelWriter.setColor(x, y, backgroundColor);
            }
        }
        clearZBuffer();

        debugTriangleCount = 0;

        Matrix4x4 viewMatrix = camera.getViewMatrix();
        Matrix4x4 projectionMatrix = camera.getProjectionMatrix();

        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            boolean textureReady = false;
            if (useTexture && model.getTexture() != null) {
                if (!model.getTexture().isBackgroundLoading() && !model.getTexture().isError()) {
                    textureReady = true;
                }
            }

            double tx = model.translateXProperty().get();
            double ty = model.translateYProperty().get();
            double tz = model.translateZProperty().get();
            double rx = Math.toRadians(model.rotateXProperty().get());
            double ry = Math.toRadians(model.rotateYProperty().get());
            double rz = Math.toRadians(model.rotateZProperty().get());
            double sx = model.scaleXProperty().get();
            double sy = model.scaleYProperty().get();
            double sz = model.scaleZProperty().get();

            for (Polygon polygon : model.getPolygons()) {
                List<Integer> indices = polygon.getVertexIndices();
                if (indices.size() != 3) continue;

                Vector3D v1 = model.getVertices().get(indices.get(0));
                Vector3D v2 = model.getVertices().get(indices.get(1));
                Vector3D v3 = model.getVertices().get(indices.get(2));

                double[] world1 = transformVertex(v1, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                double[] world2 = transformVertex(v2, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                double[] world3 = transformVertex(v3, tx, ty, tz, rx, ry, rz, sx, sy, sz);

                double[] screen1 = projectWithCamera(world1, viewMatrix, projectionMatrix);
                double[] screen2 = projectWithCamera(world2, viewMatrix, projectionMatrix);
                double[] screen3 = projectWithCamera(world3, viewMatrix, projectionMatrix);

                renderTriangle(screen1, screen2, screen3, model, polygon, textureReady);
            }
        }

        if (renderWireframe) {
            renderWireframe(models, viewMatrix, projectionMatrix);
        }

        if (showVertices) {
            renderVertices(models, viewMatrix, projectionMatrix);
        }

        if (gc != null) {
            gc.drawImage(buffer, 0, 0);
        }
    }

    /**
     * Преобразование вершины с учетом трансформаций модели
     */
    public double[] transformVertex(Vector3D v, double tx, double ty, double tz,
                                    double rx, double ry, double rz,
                                    double sx, double sy, double sz) {
        ModelTransform transform = new ModelTransform();
        transform.setTranslation((float) tx, (float) ty, (float) tz);
        transform.setRotationDeg((float) Math.toDegrees(rx), (float) Math.toDegrees(ry), (float) Math.toDegrees(rz));
        transform.setScale((float) sx, (float) sy, (float) sz);

        Vector3D transformed = transform.transformVertex(v);
        return new double[]{transformed.getX(), transformed.getY(), transformed.getZ()};
    }

    /**
     * Рендеринг одного треугольника
     */
    private void renderTriangle(double[] p1, double[] p2, double[] p3,
                                Model3D model, Polygon polygon, boolean textureReady) {

        Vector3D faceNormal = polygon.getNormal();
        if (faceNormal == null) return;

        List<Integer> indices = polygon.getVertexIndices();
        if (indices.size() < 3) return;

        double x1 = p1[0], y1 = p1[1], z1 = p1[2];
        double x2 = p2[0], y2 = p2[1], z2 = p2[2];
        double x3 = p3[0], y3 = p3[1], z3 = p3[2];

        int minX = (int) Math.max(0, Math.min(Math.min(x1, x2), x3));
        int maxX = (int) Math.min(width - 1, Math.max(Math.max(x1, x2), x3));
        int minY = (int) Math.max(0, Math.min(Math.min(y1, y2), y3));
        int maxY = (int) Math.min(height - 1, Math.max(Math.max(y1, y2), y3));

        if (minX >= maxX || minY >= maxY) return;

        double area = edgeFunction(x1, y1, x2, y2, x3, y3);
        if (Math.abs(area) < 0.0001) return;

        double[] uv1 = model.getTextureCoordsForPolygonVertex(polygon, 0);
        double[] uv2 = model.getTextureCoordsForPolygonVertex(polygon, 1);
        double[] uv3 = model.getTextureCoordsForPolygonVertex(polygon, 2);

        if (debugTriangleCount++ < 3) {
            System.out.println("=== Треугольник " + debugTriangleCount + " ===");
            System.out.println("Модель: " + model.getName());
            System.out.println("Текстура: " + (model.getTexture() != null ? "Есть" : "Нет"));
            System.out.println("Режим текстуры: " + useTexture);
            System.out.println("UV1: [" + String.format("%.3f", uv1[0]) + ", " + String.format("%.3f", uv1[1]) + "]");
            System.out.println("UV2: [" + String.format("%.3f", uv2[0]) + ", " + String.format("%.3f", uv2[1]) + "]");
            System.out.println("UV3: [" + String.format("%.3f", uv3[0]) + ", " + String.format("%.3f", uv3[1]) + "]");
            System.out.println("Нормаль: " + faceNormal.getX() + ", " + faceNormal.getY() + ", " + faceNormal.getZ());
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double w1 = edgeFunction(x2, y2, x3, y3, x, y) / area;
                double w2 = edgeFunction(x3, y3, x1, y1, x, y) / area;
                double w3 = edgeFunction(x1, y1, x2, y2, x, y) / area;

                if (w1 >= -0.0001 && w2 >= -0.0001 && w3 >= -0.0001) {
                    double depth = w1 * z1 + w2 * z2 + w3 * z3;
                    if (depth < zBuffer[x][y]) {
                        zBuffer[x][y] = depth;

                        double u = w1 * uv1[0] + w2 * uv2[0] + w3 * uv3[0];
                        double v = w1 * uv1[1] + w2 * uv2[1] + w3 * uv3[1];

                        double[] interpolatedNormal = null;
                        List<Vector3D> vertexNormals = model.getVertexNormals();

                        if (vertexNormals != null && vertexNormals.size() == model.getVertices().size()) {
                            Vector3D n1 = vertexNormals.get(indices.get(0));
                            Vector3D n2 = vertexNormals.get(indices.get(1));
                            Vector3D n3 = vertexNormals.get(indices.get(2));
                            interpolatedNormal = new double[]{
                                    w1 * n1.getX() + w2 * n2.getX() + w3 * n3.getX(),
                                    w1 * n1.getY() + w2 * n2.getY() + w3 * n3.getY(),
                                    w1 * n1.getZ() + w2 * n2.getZ() + w3 * n3.getZ()
                            };
                        } else {
                            interpolatedNormal = new double[]{
                                    faceNormal.getX(), faceNormal.getY(), faceNormal.getZ()
                            };
                        }

                        Color pixelColor;
                        if (textureReady) {
                            pixelColor = calculatePixelColor(model, u, v, interpolatedNormal);
                        } else {
                            pixelColor = model.getBaseColor();
                            if (useLighting && interpolatedNormal != null) {
                                pixelColor = applyLightingToColor(pixelColor, interpolatedNormal);
                            }
                        }

                        pixelWriter.setColor(x, y, pixelColor);
                    }
                }
            }
        }
    }

    /**
     * Вычисление функции ребра
     */
    private double edgeFunction(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    /**
     * Вычисление цвета пикселя
     */
    private Color calculatePixelColor(Model3D model, double u, double v, double[] normal) {
        Color baseColor = model.getBaseColor();
        if (useTexture && model.getTexture() != null && !model.getTextureCoords().isEmpty()) {
            baseColor = textureManager.getTextureColor(model.getTexture(), u, v);
        }
        if (useLighting && normal != null) {
            baseColor = applyLightingToColor(baseColor, normal);
        }
        return baseColor;
    }

    /**
     * Применение освещения к цвету
     */
    private Color applyLightingToColor(Color color, double[] normal) {
        if (normal == null || !useLighting) return color;

        double len = Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (len > 0) {
            normal[0] /= len;
            normal[1] /= len;
            normal[2] /= len;
        }

        Vector3D lightDir = camera.getTarget().subtract(camera.getPosition()).normalize();
        double lx = lightDir.getX();
        double ly = lightDir.getY();
        double lz = lightDir.getZ();

        double dot = normal[0] * lx + normal[1] * ly + normal[2] * lz;
        dot = Math.max(0, dot);

        double intensity = ambientLight + diffuseIntensity * dot;
        intensity = Math.max(0.2, Math.min(1.0, intensity));

        return color.deriveColor(0, 1.0, intensity, 1.0);
    }

    /**
     * Рендеринг каркаса
     */
    private void renderWireframe(List<Model3D> models, Matrix4x4 viewMatrix, Matrix4x4 projectionMatrix) {
        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            double tx = model.translateXProperty().get();
            double ty = model.translateYProperty().get();
            double tz = model.translateZProperty().get();
            double rx = Math.toRadians(model.rotateXProperty().get());
            double ry = Math.toRadians(model.rotateYProperty().get());
            double rz = Math.toRadians(model.rotateZProperty().get());
            double sx = model.scaleXProperty().get();
            double sy = model.scaleYProperty().get();
            double sz = model.scaleZProperty().get();

            for (Polygon polygon : model.getPolygons()) {
                List<Integer> indices = polygon.getVertexIndices();
                if (indices.size() < 2) continue;

                for (int i = 0; i < indices.size(); i++) {
                    int nextIndex = (i + 1) % indices.size();
                    Vector3D v1 = model.getVertices().get(indices.get(i));
                    Vector3D v2 = model.getVertices().get(indices.get(nextIndex));

                    double[] world1 = transformVertex(v1, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                    double[] world2 = transformVertex(v2, tx, ty, tz, rx, ry, rz, sx, sy, sz);

                    double[] screen1 = projectWithCamera(world1, viewMatrix, projectionMatrix);
                    double[] screen2 = projectWithCamera(world2, viewMatrix, projectionMatrix);

                    drawLine(screen1, screen2, wireframeColor);
                }
            }
        }
    }

    /**
     * Рендеринг вершин
     */
    private void renderVertices(List<Model3D> models, Matrix4x4 viewMatrix, Matrix4x4 projectionMatrix) {
        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            double tx = model.translateXProperty().get();
            double ty = model.translateYProperty().get();
            double tz = model.translateZProperty().get();
            double rx = Math.toRadians(model.rotateXProperty().get());
            double ry = Math.toRadians(model.rotateYProperty().get());
            double rz = Math.toRadians(model.rotateZProperty().get());
            double sx = model.scaleXProperty().get();
            double sy = model.scaleYProperty().get();
            double sz = model.scaleZProperty().get();

            for (Vector3D vertex : model.getVertices()) {
                double[] world = transformVertex(vertex, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                double[] screen = projectWithCamera(world, viewMatrix, projectionMatrix);

                int x = (int) Math.round(screen[0]);
                int y = (int) Math.round(screen[1]);

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int size = 2;
                    for (int dx = -size; dx <= size; dx++) {
                        for (int dy = -size; dy <= size; dy++) {
                            int px = x + dx;
                            int py = y + dy;
                            if (px >= 0 && px < width && py >= 0 && py < height) {
                                pixelWriter.setColor(px, py, vertexColor);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Рисует линию (без Z-буфера, для wireframe/вершин)
     */
    private void drawLine(double[] p1, double[] p2, Color color) {
        int x1 = (int) Math.round(p1[0]);
        int y1 = (int) Math.round(p1[1]);
        int x2 = (int) Math.round(p2[0]);
        int y2 = (int) Math.round(p2[1]);

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height) {
                pixelWriter.setColor(x1, y1, color);
            }
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    /**
     * Проекция с камерой
     */
    private double[] projectWithCamera(double[] worldPos, Matrix4x4 viewMatrix, Matrix4x4 projectionMatrix) {
        Vector4D world = new Vector4D((float)worldPos[0], (float)worldPos[1], (float)worldPos[2], 1.0f);
        Vector4D view = viewMatrix.multiply(world);
        Vector4D clip = projectionMatrix.multiply(view);

        if (Math.abs(clip.getW()) < 1e-6) {
            return new double[]{0, 0, 0};
        }

        double ndcX = clip.getX() / clip.getW();
        double ndcY = clip.getY() / clip.getW();
        double screenX = (ndcX + 1) * 0.5 * width;
        double screenY = (1 - ndcY) * 0.5 * height;
        return new double[]{screenX, screenY, clip.getZ() / clip.getW()};
    }

    public void handleMouseDragged(double x, double y, double lastX, double lastY) {
        float deltaX = (float)(x - lastX);
        float deltaY = (float)(y - lastY);
        cameraInputAdapter.onMouseDragged(deltaX, deltaY);
    }

    public void handleKeyPress(KeyCode key) {
        cameraInputAdapter.onKeyPressed(key);
    }

    public void resetCamera() {
        camera.setPosition(new Vector3D(0, 0, 5));
        camera.setTarget(new Vector3D(0, 0, 0));
    }

    public double getAmbientLight() {
        return ambientLight;
    }

    public double getDiffuseIntensity() {
        return diffuseIntensity;
    }
}