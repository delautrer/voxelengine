package de.delautrer.game.particle;

import de.delautrer.engine.graphics.MeshData;
import de.delautrer.game.entity.Entity;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class ParticleManager {
    public ParticleManager() {
    }

    public MeshData generateMesh(List<Entity> entities, Vector3f cameraFront, Vector3f cameraUp) {
        List<Particle> particles = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Particle p && !p.isDead()) {
                particles.add(p);
            }
        }

        if (particles.isEmpty()) {
            return null;
        }

        int vertexCount = particles.size() * 4;
        int indexCount = particles.size() * 6;

        float[] vertices = new float[vertexCount * 12];
        int[] indices = new int[indexCount];

        int vIdx = 0;
        int iIdx = 0;
        int vertexOffset = 0;

        Vector3f right = new Vector3f(cameraFront).cross(cameraUp).normalize();
        Vector3f up = new Vector3f(right).cross(cameraFront).normalize();

        for (Particle p : particles) {
            float halfSize = p.size * 0.5f;
            
            Vector3f pRight = new Vector3f(right);
            Vector3f pUp = new Vector3f(up);
            
            if (p.rotation != 0.0f) {
                float c = (float) Math.cos(p.rotation);
                float s = (float) Math.sin(p.rotation);
                pRight.set(
                    right.x * c + up.x * s,
                    right.y * c + up.y * s,
                    right.z * c + up.z * s
                );
                pUp.set(
                    right.x * -s + up.x * c,
                    right.y * -s + up.y * c,
                    right.z * -s + up.z * c
                );
            }

            Vector3f pPos = new Vector3f((float)p.position.x, (float)p.position.y, (float)p.position.z);
            Vector3f p1 = new Vector3f(pPos).sub(new Vector3f(pRight).mul(halfSize)).sub(new Vector3f(pUp).mul(halfSize));
            Vector3f p2 = new Vector3f(pPos).add(new Vector3f(pRight).mul(halfSize)).sub(new Vector3f(pUp).mul(halfSize));
            Vector3f p3 = new Vector3f(pPos).add(new Vector3f(pRight).mul(halfSize)).add(new Vector3f(pUp).mul(halfSize));
            Vector3f p4 = new Vector3f(pPos).sub(new Vector3f(pRight).mul(halfSize)).add(new Vector3f(pUp).mul(halfSize));

            float uMin = p.hasTexture ? p.uvRect.x : 0;
            float vMin = p.hasTexture ? p.uvRect.y : 0;
            float uMax = p.hasTexture ? p.uvRect.z : 1;
            float vMax = p.hasTexture ? p.uvRect.w : 1;
            float texLayer = p.hasTexture ? p.textureLayer : -1.0f;

            float[] v0 = { p4.x, p4.y, p4.z, p.color.x, p.color.y, p.color.z, p.alpha, uMin, vMax, texLayer, p.skyLightBrightness, p.blockLightBrightness };
            System.arraycopy(v0, 0, vertices, vIdx, 12); vIdx += 12;
            float[] v1 = { p3.x, p3.y, p3.z, p.color.x, p.color.y, p.color.z, p.alpha, uMax, vMax, texLayer, p.skyLightBrightness, p.blockLightBrightness };
            System.arraycopy(v1, 0, vertices, vIdx, 12); vIdx += 12;
            float[] v2 = { p2.x, p2.y, p2.z, p.color.x, p.color.y, p.color.z, p.alpha, uMax, vMin, texLayer, p.skyLightBrightness, p.blockLightBrightness };
            System.arraycopy(v2, 0, vertices, vIdx, 12); vIdx += 12;
            float[] v3 = { p1.x, p1.y, p1.z, p.color.x, p.color.y, p.color.z, p.alpha, uMin, vMin, texLayer, p.skyLightBrightness, p.blockLightBrightness };
            System.arraycopy(v3, 0, vertices, vIdx, 12); vIdx += 12;

            // Front face must be CCW to not be culled.
            // p4(0)=TL, p3(1)=TR, p2(2)=BR, p1(3)=BL
            // CCW: TL -> BL -> BR => 0, 3, 2 and TL -> BR -> TR => 0, 2, 1
            indices[iIdx++] = vertexOffset + 0;
            indices[iIdx++] = vertexOffset + 3;
            indices[iIdx++] = vertexOffset + 2;
            indices[iIdx++] = vertexOffset + 2;
            indices[iIdx++] = vertexOffset + 1;
            indices[iIdx++] = vertexOffset + 0;

            vertexOffset += 4;
        }

        return new MeshData(vertices, indices);
    }

    private void addVertex(float[] arr, int idx, Vector3f pos, Vector3f col, float alpha, float u, float v, int layer, float skyLight, float blockLight) {
        arr[idx] = pos.x;
        arr[idx + 1] = pos.y;
        arr[idx + 2] = pos.z;
        arr[idx + 3] = col.x;
        arr[idx + 4] = col.y;
        arr[idx + 5] = col.z;
        arr[idx + 6] = alpha;
        arr[idx + 7] = u;
        arr[idx + 8] = v;
        arr[idx + 9] = layer;
        arr[idx + 10] = skyLight;
        arr[idx + 11] = blockLight;
    }
}
