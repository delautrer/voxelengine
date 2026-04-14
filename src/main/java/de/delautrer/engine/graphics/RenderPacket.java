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

    public VulkanMesh cloudMesh; // NEU
    public Vector3f cloudOffset; // NEU

    public VulkanTexture fontTexture;
    public VulkanTextureArray worldTexture;
    public VulkanTexture guiTexture;

    public Vector3i selectedBlockPos;
    public float globalLight, skyR, skyG, skyB;
}