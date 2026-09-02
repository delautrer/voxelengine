package de.delautrer.game.commands;

import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FindCommandTest {

    public static void main(String[] args) {
        FindCommandTest test = new FindCommandTest();
        test.testTabCompletions();
        System.out.println("FindCommandTest: Tab completions for /find structure verified!");
    }

    @Test
    public void testTabCompletions() {
        Registries.init();
        FindCommand cmd = new FindCommand();

        List<String> subCmds = cmd.getTabCompletions(null, new String[]{""});
        Assertions.assertTrue(subCmds.contains("biome"), "Tab completion should contain 'biome'");
        Assertions.assertTrue(subCmds.contains("structure"), "Tab completion should contain 'structure'");

        List<String> structures = cmd.getTabCompletions(null, new String[]{"structure", ""});
        Assertions.assertFalse(structures.isEmpty(), "Structure tab completion should not be empty");
        Assertions.assertTrue(structures.contains("desert_camp") || structures.contains("oak_ruin"), "Should suggest structure names");
    }
}
