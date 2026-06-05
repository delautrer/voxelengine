package de.delautrer.engine.graphics.utils;

import de.delautrer.engine.graphics.MeshData;

import java.nio.ByteBuffer;

public class ItemMeshGenerator {
    private static boolean isSolid(ByteBuffer pixels, int px, int py, int texW, int texH) {
        int index = (py * texW + px) * 4;
        if (index < 0 || index + 3 >= pixels.capacity()) return false;
        int alpha = pixels.get(index + 3) & 0xFF;
        return alpha >= 128;
    }

    // Generate an extruded 3D mesh from a 2D texture region
    public static MeshData generateFromTexture(TextureStitcher.AtlasRegion reg, TextureStitcher.AtlasResult atlas) {
        int texW = atlas.atlasWidth;
        int texH = atlas.atlasHeight;

        int x0 = (int) (reg.u0 * texW);
        int y0 = (int) (reg.v0 * texH);
        int width = (int) ((reg.u1 - reg.u0) * texW + 0.5f);
        int height = (int) ((reg.v1 - reg.v0) * texH + 0.5f);

        java.util.List<Float> verts = new java.util.ArrayList<>();
        java.util.List<Integer> inds = new java.util.ArrayList<>();

        ByteBuffer pixels = atlas.atlasPixels;

        float thickness = 1.0f / 16.0f; // Exactly 1 pixel thick if icon is 16x16
        float pW = 1.0f / width;
        float pH = 1.0f / height;

        float r = 1, g = 1, b = 1, a = 1, sl = 1, bl = 1;

        // Front face (large quad)
        int globalOffset = verts.size() / 12;
        addQuad(verts, inds, globalOffset, 
            0, 0, thickness, reg.u0, reg.v1, // Bottom-Left
            1, 0, thickness, reg.u1, reg.v1, // Bottom-Right
            1, 1, thickness, reg.u1, reg.v0, // Top-Right
            0, 1, thickness, reg.u0, reg.v0, reg.layer); // Top-Left
        globalOffset += 4;

        // Back face (large quad)
        addQuad(verts, inds, globalOffset, 
            1, 0, 0, reg.u1, reg.v1, // Bottom-Left
            0, 0, 0, reg.u0, reg.v1, // Bottom-Right
            0, 1, 0, reg.u0, reg.v0, // Top-Right
            1, 1, 0, reg.u1, reg.v0, reg.layer); // Top-Left
        globalOffset += 4;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = x0 + x;
                int py = y0 + y;
                
                if (!isSolid(pixels, px, py, texW, texH)) continue;

                float rx0 = x * pW;
                float ry0 = 1.0f - y * pH;
                float rx1 = rx0 + pW;
                float ry1 = 1.0f - (y + 1) * pH;

                float u0 = reg.u0 + (float)x / texW;
                float v0 = reg.v0 + (float)y / texH;
                float u1 = u0 + 1.0f / texW;
                float v1 = v0 + 1.0f / texH;
                
                float uc = (u0 + u1) * 0.5f;
                float vc = (v0 + v1) * 0.5f;

                int offset = verts.size() / 12;

                // Top
                if (y == 0 || !isSolid(pixels, px, py - 1, texW, texH)) {
                    addQuad(verts, inds, offset,
                        rx0, ry0, thickness, uc, vc,
                        rx1, ry0, thickness, uc, vc,
                        rx1, ry0, 0, uc, vc,
                        rx0, ry0, 0, uc, vc, reg.layer);
                    offset += 4;
                }
                
                // Bottom
                if (y == height - 1 || !isSolid(pixels, px, py + 1, texW, texH)) {
                    addQuad(verts, inds, offset,
                        rx1, ry1, thickness, uc, vc,
                        rx0, ry1, thickness, uc, vc,
                        rx0, ry1, 0, uc, vc,
                        rx1, ry1, 0, uc, vc, reg.layer);
                    offset += 4;
                }
                
                // Left
                if (x == 0 || !isSolid(pixels, px - 1, py, texW, texH)) {
                    addQuad(verts, inds, offset,
                        rx0, ry1, thickness, uc, vc,
                        rx0, ry0, thickness, uc, vc,
                        rx0, ry0, 0, uc, vc,
                        rx0, ry1, 0, uc, vc, reg.layer);
                    offset += 4;
                }
                
                // Right
                if (x == width - 1 || !isSolid(pixels, px + 1, py, texW, texH)) {
                    addQuad(verts, inds, offset,
                        rx1, ry0, thickness, uc, vc,
                        rx1, ry1, thickness, uc, vc,
                        rx1, ry1, 0, uc, vc,
                        rx1, ry0, 0, uc, vc, reg.layer);
                    offset += 4;
                }
            }
        }

        float[] vArr = new float[verts.size()];
        for(int i = 0; i < verts.size(); i++) vArr[i] = verts.get(i);
        int[] iArr = new int[inds.size()];
        for(int i = 0; i < inds.size(); i++) iArr[i] = inds.get(i);

        return new MeshData(vArr, iArr);
    }

    public static MeshData generateBlockMesh(de.delautrer.game.blocks.models.BlockModelData model) {
        java.util.List<Float> verts = new java.util.ArrayList<>();
        java.util.List<Integer> inds = new java.util.ArrayList<>();
        float s = 1.0f;
        addQuad(verts, inds, 0,
            0, s, s, model.top.u0, model.top.v0,
            s, s, s, model.top.u1, model.top.v0,
            s, s, 0, model.top.u1, model.top.v1,
            0, s, 0, model.top.u0, model.top.v1, model.top.layer);
        addQuad(verts, inds, 4,
            0, 0, 0, model.bottom.u0, model.bottom.v0,
            s, 0, 0, model.bottom.u1, model.bottom.v0,
            s, 0, s, model.bottom.u1, model.bottom.v1,
            0, 0, s, model.bottom.u0, model.bottom.v1, model.bottom.layer);
        addQuad(verts, inds, 8,
            s, 0, 0, model.north.u0, model.north.v1,
            0, 0, 0, model.north.u1, model.north.v1,
            0, s, 0, model.north.u1, model.north.v0,
            s, s, 0, model.north.u0, model.north.v0, model.north.layer);
        addQuad(verts, inds, 12,
            0, 0, s, model.south.u0, model.south.v1,
            s, 0, s, model.south.u1, model.south.v1,
            s, s, s, model.south.u1, model.south.v0,
            0, s, s, model.south.u0, model.south.v0, model.south.layer);
        addQuad(verts, inds, 16,
            s, 0, s, model.east.u0, model.east.v1,
            s, 0, 0, model.east.u1, model.east.v1,
            s, s, 0, model.east.u1, model.east.v0,
            s, s, s, model.east.u0, model.east.v0, model.east.layer);
        addQuad(verts, inds, 20,
            0, 0, 0, model.west.u0, model.west.v1,
            0, 0, s, model.west.u1, model.west.v1,
            0, s, s, model.west.u1, model.west.v0,
            0, s, 0, model.west.u0, model.west.v0, model.west.layer);
        float[] vArr = new float[verts.size()];
        for(int i=0; i<vArr.length; i++) vArr[i] = verts.get(i);
        int[] iArr = new int[inds.size()];
        for(int i=0; i<iArr.length; i++) iArr[i] = inds.get(i);
        return new MeshData(vArr, iArr);
    }

    private static void addQuad(java.util.List<Float> verts, java.util.List<Integer> inds, int o,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float layer) {
        float r = 1, g = 1, b = 1, a = 1, sl = 1, bl = 1;
        verts.addAll(java.util.List.of(
            x0, y0, z0, r, g, b, a, u0, v0, layer, sl, bl,
            x1, y1, z1, r, g, b, a, u1, v1, layer, sl, bl,
            x2, y2, z2, r, g, b, a, u2, v2, layer, sl, bl,
            x3, y3, z3, r, g, b, a, u3, v3, layer, sl, bl
        ));
        inds.addAll(java.util.List.of(o, o+1, o+2, o+2, o+3, o));
    }
}
