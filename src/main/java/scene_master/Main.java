package scene_master;

public class Main {
    public Main() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            scene_master.util.ErrorHandler.handleException(throwable,
                    "необработанное исключение в потоке " + thread.getName());
        });

        MainApplication.main(args);
    }
}