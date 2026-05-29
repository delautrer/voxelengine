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

    public List<IMesh> opaqueMeshes;
    public List<IMesh> waterMeshes;

    public List<Entity> entities;

    public IMesh highlightMesh;

    public IMesh uiCombinedMesh;
    public List<UIDrawCall> uiDrawCalls;

    public IMesh cloudMesh;
    public Vector3f cloudOffset;
    public float cloudGridSize;
    public Vector3f sunDirection;
    public IMesh celestialMesh;
    public IMesh starMesh;
    public float starAlpha;
    public float timeOfDay;

    public boolean hideUI = false;
    public float clipY = -999.0f;

    public Vector3d cameraPos;
    public float renderDistance;

    public boolean isUnderwater;

    public ITexture fontTexture;
    public ITextureArray worldTexture;
    public ITexture uiTexture;
    public ITexture itemTexture;
    public ITexture blockUITexture;
    public IMesh overlayMesh;

    public Vector3i selectedBlockPos;
    public float globalLight, skyR, skyG, skyB;
}
