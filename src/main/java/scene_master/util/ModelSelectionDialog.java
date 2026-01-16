package scene_master.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import scene_master.model.ModelWrapper;

import java.util.List;

public class ModelSelectionDialog {

    public static ModelWrapper showModelSelectionDialog(
            List<ModelWrapper> models, String title) {

        if (models == null || models.isEmpty()) {
            DialogHelper.showWarningDialog("Ошибка", "Нет доступных моделей");
            return null;
        }

        Dialog<ModelWrapper> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Выберите модель:");

        ButtonType selectButtonType = new ButtonType("Выбрать",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                selectButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> modelComboBox = new ComboBox<>();
        for (ModelWrapper wrapper : models) {
            modelComboBox.getItems().add(wrapper.nameProperty().get());
        }

        if (!models.isEmpty()) {
            modelComboBox.getSelectionModel().selectFirst();
        }

        grid.add(new Label("Модель:"), 0, 0);
        grid.add(modelComboBox, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                int selectedIndex = modelComboBox.getSelectionModel()
                        .getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < models.size()) {
                    return models.get(selectedIndex);
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }
}