package de.delautrer.game.testing;

public class GameTestResult {
    private final boolean passed;
    private final GameTest test;
    private final int failedStepIndex;
    private final String message;

    private GameTestResult(boolean passed, GameTest test, int failedStepIndex, String message) {
        this.passed = passed;
        this.test = test;
        this.failedStepIndex = failedStepIndex;
        this.message = message;
    }

    public static GameTestResult pass(GameTest test) {
        return new GameTestResult(true, test, -1, "PASS");
    }

    public static GameTestResult fail(GameTest test, int failedStepIndex, String message) {
        return new GameTestResult(false, test, failedStepIndex, message);
    }

    public boolean isPassed() {
        return passed;
    }

    public GameTest getTest() {
        return test;
    }

    public int getFailedStepIndex() {
        return failedStepIndex;
    }

    public String getMessage() {
        return message;
    }
}
