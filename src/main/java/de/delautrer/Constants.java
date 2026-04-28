package de.delautrer;

public class Constants {
    public static final String VERSION = "0.0.3.1_alpha";
    public static final String NAMESPACE = "engine";
    public static final int RENDERDISTANCE = 6;

    public static final boolean IS_DEV = !Constants.class.getResource("Constants.class").toString().startsWith("jar:");
}