package de.delautrer.engine.graphics.meshing;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.blocks.TorchBlock;
import de.delautrer.game.blocks.TorchBlock.TorchAttach;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3f;

public class TorchMesher implements BlockMesher {
    private final TorchBlock torchBlock;

    public TorchMesher(TorchBlock block) {
        this.torchBlock = block;
    }

    @Override
    public void generate(BlockState state, int x, int y, int z, Chunk chunk, ChunkManager cm) {
        TorchAttach attach = state.getValue(TorchBlock.ATTACH);
        Vector3f[] v = TorchBlock.VERTS[attach.ordinal()];
        float light = 1.0f;

        TextureStitcher.AtlasRegion reg = torchBlock.getModel().top;
        if (reg == null)
            return;

        addQuad(chunk, x, y, z, v[4], v[7], v[6], v[5], 7f / 16f, 6f / 16f, 9f / 16f, 8f / 16f, reg, light);
        addQuad(chunk, x, y, z, v[3], v[0], v[1], v[2], 7f / 16f, 14f / 16f, 9f / 16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[3], v[2], v[6], v[7], 7f / 16f, 6f / 16f, 9f / 16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[1], v[0], v[4], v[5], 7f / 16f, 6f / 16f, 9f / 16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[0], v[3], v[7], v[4], 7f / 16f, 6f / 16f, 9f / 16f, 1.0f, reg, light);
        addQuad(chunk, x, y, z, v[2], v[1], v[5], v[6], 7f / 16f, 6f / 16f, 9f / 16f, 1.0f, reg, light);
    }

    private void addQuad(Chunk chunk, int x, int y, int z, Vector3f vec0, Vector3f vec1, Vector3f vec2, Vector3f vec3,
            float lu0, float lv0, float lu1, float lv1, TextureStitcher.AtlasRegion reg, float light) {
        float u0 = reg.u0 + (reg.u1 - reg.u0) * lu0;
        float v0 = reg.v0 + (reg.v1 - reg.v0) * lv0;
        float u1 = reg.u0 + (reg.u1 - reg.u0) * lu1;
        float v1 = reg.v0 + (reg.v1 - reg.v0) * lv1;
        chunk.addFace(
                x + vec0.x, y + vec0.y, z + vec0.z, 1.0f,
                x + vec1.x, y + vec1.y, z + vec1.z, 1.0f,
                x + vec2.x, y + vec2.y, z + vec2.z, 1.0f,
                x + vec3.x, y + vec3.y, z + vec3.z, 1.0f,
                u0, v1, u1, v1, u1, v0, u0, v0,
                reg.layer, light, torchBlock, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
    }
}
