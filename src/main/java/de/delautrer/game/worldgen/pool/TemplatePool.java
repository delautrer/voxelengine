package de.delautrer.game.worldgen.pool;

import de.delautrer.game.registry.NamespacedKey;
import java.util.List;
import java.util.Random;

public class TemplatePool {

    public static class PoolElement {
        private final int weight;
        private final String elementType;
        private final NamespacedKey templateKey;
        private final String projection;

        public PoolElement(int weight, String elementType, NamespacedKey templateKey, String projection) {
            this.weight = weight;
            this.elementType = elementType;
            this.templateKey = templateKey;
            this.projection = projection != null ? projection : "rigid";
        }

        public PoolElement(int weight, String elementType, NamespacedKey templateKey) {
            this(weight, elementType, templateKey, "rigid");
        }

        public int getWeight() { return weight; }
        public String getElementType() { return elementType; }
        public NamespacedKey getTemplateKey() { return templateKey; }
        public String getProjection() { return projection; }
    }

    private final NamespacedKey key;
    private final NamespacedKey fallbackKey;
    private final List<PoolElement> elements;
    private final int totalWeight;

    public TemplatePool(NamespacedKey key, NamespacedKey fallbackKey, List<PoolElement> elements) {
        this.key = key;
        this.fallbackKey = fallbackKey;
        this.elements = elements;
        int sum = 0;
        if (elements != null) {
            for (PoolElement el : elements) {
                sum += Math.max(0, el.weight);
            }
        }
        this.totalWeight = sum;
    }

    public NamespacedKey getKey() { return key; }
    public NamespacedKey getFallbackKey() { return fallbackKey; }
    public List<PoolElement> getElements() { return elements; }

    public PoolElement pickWeighted(Random random) {
        if (elements == null || elements.isEmpty() || totalWeight <= 0) return null;
        int r = random.nextInt(totalWeight);
        int current = 0;
        for (PoolElement el : elements) {
            current += Math.max(0, el.weight);
            if (r < current) return el;
        }
        return elements.get(0);
    }
}
