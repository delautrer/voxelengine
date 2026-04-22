package de.delautrer.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector3f;
import java.util.List;

public class RenderPacket {
    public Matrix4f mvp, proj, view, ortho;

    // Die neuen Listen für das Wasser-Splitting
    public List<VulkanMesh> opaqueMeshes;
    public List<VulkanMesh> waterMeshes;

    public VulkanMesh highlightMesh;
    public VulkanMesh uiMesh;
    public VulkanMesh topUiMesh;
    public VulkanMesh textMesh;
    public VulkanMesh itemMesh;

    public VulkanMesh cloudMesh;
    public Vector3f cloudOffset;
    public Vector3f sunDirection;

    public Vector3f cameraPos;
    public float renderDistance;

    public boolean isUnderwater;

    public VulkanTexture fontTexture;
    public VulkanTextureArray worldTexture;
    public VulkanTexture uiTexture;
    public VulkanTexture itemTexture;
    public VulkanTexture blockUITexture;
    public VulkanMesh overlayMesh;

    public Vector3i selectedBlockPos;
    public float globalLight, skyR, skyG, skyB;
}