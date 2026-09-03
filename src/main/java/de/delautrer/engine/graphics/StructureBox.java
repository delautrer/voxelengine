package de.delautrer.engine.graphics;

import org.joml.Vector4f;

public class StructureBox {
    public final double minX, minY, minZ;
    public final double maxX, maxY, maxZ;
    public final Vector4f color;

    public StructureBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vector4f color) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.color = color;
    }
}
