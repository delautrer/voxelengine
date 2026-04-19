package de.delautrer.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector3f;
import java.util.List;

public class RenderPacket {
    public Matrix4f mvp, proj, view, ortho;
    public List<VulkanMesh> visibleMeshes;
    public VulkanMesh highlightMesh;
    public VulkanMesh uiMesh;
    public VulkanMesh textMesh;
    public VulkanMesh itemMesh;

    public Matrix4f cameraProj;
    public Matrix4f cameraView;

    public Vector3f cameraPos;

    public VulkanTexture blockUITexture;
    public VulkanMesh overlayMesh;

    public float renderDistance;

    public VulkanMesh cloudMesh;
    public Vector3f cloudOffset;
    public Vector3f sunDirection;

    public VulkanTexture fontTexture;
    public VulkanTextureArray worldTexture;
    public VulkanTexture uiTexture;
    public VulkanTexture itemTexture;

    public Vector3i selectedBlockPos;
    public float globalLight, skyR, skyG, skyB;
}