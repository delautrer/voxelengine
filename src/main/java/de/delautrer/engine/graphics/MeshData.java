package de.delautrer.engine.graphics;

/**
 * Immutable data transfer object holding raw vertex and index data for a mesh.
 * Used to transfer mesh data from the game logic to the graphics backend.
 */
public record MeshData(float[] vertices, int[] indices) {}
