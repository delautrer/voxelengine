package de.delautrer;

import java.io.File;

public class Constants {
    public static final String VERSION = "0.0.3.1_alpha";
    public static final String NAMESPACE = "engine";
    public static final int RENDERDISTANCE = 6;

    public static final boolean IS_DEV = checkIsDev();

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