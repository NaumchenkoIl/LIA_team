package scene_master.renderer;

import javafx.scene.image.Image;
import scene_master.model.Model3D;
import scene_master.model.Polygon;
import scene_master.model.Vector3D;
import scene_master.model.Vertex;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.*;

public class SoftwareRenderer {
    private Canvas canvas;
    private GraphicsContext gc;
    private int width;
    private int height;

    // Z-буфер (глубина каждого пикселя)
    private double[][] zBuffer;

    // Флаги режимов рендеринга
    private boolean renderWireframe = false;
    private boolean useTexture = false;
    private boolean useLighting = false;

    private TextureManager textureManager = TextureManager.getInstance();
    private Image currentTexture = null;

    // Параметры освещения
    private double ambientLight = 0.3;
    private double diffuseIntensity = 0.7;
    private double[] lightDirection = normalize(new double[]{0, -0.7, -0.7});
    // Цвет фона
    private Color backgroundColor = Color.BLACK;

    private int debugTriangleCount = 0;

    public SoftwareRenderer(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.width = (int) canvas.getWidth();
        this.height = (int) canvas.getHeight();
        initZBuffer();
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
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);
        clearZBuffer();
    }

    /**
     * Рендеринг сцены со всеми моделями
     */
    public void renderScene(List<Model3D> models) {
        clear();

        // Сбрасываем счетчик отладки
        debugTriangleCount = 0;

        // Простая камера (орфографическая проекция)
        double cameraX = 0;
        double cameraY = 0;
        double cameraZ = 5;
        double fov = 60; // поле зрения
        double aspectRatio = (double) width / height;

        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            // Применяем трансформации модели
            double tx = model.translateXProperty().get();
            double ty = model.translateYProperty().get();
            double tz = model.translateZProperty().get();
            double rx = Math.toRadians(model.rotateXProperty().get());
            double ry = Math.toRadians(model.rotateYProperty().get());
            double rz = Math.toRadians(model.rotateZProperty().get());
            double sx = model.scaleXProperty().get();
            double sy = model.scaleYProperty().get();
            double sz = model.scaleZProperty().get();

            // Рендерим каждый полигон (треугольник)
            for (Polygon polygon : model.getPolygons()) {
                List<Integer> indices = polygon.getVertexIndices();
                if (indices.size() != 3) {
                    System.err.println("Предупреждение: полигон не треугольник (" + indices.size() + " вершин)");
                    continue; // Пропускаем не треугольники
                }

                Vertex v1 = model.getVertices().get(indices.get(0));
                Vertex v2 = model.getVertices().get(indices.get(1));
                Vertex v3 = model.getVertices().get(indices.get(2));

                // Преобразуем вершины
                double[] p1 = transformVertex(v1, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                double[] p2 = transformVertex(v2, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                double[] p3 = transformVertex(v3, tx, ty, tz, rx, ry, rz, sx, sy, sz);

                // Проекция на экран
                double[] screen1 = projectToScreen(p1, cameraX, cameraY, cameraZ, fov, aspectRatio);
                double[] screen2 = projectToScreen(p2, cameraX, cameraY, cameraZ, fov, aspectRatio);
                double[] screen3 = projectToScreen(p3, cameraX, cameraY, cameraZ, fov, aspectRatio);

                // Рендерим треугольник
                renderTriangle(screen1, screen2, screen3, model, polygon);
            }
        }

        // Если нужно рисовать каркас поверх
        if (renderWireframe) {
            renderWireframe(models);
        }
    }

    /**
     * Преобразование вершины с учетом трансформаций модели
     */
    // В SoftwareRenderer.java, метод transformVertex() должен применяться корректно:
    private double[] transformVertex(Vertex v, double tx, double ty, double tz,
                                     double rx, double ry, double rz,
                                     double sx, double sy, double sz) {
        // Преобразуем градусы в радианы
        rx = Math.toRadians(rx);
        ry = Math.toRadians(ry);
        rz = Math.toRadians(rz);

        // Масштабирование
        double x = v.x * sx;
        double y = v.y * sy;
        double z = v.z * sz;

        // Вращение вокруг X
        double y1 = y * Math.cos(rx) - z * Math.sin(rx);
        double z1 = y * Math.sin(rx) + z * Math.cos(rx);
        y = y1; z = z1;

        // Вращение вокруг Y
        double x2 = x * Math.cos(ry) + z * Math.sin(ry);
        double z2 = -x * Math.sin(ry) + z * Math.cos(ry);
        x = x2; z = z2;

        // Вращение вокруг Z
        double x3 = x * Math.cos(rz) - y * Math.sin(rz);
        double y3 = x * Math.sin(rz) + y * Math.cos(rz);
        x = x3; y = y3;

        // Перенос
        x += tx;
        y += ty;
        z += tz;

        return new double[]{x, y, z};
    }

    /**
     * Проекция 3D точки на 2D экран
     */
    private double[] projectToScreen(double[] point,
                                     double camX, double camY, double camZ,
                                     double fov, double aspectRatio) {
        // Переводим в систему координат камеры
        double x = point[0] - camX;
        double y = point[1] - camY;
        double z = point[2] - camZ;

        // Перспективная проекция
        double scale = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
        double projectedX = (x * scale) / z;
        double projectedY = (y * scale) / z;

        // Учитываем соотношение сторон
        projectedX /= aspectRatio;

        // Преобразуем в координаты экрана
        double screenX = (projectedX + 1) * 0.5 * width;
        double screenY = (1 - projectedY) * 0.5 * height; // инвертируем Y

        return new double[]{screenX, screenY, z}; // возвращаем также глубину
    }
    /**
     * Рендеринг одного треугольника
     */
    private void renderTriangle(double[] p1, double[] p2, double[] p3,
                                Model3D model, Polygon polygon) {

        // Получаем координаты вершин
        double x1 = p1[0], y1 = p1[1], z1 = p1[2];
        double x2 = p2[0], y2 = p2[1], z2 = p2[2];
        double x3 = p3[0], y3 = p3[1], z3 = p3[2];

        // Находим ограничивающий прямоугольник треугольника
        int minX = (int) Math.max(0, Math.min(Math.min(x1, x2), x3));
        int maxX = (int) Math.min(width - 1, Math.max(Math.max(x1, x2), x3));
        int minY = (int) Math.max(0, Math.min(Math.min(y1, y2), y3));
        int maxY = (int) Math.min(height - 1, Math.max(Math.max(y1, y2), y3));

        // Если треугольник полностью вне экрана
        if (minX >= maxX || minY >= maxY) return;

        // Предвычисляем константы для алгоритма барицентрических координат
        double area = edgeFunction(x1, y1, x2, y2, x3, y3);
        if (Math.abs(area) < 0.0001) return;

        // Получаем UV-координаты ДО цикла
        double[] uv1 = model.getTextureCoordsForPolygonVertex(polygon, 0);
        double[] uv2 = model.getTextureCoordsForPolygonVertex(polygon, 1);
        double[] uv3 = model.getTextureCoordsForPolygonVertex(polygon, 2);

        // Отладка
        if (debugTriangleCount++ < 3) {
            System.out.println("=== Треугольник " + debugTriangleCount + " ===");
            System.out.println("Модель: " + model.getName());
            System.out.println("Текстура: " + (model.getTexture() != null ? "Есть" : "Нет"));
            System.out.println("Режим текстуры: " + useTexture);
            System.out.println("UV1: [" + String.format("%.3f", uv1[0]) + ", " + String.format("%.3f", uv1[1]) + "]");
            System.out.println("UV2: [" + String.format("%.3f", uv2[0]) + ", " + String.format("%.3f", uv2[1]) + "]");
            System.out.println("UV3: [" + String.format("%.3f", uv3[0]) + ", " + String.format("%.3f", uv3[1]) + "]");

            List<Integer> texIndices = polygon.getTextureIndices();
            System.out.println("UV-индексов в полигоне: " +
                    (texIndices != null ? texIndices.size() : 0));
            System.out.println("Всего UV-координат в модели: " + model.getTextureCoords().size());
        }


        // Проходим по всем пикселям в ограничивающем прямоугольнике
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                // Барицентрические координаты
                double w1 = edgeFunction(x2, y2, x3, y3, x, y) / area;
                double w2 = edgeFunction(x3, y3, x1, y1, x, y) / area;
                double w3 = edgeFunction(x1, y1, x2, y2, x, y) / area;

                // Если точка внутри треугольника (с небольшим допуском)
                if (w1 >= -0.0001 && w2 >= -0.0001 && w3 >= -0.0001) {
                    // Интерполяция глубины
                    double depth = w1 * z1 + w2 * z2 + w3 * z3;

                    // Проверка Z-буфера
                    if (depth < zBuffer[x][y]) {
                        zBuffer[x][y] = depth;

                        // Интерполяция UV-координат
                        double u = w1 * uv1[0] + w2 * uv2[0] + w3 * uv3[0];
                        double v = w1 * uv1[1] + w2 * uv2[1] + w3 * uv3[1];

                        // === ИСПРАВЛЕНИЕ: правильно объявляем и инициализируем normal ===
                        double[] interpolatedNormal = null;

                        // Попытка использовать vertex normals (если они есть)
                        if (model instanceof Model3D) {
                            Model3D model3d = (Model3D) model;
                            List<Vector3D> vertexNormals = model3d.getVertexNormals();

                            if (vertexNormals != null && vertexNormals.size() == model.getVertices().size()) {
                                List<Integer> vertexIndices = polygon.getVertexIndices();
                                if (vertexIndices.size() >= 3) {
                                    Vector3D n1 = vertexNormals.get(vertexIndices.get(0));
                                    Vector3D n2 = vertexNormals.get(vertexIndices.get(1));
                                    Vector3D n3 = vertexNormals.get(vertexIndices.get(2));

                                    interpolatedNormal = new double[]{
                                            w1 * n1.getX() + w2 * n2.getX() + w3 * n3.getX(),
                                            w1 * n1.getY() + w2 * n2.getY() + w3 * n3.getY(),
                                            w1 * n1.getZ() + w2 * n2.getZ() + w3 * n3.getZ()
                                    };
                                }
                            }
                        }

                        // Если vertex normals недоступны — используем face normal
                        if (interpolatedNormal == null && polygon.getNormal() != null) {
                            Vector3D faceNormal = polygon.getNormal();
                            interpolatedNormal = new double[]{
                                    faceNormal.getX(),
                                    faceNormal.getY(),
                                    faceNormal.getZ()
                            };
                        }

                        // Вычисляем цвет пикселя
                        Color pixelColor = calculatePixelColor(model, u, v, interpolatedNormal);

                        // Рисуем пиксель
                        gc.setFill(pixelColor);
                        gc.fillRect(x, y, 1, 1);
                    }
                }
            }
        }
    }

    /**
     * Вычисление функции ребра для барицентрических координат
     */
    private double edgeFunction(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    /**
     * Вычисление цвета пикселя
     */
    private Color calculatePixelColor(Model3D model, double u, double v, double[] normal) {
        Color baseColor = model.getBaseColor();

        // Используем текстуру ТОЛЬКО если она загружена И есть UV-координаты у модели
        boolean hasTexture = useTexture && model.getTexture() != null && !model.getTextureCoords().isEmpty();

        if (hasTexture) {
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

        // 1. Нормализуем
        double length = Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (length > 0) {
            normal[0] /= length;
            normal[1] /= length;
            normal[2] /= length;
        }

        // 2. УБЕРИ ВСЮ ИНВЕРСИЮ! Нормаль уже правильная
        // НИЧЕГО не делай с нормалью здесь!

        // 3. Направление света (свет падает сверху-спереди)
        double[] lightDir = normalize(new double[]{0, -0.5, -1});

        // 4. Dot product
        double dot = normal[0] * lightDir[0] +
                normal[1] * lightDir[1] +
                normal[2] * lightDir[2];

        // 5. Dot должен быть от 0 до 1
        dot = Math.max(0, dot); // Задние грани = 0

        // 6. Интенсивность
        double intensity = ambientLight + diffuseIntensity * dot;
        intensity = Math.max(0.2, Math.min(1.0, intensity)); // Минимум 20% света

        return color.deriveColor(0, 1.0, intensity, 1.0);
    }

    /**
     * Рендеринг каркаса (только ребра) с учетом Z-буфера
     */
    private void renderWireframe(List<Model3D> models) {
        // Устанавливаем цвет и толщину линий каркаса
        gc.setStroke(Color.RED);
        gc.setLineWidth(1);

        // Простая камера (такая же как в основном рендерере)
        double cameraX = 0;
        double cameraY = 0;
        double cameraZ = 5;
        double fov = 60;
        double aspectRatio = (double) width / height;

        // Временный массив для хранения проекций вершин для текущей модели
        Map<Vertex, double[]> vertexProjections = new HashMap<>();

        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            // Очищаем кэш проекций для каждой модели
            vertexProjections.clear();

            // Получаем трансформации модели
            double tx = model.translateXProperty().get();
            double ty = model.translateYProperty().get();
            double tz = model.translateZProperty().get();
            double rx = Math.toRadians(model.rotateXProperty().get());
            double ry = Math.toRadians(model.rotateYProperty().get());
            double rz = Math.toRadians(model.rotateZProperty().get());
            double sx = model.scaleXProperty().get();
            double sy = model.scaleYProperty().get();
            double sz = model.scaleZProperty().get();

            // Рендерим каждый полигон (треугольник)
            for (Polygon polygon : model.getPolygons()) {
                List<Integer> indices = polygon.getVertexIndices();
                if (indices.size() != 3) continue;

                // Получаем вершины
                Vertex v1 = model.getVertices().get(indices.get(0));
                Vertex v2 = model.getVertices().get(indices.get(1));
                Vertex v3 = model.getVertices().get(indices.get(2));

                // Получаем или вычисляем проекции вершин
                double[] p1 = getVertexProjection(v1, vertexProjections,
                        tx, ty, tz, rx, ry, rz, sx, sy, sz,
                        cameraX, cameraY, cameraZ, fov, aspectRatio);
                double[] p2 = getVertexProjection(v2, vertexProjections,
                        tx, ty, tz, rx, ry, rz, sx, sy, sz,
                        cameraX, cameraY, cameraZ, fov, aspectRatio);
                double[] p3 = getVertexProjection(v3, vertexProjections,
                        tx, ty, tz, rx, ry, rz, sx, sy, sz,
                        cameraX, cameraY, cameraZ, fov, aspectRatio);

                // Рисуем три ребра треугольника
                drawLineWithZBuffer(p1, p2, Color.RED);
                drawLineWithZBuffer(p2, p3, Color.RED);
                drawLineWithZBuffer(p3, p1, Color.RED);
            }
        }
    }

    /**
     * Получает проекцию вершины из кэша или вычисляет новую
     */
    private double[] getVertexProjection(Vertex vertex,
                                         Map<Vertex, double[]> cache,
                                         double tx, double ty, double tz,
                                         double rx, double ry, double rz,
                                         double sx, double sy, double sz,
                                         double camX, double camY, double camZ,
                                         double fov, double aspectRatio) {

        // Проверяем, есть ли уже проекция в кэше
        if (cache.containsKey(vertex)) {
            return cache.get(vertex);
        }

        // Вычисляем новую проекцию
        double[] transformed = transformVertex(vertex, tx, ty, tz, rx, ry, rz, sx, sy, sz);
        double[] projected = projectToScreen(transformed, camX, camY, camZ, fov, aspectRatio);

        // Сохраняем в кэш
        cache.put(vertex, projected);

        return projected;
    }

    /**
     * Рисует линию с учетом Z-буфера
     */
    private void drawLineWithZBuffer(double[] p1, double[] p2, Color color) {
        double x1 = p1[0], y1 = p1[1], z1 = p1[2];
        double x2 = p2[0], y2 = p2[1], z2 = p2[2];

        // Алгоритм Брезенхема для рисования линий с Z-буфером
        int x1i = (int) Math.round(x1);
        int y1i = (int) Math.round(y1);
        int x2i = (int) Math.round(x2);
        int y2i = (int) Math.round(y2);

        // Проверяем, находится ли линия в пределах экрана
        if ((x1i < 0 && x2i < 0) || (x1i >= width && x2i >= width) ||
                (y1i < 0 && y2i < 0) || (y1i >= height && y2i >= height)) {
            return;
        }

        int dx = Math.abs(x2i - x1i);
        int dy = Math.abs(y2i - y1i);
        int sx = x1i < x2i ? 1 : -1;
        int sy = y1i < y2i ? 1 : -1;
        int err = dx - dy;

        // Интерполяция глубины
        double dz = z2 - z1;
        double steps = Math.max(dx, dy);
        double zStep = steps > 0 ? dz / steps : 0;
        double currentZ = z1;

        int x = x1i;
        int y = y1i;

        while (true) {
            // Проверяем границы экрана
            if (x >= 0 && x < width && y >= 0 && y < height) {
                // Проверка Z-буфера с некоторым допуском для линий
                if (currentZ < zBuffer[x][y] + 0.001) {
                    // Рисуем пиксель
                    gc.setFill(color);
                    gc.fillRect(x, y, 1, 1);
                }
            }

            if (x == x2i && y == y2i) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
                // Интерполируем Z при движении по X
                if (dx > 0) {
                    currentZ += (zStep * Math.abs(sx)) / dx;
                }
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
                // Интерполируем Z при движении по Y
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


    // В класс SoftwareRenderer добавьте эти методы:
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

            // Центр треугольника
            Vertex v1 = model.getVertices().get(indices.get(0));
            Vertex v2 = model.getVertices().get(indices.get(1));
            Vertex v3 = model.getVertices().get(indices.get(2));

            double centerX = (v1.x + v2.x + v3.x) / 3;
            double centerY = (v1.y + v2.y + v3.y) / 3;
            double centerZ = (v1.z + v2.z + v3.z) / 3;

            // Преобразуем центр
            double[] transformedCenter = transformVertex(
                    new Vertex(centerX, centerY, centerZ),
                    tx, ty, tz, rx, ry, rz, sx, sy, sz
            );

            // Нормаль
            Vector3D normal = polygon.getNormal();
            if (normal != null) {
                // Конец нормали
                double endX = centerX + normal.getX() * 0.5;
                double endY = centerY + normal.getY() * 0.5;
                double endZ = centerZ + normal.getZ() * 0.5;

                double[] transformedEnd = transformVertex(
                        new Vertex(endX, endY, endZ),
                        tx, ty, tz, rx, ry, rz, sx, sy, sz
                );

                // Проекция на экран
                double[] screenStart = projectToScreen(transformedCenter, 0, 0, 5, 60, (double)width/height);
                double[] screenEnd = projectToScreen(transformedEnd, 0, 0, 5, 60, (double)width/height);

                // Рисуем линию
                gc.strokeLine(screenStart[0], screenStart[1], screenEnd[0], screenEnd[1]);
            }
        }
    }

}