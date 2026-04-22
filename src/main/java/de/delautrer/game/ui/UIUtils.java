package de.delautrer.game.ui;

public class UIUtils {
    public static String formatItemName(String registryId) {
        if (registryId == null) return "Unknown Item";
        String name = registryId;
        if (name.contains(":")) name = name.split(":")[1]; // "delautrer:grass_block" -> "grass_block"
        name = name.replace("_", " ");
        // Erste Buchstaben großschreiben
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }
}
