package de.delautrer.game.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DebugOverlay {
    private boolean isVisible = false;

    // LinkedHashMap behält die Reihenfolge bei, in der die Werte hinzugefügt wurden
    private final Map<String, Supplier<String>> debugLines = new LinkedHashMap<>();

    public void toggle() {
        isVisible = !isVisible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Fügt eine neue, dynamische Zeile zum Debug-Menü hinzu.
     * @param label Der Text, der Links steht (z.B. "FPS")
     * @param valueSupplier Eine Funktion, die den Wert berechnet (wird jeden Frame aufgerufen)
     */
    public void addLine(String label, Supplier<String> valueSupplier) {
        debugLines.put(label, valueSupplier);
    }

    public void removeLine(String label) {
        debugLines.remove(label);
    }

    /**
     * Generiert die Strings, die vom Text-Renderer auf den Bildschirm gezeichnet werden sollen.
     */
    public List<String> getLinesToRender() {
        List<String> lines = new ArrayList<>();
        if (!isVisible) return lines;

        for (Map.Entry<String, Supplier<String>> entry : debugLines.entrySet()) {
            lines.add(entry.getKey() + ": " + entry.getValue().get());
        }
        return lines;
    }
}