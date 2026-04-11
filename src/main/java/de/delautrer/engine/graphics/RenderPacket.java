package de.delautrer.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import java.util.List;

public class RenderPacket {
    public Matrix4f mvp, proj, view, ortho;
    public List<VulkanMesh> visibleMeshes;
    public VulkanMesh highlightMesh;
    public VulkanMesh uiMesh;

    public VulkanTextureArray worldTexture;
    public VulkanTexture guiTexture;

    public Vector3i selectedBlockPos;
    public float globalLight, skyR, skyG, skyB;
}