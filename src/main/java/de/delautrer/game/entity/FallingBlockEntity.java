package de.delautrer.game.entity;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class FallingBlockEntity extends Entity {
    private final byte blockId;
    private final byte blockState;

    public FallingBlockEntity(byte blockId, byte blockState, Vector3d spawnPos) {
        super(spawnPos);
        this.blockId = blockId;
        this.blockState = blockState;
        this.width = 0.45f;
        this.height = 0.9f;
    }

    @Override
    public void update(float deltaTime, ChunkManager chunkManager) {
        // We overide with World reference below
    }

    public void update(float deltaTime, ChunkManager cm, World world) {
        if (isDead) return;

        velocity.y += gravity * deltaTime;
        
        double prevY = position.y;
        moveAndCollide(cm, deltaTime, false);
        
        // If we hit the ground or stopped moving vertically
        if (onGround || Math.abs(position.y - prevY) < 0.001) {
            land(world);
            return;
        }

        // Damage players in the way
        for (Entity e : world.getEntities()) {
            if (e instanceof de.delautrer.game.entity.player.Player player) {
                if (de.delautrer.engine.physics.AABB.isColliding(this.getAABB(), player.getAABB())) {
                    player.damage(2.0f); // 1 Heart damage per hit/tick while falling on them
                }
            }
        }
    }

    private void land(World world) {
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y + 0.1);
        int z = (int) Math.floor(position.z);

        if (world.getBlockAt(x, y, z) == 0) {
            world.setBlockWithState(x, y, z, blockId, blockState);
            this.setDead(true);
        } else if (world.getBlockAt(x, y + 1, z) == 0) {
            world.setBlockWithState(x, y + 1, z, blockId, blockState);
            this.setDead(true);
        } else {
            // Cannot land here, just die (or drop as item?)
            this.setDead(true);
        }
    }

    public byte getBlockId() {
        return blockId;
    }

    public byte getBlockState() {
        return blockState;
    }
}
