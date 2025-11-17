package org.newdawn.spaceinvaders.rendering;

import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 * Utility class for loading image resources from classpath.
 * Handles multiple path variants and provides logging for missing resources.
 */
public final class ResourceLoader {

    private ResourceLoader() {
        throw new AssertionError("Cannot instantiate ResourceLoader");
    }

    /**
     * Try multiple classpath variants to load an image resource; logs if not found
     * @param candidates Array of candidate paths to try
     * @return Loaded image or null if not found
     */
    public static Image loadImageResource(String... candidates) {
        for (String candidate : candidates) {
            Image image = tryLoadImageCandidate(candidate);
            if (image != null) {
                return image;
            }
        }
        System.out.println("[WARN] Image resource not found: " + java.util.Arrays.toString(candidates));
        return null;
    }

    /**
     * Try to load image from a single candidate path
     * @param candidate Path to try
     * @return Loaded image or null
     */
    private static Image tryLoadImageCandidate(String candidate) {
        if (candidate == null || candidate.isEmpty()) {
            return null;
        }

        String[] probes = new String[] { candidate, "/" + candidate };
        for (String probe : probes) {
            Image image = tryLoadImageFromProbe(probe);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    /**
     * Try to load image from a specific probe path
     * @param probe Path to probe
     * @return Loaded image or null
     */
    private static Image tryLoadImageFromProbe(String probe) {
        URL url = findResourceURL(probe);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        return null;
    }

    /**
     * Find resource URL using class loader and context class loader
     * @param path Path to find
     * @return URL if found, null otherwise
     */
    private static URL findResourceURL(String path) {
        URL url = ResourceLoader.class.getResource(path);
        if (url == null) {
            String adjustedPath = path.startsWith("/") ? path.substring(1) : path;
            url = Thread.currentThread().getContextClassLoader().getResource(adjustedPath);
        }
        return url;
    }

    /**
     * Load item icons from resources matching provided item list
     * @param itemList List of item filenames
     * @return List of loaded images
     */
    public static java.util.List<Image> loadItemUIIcons(java.util.List<String> itemList) {
        java.util.List<Image> icons = new java.util.ArrayList<>();
        for (String name : itemList) {
            String baseFilename = resolveItemIconFilename(name);
            Image img = loadItemIconImage(baseFilename);
            icons.add(img);
        }
        return icons;
    }

    /**
     * Resolve item icon filename from item name
     * @param name Item name
     * @return Icon filename
     */
    private static String resolveItemIconFilename(String name) {
        String lower = name == null ? "" : name.toLowerCase();
        if (isImageFilename(lower)) {
            return name;
        }
        return mapNameToIconFilename(lower);
    }

    /**
     * Check if string is an image filename
     * @param lower Lowercase filename
     * @return true if it's an image extension
     */
    private static boolean isImageFilename(String lower) {
        return lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    /**
     * Map item name to icon filename
     * @param lower Lowercase item name
     * @return Icon filename
     */
    private static String mapNameToIconFilename(String lower) {
        switch (lower) {
            case "ammo":          return "item_ammo_boost.png";
            case "score":         return "item_double_score.png";
            case "invincibility": return "item_invincibility.png";
            case "life":          return "item_plusLife.png";
            default:              return "item_unknown.png";
        }
    }

    /**
     * Load item icon image from base filename
     * @param base Base filename
     * @return Loaded image
     */
    private static Image loadItemIconImage(String base) {
        return loadImageResource(
                "sprites/" + base,
                "org/newdawn/spaceinvaders/sprites/" + base,
                "resources/sprites/" + base
        );
    }
}
