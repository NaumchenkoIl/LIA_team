package scene_master.renderer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {
    private static TextureManager instance;
    private final Map<String, Image> textures = new HashMap<>();
    private final Map<String, int[][]> textureData = new HashMap<>();

    private Image defaultTexture;

    private TextureManager() {
        createDefaultTexture();
    }

    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }

    private void createDefaultTexture() {
        int size = 256;
        WritableImage image = new WritableImage(size, size);
        var pixelWriter = image.getPixelWriter();

        int tileSize = 32;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean isDark = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                Color color = isDark ? Color.DARKGRAY : Color.LIGHTGRAY;
                pixelWriter.setColor(x, y, color);
            }
        }

        defaultTexture = image;
        cacheTextureData("default", defaultTexture);
    }

    public Image loadTexture(File file) throws IOException {
        String path = file.getAbsolutePath();

        if (textures.containsKey(path)) {
            return textures.get(path);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Image texture = new Image(fis);
            textures.put(path, texture);
            cacheTextureData(path, texture);
            return texture;
        }
    }

    public Image loadTexture(String resourcePath) {
        if (textures.containsKey(resourcePath)) {
            return textures.get(resourcePath);
        }

        Image texture = new Image(resourcePath);
        if (!texture.isError()) {
            textures.put(resourcePath, texture);
            cacheTextureData(resourcePath, texture);
        }
        return texture;
    }

    private void cacheTextureData(String key, Image texture) {
        int width = (int) texture.getWidth();
        int height = (int) texture.getHeight();
        int[][] data = new int[width][height];

        PixelReader reader = texture.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[x][y] = reader.getArgb(x, y);
            }
        }

        textureData.put(key, data);
    }

    public Image getDefaultTexture() {
        return defaultTexture;
    }

    public Color getTextureColor(Image texture, double u, double v) {
        if (texture == null) return Color.WHITE;

        u = u - Math.floor(u);
        v = v - Math.floor(v);

        int x = (int) (u * (texture.getWidth() - 1));
        int y = (int) ((1 - v) * (texture.getHeight() - 1));

        x = Math.max(0, Math.min(x, (int) texture.getWidth() - 1));
        y = Math.max(0, Math.min(y, (int) texture.getHeight() - 1));

        return texture.getPixelReader().getColor(x, y);
    }

    public int getTextureArgb(Image texture, double u, double v) {
        if (texture == null) return 0xFFFFFFFF;

        String key = findTextureKey(texture);
        if (key != null && textureData.containsKey(key)) {
            int[][] data = textureData.get(key);

            u = u - Math.floor(u);
            v = v - Math.floor(v);

            int x = (int) (u * (data.length - 1));
            int y = (int) ((1 - v) * (data[0].length - 1));

            x = Math.max(0, Math.min(x, data.length - 1));
            y = Math.max(0, Math.min(y, data[0].length - 1));

            return data[x][y];
        }

        return 0xFFFFFFFF;
    }

    private String findTextureKey(Image texture) {
        for (Map.Entry<String, Image> entry : textures.entrySet()) {
            if (entry.getValue() == texture) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void clear() {
        textures.clear();
        textureData.clear();
        createDefaultTexture();
    }
}