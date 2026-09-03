package de.delautrer.game.blocks;

import de.delautrer.engine.graphics.meshing.BlockMesher;

public class StructureVoidBlock extends Block {

    public StructureVoidBlock() {
        super(false, true, true, true);
        setHardness(-1.0f);
        setLootTable(null);
    }

    @Override
    public BlockMesher getMesher() {
        return null;
    }
}
