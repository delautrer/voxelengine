package de.delautrer.engine.graphics;

import de.delautrer.game.entity.Entity;
import de.delautrer.game.ui.elements.UIDrawCall;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3f;
import java.util.List;

public class RenderPacket {
    public Matrix4f mvp, proj, view, ortho;

    public List<VulkanMesh> opaqueMeshes;
    public List<VulkanMesh> waterMeshes;

    public List<Entity> entities;

    public VulkanMesh highlightMesh;

    public VulkanMesh uiCombinedMesh;
    public List<UIDrawCall> uiDrawCalls;

    public VulkanMesh cloudMesh;
    public Vector3f cloudOffset;
    public Vector3f sunDirection;
    public VulkanMesh celestialMesh;
    public VulkanMesh starMesh;
    public float starAlpha;
    public float timeOfDay;

    public boolean hideUI = false;
    public float clipY = -999.0f;

    public Vector3d cameraPos;
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