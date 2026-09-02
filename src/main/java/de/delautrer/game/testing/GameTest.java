package de.delautrer.game.testing;

import de.delautrer.game.registry.NamespacedKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameTest {
    private final NamespacedKey id;
    private final int timeoutTicks;
    private final List<String> tags;
    private final String originMode; // "player" | "absolute"
    private final List<GameTestStep> steps;

    public GameTest(NamespacedKey id, int timeoutTicks, List<String> tags, String originMode, List<GameTestStep> steps) {
        this.id = id;
        this.timeoutTicks = timeoutTicks <= 0 ? 40 : timeoutTicks;
        this.tags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags)) : Collections.emptyList();
        this.originMode = originMode != null && !originMode.trim().isEmpty() ? originMode.trim() : "player";
        this.steps = steps != null ? Collections.unmodifiableList(new ArrayList<>(steps)) : Collections.emptyList();
    }

    public NamespacedKey getId() {
        return id;
    }

    public int getTimeoutTicks() {
        return timeoutTicks;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getOriginMode() {
        return originMode;
    }

    public List<GameTestStep> getSteps() {
        return steps;
    }

    public boolean hasTag(String tag) {
        if (tag == null) return false;
        for (String t : tags) {
            if (t.equalsIgnoreCase(tag)) return true;
        }
        return false;
    }
}
