package scene_master.manager;

import scene_master.model.Model3D; // ui-представление модели
import javafx.collections.FXCollections; // утилиты для observable коллекций
import javafx.collections.ObservableList; // наблюдаемый список

public class SelectionManager {
    private final ObservableList<Model3D> selectedModels = FXCollections.observableArrayList(); // список выделенных моделей

    public ObservableList<Model3D> getSelectedModels() { // получаем выделенные модели
        return selectedModels; // возвращаем observable список
    }

    public void selectModel(Model3D model) { // выделение модели
        if (!selectedModels.contains(model)) { // если модель еще не выделена
            selectedModels.add(model); // добавляем в список выделенных
        }
    }

    public void deselectModel(Model3D model) { // снятие выделения с модели
        selectedModels.remove(model); // удаляем из списка выделенных
    }

    public void clearSelection() { // очистка всех выделений
        selectedModels.clear(); // очищаем список выделенных
    }

    public Model3D getActiveModel() { // получение активной (первой выделенной) модели
        return selectedModels.isEmpty() ? null : selectedModels.get(0); // если список пуст - null
    }// иначе первый элемент

    public boolean isSelected(Model3D model) { // проверка, выделена ли модель
        return selectedModels.contains(model); // проверяем наличие в списке выделенных
    }
}
