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
import math.ModelTransform;
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
    private boolean useTexture = false;
    private boolean useLighting = false;

    private TextureManager textureManager = TextureManager.getInstance();
    private Image currentTexture = null;

    private double ambientLight = 0.3;
    private double diffuseIntensity = 0.7;
    private double[] lightDirection = normalize(new double[]{0, -0.7, -0.7});

    private Color backgroundColor = Color.rgb(30, 30, 46);

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
        this.width = 800;
        this.height = 600;
        initZBuffer();

        buffer = new WritableImage(width, height);
        pixelWriter = buffer.getPixelWriter();
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
    }

    public void setRenderWireframe(boolean renderWireframe) {
        this.renderWireframe = renderWireframe;
    }

    public void setUseTexture(boolean useTexture) {
        this.useTexture = useTexture;
    }

    public void setUseLighting(boolean useLighting) {
        this.useLighting = useLighting;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    /**
     * Очистка экрана и Z-буфера
     */
    public void clear() {
        if (gc != null) {
            gc.setFill(backgroundColor);
            gc.fillRect(0, 0, width, height);
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

        if (gc != null) {
            gc.drawImage(buffer, 0, 0);
        }
    }

    /**
     * Преобразование вершины с учетом трансформаций модели
     */
    private double[] transformVertex(Vector3D v, double tx, double ty, double tz,
                                     double rx, double ry, double rz,
                                     double sx, double sy, double sz) {

        ModelTransform transform = new ModelTransform();
        transform.setTranslation((float) tx, (float) ty, (float) tz);
        transform.setRotationDeg((float) rx, (float) ry, (float) rz);
        transform.setScale((float) sx, (float) sy, (float) sz);

        Vector3D transformed = transform.transformVertex(v);

        return new double[]{
            transformed.getX(),
            transformed.getY(),
            transformed.getZ()
        };
    }

    /**
     * Проекция 3D точки на 2D экран
     */
    private double[] projectToScreen(double[] point,
                                     double camX, double camY, double camZ,
                                     double fov, double aspectRatio) {
        double x = point[0] - camX;
        double y = point[1] - camY;
        double z = point[2] - camZ;

        double scale = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
        double projectedX = (x * scale) / z;
        double projectedY = (y * scale) / z;

        projectedX /= aspectRatio;

        double screenX = (projectedX + 1) * 0.5 * width;
        double screenY = (1 - projectedY) * 0.5 * height;

        return new double[]{screenX, screenY, z};
    }

    /**
     * Рендеринг одного треугольника
     */
    /**
     * Рендеринг одного треугольника
     */
    private void renderTriangle(double[] p1, double[] p2, double[] p3,
                                Model3D model, Polygon polygon, boolean textureReady) {

        Vector3D faceNormal = polygon.getNormal();
        if (faceNormal == null) return;

        List<Integer> indices = polygon.getVertexIndices();
        if (indices.size() < 3) return;

        Vector3D v1 = model.getVertices().get(indices.get(0));
        Vector3D v2 = model.getVertices().get(indices.get(1));
        Vector3D v3 = model.getVertices().get(indices.get(2));

        Vector3D center = new Vector3D(
                (v1.getX() + v2.getX() + v3.getX()) / 3.0f,
                (v1.getY() + v2.getY() + v3.getY()) / 3.0f,
                (v1.getZ() + v2.getZ() + v3.getZ()) / 3.0f
        );

        Vector3D viewDir = camera.getTarget().subtract(camera.getPosition()).normalize();

        double dot = faceNormal.dot(viewDir);
        if (dot <= 0) {
            return;
        }

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
                double w1 = edgeFunction( x2, y2, x3, y3, x, y) / area;
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
                                    faceNormal.getX(),
                                    faceNormal.getY(),
                                    faceNormal.getZ()
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
     * Вычисляет значение функции ребра (edge function) для барицентрических координат.
     * Используется для определения, находится ли точка (px, py) внутри треугольника.
     */
    private double edgeFunction(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    /**
     * Вычисление цвета пикселя
     */
    private Color calculatePixelColor(Model3D model, double u, double v, double[] normal) {
        Color baseColor = model.getBaseColor();

        boolean hasTexture = useTexture && model.getTexture() != null && !model.getTextureCoords().isEmpty();

        if (hasTexture) {
            baseColor = textureManager.getTextureColor(model.getTexture(), u, v);
        }

        if (useLighting && normal != null) {
            System.out.println("Применяется освещение!"); // ← ДОБАВЬ ЭТО
            baseColor = applyLightingToColor(baseColor, normal);
        } else {
            System.out.println("Освещение выключено или нет нормали"); // ← И ЭТО
        }

        return baseColor;
    }



    /**
     * Применение освещения к цвету
     */
    private Color applyLightingToColor(Color color, double[] normal) {
        if (normal == null || !useLighting) return color;

        double nx = normal[0], ny = normal[1], nz = normal[2];
        double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 0) {
            nx /= len; ny /= len; nz /= len;
        }

        Vector3D lightDir = camera.getTarget().subtract(camera.getPosition()).normalize();
        double lx = lightDir.getX();
        double ly = lightDir.getY();
        double lz = lightDir.getZ();

        double dot = nx * lx + ny * ly + nz * lz;
        dot = Math.max(0, dot);

        double intensity = ambientLight + diffuseIntensity * dot;
        intensity = Math.max(0.1, Math.min(1.0, intensity));

        return color.deriveColor(0, 1.0, intensity, 1.0);
    }

    /**
     * Рендеринг каркаса (только ребра) с учетом Z-буфера
     */
    private void renderWireframe(List<Model3D> models) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(1);

        double cameraX = 0;
        double cameraY = 0;
        double cameraZ = 5;
        double fov = 60;
        double aspectRatio = (double) width / height;

        Map<Vector3D, double[]> vertexProjections = new HashMap<>();

        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            vertexProjections.clear();

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

                double[] p1 = getVertexProjection(v1, vertexProjections,
                        tx, ty, tz, rx, ry, rz, sx, sy, sz,
                        cameraX, cameraY, cameraZ, fov, aspectRatio);
                double[] p2 = getVertexProjection(v2, vertexProjections,
                        tx, ty, tz, rx, ry, rz, sx, sy, sz,
                        cameraX, cameraY, cameraZ, fov, aspectRatio);
                double[] p3 = getVertexProjection(v3, vertexProjections,
                        tx, ty, tz, rx, ry, rz, sx, sy, sz,
                        cameraX, cameraY, cameraZ, fov, aspectRatio);

                drawLineWithZBuffer(p1, p2, Color.RED);
                drawLineWithZBuffer(p2, p3, Color.RED);
                drawLineWithZBuffer(p3, p1, Color.RED);
            }
        }
    }

    /**
     * Получает проекцию вершины из кэша или вычисляет новую
     */
    private double[] getVertexProjection(Vector3D vertex,
                                         Map<Vector3D, double[]> cache,
                                         double tx, double ty, double tz,
                                         double rx, double ry, double rz,
                                         double sx, double sy, double sz,
                                         double camX, double camY, double camZ,
                                         double fov, double aspectRatio) {

        if (cache.containsKey(vertex)) {
            return cache.get(vertex);
        }

        double[] transformed = transformVertex(vertex, tx, ty, tz, rx, ry, rz, sx, sy, sz);
        double[] projected = projectToScreen(transformed, camX, camY, camZ, fov, aspectRatio);

        cache.put(vertex, projected);

        return projected;
    }

    /**
     * Рисует линию с учетом Z-буфера
     */
    private void drawLineWithZBuffer(double[] p1, double[] p2, Color color) {
        double x1 = p1[0], y1 = p1[1], z1 = p1[2];
        double x2 = p2[0], y2 = p2[1], z2 = p2[2];

        int x1i = (int) Math.round(x1);
        int y1i = (int) Math.round(y1);
        int x2i = (int) Math.round(x2);
        int y2i = (int) Math.round(y2);

        if ((x1i < 0 && x2i < 0) || (x1i >= width && x2i >= width) ||
                (y1i < 0 && y2i < 0) || (y1i >= height && y2i >= height)) {
            return;
        }

        int dx = Math.abs(x2i - x1i);
        int dy = Math.abs(y2i - y1i);
        int sx = x1i < x2i ? 1 : -1;
        int sy = y1i < y2i ? 1 : -1;
        int err = dx - dy;

        double dz = z2 - z1;
        double steps = Math.max(dx, dy);
        double zStep = steps > 0 ? dz / steps : 0;
        double currentZ = z1;

        int x = x1i;
        int y = y1i;

        while (true) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                if (currentZ < zBuffer[x][y] + 0.001) {
                    gc.setFill(color);
                    gc.fillRect(x, y, 1, 1);
                }
            }

            if (x == x2i && y == y2i) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
                if (dx > 0) {
                    currentZ += (zStep * Math.abs(sx)) / dx;
                }
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
                if (dy > 0) {
                    currentZ += (zStep * Math.abs(sy)) / dy;
                }
            }
        }
    }

    /**
     * Нормализация вектора
     */
    private double[] normalize(double[] v) {
        double length = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if (length > 0) {
            return new double[]{v[0]/length, v[1]/length, v[2]/length};
        }
        return v;
    }
    public void setCurrentTexture(Image texture) {
        this.currentTexture = texture;
    }

    public void setAmbientLight(double ambient) {
        this.ambientLight = Math.max(0, Math.min(1, ambient));
    }

    public void setDiffuseIntensity(double diffuse) {
        this.diffuseIntensity = Math.max(0, Math.min(1, diffuse));
    }

    public void setLightDirection(double x, double y, double z) {
        this.lightDirection = normalize(new double[]{x, y, z});
    }

    public double getAmbientLight() {
        return ambientLight;
    }

    public double getDiffuseIntensity() {
        return diffuseIntensity;
    }

    public boolean isRenderWireframe() {
        return renderWireframe;
    }

    public boolean isUseTexture() {
        return useTexture;
    }

    public boolean isUseLighting() {
        return useLighting;
    }

    public double[] getLightDirection() {
        return lightDirection;
    }



    /**
     * Отрисовка нормалей для отладки
     */
    private void renderNormalsForDebug(Model3D model,
                                       double tx, double ty, double tz,
                                       double rx, double ry, double rz,
                                       double sx, double sy, double sz) {

        gc.setStroke(Color.GREEN);
        gc.setLineWidth(1);

        for (Polygon polygon : model.getPolygons()) {
            List<Integer> indices = polygon.getVertexIndices();
            if (indices.size() != 3) continue;

            Vector3D v1 = model.getVertices().get(indices.get(0));
            Vector3D v2 = model.getVertices().get(indices.get(1));
            Vector3D v3 = model.getVertices().get(indices.get(2));

            double centerX = (v1.getX() + v2.getX() + v3.getX()) / 3;
            double centerY = (v1.getY() + v2.getY() + v3.getY()) / 3;
            double centerZ = (v1.getZ() + v2.getZ() + v3.getZ()) / 3;

            double[] transformedCenter = transformVertex(
                    new Vector3D((float) centerX, (float) centerY, (float) centerZ),
                    tx, ty, tz, rx, ry, rz, sx, sy, sz
            );

            Vector3D normal = polygon.getNormal();
            if (normal != null) {
                double endX = centerX + normal.getX() * 0.5;
                double endY = centerY + normal.getY() * 0.5;
                double endZ = centerZ + normal.getZ() * 0.5;

                double[] transformedEnd = transformVertex(
                        new Vector3D((float) endX, (float) endY, (float) endZ),
                        tx, ty, tz, rx, ry, rz, sx, sy, sz
                );

                double[] screenStart = projectToScreen(transformedCenter, 0, 0, 5, 60, (double)width/height);
                double[] screenEnd = projectToScreen(transformedEnd, 0, 0, 5, 60, (double)width/height);

                gc.strokeLine(screenStart[0], screenStart[1], screenEnd[0], screenEnd[1]);
            }
        }
    }

    /**
     * Обработка событий мыши
     */
    public void handleMouseDragged(double x, double y, double lastX, double lastY) {
        float deltaX = (float)(x - lastX);
        float deltaY = (float)(y - lastY);
        cameraInputAdapter.onMouseDragged(deltaX, deltaY);
    }

    public void handleKeyPress(KeyCode key) {
        cameraInputAdapter.onKeyPressed(key);
    }

    private double[] projectWithCamera(double[] worldPos, Matrix4x4 viewMatrix, Matrix4x4 projectionMatrix) {
        Vector4D world = new Vector4D(
                (float)worldPos[0],
                (float)worldPos[1],
                (float)worldPos[2],
                1.0f
        );

        Vector4D view = viewMatrix.multiply(world);
        Vector4D clip = projectionMatrix.multiply(view);

        if (Math.abs(clip.getW()) < 1e-6) {
            return new double[]{0, 0, 0};
        }

        double ndcX = clip.getX() / clip.getW();
        double ndcY = clip.getY() / clip.getW();
        double ndcZ = clip.getZ() / clip.getW();

        double screenX = (ndcX + 1) * 0.5 * width;
        double screenY = (1 - ndcY) * 0.5 * height;

        return new double[]{screenX, screenY, ndcZ};
    }
}