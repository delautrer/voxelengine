package de.delautrer.game.world.generation.structure;

import de.delautrer.game.registry.Registries;

public class StructureRegistryTest {

    public static void main(String[] args) throws Exception {
        StructureRegistryTest test = new StructureRegistryTest();
        test.testStructureLoading();
        System.out.println("StructureRegistryTest: All structure templates, structures, and structure sets loaded successfully!");
    }

    public void testStructureLoading() {
        Registries.init();
        if (StructureRegistry.getTemplatesCount() < 2) {
            throw new IllegalStateException("Expected at least 2 templates, got " + StructureRegistry.getTemplatesCount());
        }
        if (StructureRegistry.getStructuresCount() < 2) {
            throw new IllegalStateException("Expected at least 2 structures, got " + StructureRegistry.getStructuresCount());
        }
        if (StructureRegistry.getStructureSetsCount() < 2) {
            throw new IllegalStateException("Expected at least 2 structure sets, got " + StructureRegistry.getStructureSetsCount());
        }
    }
}
