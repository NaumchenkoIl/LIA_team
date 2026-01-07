package scene_master.renderer;

import javafx.scene.image.Image;
import scene_master.model.Model3D;
import scene_master.model.Polygon;
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
    private double[] lightDirection = normalize(new double[]{0.5, -0.5, -1});


    // Цвет фона
    private Color backgroundColor = Color.BLACK;

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

        // Простая камера (орфографическая проекция)
        double cameraX = 0;
        double cameraY = 0;
        double cameraZ = 5;
        double fov = 60; // поле зрения
        double aspectRatio = (double) width / height;

        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            // Устанавливаем текущую текстуру модели
            setCurrentTexture(model.getTexture());

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
                if (indices.size() != 3) continue; // Пропускаем не треугольники

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

                // Получаем UV-координаты для вершин
                double[] uv1 = model.getTextureCoordsForPolygonVertex(polygon, 0);
                double[] uv2 = model.getTextureCoordsForPolygonVertex(polygon, 1);
                double[] uv3 = model.getTextureCoordsForPolygonVertex(polygon, 2);

                // Рендерим треугольник
                renderTriangle(screen1, screen2, screen3, model, polygon, uv1, uv2, uv3);
            }

            // Если нужно рисовать каркас поверх
            if (renderWireframe) {
                renderWireframe(models);
            }
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
                                Model3D model, Polygon polygon,
                                double[] uv1, double[] uv2, double[] uv3) {
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
        if (Math.abs(area) < 0.0001) return; // вырожденный треугольник

        // Проходим по всем пикселям в ограничивающем прямоугольнике
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                // Барицентрические координаты
                double w1 = edgeFunction(x2, y2, x3, y3, x, y) / area;
                double w2 = edgeFunction(x3, y3, x1, y1, x, y) / area;
                double w3 = edgeFunction(x1, y1, x2, y2, x, y) / area;

                if (w1 >= -0.0001 && w2 >= -0.0001 && w3 >= -0.0001) {
                    // Интерполяция глубины
                    double depth = w1 * z1 + w2 * z2 + w3 * z3;

                    if (depth < zBuffer[x][y]) {
                        zBuffer[x][y] = depth;

                        // Интерполяция UV-координат
                        double u = w1 * uv1[0] + w2 * uv2[0] + w3 * uv3[0];
                        double v = w1 * uv1[1] + w2 * uv2[1] + w3 * uv3[1];

                        // Интерполяция нормали (если есть нормали вершин)
                        double[] normal = null;
                        if (polygon.getNormal() != null) {
                            // Пока используем нормаль полигона
                            normal = new double[]{
                                    polygon.getNormal().getX(),
                                    polygon.getNormal().getY(),
                                    polygon.getNormal().getZ()
                            };
                        }

                        // Вычисляем цвет пикселя
                        Color pixelColor = calculatePixelColor(
                                model, u, v, normal, w1, w2, w3
                        );

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
    private Color calculatePixelColor(double w1, double w2, double w3,
                                      Model3D model, Polygon polygon) {
        Color color = model.getBaseColor();

        // Освещение (если включено)
        if (useLighting && polygon.getNormal() != null) {
            // Интерполируем нормаль (пока используем нормаль полигона)
            double intensity = calculateLightingIntensity(polygon.getNormal());
            color = color.deriveColor(0, 1, intensity, 1);
        }

        // Текстура (если включена и есть)
        if (useTexture && model.getTexture() != null) {
            // TODO: Интерполяция координат текстуры
            // Здесь нужны UV-координаты для каждой вершины
        }

        return color;
    }

    /**
     * Расчет интенсивности освещения
     */
    // В SoftwareRenderer.java исправим calculateLightingIntensity:
    private double calculateLightingIntensity(scene_master.model.Vector3D normal) {
        if (normal == null) return 0.7; // Возвращаем 0.7 если нет нормали

        double nx = normal.getX();
        double ny = normal.getY();
        double nz = normal.getZ();

        // Убедимся что нормаль направлена к камере (для backface culling)
        // Если нормаль направлена от камере (z > 0), инвертируем
        if (nz > 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }

        // Скалярное произведение с направлением света (теперь свет сверху-спереди)
        double[] lightDir = normalize(new double[]{0, -1, -1}); // Свет сверху-спереди
        double dot = nx * lightDir[0] + ny * lightDir[1] + nz * lightDir[2];

        // Яркость от 0.3 (минимальная) до 1.0
        return Math.max(0.3, dot);
    }

    /**
     * Рендеринг каркаса (только ребра)
     */
    private void renderWireframe(List<Model3D> models) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(1);

        for (Model3D model : models) {
            if (!model.isVisible()) continue;

            for (Polygon polygon : model.getPolygons()) {
                List<Integer> indices = polygon.getVertexIndices();
                if (indices.size() != 3) continue;

                // Просто рисуем линии между вершинами
                // TODO: Нужно преобразовать координаты как в основном рендерере
                // Это упрощенная версия для демонстрации
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

    // Новый метод для вычисления цвета с текстурами и освещением:
    private Color calculatePixelColor(Model3D model, double u, double v,
                                      double[] normal, double w1, double w2, double w3) {
        Color baseColor = model.getBaseColor();
        Color finalColor = baseColor;

        // 1. Текстура
        if (useTexture && model.getTexture() != null) {
            Color texColor = textureManager.getTextureColor(model.getTexture(), u, v);

            if (useLighting) {
                // Текстура + освещение
                finalColor = applyLightingToTexture(texColor, normal);
            } else {
                // Только текстура
                finalColor = texColor;
            }
        }
        // 2. Только освещение
        else if (useLighting && normal != null) {
            finalColor = applyLightingToColor(baseColor, normal);
        }
        // 3. Только цвет
        else {
            finalColor = baseColor;
        }

        return finalColor;
    }

    // Применение освещения к цвету
    private Color applyLightingToColor(Color color, double[] normal) {
        if (normal == null) return color;

        // Нормализуем нормаль
        double length = Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (length > 0) {
            normal[0] /= length;
            normal[1] /= length;
            normal[2] /= length;
        }

        // Скалярное произведение с направлением света
        double dot = normal[0] * lightDirection[0] +
                normal[1] * lightDirection[1] +
                normal[2] * lightDirection[2];

        // Ограничиваем от 0 до 1
        dot = Math.max(0, Math.min(1, dot));

        // Итоговая интенсивность: ambient + diffuse
        double intensity = ambientLight + diffuseIntensity * dot;
        intensity = Math.max(0, Math.min(1, intensity));

        // Применяем освещение к цвету
        return color.deriveColor(0, 1.0, intensity, 1.0);
    }

    // Применение освещения к текстуре
    private Color applyLightingToTexture(Color texColor, double[] normal) {
        if (normal == null) return texColor;

        // Нормализуем нормаль
        double length = Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (length > 0) {
            normal[0] /= length;
            normal[1] /= length;
            normal[2] /= length;
        }

        // Скалярное произведение
        double dot = normal[0] * lightDirection[0] +
                normal[1] * lightDirection[1] +
                normal[2] * lightDirection[2];
        dot = Math.max(0, Math.min(1, dot));

        // Итоговая интенсивность
        double intensity = ambientLight + diffuseIntensity * dot;
        intensity = Math.max(0.1, Math.min(1, intensity)); // Минимум 0.1

        // Применяем освещение
        return texColor.deriveColor(0, 1.0, intensity, 1.0);
    }


}