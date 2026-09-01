package de.delautrer.game.world;

import java.util.Objects;
import org.joml.Vector3i;

public final class BlockPos implements Comparable<BlockPos> {
    public final int x;
    public final int y;
    public final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos(Vector3i vec) {
        this(vec.x, vec.y, vec.z);
    }

    public Vector3i toVector3i() {
        return new Vector3i(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPos blockPos = (BlockPos) o;
        return x == blockPos.x && y == blockPos.y && z == blockPos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public int compareTo(BlockPos o) {
        if (y != o.y) return Integer.compare(y, o.y);
        if (z != o.z) return Integer.compare(z, o.z);
        return Integer.compare(x, o.x);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}