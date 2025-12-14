package scene_master.util;

import javafx.scene.control.Alert; // диалоговое окно

public class DialogHelper {

    public static void showErrorDialog(String title, String message) { // показать диалог ошибки
        showDialog(Alert.AlertType.ERROR, title, message); // вызываем общий метод с типом ERROR
    }

    public static void showWarningDialog(String title, String message) { // показать диалог предупреждения
        showDialog(Alert.AlertType.WARNING, title, message); // вызываем общий метод с типом WARNING
    }

    public static void showInfoDialog(String title, String message) { // показать информационный диалог
        showDialog(Alert.AlertType.INFORMATION, title, message); // вызываем общий метод с типом INFORMATION
    }

    private static void showDialog(Alert.AlertType type, String title, String message) { // общий метод создания диалога
        Alert alert = new Alert(type); // создаем диалог указанного типа
        alert.setTitle(title); // устанавливаем заголовок
        alert.setHeaderText(null); // убираем дополнительный заголовок
        alert.setContentText(message); // устанавливаем текст сообщения
        alert.showAndWait(); // показываем диалог и ждем закрытия
    }
}

