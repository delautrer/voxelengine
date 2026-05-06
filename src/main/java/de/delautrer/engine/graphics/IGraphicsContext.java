package de.delautrer.engine.graphics;

/**
 * Abstraction over the GPU backend context. Provides lifecycle management
 * and a factory for creating all GPU resources.
 */
public interface IGraphicsContext {
    /** Blocks until the GPU has finished all pending work. */
    void waitIdle();

    /** Returns the factory used to create GPU resources (meshes, textures, fonts). */
    IGraphicsFactory getGraphicsFactory();

    /** Releases all backend resources. */
    void cleanup();
}
