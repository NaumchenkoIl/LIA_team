package scene_master;

import scene_master.manager.SceneManager;
import scene_master.manager.SelectionManager;
import scene_master.manager.EditManager;
import scene_master.model.Model3D;
import scene_master.model.ModelWrapper;
import scene_master.reader.ObjReader;
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
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {

    private Stage primaryStage; // ссылка на главное окно
    private SceneManager sceneManager; // управление моделями на сцене
    private SelectionManager selectionManager; // управление выделением
    private ListView<ModelWrapper> modelListView; // список моделей в ui
    private BorderPane modelPropertiesPanel; // панель свойств модели
    private EditManager editManager = new EditManager();
    private String currentTheme = "dark";

    @Override
    public void start(Stage primaryStage) { // точка входа приложения
        this.primaryStage = primaryStage; // сохраняем ссылку на окно
        this.selectionManager = new SelectionManager(); // создаем менеджер выделения
        this.sceneManager = new SceneManager(selectionManager); //сздаем менеджер сцены

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
        MenuItem saveItem = new MenuItem("Сохранить модель как...");
        MenuItem exitItem = new MenuItem("Выход");

        openItem.setOnAction(e -> openModel()); // обработчик нажатия - открыть модель
        saveItem.setOnAction(e -> saveModel()); // обработчик нажатия - сохранить модель
        exitItem.setOnAction(e -> primaryStage.close());//обработчик нажатия - закрыть приложение

        fileMenu.getItems().addAll(openItem, saveItem, new SeparatorMenuItem(), exitItem); // добавляем пункты в меню

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
        MenuItem darkThemeItem = new MenuItem("Тёмная тема");
        MenuItem lightThemeItem = new MenuItem("Светлая тема");

        darkThemeItem.setOnAction(e -> switchTheme("dark"));
        lightThemeItem.setOnAction(e -> switchTheme("light"));

        viewMenu.getItems().addAll(showWireframe, showVertices, new SeparatorMenuItem(),
                darkThemeItem, lightThemeItem); // собираем меню вида

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu); // добавляем все меню в строку меню
        return menuBar; // возвращаем созданную строку меню
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
        Pane view3d = new Pane(); // контейнер для 3d-вида
        view3d.getStyleClass().add("view-3d");
        view3d.setStyle("-fx-background-color: #1a1a2e;"); // темно-синий фон

        Label placeholder = new Label("3D Вид (Будет реализовано другими членами команды)"); // заглушка
        placeholder.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 16px; -fx-alignment: center;");
        placeholder.setMaxWidth(Double.MAX_VALUE); // растягиваем на всю ширину
        placeholder.setMaxHeight(Double.MAX_VALUE); // растягиваем на всю высоту
        VBox.setVgrow(placeholder, Priority.ALWAYS); // разрешаем растягивание

        VBox container = new VBox(placeholder); // контейнер для центрирования
        container.setAlignment(javafx.geometry.Pos.CENTER); // выравнивание по центру
        VBox.setVgrow(container, Priority.ALWAYS); // разрешаем растягивание

        view3d.getChildren().add(container); // добавляем заглушку в панель
        return view3d; // возвращаем панель
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

        statusBar.getChildren().addAll(statusLabel, editModeLabel, new Separator(),
                vertexCountLabel, polygonCountLabel);
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

        HBox colorBox = new HBox(10); // контейнер для выбора цвета
        Label colorLabel = new Label("Цвет:");
        javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.LIGHTBLUE; // цвет по умолчанию
        ColorPicker colorPicker = new ColorPicker(fxColor); // виджет выбора цвета
        colorBox.getChildren().addAll(colorLabel, colorPicker); // собираем

        Label statsLabel = new Label(String.format( // метка со статистикой
                "Статистика: Вершин: %d || Полигонов: %d",
                model.getVertices().size(), // количество вершин
                model.getPolygons().size() // количество полигонов
        ));

        properties.getChildren().addAll(nameBox, visibleCheck, colorBox, statsLabel); // собираем все свойства
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
                ErrorHandler.handleException(e, "загрузка OBJ файла");
            } catch (Exception e) {
                ErrorHandler.handleException(e, "непредвиденная ошибка при загрузке");
            }
        }
    }

    private void saveModel() { // сохранение модели в файл
        Model3D activeModel = selectionManager.getActiveModel(); // получаем активную модель
        if (activeModel != null) { // если есть что сохранять
            FileChooser fileChooser = new FileChooser(); // диалог сохранения
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(activeModel.getName() + ".obj");
            File file = fileChooser.showSaveDialog(primaryStage); // показываем диалог сохранения

            if (file != null) {
                try {
                    boolean applyTransformations = DialogHelper.showSaveOptionsDialog();// спрашиваем пользователя, как сохранять

                    ObjWriter objWriter = new ObjWriter();// создаём и используем ObjWriter
                    objWriter.writeModel(activeModel, file.getAbsolutePath(), applyTransformations);

                    DialogHelper.showInfoDialog("Сохранение завершено",
                            String.format("Модель успешно сохранена!\nФайл: %s\nВершин: %d\nПолигонов: %d\nТрансформации применены: %s",
                                    file.getName(),
                                    activeModel.getVertices().size(),
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
        Model3D testModel = new Model3D("Test Model " + (sceneManager.getModelWrappers().size() + 1)); // создаем модель
        ModelWrapper wrapper = new ModelWrapper(null, testModel.nameProperty().get()); // создаем обертку (без данных)
        sceneManager.addModelWrapper(wrapper); // добавляем на сцену
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

    public static void main(String[] args) { // точка входа для запуска (каоимагдная строка)
        launch(args); // запуск приложения
    }
}