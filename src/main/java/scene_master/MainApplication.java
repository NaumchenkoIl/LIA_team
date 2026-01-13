package scene_master;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import scene_master.calculator.NormalCalculator;
import scene_master.manager.SceneManager; // менеджер сцены с моделями
import scene_master.manager.SelectionManager; // менеджер выделения моделей
import scene_master.model.*;
import scene_master.reader.ObjReader; // загрузчик obj-файлов
import scene_master.renderer.TextureManager;
import scene_master.util.DialogHelper; // помощник для диалоговых окон
import javafx.application.Application; // базовый класс javaFX приложения
import javafx.geometry.Insets; // отступы для интерфейса
import javafx.scene.Scene; // сцена
import javafx.scene.control.*; // элементы управления
import javafx.scene.layout.*; // контейнеры для размещения элементов
import javafx.stage.FileChooser; // диалог выбора файлов
import javafx.stage.Stage; // главное окно приложения
import scene_master.util.TextureLoader;
import scene_master.renderer.RenderPanel;
import scene_master.writer.ObjWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainApplication extends Application {

    private Stage primaryStage; // ссылка на главное окно
    private SceneManager sceneManager; // управление моделями на сцене
    private SelectionManager selectionManager; // управление выделением
    private ListView<ModelWrapper> modelListView; // список моделей в ui
    private BorderPane modelPropertiesPanel; // панель свойств модели
    private RenderPanel renderPanel;

    @Override
    public void start(Stage primaryStage) { // точка входа приложения
        this.primaryStage = primaryStage; // сохраняем ссылку на окно
        this.selectionManager = new SelectionManager(); // создаем менеджер выделения
        this.sceneManager = new SceneManager(selectionManager); //сздаем менеджер сцены

        BorderPane root = new BorderPane(); // главный контейнер (распределяет элементы по сторонам)
        root.getStyleClass().add("root");

        renderPanel = new RenderPanel(800, 600);
        renderPanel.setBackgroundColor(Color.valueOf("#1a1a2e"));

        root.setTop(createMenuBar()); // меню сверху
        root.setLeft(createLeftPanel()); // список моделей слева
        root.setCenter(createCenterPanel()); // область 3d-отображения по центру
        root.setBottom(createStatusBar()); // строка состояния снизу
        root.setRight(createRightPanel()); // свойства модели справа

        Scene scene = new Scene(root, 1200, 800); // создаем сцену
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
// Сразу добавляем тестовую модель при запуске
        Platform.runLater(() -> {
            addTestModel(); // Добавляем пирамиду
        });
        primaryStage.setTitle("Редактор 3D моделей"); // заголовок окна
        primaryStage.setScene(scene); // устанавливаем сцену в окно
        primaryStage.show(); // показываем окно
    }

    private MenuBar createMenuBar() { // создает строку меню
        MenuBar menuBar = new MenuBar(); // контейнер для меню

        Menu fileMenu = new Menu("Файл"); // выпадающее меню
        MenuItem openItem = new MenuItem("Открыть модель...");
        MenuItem saveItem = new MenuItem("Сохранить модель как...");
        MenuItem exitItem = new MenuItem("Выход");

        openItem.setOnAction(e -> openModel()); // обработчик нажатия - открыть модель
        saveItem.setOnAction(e -> saveModel()); // обработчик нажатия - сохранить модель
        exitItem.setOnAction(e -> primaryStage.close());//обработчик нажатия - закрыть приложение

        fileMenu.getItems().addAll(openItem, saveItem, new SeparatorMenuItem(), exitItem); // добавляем пункты в меню

        Menu editMenu = new Menu("Редактировать");
        CheckMenuItem editModeItem = new CheckMenuItem("Режим редактирования");
        MenuItem deleteItem = new MenuItem("Удалить выделенное");
        deleteItem.setOnAction(e -> deleteSelected()); // обработчик удаления

        editMenu.getItems().addAll(editModeItem, new SeparatorMenuItem(), deleteItem); // собираем меню редактирования

        Menu viewMenu = new Menu("Вид");
        CheckMenuItem showWireframe = new CheckMenuItem("Показать каркас");
        CheckMenuItem showVertices = new CheckMenuItem("Показать вершины");
        CheckMenuItem useTextureItem = new CheckMenuItem("Использовать текстуру");
        MenuItem darkThemeItem = new MenuItem("Тёмная тема");
        MenuItem lightThemeItem = new MenuItem("Светлая тема");
        CheckMenuItem useLightingItem = new CheckMenuItem("Использовать освещение");

        // Обработчики для флажков рендеринга
        showWireframe.setOnAction(e -> {
            if (renderPanel != null) {
                renderPanel.setRenderWireframe(showWireframe.isSelected());
            }
        });

        useTextureItem.setOnAction(e -> {
            if (renderPanel != null) {
                renderPanel.setUseTexture(useTextureItem.isSelected());
            }
        });

        useLightingItem.setSelected(true); // По умолчанию включено
        useLightingItem.setOnAction(e -> {
            if (renderPanel != null) {
                renderPanel.setUseLighting(useLightingItem.isSelected());
                System.out.println("Освещение: " + (useLightingItem.isSelected() ? "ВКЛ" : "ВЫКЛ"));
            }
        });


        // Разделитель для режимов рендеринга
        SeparatorMenuItem renderModesSeparator = new SeparatorMenuItem();
        SeparatorMenuItem themeSeparator = new SeparatorMenuItem();

        viewMenu.getItems().addAll(showWireframe,  useTextureItem, useLightingItem,
                renderModesSeparator, themeSeparator,
                new SeparatorMenuItem(), showVertices, new SeparatorMenuItem(),
                darkThemeItem, lightThemeItem); // собираем меню вида

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu); // добавляем все меню в строку меню
        return menuBar; // возвращаем созданную строку меню
    }

    private void updateRender() {
        if (renderPanel != null && sceneManager != null) {
            // Собираем все UI модели
            List<Model3D> uiModels = new ArrayList<>();
            for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                if (wrapper.getUIModel() != null) {
                    uiModels.add(wrapper.getUIModel());
                }
            }

            System.out.println("Обновление рендера, моделей: " + uiModels.size());

            // Проверяем трансформации
            for (Model3D model : uiModels) {
                System.out.println("Модель: " + model.getName() +
                        ", RotY: " + model.rotateYProperty().get() +
                        ", Вершин: " + model.getVertices().size() +
                        ", Полигонов: " + model.getPolygons().size());
            }

            renderPanel.setModels(uiModels);
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
        modelListView.getSelectionModel().selectedItemProperty().addListener( //слушаткль изменения выделения
                (obs, oldVal, newVal) -> {
                    selectionManager.clearSelection(); // очищаем предыдущее выделение
                    if (newVal != null) { // если выбран новый элемент (не null)
                        selectionManager.selectModel(newVal.getUIModel()); // выделяем модель в selectionManager
                        updateModelPropertiesPanel(newVal.getUIModel()); // обновляем панель свойств
                    }
                });

        Button addTestModelBtn = new Button("Добавить тестовую модель");
        Button removeModelBtn = new Button("Удалить модель");


        // В метод createLeftPanel() добавьте:
        Button testLightingBtn = new Button("Тест освещения");
        testLightingBtn.setOnAction(e -> debugLighting());

        Button testNormalDirectionBtn = new Button("Тест нормалей");
        testNormalDirectionBtn.setOnAction(e -> debugBackfaceIssue());

        Button testNormalsBtn = new Button("Тест нормалей 2");
        testNormalsBtn.setOnAction(e -> testNormals());

        addTestModelBtn.setOnAction(e -> addTestModel());
// И добавьте в HBox modelButtons:
        removeModelBtn.setOnAction(e -> removeSelectedModel()); // обработчик удаления

        HBox modelButtons = new HBox(5, addTestModelBtn, removeModelBtn, testLightingBtn, testNormalDirectionBtn, testNormalsBtn); // горизонтальный контейнер для кнопок

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

    private Pane createCenterPanel() {
        // Используем наш RenderPanel вместо простого Pane
        renderPanel = new RenderPanel(800, 600);
        renderPanel.getStyleClass().add("view-3d");
        renderPanel.setStyle("-fx-background-color: #1a1a2e;");

        // Добавляем обработку клавиш для управления камерой

        renderPanel.setFocusTraversable(true);
        // Привязка моделей из sceneManager к рендереру
        sceneManager.getModelWrappers().addListener((ListChangeListener<ModelWrapper>) c -> {
            List<Model3D> uiModels = new ArrayList<>();
            for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                uiModels.add(wrapper.getUIModel());
            }
            renderPanel.setModels(uiModels);
        });

        return renderPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.getStyleClass().add("right-panel");
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);

        Label propertiesLabel = new Label("Свойства модели");
        propertiesLabel.getStyleClass().add("section-label");

        // Инициализируем modelPropertiesPanel только один раз
        if (modelPropertiesPanel == null) {
            modelPropertiesPanel = new BorderPane();
            modelPropertiesPanel.setCenter(new Label("Выберите модель для редактирования свойств"));
        }

        Label transformLabel = new Label("Трансформации");
        transformLabel.getStyleClass().add("section-label");

        VBox transformsPanel = createTransformsPanel();

        rightPanel.getChildren().addAll(
                propertiesLabel,
                modelPropertiesPanel,
                new Separator(),
                transformLabel,
                transformsPanel
        );

        return rightPanel;
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

    private HBox createStatusBar() { // создает строку состояния
        HBox statusBar = new HBox(10); // горизонтальный контейнер
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5)); // небольшие отступы

        Label statusLabel = new Label("Готово");
        statusLabel.setId("status-label");

        Label vertexCountLabel = new Label("Вершин: 0");
        vertexCountLabel.setId("vertex-count");

        Label polygonCountLabel = new Label("Полигонов: 0");
        polygonCountLabel.setId("polygon-count");

        statusBar.getChildren().addAll(statusLabel, new Separator(), // собираем элементы
                vertexCountLabel, polygonCountLabel); // счетчики вершин и полигонов
        return statusBar; // возвращаем строку состояния
    }

    private void updateModelPropertiesPanel(Model3D model) {
        if (modelPropertiesPanel == null) return;

        // Очищаем старые элементы перед добавлением новых
        if (modelPropertiesPanel.getCenter() instanceof VBox) {
            VBox oldProperties = (VBox) modelPropertiesPanel.getCenter();
            oldProperties.getChildren().clear();
        }

        VBox properties = new VBox(10);

        // === Секция имени ===
        HBox nameBox = new HBox(10);
        Label nameLabel = new Label("Имя:");
        TextField nameField = new TextField(model.nameProperty().get());
        nameField.textProperty().bindBidirectional(model.nameProperty());
        nameBox.getChildren().addAll(nameLabel, nameField);

        // === Секция видимости ===
        CheckBox visibleCheck = new CheckBox("Видима");
        visibleCheck.selectedProperty().bindBidirectional(model.visibleProperty());

        // === Секция цвета ===
        HBox colorBox = new HBox(10);
        Label colorLabel = new Label("Цвет:");
        ColorPicker colorPicker = new ColorPicker(model.getBaseColor());
        colorPicker.valueProperty().bindBidirectional(model.baseColorProperty());
        colorBox.getChildren().addAll(colorLabel, colorPicker);

        // === Секция текстуры ===
        Label textureSection = new Label("Текстура:");
        textureSection.getStyleClass().add("subsection-label");

        HBox textureBox = new HBox(10);
        Button loadTextureBtn = new Button("Загрузить...");
        Button clearTextureBtn = new Button("Очистить");

        Label textureStatus = new Label(
                model.getTexture() != null ? "✓ Текстура загружена" : "Нет текстуры"
        );

        loadTextureBtn.setOnAction(e -> loadTextureForModel(model));
        clearTextureBtn.setOnAction(e -> {
            model.setTexture(null);
            textureStatus.setText("Нет текстуры");
            updateRender();
        });

        textureBox.getChildren().addAll(loadTextureBtn, clearTextureBtn, textureStatus);

        // === Секция статистики ===
        Label statsLabel = new Label(String.format(
                "Статистика: Вершин: %d | Полигонов: %d | UV-координат: %d",
                model.getVertices().size(),
                model.getPolygons().size(),
                model.getTextureCoords().size()
        ));

        // === Секция трансформаций ===
        Label transformSection = new Label("Текущие трансформации:");
        transformSection.getStyleClass().add("subsection-label");

        VBox transformsInfo = new VBox(5);

        HBox translateInfo = new HBox(10);
        translateInfo.getChildren().addAll(
                new Label("Перемещение:"),
                new Label(String.format("X: %.1f", model.translateXProperty().get())),
                new Label(String.format("Y: %.1f", model.translateYProperty().get())),
                new Label(String.format("Z: %.1f", model.translateZProperty().get()))
        );

        HBox rotateInfo = new HBox(10);
        rotateInfo.getChildren().addAll(
                new Label("Вращение:"),
                new Label(String.format("X: %.1f°", model.rotateXProperty().get())),
                new Label(String.format("Y: %.1f°", model.rotateYProperty().get())),
                new Label(String.format("Z: %.1f°", model.rotateZProperty().get()))
        );

        HBox scaleInfo = new HBox(10);
        scaleInfo.getChildren().addAll(
                new Label("Масштаб:"),
                new Label(String.format("X: %.1f", model.scaleXProperty().get())),
                new Label(String.format("Y: %.1f", model.scaleYProperty().get())),
                new Label(String.format("Z: %.1f", model.scaleZProperty().get()))
        );

        transformsInfo.getChildren().addAll(translateInfo, rotateInfo, scaleInfo);

        // === Кнопки управления ===
        HBox actionButtons = new HBox(10);
        Button resetTransformBtn = new Button("Сбросить трансформации");
        Button centerModelBtn = new Button("Центрировать");

        resetTransformBtn.setOnAction(e -> {
            model.translateXProperty().set(0);
            model.translateYProperty().set(0);
            model.translateZProperty().set(0);
            model.rotateXProperty().set(0);
            model.rotateYProperty().set(0);
            model.rotateZProperty().set(0);
            model.scaleXProperty().set(1);
            model.scaleYProperty().set(1);
            model.scaleZProperty().set(1);
           // model.calculateNormals();
            model.calculateVertexNormals();
            updateRender();
            updateModelPropertiesPanel(model); // Обновляем панель
        });

        centerModelBtn.setOnAction(e -> {
            // Просто сбрасываем перемещение
            model.translateXProperty().set(0);
            model.translateYProperty().set(0);
            model.translateZProperty().set(0);
            updateRender();
            updateModelPropertiesPanel(model);
        });

        actionButtons.getChildren().addAll(resetTransformBtn, centerModelBtn);

        // === Собираем все элементы ===
        properties.getChildren().addAll(
                nameBox,
                visibleCheck,
                colorBox,
                new Separator(),
                textureSection,
                textureBox,
                new Separator(),
                statsLabel,
                new Separator(),
                transformSection,
                transformsInfo,
                new Separator(),
                actionButtons
        );

        // Устанавливаем новую панель свойств
        modelPropertiesPanel.setCenter(properties);

        // Добавляем слушатели для обновления трансформаций в реальном времени
        model.translateXProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.translateYProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.translateZProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.rotateXProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.rotateYProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.rotateZProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.scaleXProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.scaleYProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
        model.scaleZProperty().addListener((obs, oldVal, newVal) -> updateTransformsInfo(model));
    }

    // Вспомогательный метод для обновления информации о трансформациях
    private void updateTransformsInfo(Model3D model) {
        // Этот метод будет вызываться при изменении трансформаций
        // Пока просто обновляем рендер
        updateRender();
    }

    private void openModel() { // открытие модели из файла
        FileChooser fileChooser = new FileChooser(); // диалог выбора файла
        fileChooser.getExtensionFilters().add( // фильтр по расширению .obj
                new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home"))); // начальная директория - домашняя
        File file = fileChooser.showOpenDialog(primaryStage); // показываем диалог

        if (file != null) { // если файл выбран
            try {
                ObjReader objReader = new ObjReader(); // создаем загрузчик
                scene_master.model.Model loadedModel = objReader.readModel(file.getAbsolutePath()); // загружаем модель

                ModelWrapper modelWrapper = new ModelWrapper( // создаем обертку
                        loadedModel, // загруженные данные
                        file.getName().replace(".obj", "") // имя файла без расширения
                );

                sceneManager.addModelWrapper(modelWrapper); // добавляем модель на сцену

                DialogHelper.showInfoDialog("Успешно",
                        String.format("Модель успешно загружена! Вершин: %d || Полигонов: %d",
                                loadedModel.getVertexCount(), // количество вершин
                                loadedModel.getPolygonCount())); // количество полигонов

            } catch (IOException e) {
                DialogHelper.showErrorDialog("Ошибка загрузки",
                        "Не удалось загрузить модель: " + e.getMessage());
            } catch (Exception e) {
                DialogHelper.showErrorDialog("Ошибка",
                        "Непредвиденная ошибка: " + e.getMessage());
            }
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
            if (activeModel.getVertices().isEmpty()) {
                DialogHelper.showErrorDialog("Ошибка сохранения",
                        "Модель пуста — не содержит ни одной вершины. Невозможно сохранить.");
                return;
            }
            if (selectedWrapper != null) {
                Alert transformDialog = new Alert(Alert.AlertType.CONFIRMATION);
                transformDialog.setTitle("Сохранение модели");
                transformDialog.setHeaderText("Сохранить трансформации?");
                transformDialog.setContentText("Сохранить модель с текущими трансформациями?\n'Да' — сохранить как видите\n'Нет' — сохранить оригинальную модель");
                ButtonType yesButton = new ButtonType("Да", ButtonBar.ButtonData.YES);
                ButtonType noButton = new ButtonType("Нет", ButtonBar.ButtonData.NO);
                ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
                transformDialog.getButtonTypes().setAll(yesButton, noButton, cancelButton);

                java.util.Optional<ButtonType> result = transformDialog.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == yesButton) {
                        applyTransformationsToOriginalModel(selectedWrapper);
                        selectedWrapper.updateUIModel();
                    } else if (result.get() == noButton) {
                        // Оставляем оригинальную модель без изменений
                    } else {
                        return;
                    }

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    File file = fileChooser.showSaveDialog(primaryStage);
                    if (file != null) {
                        try {
                            // 🔥 Подключаем твой ObjWriter!
                            ObjWriter.write(selectedWrapper.getOriginalModel(), file.getPath());
                            DialogHelper.showInfoDialog("Успешно",
                                    "Модель сохранена!\nВершин: " + selectedWrapper.getOriginalModel().getVertexCount() +
                                            "\nПолигонов: " + selectedWrapper.getOriginalModel().getPolygonCount());
                        } catch (Exception e) {
                            DialogHelper.showErrorDialog("Ошибка сохранения", e.getMessage());
                        }
                    }
                }
            }
        } else {
            DialogHelper.showWarningDialog("Модель не выбрана", "Выберите модель для сохранения.");
        }
    }

    private void applyTransformationsToOriginalModel(ModelWrapper wrapper) {
        Model3D uiModel = wrapper.getUIModel();
        Model originalModel = wrapper.getOriginalModel();

        // Получаем текущие трансформации из UI модели
        double tx = uiModel.translateXProperty().get();
        double ty = uiModel.translateYProperty().get();
        double tz = uiModel.translateZProperty().get();

        double rx = uiModel.rotateXProperty().get();
        double ry = uiModel.rotateYProperty().get();
        double rz = uiModel.rotateZProperty().get();

        double sx = uiModel.scaleXProperty().get();
        double sy = uiModel.scaleYProperty().get();
        double sz = uiModel.scaleZProperty().get();

        // Применяем трансформации к вершинам оригинальной модели
        // (Это должна быть матричная математика от 2-го участника)
        // Временно простой вариант:
        for (Vector3D vertex : originalModel.getVertices()) {
            // Применяем масштаб
            double x = vertex.getX() * sx;
            double y = vertex.getY() * sy;
            double z = vertex.getZ() * sz;

            // TODO: Применить вращение (нужны матрицы от 2-го участника)
            // TODO: Применить перемещение

            // Обновляем вершину (но Vector3D неизменяемый, нужно создавать новый)
            // Это упрощенный пример - в реальности нужен полноценный механизм трансформаций
        }

        // После изменения оригинальной модели нужно пересчитать нормали
        NormalCalculator normalCalculator = new NormalCalculator();
        normalCalculator.calculateNormals(originalModel);
    }

    private void addTestModel() {
        Model cubeModel = new Model();

        // Вершины куба (центр в 0,0,0, размер 1)
        // ПЕРЕДНЯЯ грань (Z = -0.5)
        cubeModel.addVertex(new Vector3D(-0.5, -0.5, -0.5)); // 0
        cubeModel.addVertex(new Vector3D( 0.5, -0.5, -0.5)); // 1
        cubeModel.addVertex(new Vector3D( 0.5,  0.5, -0.5)); // 2
        cubeModel.addVertex(new Vector3D(-0.5,  0.5, -0.5)); // 3

        // ЗАДНЯЯ грань (Z = 0.5)
        cubeModel.addVertex(new Vector3D(-0.5, -0.5,  0.5)); // 4
        cubeModel.addVertex(new Vector3D( 0.5, -0.5,  0.5)); // 5
        cubeModel.addVertex(new Vector3D( 0.5,  0.5,  0.5)); // 6
        cubeModel.addVertex(new Vector3D(-0.5,  0.5,  0.5)); // 7

        // Грани (вершины в порядке ПРОТИВ часовой стрелки)
        // Передняя грань
        cubeModel.addPolygon(new Polygon(0, 1, 2, 3));
        // Задняя грань
        cubeModel.addPolygon(new Polygon(7, 6, 5, 4));
        // Верхняя грань
        cubeModel.addPolygon(new Polygon(3, 2, 6, 7));
        // Нижняя грань
        cubeModel.addPolygon(new Polygon(4, 5, 1, 0));
        // Левая грань
        cubeModel.addPolygon(new Polygon(4, 0, 3, 7));
        // Правая грань
        cubeModel.addPolygon(new Polygon(1, 5, 6, 2));

        ModelWrapper wrapper = new ModelWrapper(cubeModel, "Fixed Cube");
        sceneManager.addModelWrapper(wrapper);

        // Вычисляем нормали
        wrapper.getUIModel().calculateNormals();
        wrapper.getUIModel().calculateVertexNormals();

        updateRender();
    }

    // Метод загрузки текстуры
    private void loadTextureForModel(Model3D model) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            try {
                System.out.println("Загрузка текстуры из: " + file.getAbsolutePath());

                // Загружаем текстуру через TextureManager
                Image texture = TextureManager.getInstance().loadTexture(file);
                System.out.println("Текстура загружена: " +
                        texture.getWidth() + "x" + texture.getHeight());

                // Устанавливаем текстуру в модель
                model.setTexture(texture);

                // Проверяем и создаем UV-координаты если нужно
                checkAndFixUVCoordinates(model);

                // Включаем режим текстуры в рендерере
                if (renderPanel != null) {
                    renderPanel.setUseTexture(true);
                }

                // Обновляем рендер
                updateRender();

                DialogHelper.showInfoDialog("Текстура загружена",
                        String.format("Текстура: %s\nРазмер: %dx%d\nUV-координат: %d",
                                file.getName(),
                                (int)texture.getWidth(),
                                (int)texture.getHeight(),
                                model.getTextureCoords().size()));

            } catch (IOException e) {
                DialogHelper.showErrorDialog("Ошибка загрузки",
                        "Не удалось загрузить текстуру: " + e.getMessage());
            } catch (Exception e) {
                DialogHelper.showErrorDialog("Ошибка",
                        "Непредвиденная ошибка: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void removeSelectedModel() { // удаление выбранной модели
        ModelWrapper selected = modelListView.getSelectionModel().getSelectedItem(); // получаем выбранную модель
        if (selected != null) { // если что-то выбрано
            sceneManager.removeModelWrapper(selected); // удаляем из сцены
            updateRender();
        }
    }

    // В MainApplication в методе deleteSelected()
    private void deleteSelected() {
        Model3D activeModel = selectionManager.getActiveModel();
        if (activeModel != null) {
            // После удаления вершины/полигона из UI модели
            // Нужно найти соответствующий ModelWrapper и обновить его
            for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                if (wrapper.getUIModel() == activeModel) {
                    wrapper.updateUIModel(); // Вот здесь!
                    break;
                }
            }
        }
    }

    public static void main(String[] args) { // точка входа для запуска (каоимагдная строка)
        launch(args); // запуск приложения
    }

    // В MainApplication.java добавьте:
    private void checkAndFixUVCoordinates(Model3D model) {
        System.out.println("=== Проверка UV-координат для модели: " + model.getName() + " ===");
        System.out.println("Вершин: " + model.getVertices().size());
        System.out.println("UV-координат: " + model.getTextureCoords().size());
        System.out.println("Текстура: " + (model.getTexture() != null ? "Загружена" : "Нет"));

        // Если UV-координат нет, создаем их
        if (model.getTextureCoords().isEmpty()) {
            System.out.println("Создаем UV-координаты...");
            createSimpleUVCoordinates(model);
        }

        // Проверяем, привязаны ли UV к полигонам
        int polygonsWithUV = 0;
        for (Polygon polygon : model.getPolygons()) {
            if (!polygon.getTextureIndices().isEmpty()) {
                polygonsWithUV++;
            }
        }
        System.out.println("Полигонов с UV: " + polygonsWithUV + " из " + model.getPolygons().size());

        // Если UV не привязаны к полигонам, привязываем
        if (polygonsWithUV == 0) {
            System.out.println("Привязываем UV к полигонам...");
            for (Polygon polygon : model.getPolygons()) {
                List<Integer> vertexIndices = polygon.getVertexIndices();
                for (int vertexIndex : vertexIndices) {
                    if (vertexIndex < model.getTextureCoords().size()) {
                        polygon.addTextureIndex(vertexIndex);
                    }
                }
            }
        }
    }

    private void createSimpleUVCoordinates(Model3D model) {
        model.clearTextureCoords();

        System.out.println("Создание ПРАВИЛЬНЫХ UV для куба...");

        // Для куба с 8 вершинами - создаем 8 UV координат
        // Каждая грань куба имеет 4 вершины, но вершины могут повторяться

        if (model.getVertices().size() == 8) {
            // UV координаты для КУБА (стандартные)
            // Каждая вершина получает уникальную UV
            float[][] cubeUVs = {
                    {0.0f, 0.0f}, // 0: лево-низ-зад
                    {1.0f, 0.0f}, // 1: право-низ-зад
                    {1.0f, 1.0f}, // 2: право-верх-зад
                    {0.0f, 1.0f}, // 3: лево-верх-зад
                    {0.0f, 0.0f}, // 4: лево-низ-перед
                    {1.0f, 0.0f}, // 5: право-низ-перед
                    {1.0f, 1.0f}, // 6: право-верх-перед
                    {0.0f, 1.0f}  // 7: лево-верх-перед
            };

            for (float[] uv : cubeUVs) {
                model.addTextureCoord(uv[0], uv[1]);
            }

            System.out.println("Создано 8 UV-координат для куба");

            // Теперь нужно ПРИВЯЗАТЬ UV к полигонам
            // Для куба обычно 6 граней = 12 треугольников
            // Каждому полигону нужно указать индексы UV

            if (model.getPolygons().size() == 12) {
                // Индексы UV для каждого полигона (триангулированного куба)
                int[][] uvIndicesForCube = {
                        {4,5,6}, {4,6,7}, // передняя грань
                        {5,1,2}, {5,2,6}, // правая грань
                        {1,0,3}, {1,3,2}, // задняя грань
                        {0,4,7}, {0,7,3}, // левая грань
                        {7,6,2}, {7,2,3}, // верхняя грань
                        {0,1,5}, {0,5,4}  // нижняя грань
                };

                for (int i = 0; i < model.getPolygons().size() && i < uvIndicesForCube.length; i++) {
                    Polygon polygon = model.getPolygons().get(i);
                    polygon.getTextureIndices().clear(); // Очищаем старые

                    for (int uvIdx : uvIndicesForCube[i]) {
                        polygon.addTextureIndex(uvIdx);
                    }
                }
                System.out.println("UV привязаны к полигонам");
            }
        } else {
            // Для не-куба: простые UV
            for (int i = 0; i < model.getVertices().size(); i++) {
                double u = (i % 10) / 10.0;
                double v = ((i / 10) % 10) / 10.0;
                model.addTextureCoord(u, v);
            }
        }
    }

    private void debugLighting() {
        System.out.println("=== Детальная диагностика освещения ===");

        // Проверяем одну нормаль
        Model3D model = sceneManager.getModelWrappers().get(0).getUIModel();
        if (!model.getPolygons().isEmpty()) {
            Polygon poly = model.getPolygons().get(0);
            Vector3D normal = poly.getNormal();

            if (normal != null) {
                System.out.println("Нормаль первого полигона:");
                System.out.println("  X: " + normal.getX());
                System.out.println("  Y: " + normal.getY());
                System.out.println("  Z: " + normal.getZ());

                // Проверяем направление
                System.out.println("  Z компонент: " + normal.getZ() +
                        " (должен быть < 0 для граней, обращенных к камере)");

                // Простой расчет dot product
                double dot = normal.getX() * 0.5 + normal.getY() * (-0.5) + normal.getZ() * (-1);
                System.out.println("  Dot с светом (0.5, -0.5, -1): " + dot);

                if (dot < 0) {
                    System.out.println("  ВНИМАНИЕ: dot < 0! Освещение будет темным.");
                    System.out.println("  Решение: инвертировать нормали при Z > 0");
                }
            }
        }

        // Проверяем трансформации
        System.out.println("\nТрансформации модели:");
        System.out.println("  RotY: " + model.rotateYProperty().get());

        // Рекомендации
        System.out.println("\nРекомендации:");
        System.out.println("  1. Проверить метод applyLightingToColor в SoftwareRenderer");
        System.out.println("  2. Убедиться, что нормали инвертируются при Z > 0");
        System.out.println("  3. Увеличить ambient свет до 0.4-0.5");
    }

    private void debugBackfaceIssue() {
        for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
            Model3D model = wrapper.getUIModel();

            // Если UV-координат нет, создаем простые
            if (model.getTextureCoords().isEmpty()) {
                System.out.println("Создаем UV для модели: " + model.getName());

                for (int i = 0; i < model.getVertices().size(); i++) {
                    // Простая UV-развертка
                    double u = Math.random(); // Временное решение
                    double v = Math.random();
                    model.addTextureCoord(u, v);
                }
            }

            // Привязываем UV к полигонам
            for (Polygon polygon : model.getPolygons()) {
                List<Integer> vertexIndices = polygon.getVertexIndices();
                polygon.getTextureIndices().clear();

                for (int i = 0; i < vertexIndices.size(); i++) {
                    polygon.addTextureIndex(vertexIndices.get(i));
                }
            }
        }

        updateRender();
    }

    private void testNormals() {
        if (!sceneManager.getModelWrappers().isEmpty()) {
            Model3D model = sceneManager.getModelWrappers().get(0).getUIModel();

            System.out.println("=== ТЕСТ НОРМАЛЕЙ ===");
            System.out.println("Вершин: " + model.getVertices().size());
            System.out.println("Полигонов: " + model.getPolygons().size());

            // Проверяем первые 5 нормалей
            for (int i = 0; i < Math.min(5, model.getPolygons().size()); i++) {
                Polygon poly = model.getPolygons().get(i);
                Vector3D normal = poly.getNormal();

                if (normal == null) {
                    System.out.println(i + ": Нормаль = NULL");
                } else {
                    System.out.println(i + ": Нормаль = " + normal +
                            ", Z = " + normal.getZ() +
                            (normal.getZ() > 0 ? " ← ПРОБЛЕМА!" : " OK"));
                }
            }
        }
    }

    private void createCubeUV(Model3D model) {
        System.out.println("Создание UV-координат для куба...");
        model.clearTextureCoords();

        // Куб имеет 8 вершин, но для текстурирования нужно 24 UV-координаты
        // (каждая вершина используется 3 раза с разными UV)

        // Простая UV развертка куба
        // Передняя грань
        model.addTextureCoord(0.25, 0.75); // 0
        model.addTextureCoord(0.50, 0.75); // 1
        model.addTextureCoord(0.50, 0.50); // 2
        model.addTextureCoord(0.25, 0.50); // 3

        // Задняя грань
        model.addTextureCoord(0.75, 0.75); // 4
        model.addTextureCoord(1.00, 0.75); // 5
        model.addTextureCoord(1.00, 0.50); // 6
        model.addTextureCoord(0.75, 0.50); // 7

        // Верхняя грань
        model.addTextureCoord(0.25, 1.00); // 8
        model.addTextureCoord(0.50, 1.00); // 9
        model.addTextureCoord(0.50, 0.75); // 10
        model.addTextureCoord(0.25, 0.75); // 11

        // Нижняя грань
        model.addTextureCoord(0.25, 0.50); // 12
        model.addTextureCoord(0.50, 0.50); // 13
        model.addTextureCoord(0.50, 0.25); // 14
        model.addTextureCoord(0.25, 0.25); // 15

        // Левая грань
        model.addTextureCoord(0.00, 0.75); // 16
        model.addTextureCoord(0.25, 0.75); // 17
        model.addTextureCoord(0.25, 0.50); // 18
        model.addTextureCoord(0.00, 0.50); // 19

        // Правая грань
        model.addTextureCoord(0.50, 0.75); // 20
        model.addTextureCoord(0.75, 0.75); // 21
        model.addTextureCoord(0.75, 0.50); // 22
        model.addTextureCoord(0.50, 0.50); // 23

        System.out.println("Создано " + model.getTextureCoords().size() + " UV-координат");
    }

    private void fixPolygonVertexOrder(Model3D model) {
        System.out.println("\n=== ПРОСТОЙ ТЕСТ НОРМАЛЕЙ ===");

        // Создаем простой треугольник вручную
        Model3D testModel = new Model3D("Test Triangle");

        // Треугольник, обращенный к камере
        testModel.getVertices().add(new Vertex(0, 0, -1));  // ближе к камере
        testModel.getVertices().add(new Vertex(1, 0, -1));
        testModel.getVertices().add(new Vertex(0, 1, -1));

        testModel.getPolygons().add(new Polygon(0, 1, 2));

        // Вычисляем нормали
        //testModel.calculateNormals();

        // Проверяем
        Polygon poly = testModel.getPolygons().get(0);
        Vector3D normal = poly.getNormal();

        System.out.println("Вершины треугольника:");
        System.out.println("  V0: " + testModel.getVertices().get(0));
        System.out.println("  V1: " + testModel.getVertices().get(1));
        System.out.println("  V2: " + testModel.getVertices().get(2));
        System.out.println("Нормаль: " + normal);
        System.out.println("Z компонент: " + normal.getZ() + " (ожидается < 0)");

        // Тест освещения
        double dot = normal.getZ() * -1; // свет сзади
        System.out.println("Dot с направлением (0,0,-1): " + dot);
    }
}