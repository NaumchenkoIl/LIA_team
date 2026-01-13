package scene_master.renderer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import scene_master.model.Model3D;
import java.util.ArrayList;
import java.util.List;

public class RenderPanel extends Pane {
    private Canvas canvas;
    private SoftwareRenderer renderer;
    private List<Model3D> models = new ArrayList<>();

    // Флаги режимов рендеринга
    private boolean renderWireframe = false;
    private boolean useTexture = false;
    private boolean useLighting = false;

    public RenderPanel(double width, double height) {
        canvas = new Canvas(width, height);
        getChildren().add(canvas);

        // Привязка размеров canvas к размерам панели
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // Инициализация рендерера
        renderer = new SoftwareRenderer(canvas);

        // НАСТРОЙКА ОСВЕЩЕНИЯ ПО УМОЛЧАНИЮ
        renderer.setAmbientLight(0.5);      // Увеличиваем ambient до 50%
        renderer.setDiffuseIntensity(0.8);  // Увеличиваем diffuse до 80%
        renderer.setLightDirection(0, -0.5, -1); // Оптимальное направление света
        // Обработка изменений размера
        this.widthProperty().addListener((obs, oldVal, newVal) -> {
            renderer.resize(newVal.intValue(), (int) canvas.getHeight());
            render();
        });

        this.heightProperty().addListener((obs, oldVal, newVal) -> {
            renderer.resize((int) canvas.getWidth(), newVal.intValue());
            render();
        });
    }

    public void setModels(List<Model3D> models) {
        this.models = models;
        render();
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
        renderer.setUseLighting(useLighting); // Эта строка должна быть
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

    // Методы для управления освещением
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
    // В класс RenderPanel добавьте эти методы:
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

    // В SoftwareRenderer нужно передать параметры камеры:
    public void renderScene(List<Model3D> models, double camX, double camY, double camZ, double camRotY) {
        // Используть эти параметры в проекции
    }
}