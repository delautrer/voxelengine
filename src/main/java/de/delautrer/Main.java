package de.delautrer;

import de.delautrer.engine.Engine;
import de.delautrer.engine.utils.GameLogger;

public class Main {
    public static void main(String[] args) {
        GameLogger.init(Constants.IS_DEV);

        Engine engine = new Engine();
        engine.run();
    }
}