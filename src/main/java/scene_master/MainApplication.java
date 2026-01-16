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
import javafx.scene.paint.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class MainApplication extends Application {

    private Stage primaryStage; // ссылка на главное окно
    private SceneManager sceneManager; // управление моделями на сцене
    private SelectionManager selectionManager; // управление выделением
    private ListView<ModelWrapper> modelListView; // список моделей в ui
    private BorderPane modelPropertiesPanel; // панель свойств модели
    private EditManager editManager = new EditManager();
    private String currentTheme = "dark";
    private RenderPanel renderPanel;
    private Stage loadingStage;

    @Override
    public void start(Stage primaryStage) { // точка входа приложения
        this.primaryStage = primaryStage; // сохраняем ссылку на окно
        this.selectionManager = new SelectionManager(); // создаем менеджер выделения
        this.sceneManager = new SceneManager(selectionManager); //создаем менеджер сцены

        BorderPane root = new BorderPane(); // главный контейнер (распределяет элементы по сторонам)
        root.getStyleClass().add("root");

        root.setTop(createMenuBar()); // меню сверху
        root.setLeft(createLeftPanel()); // список моделей слева
        root.setCenter(createCenterPanel()); // область 3d-отображения по центру
        root.setBottom(createStatusBar()); // строка состояния снизу
        root.setRight(createRightPanel()); // свойства модели справа

        Scene scene = new Scene(root, 1200, 800); // создаем сцену

        switchTheme("dark");// загружаем тему по умолчанию

        primaryStage.setTitle("Редактор 3D моделей"); // заголовок окна
        primaryStage.setScene(scene); // устанавливаем сцену в окно
        primaryStage.show(); // показываем окно

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelected();
                event.consume();
            }
        });
    }

    private MenuBar createMenuBar() { // создает строку меню
        MenuBar menuBar = new MenuBar(); // контейнер для меню

        Menu fileMenu = new Menu("Файл"); // выпадающее меню
        MenuItem openItem = new MenuItem("Открыть модель...");
        MenuItem loadTextureItem = new MenuItem("Загрузить текстуру...");
        MenuItem saveItem = new MenuItem("Сохранить модель как...");
        MenuItem exitItem = new MenuItem("Выход");

        openItem.setOnAction(e -> openModel()); // обработчик нажатия - открыть модель
        loadTextureItem.setOnAction(e -> loadTexture()); // обработчик нажатия - загрузить текстуру
        saveItem.setOnAction(e -> saveModel()); // обработчик нажатия - сохранить модель
        exitItem.setOnAction(e -> primaryStage.close());//обработчик нажатия - закрыть приложение

        fileMenu.getItems().addAll(openItem, loadTextureItem, saveItem, new SeparatorMenuItem(), exitItem); // добавляем пункты в меню

        Menu editMenu = new Menu("Редактировать");
        CheckMenuItem editModeItem = new CheckMenuItem("Режим редактирования");
        editModeItem.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
        editModeItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            setEditMode(newVal);
        });
        MenuItem deleteItem = new MenuItem("Удалить выделенное");
        deleteItem.setOnAction(e -> deleteSelected()); // обработчик удаления
        MenuItem deleteVertexItem = new MenuItem("Удалить вершину");
        MenuItem deletePolygonItem = new MenuItem("Удалить полигон");
        deleteVertexItem.setOnAction(e -> deleteSelectedVertex());
        deletePolygonItem.setOnAction(e -> deleteSelectedPolygon());

        editMenu.getItems().addAll(editModeItem, new SeparatorMenuItem(),
                deleteVertexItem, deletePolygonItem, new SeparatorMenuItem(), deleteItem);// собираем меню редактирования

        Menu viewMenu = new Menu("Вид");
        CheckMenuItem showWireframe = new CheckMenuItem("Показать каркас");
        CheckMenuItem showVertices = new CheckMenuItem("Показать вершины");
        CheckMenuItem useTextureItem = new CheckMenuItem("Использовать текстуру");
        CheckMenuItem useLightingItem = new CheckMenuItem("Использовать освещение");
        MenuItem darkThemeItem = new MenuItem("Тёмная тема");
        MenuItem lightThemeItem = new MenuItem("Светлая тема");

        useTextureItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.setUseTexture(newVal);
            }
        });

        selectionManager.getSelectedModels().addListener((ListChangeListener<Model3D>) c -> {
            boolean hasTexture = selectionManager.getActiveModel() != null &&
                    selectionManager.getActiveModel().getTexture() != null;
            useTextureItem.setDisable(!hasTexture);
        });

        useLightingItem.setDisable(true); // пока недоступно

        darkThemeItem.setOnAction(e -> switchTheme("dark"));
        lightThemeItem.setOnAction(e -> switchTheme("light"));

        viewMenu.getItems().addAll(showWireframe, showVertices, useTextureItem, useLightingItem,
                new SeparatorMenuItem(), darkThemeItem, lightThemeItem); // собираем меню вида

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu); // добавляем все меню в строку меню
        return menuBar; // возвращаем созданную строку меню
    }

    private void loadTexture() { // загрузка текстуры из файла
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
                    createDefaultUVCoordinates(activeModel);
                }

                if (renderPanel != null) {
                    renderPanel.setUseTexture(true);
                    renderPanel.render();
                }

                updateModelPropertiesPanel(activeModel);
                showLoadingIndicator(false);

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

    private VBox createLeftPanel() { // создает левую панель со списком моделей
        VBox leftPanel = new VBox(10); // вертикальный контейнер с отступом 10px между элементами
        leftPanel.getStyleClass().add("left-panel");
        leftPanel.setPadding(new Insets(10)); // внутренние отступы 10px со всех сторон

        Label modelsLabel = new Label("Модели");
        modelsLabel.getStyleClass().add("section-label");

        modelListView = new ListView<>(); // список моделей (виджет)
        modelListView.setItems(sceneManager.getModelWrappers());// привязываем данные из sceneManager
        modelListView.setCellFactory(lv -> new ModelListCell()); // настраиваем отображение элементов списка
        modelListView.getSelectionModel().selectedItemProperty().addListener( //слушатель изменения выделения
                (obs, oldVal, newVal) -> {
                    selectionManager.clearSelection(); // очищаем предыдущее выделение
                    if (newVal != null) { // если выбран новый элемент (не null)
                        selectionManager.selectModel(newVal.getUIModel()); // выделяем модель в selectionManager
                        updateModelPropertiesPanel(newVal.getUIModel()); // обновляем панель свойств
                    }
                });

        Button addTestModelBtn = new Button("Добавить тестовую модель");
        Button removeModelBtn = new Button("Удалить модель");
        addTestModelBtn.setOnAction(e -> addTestModel()); // обработчик добавления
        removeModelBtn.setOnAction(e -> removeSelectedModel()); // обработчик удаления

        HBox modelButtons = new HBox(5, addTestModelBtn, removeModelBtn); // горизонтальный контейнер для кнопок

        leftPanel.getChildren().addAll(modelsLabel, modelListView, modelButtons); // собираем все элементы панели
        return leftPanel; // возвращаем готовую панель
    }

    private class ModelListCell extends ListCell<ModelWrapper> { // кастомная ячейка для списка моделей
        @Override
        protected void updateItem(ModelWrapper wrapper, boolean empty) { // вызывается при обновлении элемента
            super.updateItem(wrapper, empty); // вызываем родительский метод
            if (empty || wrapper == null) { // если ячейка пустая или данные null
                setText(null); // очищаем текст
                setStyle(""); // сбрасываем стили
            } else {
                setText(wrapper.nameProperty().get()); // устанавливаем имя модели как текст
                if (wrapper.getUIModel() != null && // если есть ui-модель
                        selectionManager.isSelected(wrapper.getUIModel())) { // и она выделена
                    setStyle("-fx-background-color: #2a4d69; -fx-text-fill: white;"); // подсвечиваем синим
                } else {
                    setStyle(""); // иначе обычный стиль
                }
            }
        }
    }

    private Pane createCenterPanel() { // создает центральную панель для 3d-отображения
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
        });

        return renderPanel;
    }

    private VBox createRightPanel() { // создает правую панель свойств
        VBox rightPanel = new VBox(10); // вертикальный контейнер
        rightPanel.getStyleClass().add("right-panel");
        rightPanel.setPadding(new Insets(10)); // отступы
        rightPanel.setPrefWidth(300); //  ширина 300px

        Label propertiesLabel = new Label("Свойства модели");
        propertiesLabel.getStyleClass().add("section-label");

        modelPropertiesPanel = new BorderPane(); // контейнер для свойств (пока пустой)
        modelPropertiesPanel.setCenter(new Label("Select a model to edit properties")); // инструкция

        Label transformLabel = new Label("Трансформации");
        transformLabel.getStyleClass().add("section-label");

        VBox transformsPanel = createTransformsPanel(); // создаем панель с ползунками трансформаций

        rightPanel.getChildren().addAll(propertiesLabel, modelPropertiesPanel, // собираем все элементы
                new Separator(), transformLabel, transformsPanel); // разделитель и трансформации
        return rightPanel; // возвращаем готовую панель
    }

    private VBox createTransformsPanel() { // создает панель с ползунками для трансформаций
        VBox transformsPanel = new VBox(5); // вертикальный контейнер

        transformsPanel.getChildren().addAll( // добавляем все ползунки
                createSliderControl("Translate X", -10, 10, 0), // перемещение по X
                createSliderControl("Translate Y", -10, 10, 0), // перемещение по Y
                createSliderControl("Translate Z", -10, 10, 0), // перемещение по Z
                createSliderControl("Rotate X", -180, 180, 0), // вращение вокруг X
                createSliderControl("Rotate Y", -180, 180, 0), // вращение вокруг Y
                createSliderControl("Rotate Z", -180, 180, 0), // вращение вокруг Z
                createSliderControl("Scale X", 0.1, 5, 1), // масштабирование по X
                createSliderControl("Scale Y", 0.1, 5, 1), // масштабирование по Y
                createSliderControl("Scale Z", 0.1, 5, 1) // масштабирование по Z
        );

        return transformsPanel; // возвращаем панель
    }

    private HBox createSliderControl(String label, double min, double max, double initial) { // создает один ползунок
        HBox hbox = new HBox(10); // горизонтальный контейнер для метки и ползунка
        Label nameLabel = new Label(label); // метка (например, "Translate X")
        nameLabel.setPrefWidth(80); // фиксированная ширина метки

        Slider slider = new Slider(min, max, initial); // создаем ползунок с диапазоном и начальным значением
        slider.setShowTickLabels(true); // показываем метки значений
        slider.setShowTickMarks(true); // показываем деления
        slider.setMajorTickUnit((max - min) / 4); // шаг делений

        Label valueLabel = new Label(String.format("%.1f", initial)); // метка с текущим значением
        slider.valueProperty().addListener((obs, oldVal, newVal) -> // слушатель изменения значения
                valueLabel.setText(String.format("%.1f", newVal))); // обновляем метку

        hbox.getChildren().addAll(nameLabel, slider, valueLabel); // собираем элементы
        return hbox; // возвращаем готовый контрол
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5));
        statusBar.setId("status-bar"); // ID для поиска

        Label statusLabel = new Label("Готово");
        statusLabel.setId("status-label");

        Label editModeLabel = new Label("[Режим редактирования: ВЫКЛ]");
        editModeLabel.setId("edit-mode-label");
        editModeLabel.setTextFill(Color.GRAY);

        editManager.setEditModeListener(enabled -> {// обновляем метку при изменении режима
            editModeLabel.setText(enabled ?
                    "[Режим редактирования: ВКЛ]" :
                    "[Режим редактирования: ВЫКЛ]");
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

        statusBar.getChildren().addAll(statusLabel, editModeLabel, new Separator(),
                vertexCountLabel, polygonCountLabel, textureCountLabel, normalCountLabel,
                new Separator(), textureInfoLabel);
        return statusBar;
    }

    private void updateModelPropertiesPanel(Model3D model) { // обновляет панель свойств для выбранной модели
        VBox properties = new VBox(10); // вертикальный контейнер для свойств

        HBox nameBox = new HBox(10); // контейнер для имени
        Label nameLabel = new Label("Имя:");
        TextField nameField = new TextField(model.nameProperty().get()); // поле ввода имени
        nameField.textProperty().bindBidirectional(model.nameProperty()); // двусторонняя привязка к свойству модели
        nameBox.getChildren().addAll(nameLabel, nameField); // собираем

        CheckBox visibleCheck = new CheckBox("Видима");
        visibleCheck.selectedProperty().bindBidirectional(model.visibleProperty()); // привязка к свойству видимости

        model.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.render();
            }
        });

        HBox colorBox = new HBox(10); // контейнер для выбора цвета
        Label colorLabel = new Label("Цвет:");
        ColorPicker colorPicker = new ColorPicker(model.getBaseColor());
        colorPicker.valueProperty().bindBidirectional(model.baseColorProperty());
        colorBox.getChildren().addAll(colorLabel, colorPicker);

        model.baseColorProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.render();
            }
        });

        model.textureProperty().addListener((obs, oldVal, newVal) -> {
            if (renderPanel != null) {
                renderPanel.setUseTexture(newVal != null);
                renderPanel.render();
            }
        });

        HBox textureBox = new HBox(10);// информация о текстуре
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
                model.getVertices().size(), // количество вершин
                model.getTexturePoints().size(), // количество текстурных координат
                model.getNormals().size(), // количество нормалей
                model.getPolygons().size() // количество полигонов
        ));
        statsLabel.setWrapText(true);
        statsLabel.setStyle("-fx-font-size: 12px;");

        properties.getChildren().addAll(nameBox, visibleCheck, colorBox, textureBox, statsLabel); // собираем все свойства
        modelPropertiesPanel.setCenter(properties); // устанавливаем свойства в центр панели
    }

    private void openModel() { // открытие модели из файла
        FileChooser fileChooser = new FileChooser(); // диалог выбора файла
        fileChooser.getExtensionFilters().add( // фильтр по расширению .obj
                new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home"))); // начальная директория - домашняя
        File file = fileChooser.showOpenDialog(primaryStage); // показываем диалог

        if (file != null) { // если файл выбран
            showLoadingIndicator(true);

            Task<ModelWrapper> loadTask = new Task<>() {
                @Override
                protected ModelWrapper call() throws Exception {
                    ObjReader objReader = new ObjReader();
                    Model loadedModel = objReader.readModel(file.getAbsolutePath());

                    return new ModelWrapper(loadedModel,
                            file.getName().replace(".obj", ""));
                }
            };

            loadTask.setOnSucceeded(event -> {
                ModelWrapper wrapper = loadTask.getValue();
                sceneManager.addModelWrapper(wrapper);
                showLoadingIndicator(false);

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

    private void saveModel() { // сохранение модели в файл
        Model3D activeModel = selectionManager.getActiveModel(); // получаем активную модель
        if (activeModel != null) { // если есть что сохранять
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

            FileChooser fileChooser = new FileChooser(); // диалог сохранения
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(activeModel.getName() + ".obj");
            File file = fileChooser.showSaveDialog(primaryStage); // показываем диалог сохранения

            if (file != null) {
                try {
                    boolean applyTransformations = DialogHelper.showSaveOptionsDialog();// спрашиваем пользователя, как сохранять

                    if (applyTransformations) {
                        applyTransformationsToOriginalModel(selectedWrapper);
                        selectedWrapper.updateUIModel(); // синхронизируем UI
                    }

                    ObjWriter objWriter = new ObjWriter();// создаём и используем ObjWriter
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
        } else { // если нет активной модели
            ErrorHandler.handleWarning("Модель не выбрана", "сохранение");
        }
    }

    private void addTestModel() { // добавление тестовой модели
        try {
            String filePath = "C:/Users/Александр/Desktop/for3person/LIA_team/src/test/test_cube.obj";

            ObjReader reader = new ObjReader();
            Model originalModel = reader.readModel(filePath);

            Triangulator triangulator = new Triangulator();
            triangulator.triangulateModel(originalModel);

            String name = "Cube from file " + (sceneManager.getModelWrappers().size() + 1);
            ModelWrapper wrapper = new ModelWrapper(originalModel, name);

            sceneManager.addModelWrapper(wrapper);

            if (renderPanel != null) {
                List<Model3D> models = sceneManager.getModelWrappers().stream()
                        .map(ModelWrapper::getUIModel)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                renderPanel.setModels(models);
            }

        } catch (Exception e) {
            ErrorHandler.handleException(e, "загрузка тестовой модели");
        }
    }

    private void removeSelectedModel() { // удаление выбранной модели
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem(); // получаем выбранную модель
        if (selected != null) { // если что-то выбрано
            sceneManager.removeModelWrapper(selected); // удаляем из сцены
        }
    }

    private void setEditMode(boolean enabled) {// метод для установки режима редактирования
        editManager.setEditMode(enabled);
        if (enabled) {
            DialogHelper.showInfoDialog("Режим редактирования",
                    "Режим редактирования включен.\n" +
                            "1. Включите 3D-вид (когда будет готов)\n" +
                            "2. Кликните на вершину/полигон для выбора\n" +
                            "3. Удалите через меню или клавишу Delete");
        }
    }

    private void deleteSelectedVertex() { // методы удаления
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null && editManager.isEditMode()) {
            if (editManager.getSelectedVertexIndex() != -1) {
                editManager.deleteSelectedVertex(activeModel);
                updateModelPropertiesPanel(activeModel);
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
                DialogHelper.showInfoDialog("Успех", "Полигон удалена");
            } else {
                DialogHelper.showWarningDialog("Внимание", "Выберите полигон для удаления");
            }
        }
    }

    private void deleteSelected() {
        if (editManager.isEditMode()) {
            if (editManager.getSelectedVertexIndex() != -1) {// в режиме редактирования удаляем выбранный элемент
                deleteSelectedVertex();
            } else if (editManager.getSelectedPolygonIndex() != -1) {
                deleteSelectedPolygon();
            }
        } else {
            ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem();// в обычном режиме удаляем модель
            if (selected != null) {
                sceneManager.removeModelWrapper(selected);
            }
        }
    }

    private void switchTheme(String theme) {// метод переключения темы
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

    private void updateStatistics() { // обновляет статистику в статус баре
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
    }

    private void applyTransformationsToOriginalModel(ModelWrapper wrapper) {
        Model3D uiModel = wrapper.getUIModel();
        Model originalModel = wrapper.getOriginalModel();

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

    /**
     * Создаёт UV-координаты по умолчанию для модели (плоская проекция по XY)
     */
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

    public static void main(String[] args) { // точка входа для запуска (командная строка)
        launch(args); // запуск приложения
    }
}