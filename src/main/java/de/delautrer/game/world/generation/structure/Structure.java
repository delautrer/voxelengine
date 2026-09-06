package de.delautrer.game.world.generation.structure;

import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.generation.structure.processor.GravityProcessor;
import de.delautrer.game.world.generation.structure.processor.StructureProcessor;

import java.util.List;
import java.util.Set;

public class Structure {
    private final NamespacedKey key;
    private final String type;
    private final StructureTemplate template;
    private final NamespacedKey startPoolKey;
    private final int maxDepth;
    private final String step;
    private final Set<NamespacedKey> allowedBiomes;
    private final List<StructureProcessor> processors;

    // Single template constructor
    public Structure(NamespacedKey key, StructureTemplate template, String step, Set<NamespacedKey> allowedBiomes, List<StructureProcessor> processors) {
        this(key, "template", template, null, 0, step, allowedBiomes, processors);
    }

    // Full constructor (including jigsaw)
    public Structure(NamespacedKey key, String type, StructureTemplate template, NamespacedKey startPoolKey, int maxDepth, String step, Set<NamespacedKey> allowedBiomes, List<StructureProcessor> processors) {
        this.key = key;
        this.type = type != null ? type : "template";
        this.template = template;
        this.startPoolKey = startPoolKey;
        this.maxDepth = maxDepth > 0 ? maxDepth : 3;
        this.step = (step != null) ? step.toLowerCase() : "surface_structures";
        this.allowedBiomes = allowedBiomes;
        this.processors = processors;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public boolean isJigsaw() {
        return "jigsaw".equalsIgnoreCase(type);
    }

    public StructureTemplate getTemplate() {
        return template;
    }

    public NamespacedKey getStartPoolKey() {
        return startPoolKey;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public String getStep() {
        return step;
    }

    public Set<NamespacedKey> getAllowedBiomes() {
        return allowedBiomes;
    }

    public List<StructureProcessor> getProcessors() {
        return processors;
    }

    public boolean isBiomeAllowed(NamespacedKey biomeKey) {
        if (allowedBiomes == null || allowedBiomes.isEmpty()) {
            return true;
        }
        return allowedBiomes.contains(biomeKey);
    }

    public boolean hasGravityProcessor() {
        if (processors == null) return false;
        for (StructureProcessor p : processors) {
            if (p instanceof GravityProcessor) return true;
        }
        return false;
    }
}
