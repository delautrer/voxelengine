package de.delautrer.game.world;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import org.joml.Vector3i;
import java.util.*;

public class TickScheduler {

    public static final class TickKey {
        public final BlockPos pos;
        public final NamespacedKey blockKey;

        public TickKey(BlockPos pos, NamespacedKey blockKey) {
            this.pos = pos;
            this.blockKey = blockKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TickKey tickKey = (TickKey) o;
            return Objects.equals(pos, tickKey.pos) && Objects.equals(blockKey, tickKey.blockKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, blockKey);
        }
    }

    public static class ScheduledTick implements Comparable<ScheduledTick> {
        public final BlockPos pos;
        public final Block block;
        public final long triggerTick;

        public ScheduledTick(BlockPos pos, Block block, long triggerTick) {
            this.pos = pos;
            this.block = block;
            this.triggerTick = triggerTick;
        }

        @Override
        public int compareTo(ScheduledTick other) {
            return Long.compare(this.triggerTick, other.triggerTick);
        }
    }

    private final PriorityQueue<ScheduledTick> tickQueue = new PriorityQueue<>();
    private final Map<TickKey, ScheduledTick> scheduledMap = new HashMap<>();

    private final World world;
    private long currentTick = 0;
    private float tickTimer = 0.0f;
    private static final float TICK_RATE = 1.0f / 20.0f;

    public TickScheduler(World world) {
        this.world = world;
    }

    public void update(float deltaTime, LocalPlayer localPlayer) {
        tickTimer += deltaTime;
        while (tickTimer >= TICK_RATE) {
            tickTimer -= TICK_RATE;
            currentTick++;
            world.onTick(localPlayer);
            executeTicks();
        }
    }

    private void executeTicks() {
        while (!tickQueue.isEmpty() && tickQueue.peek().triggerTick <= currentTick) {
            ScheduledTick tick = tickQueue.poll();
            if (tick == null) continue;
            NamespacedKey key = Registries.BLOCKS.getKey(tick.block);
            if (key != null) {
                scheduledMap.remove(new TickKey(tick.pos, key));
            }

            Block currentBlock = world.getBlock(tick.pos.x, tick.pos.y, tick.pos.z);
            if (currentBlock == tick.block) {
                tick.block.scheduledTick(world, tick.pos.x, tick.pos.y, tick.pos.z);
            }
        }
    }

    public void scheduleTick(Vector3i pos, Block block, int delayInTicks) {
        scheduleTick(new BlockPos(pos), block, delayInTicks);
    }

    public void scheduleTick(BlockPos pos, Block block, int delayInTicks) {
        if (block == null) return;
        long trigger = currentTick + delayInTicks;
        restoreTick(pos, block, trigger);
    }

    public void restoreTick(BlockPos pos, Block block, long absoluteTriggerTick) {
        if (block == null || pos == null) return;
        NamespacedKey key = Registries.BLOCKS.getKey(block);
        if (key == null) key = NamespacedKey.fromString("veinstride:air");

        TickKey tk = new TickKey(pos, key);
        ScheduledTick existing = scheduledMap.get(tk);
        if (existing == null || absoluteTriggerTick < existing.triggerTick) {
            if (existing != null) {
                tickQueue.remove(existing);
            }
            ScheduledTick newTick = new ScheduledTick(pos, block, absoluteTriggerTick);
            tickQueue.add(newTick);
            scheduledMap.put(tk, newTick);
        }
    }

    public long getCurrentTick() { return currentTick; }
    public void setCurrentTick(long tick) { this.currentTick = tick; }

    public List<ScheduledTick> getTicksForChunk(int chunkX, int chunkZ) {
        List<ScheduledTick> list = new ArrayList<>();
        int minX = chunkX * Chunk.SIZE;
        int maxX = minX + Chunk.SIZE - 1;
        int minZ = chunkZ * Chunk.SIZE;
        int maxZ = minZ + Chunk.SIZE - 1;

        for (ScheduledTick tick : tickQueue) {
            if (tick.pos.x >= minX && tick.pos.x <= maxX && tick.pos.z >= minZ && tick.pos.z <= maxZ) {
                list.add(tick);
            }
        }
        return list;
    }

    public List<ScheduledTick> extractTicksForChunk(int chunkX, int chunkZ) {
        List<ScheduledTick> list = getTicksForChunk(chunkX, chunkZ);
        for (ScheduledTick tick : list) {
            tickQueue.remove(tick);
            NamespacedKey key = Registries.BLOCKS.getKey(tick.block);
            if (key != null) {
                scheduledMap.remove(new TickKey(tick.pos, key));
            }
        }
        return list;
    }
}