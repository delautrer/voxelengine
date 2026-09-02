package de.delautrer.game.entity;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class FallingBlockEntity extends Entity {
    private final Block block;
    private final byte blockState;

    public FallingBlockEntity(Block block, byte blockState, Vector3d spawnPos) {
        super(spawnPos);
        this.block = block;
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

        int landY = -1;
        for (int curY = Math.min(y, Chunk.MAX_Y - 1); curY >= Chunk.MIN_Y; curY--) {
            Block b = world.getBlock(x, curY, z);
            if (b != null && b.isSolid && !(b instanceof de.delautrer.game.blocks.WaterBlock) && !b.isPassable && !b.isAir()) {
                landY = curY + 1;
                break;
            }
        }

        if (landY >= Chunk.MIN_Y && landY < Chunk.MAX_Y) {
            Block targetBlock = world.getBlock(x, landY, z);
            if (targetBlock == null || targetBlock.isAir() || targetBlock instanceof de.delautrer.game.blocks.WaterBlock || targetBlock.isPassable || !targetBlock.isSolid) {
                world.setBlockWithState(x, landY, z, block, blockState, true);
                this.setDead(true);
                return;
            }
        }

        // Spawn ItemEntity if block cannot be placed
        de.delautrer.game.registry.NamespacedKey key = de.delautrer.game.registry.Registries.BLOCKS.getKey(block);
        de.delautrer.game.items.Item item = (key != null) ? de.delautrer.game.registry.Registries.ITEMS.get(key.toString()) : null;
        if (item != null) {
            de.delautrer.game.entity.ItemEntity ie = new de.delautrer.game.entity.ItemEntity(
                new de.delautrer.game.items.ItemStack(item, 1),
                new Vector3d(position.x, position.y, position.z),
                new Vector3f(0, 0, 0)
            );
            world.spawnEntity(ie);
        }
        this.setDead(true);
    }

    public Block getBlock() {
        return block;
    }

    public byte getBlockState() {
        return blockState;
    }
}
