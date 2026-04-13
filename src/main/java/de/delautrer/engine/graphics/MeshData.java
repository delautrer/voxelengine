package de.delautrer.engine.graphics;

public class MeshData {
    public final float[] vertices;
    public final int[] indices;

    public MeshData(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
    }
}