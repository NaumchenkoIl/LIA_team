package scene_master.util;

import javafx.scene.image.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextureLoader {

    public static Image loadTexture(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return new Image(is);
        }
    }

    public static Image loadTexture(String path) {
        return new Image(path);
    }

    public static Image createDefaultTexture(int width, int height) {
        // создаем простую текстуру по умолчанию (шахматная доска)
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(width, height);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        int tileSize = 32;
        boolean color = true;

        for (int y = 0; y < height; y += tileSize) {
            for (int x = 0; x < width; x += tileSize) {
                gc.setFill(color ? javafx.scene.paint.Color.LIGHTGRAY : javafx.scene.paint.Color.DARKGRAY);
                gc.fillRect(x, y, tileSize, tileSize);
                color = !color;
            }
            color = !color;
        }

        return canvas.snapshot(null, null);
    }
}