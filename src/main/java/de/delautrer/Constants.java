package de.delautrer;

import java.io.File;

public class Constants {
    public static final String VERSION = "0.0.4.0_alpha";
    public static final String NAMESPACE = "engine";

    public static final boolean IS_DEV = checkIsDev();
    public static final boolean DEBUG = true;
    public static final boolean VULKAN_DEBUG = false;

    public static final String GUI_FONT_NAME = "monogram-extended.ttf";
    public static final float GUI_FONT_HEIGHT = 24.0f;

    private static boolean checkIsDev() {
        try {
            java.net.URL resource = Constants.class.getResource("Constants.class");
            if (resource != null) {
                return "file".equals(resource.getProtocol());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}