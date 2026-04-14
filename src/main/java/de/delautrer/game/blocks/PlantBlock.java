package de.delautrer.game.blocks;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class PlantBlock extends Block {

    private final int texIndex;

    public PlantBlock(int texIndex) {
        super(false, true, true, true);
        this.texIndex = texIndex;
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        float light = 1.0f;

        float sl = chunk.getSmoothSkyLight(x, y, z, 0, 0, 0, 0, 0, 0, cm);
        float bl = chunk.getSmoothBlockLight(x, y, z, 0, 0, 0, 0, 0, 0, cm);

        // --- DER FAKE-AO TRICK ---
        float slBottom = sl * 0.67f;
        float blBottom = bl * 0.67f;

        // --- ERSTES KREUZ ---

        // Vorderseite (y, y, y+1, y+1  => Unten, Unten, Oben, Oben)
        chunk.addFace(
                x, y, z, 1.0f,
                x + 1, y, z + 1, 1.0f,
                x + 1, y + 1, z + 1, 1.0f,
                x, y + 1, z, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                texIndex, light, this,
                slBottom, slBottom, sl, sl, // <--- Die Magie!
                blBottom, blBottom, bl, bl  // <--- Die Magie!
        );

        // Rückseite
        chunk.addFace(
                x + 1, y, z + 1, 1.0f,
                x, y, z, 1.0f,
                x, y + 1, z, 1.0f,
                x + 1, y + 1, z + 1, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                texIndex, light, this,
                slBottom, slBottom, sl, sl,
                blBottom, blBottom, bl, bl
        );

        // --- ZWEITES KREUZ ---

        // Vorderseite
        chunk.addFace(
                x + 1, y, z, 1.0f,
                x, y, z + 1, 1.0f,
                x, y + 1, z + 1, 1.0f,
                x + 1, y + 1, z, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                texIndex, light, this,
                slBottom, slBottom, sl, sl,
                blBottom, blBottom, bl, bl
        );

        // Rückseite
        chunk.addFace(
                x, y, z + 1, 1.0f,
                x + 1, y, z, 1.0f,
                x + 1, y + 1, z, 1.0f,
                x, y + 1, z + 1, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                texIndex, light, this,
                slBottom, slBottom, sl, sl,
                blBottom, blBottom, bl, bl
        );
    }
}