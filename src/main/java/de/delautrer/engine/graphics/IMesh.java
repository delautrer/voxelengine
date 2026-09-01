package de.delautrer.engine.graphics;

public interface IMesh {
    void cleanup();
    void updateMesh(float[] vertices, int[] indices);
    int getIndexCount();
    default void setChunkOffset(float x, float z) {}
}
