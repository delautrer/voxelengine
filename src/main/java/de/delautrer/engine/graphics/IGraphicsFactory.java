package de.delautrer.engine.graphics;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import java.nio.ByteBuffer;

public interface IGraphicsFactory {
    IMesh createMesh(float[] vertices, int[] indices);
    IMesh createMesh(MeshData data);
    ITexture createTexture(String path);
    ITexture createTexture(TextureStitcher.AtlasResult atlas);
    ITexture createTexture(ByteBuffer rgbaPixels, int width, int height);
    ITextureArray createTextureArray(TextureStitcher.AtlasResult atlas);
    ITextureArray createSingleLayerTextureArray(TextureStitcher.AtlasResult atlas);
    IFont createFont(String path, float size);
    void waitIdle();
}
