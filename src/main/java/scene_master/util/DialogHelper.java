package scene_master.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.application.Platform;

import java.util.Optional;

public class DialogHelper {

    public static void showErrorDialog(String title, String message) { // показать диалог ошибки
        ErrorHandler.handleWarning(message, title);
    }

    public static void showWarningDialog(String title, String message) { // показать диалог предупреждения
        ErrorHandler.handleWarning(message, title);
    }

    public static void showInfoDialog(String title, String message) { // показать информационный диалог
        ErrorHandler.logInfo(message, title);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static boolean showSaveOptionsDialog() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Исходная модель", "Исходная модель", "Модель с трансформациями");
        dialog.setTitle("Параметры сохранения");
        dialog.setHeaderText("Как сохранить модель?");
        dialog.setContentText("Выберите вариант:");

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && result.get().equals("Модель с трансформациями");
    }
}

