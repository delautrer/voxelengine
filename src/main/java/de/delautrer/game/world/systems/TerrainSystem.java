package de.delautrer.game.world.systems;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;

public class TerrainSystem implements WorldSystem {

    private final ChunkManager chunkManager;

    public TerrainSystem(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Override
    public void update(World world, float deltaTime, LocalPlayer localPlayer) {
        chunkManager.getLightEngine().processLightUpdates();
        for (Chunk c : chunkManager.getLightEngine().getAndClearDirtiedChunks()) {
            c.requestMeshUpdate();
        }

        chunkManager.update(localPlayer.position.x, localPlayer.position.z);
        chunkManager.getAsyncBuilder().uploadReadyMeshes(chunkManager);
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }
}
