package scene_master.manager;

import scene_master.model.ModelWrapper; // обертка модели
import javafx.collections.FXCollections; // утилиты для observable коллекций
import javafx.collections.ObservableList; // наблюдаемый список (автоматически обновляет UI)

public class SceneManager {
    private final ObservableList<ModelWrapper> modelWrappers = FXCollections.observableArrayList(); // список моделей на сцене
    private final SelectionManager selectionManager; // ссылка на менеджер выделения

    public SceneManager(SelectionManager selectionManager) { // конструктор
        this.selectionManager = selectionManager; // сохраняем менеджер выделения
    }

    public ObservableList<ModelWrapper> getModelWrappers() { // получаем спиок моделей
        return modelWrappers; // возвращаем observable список
    }

    public void addModelWrapper(ModelWrapper modelWrapper) { // добавление модели на сцену
        modelWrappers.add(modelWrapper); // добавляем в список
        selectionManager.selectModel(modelWrapper.getUIModel()); // автоматически выделяем добавленную модель
    }

    public void removeModelWrapper(ModelWrapper modelWrapper) { // удаление модели со сцены
        modelWrappers.remove(modelWrapper); // удаляем из списка
        selectionManager.deselectModel(modelWrapper.getUIModel()); // снимаем выделение
    }

    public void clear() { // очистка всей сцены
        modelWrappers.clear(); // очищаем список моделей
        selectionManager.clearSelection(); // очищаем выделение
    }
}