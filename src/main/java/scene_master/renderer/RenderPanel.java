package scene_master.renderer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import math.Camera;
import math.LinealAlgebra.Vector3D;
import scene_master.model.Model3D;
import java.util.ArrayList;
import java.util.List;

public class RenderPanel extends Pane {
    private Canvas canvas;
    private SoftwareRenderer renderer;
    private List<Model3D> models = new ArrayList<>();
    private Camera camera;

    private boolean renderWireframe = false;
    private boolean useTexture = false;
    private boolean useLighting = false;

    public RenderPanel(double width, double height) {
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
    }

    public void setRenderWireframe(boolean renderWireframe) {
        this.renderWireframe = renderWireframe;
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

    public void setAmbientLight(double ambient) {
        renderer.setAmbientLight(ambient);
        render();
    }

    public void setDiffuseIntensity(double diffuse) {
        renderer.setDiffuseIntensity(diffuse);
        render();
    }

    public void setLightDirection(double x, double y, double z) {
        renderer.setLightDirection(x, y, z);
        render();
    }

    public void setBackgroundColor(Color color) {
        renderer.setBackgroundColor(color);
        render();
    }


    public Canvas getCanvas() {
        return canvas;
    }

    public double getAmbientLight() {
        return renderer.getAmbientLight();
    }

    public double getDiffuseIntensity() {
        return renderer.getDiffuseIntensity();
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

    public SoftwareRenderer getRenderer() {
        return renderer;
    }

    public void renderScene(List<Model3D> models, double camX, double camY, double camZ, double camRotY) {
    }
}