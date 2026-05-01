package de.delautrer;

import de.delautrer.engine.Engine;
import de.delautrer.engine.utils.GameLogger;
import de.delautrer.engine.utils.GamePaths;
import de.delautrer.game.settings.SettingsManager;

public class Main {
    public static void main(String[] args) {
        GamePaths.initDirectories();
        GameLogger.init(Constants.IS_DEV);
        SettingsManager.load();

        Engine engine = new Engine();
        engine.run();
    }
}