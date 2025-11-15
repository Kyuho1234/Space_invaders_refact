package org.newdawn.spaceinvaders;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {

    private static final Map<String, Image> imageCache = new HashMap<>();

    public static Image loadImage(String path) {
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }

        try {
            Image img = ImageIO.read(ResourceManager.class.getResource(path));
            imageCache.put(path, img);
            return img;
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Image load failed: " + path, e);
        }
    }
}