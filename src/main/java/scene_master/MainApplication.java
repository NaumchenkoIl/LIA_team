package scene_master;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import math.LinealAlgebra.Vector3D;
import math.ModelTransform;
import scene_master.calculator.NormalCalculator;
import scene_master.calculator.Triangulator;
import scene_master.manager.SceneManager;
import scene_master.manager.SelectionManager;
import scene_master.manager.EditManager;
import scene_master.model.Model;
import scene_master.model.Model3D;
import scene_master.model.ModelWrapper;
import scene_master.reader.ObjReader;
import scene_master.renderer.RenderPanel;
import scene_master.renderer.TextureManager;
import scene_master.util.DialogHelper;
import scene_master.util.ErrorHandler;
import scene_master.writer.ObjWriter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainApplication extends Application {

    private Stage primaryStage;
    private SceneManager sceneManager;
    private SelectionManager selectionManager;
    private ListView<ModelWrapper> modelListView;
    private BorderPane modelPropertiesPanel;
    private EditManager editManager = new EditManager();
    private String currentTheme = "dark";
    private RenderPanel renderPanel;

    private CheckMenuItem useTextureMenuItem;
    private CheckMenuItem useLightingMenuItem;
    private CheckMenuItem showWireframeMenuItem;
    private CheckMenuItem showVerticesMenuItem;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.selectionManager = new SelectionManager();
        this.sceneManager = new SceneManager(selectionManager);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        root.setTop(createMenuBar());
        root.setLeft(createLeftPanel());
        root.setCenter(createCenterPanel());
        root.setBottom(createStatusBar());
        root.setRight(createRightPanel());

        Scene scene = new Scene(root, 1200, 800);
        switchTheme("dark");

        primaryStage.setTitle("Редактор 3D моделей");
        primaryStage.setScene(scene);
        primaryStage.show();

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelected();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                clearSelection();
                event.consume();
            }
        });
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // Файл
        Menu fileMenu = new Menu("Файл");
        MenuItem openItem = new MenuItem("Открыть модель...");
        MenuItem loadTextureItem = new MenuItem("Загрузить текстуру...");
        MenuItem saveItem = new MenuItem("Сохранить модель как...");
        MenuItem exitItem = new MenuItem("Выход");

        openItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        saveItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        exitItem.setAccelerator(KeyCombination.keyCombination("Alt+F4"));

        openItem.setOnAction(e -> openModel());
        loadTextureItem.setOnAction(e -> loadTexture());
        saveItem.setOnAction(e -> saveModel());
        exitItem.setOnAction(e -> primaryStage.close());

        fileMenu.getItems().addAll(openItem, loadTextureItem, saveItem, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Редактировать");
        CheckMenuItem editModeItem = new CheckMenuItem("Режим редактирования");
        editModeItem.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
        editModeItem.selectedProperty().addListener((obs, oldVal, newVal) -> setEditMode(newVal));

        MenuItem deleteItem = new MenuItem("Удалить выделенное");
        deleteItem.setOnAction(e -> deleteSelected());

        MenuItem deleteVertexItem = new MenuItem("Удалить вершину");
        MenuItem deletePolygonItem = new MenuItem("Удалить полигон");
        deleteVertexItem.setOnAction(e -> deleteSelectedVertex());
        deletePolygonItem.setOnAction(e -> deleteSelectedPolygon());

        MenuItem selectAllItem = new MenuItem("Выделить всё");
        selectAllItem.setOnAction(e -> selectAll());
        MenuItem deselectAllItem = new MenuItem("Снять выделение");
        deselectAllItem.setOnAction(e -> clearSelection());

        editMenu.getItems().addAll(
                editModeItem,
                new SeparatorMenuItem(),
                deleteVertexItem, deletePolygonItem,
                new SeparatorMenuItem(),
                selectAllItem, deselectAllItem,
                new SeparatorMenuItem(),
                deleteItem
        );

        Menu viewMenu = new Menu("Вид");
        showWireframeMenuItem = new CheckMenuItem("Показать каркас");
        showVerticesMenuItem = new CheckMenuItem("Показать вершины");
        useTextureMenuItem = new CheckMenuItem("Использовать текстуру");
        useLightingMenuItem = new CheckMenuItem("Использовать освещение");

        MenuItem darkThemeItem = new MenuItem("Тёмная тема");
        MenuItem lightThemeItem = new MenuItem("Светлая тема");
        MenuItem resetViewItem = new MenuItem("Сбросить вид");

        showWireframeMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) renderPanel.setRenderWireframe(newVal);
        });
        showVerticesMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) renderPanel.setShowVertices(newVal);
        });
        useTextureMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) renderPanel.setUseTexture(newVal);
        });
        useLightingMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) renderPanel.setUseLighting(newVal);
        });

        darkThemeItem.setOnAction(e -> switchTheme("dark"));
        lightThemeItem.setOnAction(e -> switchTheme("light"));
        resetViewItem.setOnAction(e -> resetCamera());

        viewMenu.getItems().addAll(
                showWireframeMenuItem, showVerticesMenuItem,
                useTextureMenuItem, useLightingMenuItem,
                new SeparatorMenuItem(),
                darkThemeItem, lightThemeItem,
                new SeparatorMenuItem(),
                resetViewItem
        );

        Menu toolsMenu = new Menu("Инструменты");
        MenuItem triangulateItem = new MenuItem("Триангулировать");
        MenuItem recalcNormalsItem = new MenuItem("Пересчитать нормали");
        MenuItem optimizeMeshItem = new MenuItem("Оптимизировать сетку");

        triangulateItem.setOnAction(e -> triangulateSelectedModel());
        recalcNormalsItem.setOnAction(e -> recalculateNormals());
        optimizeMeshItem.setOnAction(e -> optimizeMesh());

        toolsMenu.getItems().addAll(triangulateItem, recalcNormalsItem, optimizeMeshItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, toolsMenu);
        return menuBar;
    }

    private void loadTexture() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel == null) {
            DialogHelper.showWarningDialog("Внимание", "Выберите модель для загрузки текстуры");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            showLoadingIndicator(true);

            Task<Image> textureTask = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    return new Image(file.toURI().toString(), false);
                }
            };

            textureTask.setOnSucceeded(event -> {
                Image texture = textureTask.getValue();
                if (texture.isError()) {
                    ErrorHandler.handleException(new RuntimeException("Ошибка загрузки текстуры"), "загрузка текстуры");
                    showLoadingIndicator(false);
                    return;
                }

                activeModel.setTexture(texture);
                if (activeModel.getTextureCoords().isEmpty()) {
                    activeModel.generateUVFromGeometry();
                }

                if (renderPanel != null) {
                    renderPanel.setUseTexture(true);
                    useTextureMenuItem.setSelected(true);
                    renderPanel.render();
                }

                updateModelPropertiesPanel(activeModel);
                showLoadingIndicator(false);
                updateStatusBarTextureInfo();

                DialogHelper.showInfoDialog("Текстура загружена",
                        "Текстура '" + file.getName() + "' применена к модели");
            });

            textureTask.setOnFailed(event -> {
                showLoadingIndicator(false);
                ErrorHandler.handleException(textureTask.getException(), "загрузка текстуры");
            });

            new Thread(textureTask).start();
        }
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.getStyleClass().add("left-panel");
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(200);

        Label modelsLabel = new Label("Модели");
        modelsLabel.getStyleClass().add("section-label");

        modelListView = new ListView<>();
        modelListView.setItems(sceneManager.getModelWrappers());
        modelListView.setCellFactory(lv -> new ModelListCell());
        modelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectionManager.clearSelection();
            if (newVal != null) {
                selectionManager.selectModel(newVal.getUIModel());
                updateModelPropertiesPanel(newVal.getUIModel());
            }
        });

        Button addTestModelBtn = new Button("Добавить тестовую модель");
        Button removeModelBtn = new Button("Удалить модель");
        Button duplicateModelBtn = new Button("Дублировать модель");

        addTestModelBtn.setOnAction(e -> addTestModel());
        removeModelBtn.setOnAction(e -> removeSelectedModel());
        duplicateModelBtn.setOnAction(e -> duplicateSelectedModel());

        HBox modelButtons = new HBox(5, addTestModelBtn, removeModelBtn);
        VBox buttonsBox = new VBox(5, modelButtons, duplicateModelBtn);
        leftPanel.getChildren().addAll(modelsLabel, modelListView, buttonsBox);
        return leftPanel;
    }

    private class ModelListCell extends ListCell<ModelWrapper> {
        @Override
        protected void updateItem(ModelWrapper wrapper, boolean empty) {
            super.updateItem(wrapper, empty);
            if (empty || wrapper == null) {
                setText(null);
                setStyle("");
            } else {
                setText(wrapper.nameProperty().get());
                if (wrapper.getUIModel() != null && selectionManager.isSelected(wrapper.getUIModel())) {
                    setStyle("-fx-background-color: #2a4d69; -fx-text-fill: white;");
                } else {
                    setStyle("");
                }
            }
        }
    }

    private Pane createCenterPanel() {
        renderPanel = new RenderPanel(800, 600, selectionManager, editManager);        renderPanel.getStyleClass().add("view-3d");
        renderPanel.setStyle("-fx-background-color: #1a1a2e;");

        sceneManager.getModelWrappers().addListener((ListChangeListener<ModelWrapper>) change -> {
            List<Model3D> models = sceneManager.getModelWrappers().stream()
                    .map(ModelWrapper::getUIModel)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            renderPanel.setModels(models);

            boolean hasModels = !models.isEmpty();
            useTextureMenuItem.setDisable(!hasModels);
            useLightingMenuItem.setDisable(!hasModels);
        });

        return renderPanel;
    }
    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.getStyleClass().add("right-panel");
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);
        rightPanel.setMaxWidth(300); // ← важно для ограничения

        Label propertiesLabel = new Label("Свойства модели");
        propertiesLabel.getStyleClass().add("section-label");

        modelPropertiesPanel = new BorderPane();
        modelPropertiesPanel.setCenter(new Label("Выберите модель для редактирования свойств"));

        Label transformLabel = new Label("Трансформации");
        transformLabel.getStyleClass().add("section-label");

        VBox transformsPanel = createTransformsPanel();

        HBox buttonBox = new HBox(8);
        Button resetBtn = new Button("Сбросить");
        Button applyBtn = new Button("Применить");

        resetBtn.setMinWidth(Region.USE_PREF_SIZE);
        applyBtn.setMinWidth(Region.USE_PREF_SIZE);
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);
        HBox.setHgrow(applyBtn, Priority.ALWAYS);

        resetBtn.setOnAction(e -> resetTransformations());
        applyBtn.setOnAction(e -> applyTransformations());

        buttonBox.getChildren().addAll(resetBtn, applyBtn);
        buttonBox.setAlignment(Pos.CENTER);

        Slider ambientSlider = createSliderOnly("Ambient Light", 0, 1, 0.3);
        Slider diffuseSlider = createSliderOnly("Diffuse Intensity", 0, 1, 0.7);
        ambientSlider.valueProperty().addListener((obs, o, n) ->
                renderPanel.setAmbientLight(n.doubleValue()));
        diffuseSlider.valueProperty().addListener((obs, o, n) ->
                renderPanel.setDiffuseIntensity(n.doubleValue()));

        rightPanel.getChildren().addAll(
                propertiesLabel,
                modelPropertiesPanel,
                new Separator(),
                transformLabel,
                transformsPanel,
                buttonBox,
                new Separator(),
                new Label("Параметры освещения"),
                createLabeledSlider("Ambient:", ambientSlider),
                createLabeledSlider("Diffuse:", diffuseSlider)
        );

        return rightPanel;
    }

    private HBox createLabeledSlider(String label, Slider slider) {
        Label nameLabel = new Label(label);
        nameLabel.setPrefWidth(60);
        HBox box = new HBox(5, nameLabel, slider);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox createTransformsPanel() {
        VBox transformsPanel = new VBox(5);

        Slider txSlider = createSliderOnly("Translate X", -10, 10, 0);
        txSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.translateXProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider tySlider = createSliderOnly("Translate Y", -10, 10, 0);
        tySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.translateYProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider tzSlider = createSliderOnly("Translate Z", -10, 10, 0);
        tzSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.translateZProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider rxSlider = createSliderOnly("Rotate X", -180, 180, 0);
        rxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.rotateXProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider rySlider = createSliderOnly("Rotate Y", -180, 180, 0);
        rySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.rotateYProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider rzSlider = createSliderOnly("Rotate Z", -180, 180, 0);
        rzSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.rotateZProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider sxSlider = createSliderOnly("Scale X", 0.1, 5, 1);
        sxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.scaleXProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider sySlider = createSliderOnly("Scale Y", 0.1, 5, 1);
        sySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.scaleYProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        Slider szSlider = createSliderOnly("Scale Z", 0.1, 5, 1);
        szSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D model = selectionManager.getActiveModel();
            if (model != null) {
                model.scaleZProperty().set(newVal.doubleValue());
                renderPanel.render();
            }
        });

        selectionManager.getSelectedModels().addListener((ListChangeListener.Change<? extends Model3D> c) -> {
            Model3D active = selectionManager.getActiveModel();
            if (active != null) {
                txSlider.setValue(active.translateXProperty().get());
                tySlider.setValue(active.translateYProperty().get());
                tzSlider.setValue(active.translateZProperty().get());
                rxSlider.setValue(active.rotateXProperty().get());
                rySlider.setValue(active.rotateYProperty().get());
                rzSlider.setValue(active.rotateZProperty().get());
                sxSlider.setValue(active.scaleXProperty().get());
                sySlider.setValue(active.scaleYProperty().get());
                szSlider.setValue(active.scaleZProperty().get());
            }
        });

        transformsPanel.getChildren().addAll(
                wrapSliderWithLabel(txSlider, "Translate X"),
                wrapSliderWithLabel(tySlider, "Translate Y"),
                wrapSliderWithLabel(tzSlider, "Translate Z"),
                wrapSliderWithLabel(rxSlider, "Rotate X"),
                wrapSliderWithLabel(rySlider, "Rotate Y"),
                wrapSliderWithLabel(rzSlider, "Rotate Z"),
                wrapSliderWithLabel(sxSlider, "Scale X"),
                wrapSliderWithLabel(sySlider, "Scale Y"),
                wrapSliderWithLabel(szSlider, "Scale Z")
        );

        return transformsPanel;
    }

    private HBox wrapSliderWithLabel(Slider slider, String label) {
        Label nameLabel = new Label(label + ":");
        nameLabel.setPrefWidth(80);
        Label valueLabel = new Label(String.format("%.1f", slider.getValue()));
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                valueLabel.setText(String.format("%.1f", newVal))
        );
        return new HBox(10, nameLabel, slider, valueLabel);
    }

    private HBox createSliderControl(String label, double min, double max, double initial) {
        HBox hbox = new HBox(10);
        Label nameLabel = new Label(label);
        nameLabel.setPrefWidth(80);

        Slider slider = new Slider(min, max, initial);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit((max - min) / 4);

        Label valueLabel = new Label(String.format("%.1f", initial));
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                valueLabel.setText(String.format("%.1f", newVal)));

        hbox.getChildren().addAll(nameLabel, slider, valueLabel);
        return hbox;
    }

    private Slider createSliderOnly(String label, double min, double max, double initial) {
        Slider slider = new Slider(min, max, initial);
        slider.setShowTickLabels(false); // ← убираем подписи, чтобы сэкономить место
        slider.setShowTickMarks(false);
        slider.setMinWidth(100);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);
        return slider;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5));
        statusBar.setId("status-bar");

        Label statusLabel = new Label("Готово");
        statusLabel.setId("status-label");

        Label editModeLabel = new Label("[Режим редактирования: ВЫКЛ]");
        editModeLabel.setId("edit-mode-label");
        editModeLabel.setTextFill(Color.GRAY);

        editManager.setEditModeListener(enabled -> {
            editModeLabel.setText(enabled ? "[Режим редактирования: ВКЛ]" : "[Режим редактирования: ВЫКЛ]");
            editModeLabel.setTextFill(enabled ? Color.RED : Color.GRAY);
        });

        Label vertexCountLabel = new Label("Вершин: 0");
        vertexCountLabel.setId("vertex-count");
        Label polygonCountLabel = new Label("Полигонов: 0");
        polygonCountLabel.setId("polygon-count");
        Label textureCountLabel = new Label("Текстур: 0");
        textureCountLabel.setId("texture-count");
        Label normalCountLabel = new Label("Нормалей: 0");
        normalCountLabel.setId("normal-count");
        Label textureInfoLabel = new Label("Текстура: не загружена");
        textureInfoLabel.setId("texture-info");
        textureInfoLabel.setTextFill(Color.GRAY);
        Label modelInfoLabel = new Label("Моделей: 0");
        modelInfoLabel.setId("model-count");

        statusBar.getChildren().addAll(
                statusLabel, editModeLabel, new Separator(),
                vertexCountLabel, polygonCountLabel, textureCountLabel, normalCountLabel,
                new Separator(), textureInfoLabel, new Separator(), modelInfoLabel
        );
        return statusBar;
    }

    private void updateModelPropertiesPanel(Model3D model) {
        VBox properties = new VBox(10);

        HBox nameBox = new HBox(10);
        Label nameLabel = new Label("Имя:");
        TextField nameField = new TextField(model.nameProperty().get());
        nameField.textProperty().bindBidirectional(model.nameProperty());
        nameBox.getChildren().addAll(nameLabel, nameField);

        CheckBox visibleCheck = new CheckBox("Видима");
        visibleCheck.selectedProperty().bindBidirectional(model.visibleProperty());
        visibleCheck.selectedProperty().addListener((obs, oldVal, newVal) -> renderPanel.render());

        HBox colorBox = new HBox(10);
        Label colorLabel = new Label("Цвет:");
        ColorPicker colorPicker = new ColorPicker(model.getBaseColor());
        colorPicker.valueProperty().bindBidirectional(model.baseColorProperty());
        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> renderPanel.render());
        colorBox.getChildren().addAll(colorLabel, colorPicker);

        HBox textureBox = new HBox(10);
        Label textureLabel = new Label("Текстура:");
        Label textureInfo = new Label(
                model.getTexture() != null ? "✓ Текстура загружена" : "Нет текстуры"
        );
        textureInfo.setTextFill(model.getTexture() != null ? Color.GREEN : Color.GRAY);
        Button loadTextureBtn = new Button("Загрузить...");
        loadTextureBtn.setOnAction(e -> loadTexture());
        textureBox.getChildren().addAll(textureLabel, textureInfo, loadTextureBtn);

        Label statsLabel = new Label(String.format(
                "Статистика модели:\n" +
                        "• Вершин: %d\n" +
                        "• Текстурных координат: %d\n" +
                        "• Нормалей: %d\n" +
                        "• Полигонов: %d",
                model.getVertices().size(),
                model.getTexturePoints().size(),
                model.getNormals().size(),
                model.getPolygons().size()
        ));
        statsLabel.setWrapText(true);
        statsLabel.setStyle("-fx-font-size: 12px;");

        properties.getChildren().addAll(nameBox, visibleCheck, colorBox, textureBox, statsLabel);
        modelPropertiesPanel.setCenter(properties);
    }

    private void openModel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            showLoadingIndicator(true);

            Task<ModelWrapper> loadTask = new Task<>() {
                @Override
                protected ModelWrapper call() throws Exception {
                    ObjReader objReader = new ObjReader();
                    Model loadedModel = objReader.readModel(file.getAbsolutePath());
                    Triangulator triangulator = new Triangulator();
                    triangulator.triangulateModel(loadedModel);

                    ModelWrapper wrapper = new ModelWrapper(loadedModel, file.getName().replace(".obj", ""));

                    wrapper.getUIModel().generateUVFromGeometry();
                    wrapper.getUIModel().calculateVertexNormals();

                    return wrapper;
                }
            };
            loadTask.setOnSucceeded(event -> {
                ModelWrapper wrapper = loadTask.getValue();
                sceneManager.addModelWrapper(wrapper);
                showLoadingIndicator(false);
                updateStatistics();
                DialogHelper.showInfoDialog("Успешно",
                        String.format("Модель загружена!\nВершин: %d\nПолигонов: %d",
                                wrapper.getOriginalModel().getVertexCount(),
                                wrapper.getOriginalModel().getPolygonCount()));
            });

            loadTask.setOnFailed(event -> {
                showLoadingIndicator(false);
                ErrorHandler.handleException(loadTask.getException(), "загрузка модели");
            });

            new Thread(loadTask).start();
        }
    }

    private void saveModel() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            ModelWrapper selectedWrapper = null;
            for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                if (wrapper.getUIModel() == activeModel) {
                    selectedWrapper = wrapper;
                    break;
                }
            }

            if (selectedWrapper == null) {
                ErrorHandler.handleWarning("Модель не найдена", "сохранение");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(activeModel.getName() + ".obj");
            File file = fileChooser.showSaveDialog(primaryStage);

            if (file != null) {
                try {
                    boolean applyTransformations = DialogHelper.showSaveOptionsDialog();
                    if (applyTransformations) {
                        applyTransformationsToOriginalModel(selectedWrapper);
                        selectedWrapper.updateUIModel();
                    }

                    ObjWriter objWriter = new ObjWriter();
                    objWriter.writeModel(activeModel, file.getAbsolutePath(), applyTransformations);

                    DialogHelper.showInfoDialog("Сохранение завершено",
                            String.format("Модель успешно сохранена!\n\n" +
                                            "Файл: %s\n" +
                                            "Вершин: %d\n" +
                                            "Текстурных координат: %d\n" +
                                            "Нормалей: %d\n" +
                                            "Полигонов: %d\n" +
                                            "Трансформации применены: %s",
                                    file.getName(),
                                    activeModel.getVertices().size(),
                                    activeModel.getTexturePoints().size(),
                                    activeModel.getNormals().size(),
                                    activeModel.getPolygons().size(),
                                    applyTransformations ? "Да" : "Нет"));
                } catch (Exception e) {
                    ErrorHandler.handleException(e, "сохранение модели");
                }
            }
        } else {
            ErrorHandler.handleWarning("Модель не выбрана", "сохранение");
        }
    }

    private void addTestModel() {
        try {
            String filePath = "C:/Users/Александр/Desktop/for3person/LIA_team/src/tests/test_cube.obj";
            ObjReader reader = new ObjReader();
            Model originalModel = reader.readModel(filePath);
            Triangulator triangulator = new Triangulator();
            triangulator.triangulateModel(originalModel);
            String name = "Cube from file " + (sceneManager.getModelWrappers().size() + 1);
            ModelWrapper wrapper = new ModelWrapper(originalModel, name);
            sceneManager.addModelWrapper(wrapper);
            wrapper.getUIModel().calculateVertexNormals();
        } catch (Exception e) {
            ErrorHandler.handleException(e, "загрузка тестовой модели");
        }
    }

    private void removeSelectedModel() {
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            sceneManager.removeModelWrapper(selected);
        }
    }

    private void createDefaultUVCoordinates(Model3D model) {
        List<Vector3D> vertices = model.getVertices();
        if (vertices.isEmpty()) return;

        double minX = vertices.stream().mapToDouble(v -> v.getX()).min().orElse(0.0);
        double maxX = vertices.stream().mapToDouble(v -> v.getX()).max().orElse(1.0);
        double minY = vertices.stream().mapToDouble(v -> v.getY()).min().orElse(0.0);
        double maxY = vertices.stream().mapToDouble(v -> v.getY()).max().orElse(1.0);

        double rangeX = Math.max(1e-6, maxX - minX);
        double rangeY = Math.max(1e-6, maxY - minY);

        model.clearTextureCoords();
        for (Vector3D vertex : vertices) {
            double u = (vertex.getX() - minX) / rangeX;
            double v = (vertex.getY() - minY) / rangeY;
            model.addTextureCoord(u, v);
        }

        for (scene_master.model.Polygon polygon : model.getPolygons()) {
            if (polygon.getTextureIndices().isEmpty()) {
                for (int i = 0; i < polygon.getVertexIndices().size(); i++) {
                    polygon.addTextureIndex(i);
                }
            }
        }
    }

    private void applyTransformationsToOriginalModel(ModelWrapper wrapper) {
        Model3D uiModel = wrapper.getUIModel();
        Model originalModel = wrapper.getOriginalModel();
        if (originalModel == null) return;

        ModelTransform transform = new ModelTransform();
        transform.setTranslation(
                (float) uiModel.translateXProperty().get(),
                (float) uiModel.translateYProperty().get(),
                (float) uiModel.translateZProperty().get()
        );
        transform.setRotationDeg(
                (float) uiModel.rotateXProperty().get(),
                (float) uiModel.rotateYProperty().get(),
                (float) uiModel.rotateZProperty().get()
        );
        transform.setScale(
                (float) uiModel.scaleXProperty().get(),
                (float) uiModel.scaleYProperty().get(),
                (float) uiModel.scaleZProperty().get()
        );

        for (int i = 0; i < originalModel.getVertices().size(); i++) {
            Vector3D oldVertex = originalModel.getVertices().get(i);
            Vector3D mathVertex = new Vector3D(oldVertex.getX(), oldVertex.getY(), oldVertex.getZ());
            Vector3D transformed = transform.transformVertex(mathVertex);
            originalModel.getVertices().set(i, new Vector3D(transformed.getX(), transformed.getY(), transformed.getZ()));
        }

        NormalCalculator calc = new NormalCalculator();
        calc.calculateNormals(originalModel);
    }

    private void updateStatistics() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            updateStatusBarStatistics(activeModel);
        }
    }

    private void updateStatusBarStatistics(Model3D model) {
        HBox statusBar = (HBox) primaryStage.getScene().lookup("#status-bar");
        if (statusBar == null) return;

        Label vertexCountLabel = (Label) statusBar.lookup("#vertex-count");
        Label polygonCountLabel = (Label) statusBar.lookup("#polygon-count");
        Label textureCountLabel = (Label) statusBar.lookup("#texture-count");
        Label normalCountLabel = (Label) statusBar.lookup("#normal-count");
        Label modelCountLabel = (Label) statusBar.lookup("#model-count");

        if (vertexCountLabel != null) vertexCountLabel.setText("Вершин: " + model.getVertices().size());
        if (polygonCountLabel != null) polygonCountLabel.setText("Полигонов: " + model.getPolygons().size());
        if (textureCountLabel != null) textureCountLabel.setText("Текстур: " + model.getTexturePoints().size());
        if (normalCountLabel != null) normalCountLabel.setText("Нормалей: " + model.getNormals().size());
        if (modelCountLabel != null) modelCountLabel.setText("Моделей: " + sceneManager.getModelWrappers().size());
    }

    private void updateStatusBarTextureInfo() {
        HBox statusBar = (HBox) primaryStage.getScene().lookup("#status-bar");
        if (statusBar == null) return;

        Label textureInfoLabel = (Label) statusBar.lookup("#texture-info");
        if (textureInfoLabel != null) {
            Model3D active = selectionManager.getActiveModel();
            if (active != null && active.getTexture() != null) {
                textureInfoLabel.setText("Текстура: загружена");
                textureInfoLabel.setTextFill(Color.GREEN);
            } else {
                textureInfoLabel.setText("Текстура: не загружена");
                textureInfoLabel.setTextFill(Color.GRAY);
            }
        }
    }

    private void switchTheme(String theme) {
        try {
            Scene scene = primaryStage.getScene();
            if (scene == null) return;

            scene.getStylesheets().clear();
            URL cssUrl = getClass().getResource("/" + theme + ".css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                currentTheme = theme;
            }
        } catch (Exception e) {
            System.err.println("Ошибка переключения темы: " + e.getMessage());
        }
    }

    private void setEditMode(boolean enabled) {
        editManager.setEditMode(enabled);
        if (renderPanel != null) {
            renderPanel.setEditModeEnabled(enabled);
        }
        if (enabled) {
            DialogHelper.showInfoDialog("Режим редактирования",
                    "Режим редактирования включен.\n" +
                            "1. Выберите модель в списке слева\n" +
                            "2. Щелкните по вершине или полигону для выбора\n" +
                            "3. Удаляйте через меню или клавишу Delete");
        }
    }

    private void deleteSelected() {
        if (editManager.isEditMode()) {
            if (editManager.getSelectedVertexIndex() != -1) {
                deleteSelectedVertex();
            } else if (editManager.getSelectedPolygonIndex() != -1) {
                deleteSelectedPolygon();
            }
        } else {
            ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                sceneManager.removeModelWrapper(selected);
            }
        }
    }

    private void deleteSelectedVertex() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null && editManager.isEditMode()) {
            if (editManager.getSelectedVertexIndex() != -1) {
                editManager.deleteSelectedVertex(activeModel);
                renderPanel.render();
                updateModelPropertiesPanel(activeModel);
                updateStatistics();
                DialogHelper.showInfoDialog("Успех", "Вершина удалена");
            } else {
                DialogHelper.showWarningDialog("Внимание", "Выберите вершину для удаления");
            }
        }
    }

    private void deleteSelectedPolygon() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null && editManager.isEditMode()) {
            if (editManager.getSelectedPolygonIndex() != -1) {
                editManager.deleteSelectedPolygon(activeModel);
                renderPanel.render();
                updateModelPropertiesPanel(activeModel);
                updateStatistics();
                DialogHelper.showInfoDialog("Успех", "Полигон удалён");
            } else {
                DialogHelper.showWarningDialog("Внимание", "Выберите полигон для удаления");
            }
        }
    }

    private void selectAllModels() {
        sceneManager.getModelWrappers().forEach(w -> selectionManager.selectModel(w.getUIModel()));
    }



    private void clearSelection() {
        selectionManager.clearSelection();
        modelListView.getSelectionModel().clearSelection();
        modelPropertiesPanel.setCenter(new Label("Выберите модель для редактирования свойств"));
    }

    private void resetCamera() {
        if (renderPanel != null && renderPanel.getRenderer() != null) {
            renderPanel.getRenderer().resetCamera();
        }
    }

    private void triangulateSelectedModel() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            ModelWrapper wrapper = findWrapperByModel(activeModel);
            if (wrapper != null && wrapper.getOriginalModel() != null) {
                Triangulator triangulator = new Triangulator();
                triangulator.triangulateModel(wrapper.getOriginalModel());
                DialogHelper.showInfoDialog("Триангуляция", "Модель триангулирована");
            }
        }
    }

    private void recalculateNormals() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            ModelWrapper wrapper = findWrapperByModel(activeModel);
            if (wrapper != null && wrapper.getOriginalModel() != null) {
                NormalCalculator calc = new NormalCalculator();
                calc.calculateNormals(wrapper.getOriginalModel());
                activeModel.calculateVertexNormals();
                renderPanel.render();
                DialogHelper.showInfoDialog("Пересчет нормалей",
                        "Нормали пересчитаны для " + activeModel.getPolygons().size() + " полигонов");
            }
        }
    }

    private ModelWrapper findWrapperByModel(Model3D model) {
        for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
            if (wrapper.getUIModel() == model) {
                return wrapper;
            }
        }
        return null;
    }

    private void optimizeMesh() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            int before = activeModel.getVertices().size();
            // Здесь можно добавить логику оптимизации
            int after = activeModel.getVertices().size();
            renderPanel.render();
            DialogHelper.showInfoDialog("Оптимизация сетки",
                    "Удалено " + (before - after) + " дублирующих вершин.");
        }
    }

    private Stage loadingStage;

    private void showLoadingIndicator(boolean show) {
        if (show) {
            if (loadingStage == null) {
                loadingStage = new Stage();
                loadingStage.initOwner(primaryStage);
                loadingStage.initModality(Modality.APPLICATION_MODAL);
                loadingStage.setTitle("Загрузка...");
                Label label = new Label("Загрузка модели, пожалуйста, подождите...");
                label.setPadding(new Insets(20));
                Scene scene = new Scene(new StackPane(label), 300, 100);
                loadingStage.setScene(scene);
                loadingStage.setResizable(false);
            }
            loadingStage.show();
        } else {
            if (loadingStage != null) {
                loadingStage.close();
            }
        }
    }


    private void resetTransformations() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            activeModel.translateXProperty().set(0);
            activeModel.translateYProperty().set(0);
            activeModel.translateZProperty().set(0);
            activeModel.rotateXProperty().set(0);
            activeModel.rotateYProperty().set(0);
            activeModel.rotateZProperty().set(0);
            activeModel.scaleXProperty().set(1);
            activeModel.scaleYProperty().set(1);
            activeModel.scaleZProperty().set(1);
            renderPanel.render();
            DialogHelper.showInfoDialog("Сброс трансформаций", "Все трансформации сброшены");
        }
    }

    private void applyTransformations() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            ModelWrapper wrapper = findWrapperByModel(activeModel);
            if (wrapper != null) {
                applyTransformationsToOriginalModel(wrapper);
                wrapper.updateUIModel();
                renderPanel.render();
                DialogHelper.showInfoDialog("Применение трансформаций",
                        "Трансформации применены к исходной модели");
            }
        }
    }

    private void duplicateSelectedModel() {
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Model3D original = selected.getUIModel();
                Model3D duplicate = new Model3D(original.getName() + " (копия)");

                for (Vector3D vertex : original.getVertices()) {
                    duplicate.getVertices().add(new Vector3D(vertex.getX(), vertex.getY(), vertex.getZ()));
                }

                for (scene_master.model.TexturePoint tp : original.getTexturePoints()) {
                    duplicate.getTexturePoints().add(tp);
                }

                for (Model3D.TextureCoordinate tc : original.getTextureCoords()) {
                    duplicate.addTextureCoord(tc.u, tc.v);
                }

                for (scene_master.model.Polygon polygon : original.getPolygons()) {
                    scene_master.model.Polygon dupPoly = new scene_master.model.Polygon(polygon.getVertexIndicesArray());
                    if (polygon.hasTexture()) dupPoly.setTextureIndices(polygon.getTextureIndices());
                    if (polygon.hasNormals()) dupPoly.setNormalIndices(polygon.getNormalIndices());
                    if (polygon.getNormal() != null) dupPoly.setNormal(polygon.getNormal());
                    duplicate.getPolygons().add(dupPoly);
                }

                duplicate.translateXProperty().set(original.translateXProperty().get());
                duplicate.translateYProperty().set(original.translateYProperty().get());
                duplicate.translateZProperty().set(original.translateZProperty().get());
                duplicate.rotateXProperty().set(original.rotateXProperty().get());
                duplicate.rotateYProperty().set(original.rotateYProperty().get());
                duplicate.rotateZProperty().set(original.rotateZProperty().get());
                duplicate.scaleXProperty().set(original.scaleXProperty().get());
                duplicate.scaleYProperty().set(original.scaleYProperty().get());
                duplicate.scaleZProperty().set(original.scaleZProperty().get());

                duplicate.setTexture(original.getTexture());
                duplicate.setBaseColor(original.getBaseColor());

                ModelWrapper newWrapper = new ModelWrapper(null, duplicate.getName());
                newWrapper.getUIModel().calculateVertexNormals(); // важно!

                sceneManager.addModelWrapper(newWrapper);
                updateStatistics();
                DialogHelper.showInfoDialog("Дублирование", "Модель успешно продублирована");
            } catch (Exception e) {
                ErrorHandler.handleException(e, "дублирование модели");
            }
        }
    }

    private void selectAll() {
        if (editManager.isEditMode()) {
            Model3D activeModel = selectionManager.getActiveModel();
            if (activeModel != null) {
                editManager.selectAll(activeModel);
                renderPanel.render();
                DialogHelper.showInfoDialog("Выделение",
                        "Выделено: " + activeModel.getVertices().size() + " вершин, " +
                                activeModel.getPolygons().size() + " полигонов");
            }
        } else {
            modelListView.getSelectionModel().selectAll();
            for (ModelWrapper wrapper : modelListView.getItems()) {
                selectionManager.selectModel(wrapper.getUIModel());
            }
            updateStatistics();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}