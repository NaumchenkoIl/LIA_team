package scene_master;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import scene_master.calculator.NormalCalculator;
import scene_master.manager.SceneManager; // менеджер сцены с моделями
import scene_master.manager.SelectionManager; // менеджер выделения моделей
import scene_master.model.*;
import scene_master.reader.ObjReader; // загрузчик obj-файлов
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

        viewMenu.getItems().addAll(showWireframe,  useTextureItem,
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
        Button rotateModelBtn = new Button("Вращать модель");

        addTestModelBtn.setOnAction(e -> addTestModel()); // обработчик добавления
        removeModelBtn.setOnAction(e -> removeSelectedModel()); // обработчик удаления

        rotateModelBtn.setOnAction(e -> {
            Model3D activeModel = selectionManager.getActiveModel();
            if (activeModel != null) {
                System.out.println("Вращение модели: " + activeModel.getName());
                System.out.println("Текущий угол Y: " + activeModel.rotateYProperty().get());

                // Вращаем на 15 градусов по Y
                double current = activeModel.rotateYProperty().get();
                activeModel.rotateYProperty().set(current + 15);

                System.out.println("Новый угол Y: " + activeModel.rotateYProperty().get());

                // Пересчитываем нормали (обязательно после вращения!)
                activeModel.calculateNormals();

                // Принудительно обновляем рендер
                updateRender();

                // Обновляем панель свойств
                updateModelPropertiesPanel(activeModel);

                System.out.println("Вращение выполнено");
            } else {
                DialogHelper.showWarningDialog("Нет выбранной модели",
                        "Выберите модель для вращения из списка слева.");
            }
        });

        HBox modelButtons = new HBox(5, addTestModelBtn, removeModelBtn, rotateModelBtn); // горизонтальный контейнер для кнопок

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
        renderPanel.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case W: renderPanel.moveCameraForward(); break;
                case S: renderPanel.moveCameraBackward(); break;
                case A: renderPanel.moveCameraLeft(); break;
                case D: renderPanel.moveCameraRight(); break;
                case Q: renderPanel.rotateCameraLeft(); break;
                case E: renderPanel.rotateCameraRight(); break;
            }
        });

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

    private void updateModelPropertiesPanel(Model3D model) { // обновляет панель свойств для выбранной модели
        VBox properties = new VBox(10); // вертикальный контейнер для свойств

        HBox nameBox = new HBox(10); // контейнер для имени
        Label nameLabel = new Label("Имя:");
        TextField nameField = new TextField(model.nameProperty().get()); // поле ввода имени
        nameField.textProperty().bindBidirectional(model.nameProperty()); // двусторонняя привязка к свойству модели
        nameBox.getChildren().addAll(nameLabel, nameField); // собираем

        CheckBox visibleCheck = new CheckBox("Видима");
        visibleCheck.selectedProperty().bindBidirectional(model.visibleProperty()); // привязка к свойству видимости

        HBox colorBox = new HBox(10); // контейнер для выбора цвета
        Label colorLabel = new Label("Цвет:");
        javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.LIGHTBLUE; // цвет по умолчанию
        ColorPicker colorPicker = new ColorPicker(model.getBaseColor()); // виджет выбора цвета
        colorPicker.valueProperty().bindBidirectional(model.baseColorProperty());
        colorBox.getChildren().addAll(colorLabel, colorPicker); // собираем

        // Текстура
        HBox textureBox = new HBox(10);
        Label textureLabel = new Label("Текстура:");
        Button loadTextureBtn = new Button("Загрузить...");
        Button clearTextureBtn = new Button("Очистить");

        // Показываем имя файла текстуры
        Label textureInfo = new Label(model.getTexture() == null ? "Нет текстуры" : "Текстура загружена");

        loadTextureBtn.setOnAction(e -> loadTextureForModel(model));
        clearTextureBtn.setOnAction(e -> {
            model.setTexture(null);
            textureInfo.setText("Нет текстуры");
        });

        // Слушатель изменения текстуры
        model.textureProperty().addListener((obs, oldTex, newTex) -> {
            textureInfo.setText(newTex == null ? "Нет текстуры" : "Текстура загружена");
        });

        textureBox.getChildren().addAll(textureLabel, loadTextureBtn, clearTextureBtn, textureInfo);

        Label statsLabel = new Label(String.format( // метка со статистикой
                "Статистика: Вершин: %d || Полигонов: %d",
                model.getVertices().size(), // количество вершин
                model.getPolygons().size() // количество полигонов
        ));

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
            // Найти соответствующий wrapper
            ModelWrapper selectedWrapper = null;
            for (ModelWrapper wrapper : sceneManager.getModelWrappers()) {
                if (wrapper.getUIModel() == activeModel) {
                    selectedWrapper = wrapper;
                    break;
                }
            }

            if (selectedWrapper != null) {
                // Спросить пользователя: сохранить с трансформациями или без?
                Alert transformDialog = new Alert(Alert.AlertType.CONFIRMATION);
                transformDialog.setTitle("Сохранение модели");
                transformDialog.setHeaderText("Сохранить трансформации?");
                transformDialog.setContentText("Сохранить модель с текущими трансформациями (перемещение, вращение, масштаб)?" +
                        "\n'Да' - сохранить как видите\n'Нет' - сохранить оригинальную модель");

                ButtonType yesButton = new ButtonType("Да", ButtonBar.ButtonData.YES);
                ButtonType noButton = new ButtonType("Нет", ButtonBar.ButtonData.NO);
                ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

                transformDialog.getButtonTypes().setAll(yesButton, noButton, cancelButton);

                java.util.Optional<ButtonType> result = transformDialog.showAndWait();

                if (result.isPresent()) {
                    if (result.get() == yesButton) {
                        // Применить трансформации
                        applyTransformationsToOriginalModel(selectedWrapper);
                        selectedWrapper.updateUIModel();
                    } else if (result.get() == noButton) {
                        // Просто сохранить оригинальную модель
                        // Ничего не делаем, оригинальная модель уже в исходном состоянии
                    } else {
                        // Отмена
                        return;
                    }

                    // Продолжаем с диалогом сохранения файла
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    File file = fileChooser.showSaveDialog(primaryStage);

                    if (file != null) {
                        try {
                            // Здесь будет вызов ObjWriter (когда он будет реализован)
                            // ObjWriter.saveModel(selectedWrapper.getOriginalModel(), file.getPath());

                            DialogHelper.showInfoDialog("Сохранение модели",
                                    String.format("Модель сохранена в файл: %s\n" +
                                                    "Вершин: %d, Полигонов: %d",
                                            file.getName(),
                                            selectedWrapper.getOriginalModel().getVertexCount(),
                                            selectedWrapper.getOriginalModel().getPolygonCount()));
                        } catch (Exception e) {
                            DialogHelper.showErrorDialog("Ошибка сохранения",
                                    "Не удалось сохранить модель: " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            DialogHelper.showWarningDialog("Модель не выбрана",
                    "Пожалуйста, выберите модель для сохранения.");
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
        try {
            // Пробуем несколько возможных путей
            String[] possiblePaths = {
                    "test_cube.obj",                                    // Текущая директория
                    "src/test/test_cube.obj",                           // Относительный путь
                    "C:\\Users\\Александр\\Desktop\\for3person\\LIA_team\\src\\test\\test_cube.obj", // Абсолютный путь
                    System.getProperty("user.dir") + "/src/test/test_cube.obj" // Динамический путь
            };

            File file = null;
            for (String path : possiblePaths) {
                File testFile = new File(path);
                if (testFile.exists()) {
                    file = testFile;
                    System.out.println("Файл найден: " + file.getAbsolutePath());
                    break;
                }
            }

            if (file == null || !file.exists()) {
                // Создаем тестовый куб в памяти
              //  createTestCubeInMemory();
                return;
            }

            // Загружаем модель через ObjReader
            ObjReader objReader = new ObjReader();
            scene_master.model.Model loadedModel = objReader.readModel(file.getAbsolutePath());

            // Создаем обертку
            ModelWrapper modelWrapper = new ModelWrapper(
                    loadedModel,
                    "Test Cube"
            );

            // Добавляем на сцену
            sceneManager.addModelWrapper(modelWrapper);

            // Устанавливаем цвет и немного вращаем для лучшего вида
            modelWrapper.getUIModel().setBaseColor(javafx.scene.paint.Color.CYAN);
            modelWrapper.getUIModel().rotateYProperty().set(20);
            modelWrapper.getUIModel().calculateNormals();

            updateRender();

        } catch (IOException e) {
            // Если не удалось загрузить файл, создаем модель программно
            DialogHelper.showWarningDialog("Файл не найден",
                    "Создаем тестовый куб в памяти.\n" + e.getMessage());
            //createTestCubeInMemory();
        }
    }

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
                Image texture = TextureLoader.loadTexture(file);
                model.setTexture(texture);

                // Если у модели нет координат текстуры, создаем простые
                if (model.getTextureCoordinates().isEmpty() && !model.getVertices().isEmpty()) {
                    // Простая UV-развертка: просто проецируем вершины на плоскость XY
                    for (int i = 0; i < model.getVertices().size(); i++) {
                        Vertex v = model.getVertices().get(i);
                        // Простейшее отображение: x,y -> u,v
                        double u = (v.x + 1) / 2; // [-1,1] -> [0,1]
                        double vCoord = (v.y + 1) / 2;
                        model.addTextureCoordinate(u, vCoord);
                    }
                }

                DialogHelper.showInfoDialog("Текстура загружена",
                        String.format("Текстура успешно загружена: %s\nРазмер: %.0fx%.0f",
                                file.getName(), texture.getWidth(), texture.getHeight()));

            } catch (IOException e) {
                DialogHelper.showErrorDialog("Ошибка загрузки текстуры",
                        "Не удалось загрузить текстуру: " + e.getMessage());
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
}