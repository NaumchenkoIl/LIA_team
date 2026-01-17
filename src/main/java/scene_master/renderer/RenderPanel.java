package scene_master.renderer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import math.Camera;
import math.LinealAlgebra.Vector3D;
import math.Matrix.Matrix4x4;
import scene_master.manager.EditManager;
import scene_master.manager.SelectionManager;
import scene_master.model.Model3D;
import scene_master.model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class RenderPanel extends Pane {
    private Canvas canvas;
    private SoftwareRenderer renderer;
    private List<Model3D> models = new ArrayList<>();
    private Camera camera;

    private boolean renderWireframe = false;
    private boolean showVertices = false;
    private boolean useTexture = false;
    private boolean useLighting = false;
    private boolean editModeEnabled = false;

    private double vertexSize = 5.0;
    private Color vertexColor = Color.YELLOW;
    private Color selectedVertexColor = Color.RED;
    private EditManager editManager = new EditManager();
    private SelectionManager selectionManager;

    public RenderPanel(double width, double height, SelectionManager selectionManager, EditManager editManager) {
        this.selectionManager = selectionManager;
        this.editManager = editManager;

        canvas = new Canvas(width, height);
        getChildren().add(canvas);

        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        camera = new Camera(new Vector3D(0, 0, 5), new Vector3D(0, 0, 0));

        renderer = new SoftwareRenderer(canvas, camera);

        setupMouseHandlers();

        setFocusTraversable(true);
        canvas.setOnMouseClicked(e -> requestFocus());
    }

    public void setModels(List<Model3D> models) {
        this.models = models;
        render();
    }

    private void setupMouseHandlers() {
        double[] lastMousePos = {0, 0};

        setOnMousePressed(event -> {
            lastMousePos[0] = event.getX();
            lastMousePos[1] = event.getY();

            if (editModeEnabled) {
                handleVertexSelection(event.getX(), event.getY());
            }
        });

        setOnMouseDragged(event -> {
            renderer.handleMouseDragged(event.getX(), event.getY(), lastMousePos[0], lastMousePos[1]);
            lastMousePos[0] = event.getX();
            lastMousePos[1] = event.getY();
            render();
        });

        setFocusTraversable(true);
        setOnKeyPressed(event -> {
            renderer.handleKeyPress(event.getCode());
            render();
        });
    }

    private void handleVertexSelection(double mouseX, double mouseY) {
        double minDistance = Double.MAX_VALUE;
        int closestVertexIndex = -1;
        Model3D closestModel = null;

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

            for (int i = 0; i < model.getVertices().size(); i++) {
                Vector3D vertex = model.getVertices().get(i);
                double[] world = renderer.transformVertex(vertex, tx, ty, tz, rx, ry, rz, sx, sy, sz);
                Vector3D worldVec = new Vector3D((float)world[0], (float)world[1], (float)world[2]);
                double[] screen = projectVertex(worldVec, model);

                double distance = Math.sqrt(
                        Math.pow(screen[0] - mouseX, 2) +
                                Math.pow(screen[1] - mouseY, 2)
                );

                if (distance < minDistance && distance < 20) {
                    minDistance = distance;
                    closestVertexIndex = i;
                    closestModel = model;
                }
            }
        }

        if (closestVertexIndex != -1 && closestModel != null) {
            editManager.selectVertex(closestVertexIndex);
            render();

            System.out.println("Выбрана вершина #" + closestVertexIndex +
                    " в модели " + closestModel.getName());
        }
    }

    private double[] projectVertex(Vector3D vertex, Model3D model) {
        double tx = model.translateXProperty().get();
        double ty = model.translateYProperty().get();
        double tz = model.translateZProperty().get();
        double rx = Math.toRadians(model.rotateXProperty().get());
        double ry = Math.toRadians(model.rotateYProperty().get());
        double rz = Math.toRadians(model.rotateZProperty().get());
        double sx = model.scaleXProperty().get();
        double sy = model.scaleYProperty().get();
        double sz = model.scaleZProperty().get();

        double[] world = renderer.transformVertex(vertex, tx, ty, tz, rx, ry, rz, sx, sy, sz);

        Matrix4x4 viewMatrix = camera.getViewMatrix();
        Matrix4x4 projectionMatrix = camera.getProjectionMatrix();

        return renderer.projectWithCamera(world, viewMatrix, projectionMatrix);
    }

    public Camera getCamera() {
        return camera;
    }

    public void addModel(Model3D model) {
        models.add(model);
        render();
    }

    public void removeModel(Model3D model) {
        models.remove(model);
        render();
    }

    public void clearModels() {
        models.clear();
        render();
    }

    public void render() {
        renderer.setRenderWireframe(renderWireframe);
        renderer.setUseTexture(useTexture);
        renderer.setUseLighting(useLighting);
        renderer.renderScene(models);

        if (showVertices) {
            renderVertices();
        }
    }

    private void renderVertices() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

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

            for (int i = 0; i < model.getVertices().size(); i++) {
                Vector3D vertex = model.getVertices().get(i);

                double[] world = renderer.transformVertex(vertex, tx, ty, tz, rx, ry, rz, sx, sy, sz);

                Vector3D worldVec = new Vector3D((float)world[0], (float)world[1], (float)world[2]);

                double[] screen = projectVertex(worldVec, model);

                Color currentVertexColor = vertexColor;
                if (editModeEnabled &&
                        editManager.getSelectedVertexIndex() == i &&
                        selectionManager.getActiveModel() == model) {
                    currentVertexColor = selectedVertexColor;
                }

                gc.setFill(currentVertexColor);
                gc.fillOval(screen[0] - vertexSize/2, screen[1] - vertexSize/2,
                        vertexSize, vertexSize);

                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeOval(screen[0] - vertexSize/2, screen[1] - vertexSize/2,
                        vertexSize, vertexSize);

                if (editModeEnabled && vertexSize > 6) {
                    gc.setFill(Color.WHITE);
                    gc.setFont(javafx.scene.text.Font.font(10));
                    gc.fillText(String.valueOf(i),
                            screen[0] - 4, screen[1] + 4);
                }
            }

            if (editModeEnabled) {
                renderPolygonCenters(model, tx, ty, tz, rx, ry, rz, sx, sy, sz);
            }
        }
    }

    private void renderPolygonCenters(Model3D model,
                                      double tx, double ty, double tz,
                                      double rx, double ry, double rz,
                                      double sx, double sy, double sz) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        for (int i = 0; i < model.getPolygons().size(); i++) {
            Polygon polygon = model.getPolygons().get(i);
            List<Integer> indices = polygon.getVertexIndices();

            if (indices.isEmpty()) continue;

            double centerX = 0, centerY = 0, centerZ = 0;
            for (int idx : indices) {
                Vector3D vertex = model.getVertices().get(idx);
                centerX += vertex.getX();
                centerY += vertex.getY();
                centerZ += vertex.getZ();
            }

            centerX /= indices.size();
            centerY /= indices.size();
            centerZ /= indices.size();

            Vector3D center = new Vector3D((float)centerX, (float)centerY, (float)centerZ);

            double[] screen = projectVertex(center, model); // ← ПЕРЕДАЁМ Vector3D и Model3D

            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(screen[0] - 4, screen[1] - 4, 8, 8);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(10));
            gc.fillText("P" + i, screen[0] + 6, screen[1] + 4);
        }
    }

    public void setRenderWireframe(boolean renderWireframe) {
        this.renderWireframe = renderWireframe;
        render();
    }

    public void setShowVertices(boolean showVertices) {
        this.showVertices = showVertices;
        render();
    }

    public void setUseTexture(boolean useTexture) {
        this.useTexture = useTexture;
        render();
    }

    public void setUseLighting(boolean useLighting) {
        this.useLighting = useLighting;
        render();
    }

    public void setEditModeEnabled(boolean enabled) {
        this.editModeEnabled = enabled;
        if (enabled) {
            setVertexSize(8.0);
            setSelectedVertexColor(Color.RED);
        } else {
            setVertexSize(5.0);
            setVertexColor(Color.YELLOW);
        }
        render();
    }

    public void setVertexSize(double size) {
        this.vertexSize = Math.max(1.0, Math.min(20.0, size));
    }

    public void setVertexColor(Color color) {
        this.vertexColor = color;
    }

    public void setSelectedVertexColor(Color color) {
        this.selectedVertexColor = color;
    }

    public void setAmbientLight(double ambient) {
        renderer.setAmbientLight(ambient);
        render();
    }

    public void setDiffuseIntensity(double diffuse) {
        renderer.setDiffuseIntensity(diffuse);
        render();
    }

    public void setBackgroundColor(Color color) {
        renderer.setBackgroundColor(color);
        render();
    }

    public Canvas getCanvas() { return canvas; }
    public double getAmbientLight() { return renderer.getAmbientLight(); }
    public double getDiffuseIntensity() { return renderer.getDiffuseIntensity(); }
    public boolean isRenderWireframe() { return renderWireframe; }
    public boolean isShowVertices() { return showVertices; }
    public boolean isUseTexture() { return useTexture; }
    public boolean isUseLighting() { return useLighting; }
    public boolean isEditModeEnabled() { return editModeEnabled; }
    public SoftwareRenderer getRenderer() { return renderer; }
}