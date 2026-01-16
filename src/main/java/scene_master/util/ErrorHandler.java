package scene_master.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorHandler {

    public static void handleException(Throwable throwable, String context) {
        System.err.println("ОШИБКА");// логируем в консоль
        System.err.println("Контекст: " + context);
        System.err.println("Сообщение: " + throwable.getMessage());
        System.err.println("Тип: " + throwable.getClass().getName());
        throwable.printStackTrace();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Критическая ошибка");
            alert.setHeaderText("Произошла ошибка в " + context);

            StringWriter sw = new StringWriter();// детали ошибки
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String exceptionText = sw.toString();

            String details = throwable.getMessage() + "\n\n" +
                    "Тип ошибки: " + throwable.getClass().getSimpleName();

            if (exceptionText.length() > 1000) {
                details += "\n\nДетали (обрезано):\n" +
                        exceptionText.substring(0, 1000) + "...";
            } else {
                details += "\n\nДетали:\n" + exceptionText;
            }

            alert.setContentText(details);

            alert.getDialogPane().setPrefSize(600, 400);

            alert.showAndWait();
        });
    }

    public static void handleWarning(String message, String context) {
        System.err.println("ПРЕДУПРЕЖДЕНИЕ");
        System.err.println("Контекст: " + context);
        System.err.println("Сообщение: " + message);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение");
            alert.setHeaderText("Внимание в " + context);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void logInfo(String message, String context) {
        System.out.println("ИНФОРМАЦИЯ");
        System.out.println("Контекст: " + context);
        System.out.println("Сообщение: " + message);
    }
}