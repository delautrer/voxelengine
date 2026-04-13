package de.delautrer.game.blocks;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

public class TorchBlock extends Block {

    private final int texLayer;

    public TorchBlock(int texLayer) {
        super(false, true, true);
        this.texLayer = texLayer;
        this.setLightEmission(14);
    }

    @Override
    public boolean shouldRenderFaceAgainst(Block neighborBlock, float myHeight, float neighborHeight) {
        return true;
    }

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        // --- 1. Die Sub-Voxel Geometrie (2x10x2 in der Mitte des Blocks) ---
        float minX = x + 7.0f / 16.0f;
        float maxX = x + 9.0f / 16.0f;
        float minY = y;
        float maxY = y + 10.0f / 16.0f;
        float minZ = z + 7.0f / 16.0f;
        float maxZ = z + 9.0f / 16.0f;

        // --- 2. Custom UV-Mapping (Wir schneiden die Fackel aus der Textur aus!) ---
        // Seiten der Fackel: X=7 bis 9, Y=10 bis 16 (Unten ausgerichtet)
        float u0 = 7.0f / 16.0f;
        float u1 = 9.0f / 16.0f;
        float v0 = 6.0f / 16.0f; // V0 ist Oben in Vulkan
        float v1 = 1.0f;          // V1 ist Unten

        // Oben der Fackel (2x2 Pixel aus dem oberen Bereich der Fackel-Textur)
        float topV0 = 6.0f / 16.0f;
        float topV1 = 8.0f / 16.0f;
        // Oben der Fackel (2x2 Pixel aus dem oberen Bereich der Fackel-Textur)
        float botV0 = 12.0f / 16.0f;
        float botV1 = 14.0f / 16.0f;

        // --- 3. Licht und AO ---
        float ao = 1.0f; // Fackeln werfen keine künstlichen Schatten auf sich selbst
        float dlSide = 0.8f, dlTop = 1.0f, dlBot = 0.4f;

        // Fackeln leuchten von sich aus, daher hat ihre eigene Textur IMMER 100% Blocklicht!
        float sl = chunk.getSkyLightAt(x, y, z, cm) / 15.0f; // Sonnenlicht aus dem Raum nehmen
        float bl = 1.0f; // 100% Blocklicht!

        // Z PLUS (Front)
        chunk.addFace(minX, minY, maxZ, ao,  maxX, minY, maxZ, ao,  maxX, maxY, maxZ, ao,  minX, maxY, maxZ, ao,
                u0, v0, u1, v1, texLayer, dlSide, this, sl, sl, sl, sl, bl, bl, bl, bl);

        // Z MINUS (Back)
        chunk.addFace(maxX, minY, minZ, ao,  minX, minY, minZ, ao,  minX, maxY, minZ, ao,  maxX, maxY, minZ, ao,
                u0, v0, u1, v1, texLayer, dlSide, this, sl, sl, sl, sl, bl, bl, bl, bl);

        // X MINUS (Left)
        chunk.addFace(minX, minY, minZ, ao,  minX, minY, maxZ, ao,  minX, maxY, maxZ, ao,  minX, maxY, minZ, ao,
                u0, v0, u1, v1, texLayer, dlSide, this, sl, sl, sl, sl, bl, bl, bl, bl);

        // X PLUS (Right)
        chunk.addFace(maxX, minY, maxZ, ao,  maxX, minY, minZ, ao,  maxX, maxY, minZ, ao,  maxX, maxY, maxZ, ao,
                u0, v0, u1, v1, texLayer, dlSide, this, sl, sl, sl, sl, bl, bl, bl, bl);

        // TOP
        chunk.addFace(minX, maxY, minZ, ao,  minX, maxY, maxZ, ao,  maxX, maxY, maxZ, ao,  maxX, maxY, minZ, ao,
                u0, topV0, u1, topV1, texLayer, dlTop, this, sl, sl, sl, sl, bl, bl, bl, bl);

        // BOTTOM
        chunk.addFace(minX, minY, maxZ, ao,  minX, minY, minZ, ao,  maxX, minY, minZ, ao,  maxX, minY, maxZ, ao,
                u0, botV0, u1, botV1, texLayer, dlBot, this, sl, sl, sl, sl, bl, bl, bl, bl);
    }

    @Override
    public float[] getHighlightVertices() {
        return new float[]{
                //   z, y, x
                7f/16f, 0, 7f/16f,  // A
                9f/16f, 0, 7f/16f,  // C
                9f/16f, 10f/16f, 7f/16f,  // G
                7f/16f, 10f/16f, 7f/16f,  // E
                7f/16f, 0, 9f/16f,  // B
                9f/16f, 0, 9f/16f,  // D
                9f/16f, 10f/16f, 9f/16f,  // H
                7f/16f, 10f/16f, 9f/16f,  // F
        };
    }

}