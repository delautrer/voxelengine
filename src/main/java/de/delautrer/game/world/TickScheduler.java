package de.delautrer.game.world;

import org.joml.Vector3i;
import de.delautrer.game.blocks.Block;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;


public class TickScheduler {

    private static class ScheduledTick implements Comparable<ScheduledTick> {
        public final Vector3i pos;
        public final Block block;
        public final long triggerTick;

        public ScheduledTick(Vector3i pos, Block block, long triggerTick) {
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
    private final Set<Vector3i> scheduledPositions = new HashSet<>();

    private final World world;
    private long currentTick = 0;
    private float tickTimer = 0.0f;
    private static final float TICK_RATE = 1.0f / 20.0f;

    public TickScheduler(World world) { this.world = world; }

    public void update(float deltaTime) {
        tickTimer += deltaTime;
        while (tickTimer >= TICK_RATE) {
            tickTimer -= TICK_RATE;
            currentTick++;
            executeTicks();
        }
    }

    private void executeTicks() {
        while (!tickQueue.isEmpty() && tickQueue.peek().triggerTick <= currentTick) {
            ScheduledTick tick = tickQueue.poll();
            scheduledPositions.remove(tick.pos);

            byte currentBlockId = world.getBlockAt(tick.pos.x, tick.pos.y, tick.pos.z);
            if (currentBlockId == tick.block.getId()) {
                tick.block.scheduledTick(world, tick.pos.x, tick.pos.y, tick.pos.z);
            }
        }
    }

    public void scheduleTick(Vector3i pos, Block block, int delayInTicks) {
        if (scheduledPositions.add(pos)) {
            tickQueue.add(new ScheduledTick(pos, block, currentTick + delayInTicks));
        }
    }
}