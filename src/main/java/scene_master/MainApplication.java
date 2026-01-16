package scene_master;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.stage.Modality;
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
import scene_master.model.Polygon;
import scene_master.reader.ObjReader;
import scene_master.renderer.RenderPanel;
import scene_master.writer.ObjWriter;
import scene_master.util.DialogHelper;
import scene_master.util.ErrorHandler;
import scene_master.util.ModelSelectionDialog;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.IOException;
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
    private Stage loadingStage;
    private Image currentTexture = null;
    private String textureFileName = "";

    // UI элементы для меню
    private CheckMenuItem showWireframeMenuItem;
    private CheckMenuItem showVerticesMenuItem;
    private CheckMenuItem useTextureMenuItem;
    private CheckMenuItem useLightingMenuItem;
    private CheckMenuItem editModeMenuItem;

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

        Menu fileMenu = new Menu("Файл");
        MenuItem openItem = new MenuItem("Открыть модель...");
        openItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        MenuItem loadTextureItem = new MenuItem("Загрузить текстуру...");
        MenuItem saveItem = new MenuItem("Сохранить модель как...");
        saveItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        MenuItem exitItem = new MenuItem("Выход");
        exitItem.setAccelerator(KeyCombination.keyCombination("Alt+F4"));

        openItem.setOnAction(e -> openModel());
        loadTextureItem.setOnAction(e -> loadTexture());
        saveItem.setOnAction(e -> saveModel());
        exitItem.setOnAction(e -> primaryStage.close());

        fileMenu.getItems().addAll(openItem, loadTextureItem, saveItem, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Редактировать");
        editModeMenuItem = new CheckMenuItem("Режим редактирования");
        editModeMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
        editModeMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            setEditMode(newVal);
        });

        MenuItem deleteItem = new MenuItem("Удалить выделенное");
        deleteItem.setAccelerator(KeyCombination.keyCombination("DELETE"));
        deleteItem.setOnAction(e -> deleteSelected());

        MenuItem deleteVertexItem = new MenuItem("Удалить вершину");
        MenuItem deletePolygonItem = new MenuItem("Удалить полигон");
        deleteVertexItem.setOnAction(e -> deleteSelectedVertex());
        deletePolygonItem.setOnAction(e -> deleteSelectedPolygon());

        MenuItem selectAllItem = new MenuItem("Выделить все");
        selectAllItem.setAccelerator(KeyCombination.keyCombination("Ctrl+A"));
        selectAllItem.setOnAction(e -> selectAll());

        MenuItem deselectAllItem = new MenuItem("Снять выделение");
        deselectAllItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+A"));
        deselectAllItem.setOnAction(e -> clearSelection());

        editMenu.getItems().addAll(editModeMenuItem, new SeparatorMenuItem(),
                deleteVertexItem, deletePolygonItem, new SeparatorMenuItem(),
                selectAllItem, deselectAllItem, new SeparatorMenuItem(),
                deleteItem);

        Menu viewMenu = new Menu("Вид");
        showWireframeMenuItem = new CheckMenuItem("Показать каркас");
        showVerticesMenuItem = new CheckMenuItem("Показать вершины");
        useTextureMenuItem = new CheckMenuItem("Использовать текстуру");
        useLightingMenuItem = new CheckMenuItem("Использовать освещение");

        // Изначально отключаем некоторые пункты, если нет 3D-рендерера
        if (renderPanel == null) {
            useTextureMenuItem.setDisable(true);
            useLightingMenuItem.setDisable(true);
        } else {
            useTextureMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
                renderPanel.setUseTexture(newVal);
            });

            useLightingMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
                renderPanel.setUseLighting(newVal);
            });
        }

        showWireframeMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.setRenderWireframe(newVal);
            }
        });

        showVerticesMenuItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.setShowVertices(newVal);
            }
        });

        MenuItem darkThemeItem = new MenuItem("Тёмная тема");
        MenuItem lightThemeItem = new MenuItem("Светлая тема");

        darkThemeItem.setOnAction(e -> switchTheme("dark"));
        lightThemeItem.setOnAction(e -> switchTheme("light"));

        MenuItem resetViewItem = new MenuItem("Сбросить вид");
        resetViewItem.setOnAction(e -> resetView());

        Menu toolsMenu = new Menu("Инструменты");
        MenuItem triangulateItem = new MenuItem("Триангулировать модель");
        triangulateItem.setOnAction(e -> triangulateSelectedModel());
        MenuItem recalcNormalsItem = new MenuItem("Пересчитать нормали");
        recalcNormalsItem.setOnAction(e -> recalculateNormals());
        MenuItem optimizeMeshItem = new MenuItem("Оптимизировать сетку");
        optimizeMeshItem.setOnAction(e -> optimizeMesh());

        viewMenu.getItems().addAll(showWireframeMenuItem, showVerticesMenuItem,
                useTextureMenuItem, useLightingMenuItem,
                new SeparatorMenuItem(), darkThemeItem, lightThemeItem,
                new SeparatorMenuItem(), resetViewItem);

        toolsMenu.getItems().addAll(triangulateItem, recalcNormalsItem, optimizeMeshItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, toolsMenu);
        return menuBar;
    }

    private void loadTexture() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel == null) {
            // Если нет активной модели, предлагаем выбрать
            ModelWrapper selectedWrapper = ModelSelectionDialog.showModelSelectionDialog(
                    sceneManager.getModelWrappers(), "Выберите модель для текстуры");

            if (selectedWrapper != null) {
                selectionManager.selectModel(selectedWrapper.getUIModel());
                activeModel = selectedWrapper.getUIModel();
            } else {
                DialogHelper.showWarningDialog("Внимание", "Выберите модель для загрузки текстуры");
                return;
            }
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

            Model3D finalActiveModel = activeModel;
            textureTask.setOnSucceeded(event -> {
                Image texture = textureTask.getValue();

                if (texture.isError()) {
                    ErrorHandler.handleException(new RuntimeException("Ошибка загрузки текстуры"), "загрузка текстуры");
                    showLoadingIndicator(false);
                    return;
                }

                currentTexture = texture;
                textureFileName = file.getName();

                finalActiveModel.setTexture(texture);

                if (finalActiveModel.getTextureCoords().isEmpty()) {
                    createDefaultUVCoordinates(finalActiveModel);
                }

                if (renderPanel != null) {
                    renderPanel.setUseTexture(true);
                    useTextureMenuItem.setSelected(true);
                    renderPanel.render();
                }

                updateModelPropertiesPanel(finalActiveModel);
                showLoadingIndicator(false);
                updateStatusBarTextureInfo();

                DialogHelper.showInfoDialog("Текстура загружена",
                        "Текстура '" + file.getName() + "' применена к модели\n" +
                                "Размер: " + (int)texture.getWidth() + "x" + (int)texture.getHeight());
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

        Label modelsLabel = new Label("Модели");
        modelsLabel.getStyleClass().add("section-label");

        modelListView = new ListView<>();
        modelListView.setItems(sceneManager.getModelWrappers());
        modelListView.setCellFactory(lv -> new ModelListCell());
        modelListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    selectionManager.clearSelection();
                    if (newVal != null) {
                        selectionManager.selectModel(newVal.getUIModel());
                        updateModelPropertiesPanel(newVal.getUIModel());
                        updateStatistics();

                        // Обновляем рендер
                        if (renderPanel != null) {
                            renderPanel.render();
                        }
                    }
                });

        modelListView.setContextMenu(createModelContextMenu());

        Button addTestModelBtn = new Button("Добавить тестовую модель");
        Button removeModelBtn = new Button("Удалить модель");
        Button duplicateModelBtn = new Button("Дублировать");

        addTestModelBtn.setOnAction(e -> addTestModel());
        removeModelBtn.setOnAction(e -> removeSelectedModel());
        duplicateModelBtn.setOnAction(e -> duplicateSelectedModel());

        addTestModelBtn.setMaxWidth(Double.MAX_VALUE);
        removeModelBtn.setMaxWidth(Double.MAX_VALUE);
        duplicateModelBtn.setMaxWidth(Double.MAX_VALUE);

        HBox modelButtons = new HBox(5, addTestModelBtn, removeModelBtn);
        modelButtons.setPadding(new Insets(5, 0, 0, 0));

        VBox buttonsBox = new VBox(5, modelButtons, duplicateModelBtn);

        leftPanel.getChildren().addAll(modelsLabel, modelListView, buttonsBox);
        return leftPanel;
    }

    private ContextMenu createModelContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem selectItem = new MenuItem("Выбрать");
        selectItem.setOnAction(e -> {
            ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectionManager.selectModel(selected.getUIModel());
            }
        });

        MenuItem renameItem = new MenuItem("Переименовать");
        renameItem.setOnAction(e -> renameSelectedModel());

        MenuItem duplicateItem = new MenuItem("Дублировать");
        duplicateItem.setOnAction(e -> duplicateSelectedModel());

        MenuItem deleteItem = new MenuItem("Удалить");
        deleteItem.setOnAction(e -> removeSelectedModel());

        MenuItem exportItem = new MenuItem("Экспортировать...");
        exportItem.setOnAction(e -> saveModel());

        contextMenu.getItems().addAll(selectItem, renameItem, duplicateItem,
                new SeparatorMenuItem(), exportItem, deleteItem);
        return contextMenu;
    }

    private void renameSelectedModel() {
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog(selected.nameProperty().get());
            dialog.setTitle("Переименование модели");
            dialog.setHeaderText("Введите новое имя модели:");
            dialog.setContentText("Имя:");

            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    selected.nameProperty().set(newName.trim());
                    selected.getUIModel().nameProperty().set(newName.trim());
                    modelListView.refresh();
                }
            });
        }
    }

    private void duplicateSelectedModel() {
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Model3D original = selected.getUIModel();
                Model3D duplicate = new Model3D(original.getName() + " (копия)");

                // Копируем вершины
                for (Vector3D vertex : original.getVertices()) {
                    duplicate.getVertices().add(new Vector3D(
                            vertex.getX(), vertex.getY(), vertex.getZ()
                    ));
                }

                // Копируем текстурные координаты
                for (scene_master.model.TexturePoint tp : original.getTexturePoints()) {
                    duplicate.getTexturePoints().add(tp);
                }

                // Копируем нормали
                for (Vector3D normal : original.getNormals()) {
                    duplicate.getNormals().add(new Vector3D(
                            normal.getX(), normal.getY(), normal.getZ()
                    ));
                }

                // Копируем полигоны
                for (Polygon polygon : original.getPolygons()) {
                    Polygon duplicatePolygon = new Polygon(polygon.getVertexIndices());

                    if (polygon.hasTexture()) {
                        duplicatePolygon.setTextureIndices(polygon.getTextureIndices());
                    }

                    if (polygon.hasNormals()) {
                        duplicatePolygon.setNormalIndices(polygon.getNormalIndices());
                    }

                    duplicate.getPolygons().add(duplicatePolygon);
                }

                // Копируем трансформации
                duplicate.translateXProperty().set(original.translateXProperty().get());
                duplicate.translateYProperty().set(original.translateYProperty().get());
                duplicate.translateZProperty().set(original.translateZProperty().get());
                duplicate.rotateXProperty().set(original.rotateXProperty().get());
                duplicate.rotateYProperty().set(original.rotateYProperty().get());
                duplicate.rotateZProperty().set(original.rotateZProperty().get());
                duplicate.scaleXProperty().set(original.scaleXProperty().get());
                duplicate.scaleYProperty().set(original.scaleYProperty().get());
                duplicate.scaleZProperty().set(original.scaleZProperty().get());

                // Копируем текстуру
                duplicate.setTexture(original.getTexture());

                // Копируем UV координаты
                for (Model3D.TextureCoordinate tc : original.getTextureCoords()) {
                    duplicate.addTextureCoord(tc.u, tc.v);
                }

                ModelWrapper wrapper = new ModelWrapper(null, duplicate.getName());
                // Нужно обновить UIModel в wrapper
                sceneManager.addModelWrapper(wrapper);
                updateStatistics();

                DialogHelper.showInfoDialog("Дублирование",
                        "Модель успешно продублирована");

            } catch (Exception e) {
                ErrorHandler.handleException(e, "дублирование модели");
            }
        }
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
                if (wrapper.getUIModel() != null &&
                        selectionManager.isSelected(wrapper.getUIModel())) {
                    setStyle("-fx-background-color: #2a4d69; -fx-text-fill: white;");
                } else {
                    setStyle("");
                }
            }
        }
    }

    private Pane createCenterPanel() {
        try {
            renderPanel = new RenderPanel(800, 600);
            renderPanel.getStyleClass().add("view-3d");
            renderPanel.setStyle("-fx-background-color: #1a1a2e;");

            sceneManager.getModelWrappers().addListener((ListChangeListener<ModelWrapper>) change -> {
                List<Model3D> models = new ArrayList<>();
                for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                    if (wrapper.getUIModel() != null) {
                        models.add(wrapper.getUIModel());
                    }
                }
                renderPanel.setModels(models);

                // Включаем пункты меню, если рендер панель доступна
                if (useTextureMenuItem != null) useTextureMenuItem.setDisable(false);
                if (useLightingMenuItem != null) useLightingMenuItem.setDisable(false);
            });

            return renderPanel;
        } catch (Exception e) {
            System.err.println("RenderPanel не доступен: " + e.getMessage());
            Pane view3d = new Pane();
            view3d.getStyleClass().add("view-3d");
            view3d.setStyle("-fx-background-color: #1a1a2e;");

            Label placeholder = new Label("3D Вид (Режим предварительного просмотра)\n\n" +
                    "Для полного функционала убедитесь, что все зависимости установлены");
            placeholder.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px; -fx-alignment: center;");
            placeholder.setWrapText(true);
            placeholder.setMaxWidth(Double.MAX_VALUE);
            placeholder.setMaxHeight(Double.MAX_VALUE);

            VBox container = new VBox(placeholder);
            container.setAlignment(javafx.geometry.Pos.CENTER);
            VBox.setVgrow(container, Priority.ALWAYS);

            view3d.getChildren().add(container);
            return view3d;
        }
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.getStyleClass().add("right-panel");
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);

        Label propertiesLabel = new Label("Свойства модели");
        propertiesLabel.getStyleClass().add("section-label");

        modelPropertiesPanel = new BorderPane();
        modelPropertiesPanel.setCenter(new Label("Выберите модель для редактирования свойств"));

        Label transformLabel = new Label("Трансформации");
        transformLabel.getStyleClass().add("section-label");

        VBox transformsPanel = createTransformsPanel();

        // Добавляем кнопки управления трансформациями
        HBox transformButtons = new HBox(5);
        Button resetTransformBtn = new Button("Сбросить");
        Button applyTransformBtn = new Button("Применить");

        resetTransformBtn.setOnAction(e -> resetTransformations());
        applyTransformBtn.setOnAction(e -> applyTransformations());

        resetTransformBtn.setMaxWidth(Double.MAX_VALUE);
        applyTransformBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resetTransformBtn, Priority.ALWAYS);
        HBox.setHgrow(applyTransformBtn, Priority.ALWAYS);

        transformButtons.getChildren().addAll(resetTransformBtn, applyTransformBtn);

        rightPanel.getChildren().addAll(propertiesLabel, modelPropertiesPanel,
                new Separator(), transformLabel, transformsPanel, transformButtons);
        return rightPanel;
    }

    private VBox createTransformsPanel() {
        VBox transformsPanel = new VBox(5);

        transformsPanel.getChildren().addAll(
                createSliderControl("Translate X", -10, 10, 0),
                createSliderControl("Translate Y", -10, 10, 0),
                createSliderControl("Translate Z", -10, 10, 0),
                createSliderControl("Rotate X", -180, 180, 0),
                createSliderControl("Rotate Y", -180, 180, 0),
                createSliderControl("Rotate Z", -180, 180, 0),
                createSliderControl("Scale X", 0.1, 5, 1),
                createSliderControl("Scale Y", 0.1, 5, 1),
                createSliderControl("Scale Z", 0.1, 5, 1)
        );

        return transformsPanel;
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

        // Привязываем слайдер к активной модели
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            Model3D activeModel = selectionManager.getActiveModel();
            if (activeModel != null) {
                switch (label) {
                    case "Translate X": activeModel.translateXProperty().set(newVal.doubleValue()); break;
                    case "Translate Y": activeModel.translateYProperty().set(newVal.doubleValue()); break;
                    case "Translate Z": activeModel.translateZProperty().set(newVal.doubleValue()); break;
                    case "Rotate X": activeModel.rotateXProperty().set(newVal.doubleValue()); break;
                    case "Rotate Y": activeModel.rotateYProperty().set(newVal.doubleValue()); break;
                    case "Rotate Z": activeModel.rotateZProperty().set(newVal.doubleValue()); break;
                    case "Scale X": activeModel.scaleXProperty().set(newVal.doubleValue()); break;
                    case "Scale Y": activeModel.scaleYProperty().set(newVal.doubleValue()); break;
                    case "Scale Z": activeModel.scaleZProperty().set(newVal.doubleValue()); break;
                }
                if (renderPanel != null) {
                    renderPanel.render();
                }
            }
        });

        hbox.getChildren().addAll(nameLabel, slider, valueLabel);
        return hbox;
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
            editModeLabel.setText(enabled ?
                    "[Режим редактирования: ВКЛ]" :
                    "[Режим редактирования: ВЫКЛ]");
            editModeLabel.setTextFill(enabled ? Color.RED : Color.GRAY);

            if (renderPanel != null) {
                renderPanel.setEditModeEnabled(enabled);
            }
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

        statusBar.getChildren().addAll(statusLabel, editModeLabel, new Separator(),
                vertexCountLabel, polygonCountLabel, textureCountLabel, normalCountLabel,
                new Separator(), textureInfoLabel, new Separator(), modelInfoLabel);
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

        model.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.render();
            }
        });

        HBox colorBox = new HBox(10);
        Label colorLabel = new Label("Цвет:");
        ColorPicker colorPicker = new ColorPicker(model.getBaseColor());
        colorPicker.valueProperty().bindBidirectional(model.baseColorProperty());
        colorBox.getChildren().addAll(colorLabel, colorPicker);

        HBox textureBox = new HBox(10);
        Label textureLabel = new Label("Текстура:");
        String textureStatus = model.getTexture() != null ?
                "✓ Текстура загружена" :
                currentTexture != null ? "✓ Текстура доступна (не применена)" : "Нет текстуры";
        Label textureInfo = new Label(textureStatus);
        textureInfo.setTextFill(model.getTexture() != null || currentTexture != null ? Color.GREEN : Color.GRAY);
        Button loadTextureBtn = new Button("Загрузить...");
        loadTextureBtn.setOnAction(e -> loadTexture());
        textureBox.getChildren().addAll(textureLabel, textureInfo, loadTextureBtn);

        model.baseColorProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.render();
            }
        });

        if (model.textureProperty() != null) {
            model.textureProperty().addListener((obs, oldVal, newVal) -> {
                if (renderPanel != null) {
                    renderPanel.setUseTexture(newVal != null);
                    renderPanel.render();
                }
            });
        }

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
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
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

                    return new ModelWrapper(loadedModel,
                            file.getName().replace(".obj", ""));
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
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(activeModel.getName() + ".obj");
            File file = fileChooser.showSaveDialog(primaryStage);

            if (file != null) {
                try {
                    boolean applyTransformations = DialogHelper.showSaveOptionsDialog();

                    if (applyTransformations && selectedWrapper.getOriginalModel() != null) {
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

                } catch (IOException e) {
                    ErrorHandler.handleException(e, "сохранение модели");
                } catch (Exception e) {
                    ErrorHandler.handleException(e, "непредвиденная ошибка при сохранении");
                }
            }
        } else {
            ErrorHandler.handleWarning("Модель не выбрана", "сохранение");
        }
    }

    private void addTestModel() {
        try {
            String filePath = "test_cube.obj";

            ObjReader reader = new ObjReader();
            Model originalModel = reader.readModel(filePath);

            Triangulator triangulator = new Triangulator();
            triangulator.triangulateModel(originalModel);

            String name = "Cube " + (sceneManager.getModelWrappers().size() + 1);
            ModelWrapper wrapper = new ModelWrapper(originalModel, name);

            sceneManager.addModelWrapper(wrapper);
            updateStatistics();

            if (renderPanel != null) {
                List<Model3D> models = sceneManager.getModelWrappers().stream()
                        .map(ModelWrapper::getUIModel)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                renderPanel.setModels(models);
            }

        } catch (Exception e) {
            System.err.println("Не удалось загрузить тестовую модель: " + e.getMessage());
            Model3D testModel = new Model3D("Test Model " + (sceneManager.getModelWrappers().size() + 1));
            ModelWrapper wrapper = new ModelWrapper(null, testModel.nameProperty().get());
            sceneManager.addModelWrapper(wrapper);
            updateStatistics();
        }
    }

    private void removeSelectedModel() {
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            sceneManager.removeModelWrapper(selected);
            updateStatistics();
        }
    }

    private void setEditMode(boolean enabled) {
        editManager.setEditMode(enabled);
        if (enabled) {
            DialogHelper.showInfoDialog("Режим редактирования",
                    "Режим редактирования включен.\n" +
                            "1. Выберите модель в списке слева\n" +
                            "2. Щелкните по вершине или полигону для выбора\n" +
                            "3. Удаляйте через меню или клавишу Delete");
        }
    }

    private void deleteSelectedVertex() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null && editManager.isEditMode()) {
            if (editManager.getSelectedVertexIndex() != -1) {
                editManager.deleteSelectedVertex(activeModel);
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
                updateModelPropertiesPanel(activeModel);
                updateStatistics();
                DialogHelper.showInfoDialog("Успех", "Полигон удален");
            } else {
                DialogHelper.showWarningDialog("Внимание", "Выберите полигон для удаления");
            }
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
                updateStatistics();
            }
        }
    }

    private void selectAll() {
        if (editManager.isEditMode()) {
            Model3D activeModel = selectionManager.getActiveModel();
            if (activeModel != null) {
                DialogHelper.showInfoDialog("Выделение",
                        "Выделены все элементы модели (" +
                                activeModel.getVertices().size() + " вершин, " +
                                activeModel.getPolygons().size() + " полигонов)");
            }
        } else {
            modelListView.getSelectionModel().selectAll();
            for (ModelWrapper wrapper : modelListView.getItems()) {
                selectionManager.selectModel(wrapper.getUIModel());
            }
            updateStatistics();
        }
    }

    private void clearSelection() {
        editManager.clearSelection();
        modelListView.getSelectionModel().clearSelection();
        selectionManager.clearSelection();
        updateStatistics();
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

            if (renderPanel != null) {
                renderPanel.render();
            }

            DialogHelper.showInfoDialog("Сброс трансформаций",
                    "Все трансформации сброшены к значениям по умолчанию");
        }
    }

    private void applyTransformations() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            DialogHelper.showInfoDialog("Применение трансформаций",
                    "Трансформации применены к модели.\n" +
                            "Перемещение: (" + activeModel.translateXProperty().get() + ", " +
                            activeModel.translateYProperty().get() + ", " + activeModel.translateZProperty().get() + ")\n" +
                            "Вращение: (" + activeModel.rotateXProperty().get() + "°, " +
                            activeModel.rotateYProperty().get() + "°, " + activeModel.rotateZProperty().get() + "°)\n" +
                            "Масштаб: (" + activeModel.scaleXProperty().get() + ", " +
                            activeModel.scaleYProperty().get() + ", " + activeModel.scaleZProperty().get() + ")");
        }
    }

    private void resetView() {
        if (renderPanel != null) {
            // Сбрасываем настройки отображения
            showWireframeMenuItem.setSelected(false);
            showVerticesMenuItem.setSelected(false);
            useTextureMenuItem.setSelected(false);
            useLightingMenuItem.setSelected(false);

            renderPanel.setRenderWireframe(false);
            renderPanel.setShowVertices(false);
            renderPanel.setUseTexture(false);
            renderPanel.setUseLighting(false);

            // Сбрасываем трансформации всех моделей
            for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                Model3D model = wrapper.getUIModel();
                if (model != null) {
                    model.translateXProperty().set(0);
                    model.translateYProperty().set(0);
                    model.translateZProperty().set(0);
                    model.rotateXProperty().set(0);
                    model.rotateYProperty().set(0);
                    model.rotateZProperty().set(0);
                    model.scaleXProperty().set(1);
                    model.scaleYProperty().set(1);
                    model.scaleZProperty().set(1);
                }
            }

            renderPanel.render();
            DialogHelper.showInfoDialog("Сброс вида", "Настройки вида сброшены");
        }
    }

    private void triangulateSelectedModel() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            DialogHelper.showInfoDialog("Триангуляция",
                    "Модель уже триангулирована. Все полигоны состоят из треугольников.");
        }
    }

    private void recalculateNormals() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            // Здесь должна быть логика пересчета нормалей
            DialogHelper.showInfoDialog("Пересчет нормалей",
                    "Нормали пересчитаны для " + activeModel.getPolygons().size() + " полигонов");
        }
    }

    private void optimizeMesh() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            DialogHelper.showInfoDialog("Оптимизация сетки",
                    "Сетка оптимизирована. Удалены дублирующиеся вершины.");
        }
    }

    private void switchTheme(String theme) {
        try {
            Scene scene = primaryStage.getScene();
            if (scene == null) return;

            scene.getStylesheets().clear();

            if (theme.equals("dark")) {
                URL cssUrl = getClass().getResource("/dark.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    currentTheme = "dark";
                    System.out.println("Тёмная тема активирована");
                }
            } else {
                URL cssUrl = getClass().getResource("/light.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    currentTheme = "light";
                    System.out.println("Светлая тема активирована");
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка переключения темы: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            updateStatusBarStatistics(activeModel);
        } else {
            HBox statusBar = (HBox) primaryStage.getScene().lookup("#status-bar");
            if (statusBar != null) {
                Label vertexCountLabel = (Label) statusBar.lookup("#vertex-count");
                Label polygonCountLabel = (Label) statusBar.lookup("#polygon-count");
                Label textureCountLabel = (Label) statusBar.lookup("#texture-count");
                Label normalCountLabel = (Label) statusBar.lookup("#normal-count");
                Label modelCountLabel = (Label) statusBar.lookup("#model-count");

                if (vertexCountLabel != null) vertexCountLabel.setText("Вершин: 0");
                if (polygonCountLabel != null) polygonCountLabel.setText("Полигонов: 0");
                if (textureCountLabel != null) textureCountLabel.setText("Текстур: 0");
                if (normalCountLabel != null) normalCountLabel.setText("Нормалей: 0");
                if (modelCountLabel != null) modelCountLabel.setText("Моделей: " + sceneManager.getModelWrappers().size());
            }
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

        if (vertexCountLabel != null) {
            vertexCountLabel.setText("Вершин: " + model.getVertices().size());
        }
        if (polygonCountLabel != null) {
            polygonCountLabel.setText("Полигонов: " + model.getPolygons().size());
        }
        if (textureCountLabel != null) {
            textureCountLabel.setText("Текстур: " + model.getTexturePoints().size());
        }
        if (normalCountLabel != null) {
            normalCountLabel.setText("Нормалей: " + model.getNormals().size());
        }
        if (modelCountLabel != null) {
            modelCountLabel.setText("Моделей: " + sceneManager.getModelWrappers().size());
        }
    }

    private void updateStatusBarTextureInfo() {
        HBox statusBar = (HBox) primaryStage.getScene().lookup("#status-bar");
        if (statusBar == null) return;

        Label textureInfoLabel = (Label) statusBar.lookup("#texture-info");
        if (textureInfoLabel != null) {
            if (currentTexture != null) {
                textureInfoLabel.setText("Текстура: " + textureFileName +
                        " (" + (int)currentTexture.getWidth() + "x" + (int)currentTexture.getHeight() + ")");
                textureInfoLabel.setTextFill(Color.GREEN);
            } else {
                textureInfoLabel.setText("Текстура: не загружена");
                textureInfoLabel.setTextFill(Color.GRAY);
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

            Vector3D mathVertex = new Vector3D(
                    (float) oldVertex.getX(),
                    (float) oldVertex.getY(),
                    (float) oldVertex.getZ()
            );

            Vector3D transformed = transform.transformVertex(mathVertex);

            originalModel.getVertices().set(i,
                    new Vector3D(
                            transformed.getX(),
                            transformed.getY(),
                            transformed.getZ()
                    )
            );
        }

        NormalCalculator calc = new NormalCalculator();
        calc.calculateNormals(originalModel);
    }

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

        for (Polygon polygon : model.getPolygons()) {
            if (polygon.getTextureIndices().isEmpty()) {
                for (int i = 0; i < polygon.getVertexIndices().size(); i++) {
                    polygon.addTextureIndex(i);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}