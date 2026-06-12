package de.delautrer;

import java.io.File;

public class Constants {
    public static final String VERSION = "0.0.4.0_alpha";
    public static final String NAMESPACE = "engine";

    public static final boolean IS_DEV = checkIsDev();

    public static final String GUI_FONT_NAME = "monogram-extended.ttf";
    public static final float GUI_FONT_HEIGHT = 24.0f;

    private static boolean checkIsDev() {
        if (new File("src/main").exists()) {
            return true;
        }

        try {
            String path = Constants.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            return !(path.endsWith(".jar") || path.endsWith(".exe"));
        } catch (Exception e) {
            return false;
        }
    }
}