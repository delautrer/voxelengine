package de.delautrer.game.world;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.MeshData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class StarSystem {

    public MeshData generateStarMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        Random random = new Random(10842L);
        int indexOffset = 0;
        float starDistance = 100.0f;
        float baseStarSize = 0.06f;

        for (int i = 0; i < 1500; i++) {
            float dx, dy, dz, lengthSq;
            do {
                dx = random.nextFloat() * 2.0f - 1.0f;
                dy = random.nextFloat() * 2.0f - 1.0f;
                dz = random.nextFloat() * 2.0f - 1.0f;
                lengthSq = dx * dx + dy * dy + dz * dz;
            } while (lengthSq >= 1.0f || lengthSq == 0.0f);

            float length = (float) Math.sqrt(lengthSq);
            dx = (dx / length) * starDistance;
            dy = (dy / length) * starDistance;
            dz = (dz / length) * starDistance;

            float nx = dx / starDistance;
            float ny = dy / starDistance;
            float nz = dz / starDistance;

            float upX = 0.0f, upY = 1.0f, upZ = 0.0f;
            if (Math.abs(ny) > 0.99f) {
                upX = 1.0f; upY = 0.0f; upZ = 0.0f;
            }

            float rx = upY * nz - upZ * ny;
            float ry = upZ * nx - upX * nz;
            float rz = upX * ny - upY * nx;
            float rLen = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
            rx /= rLen; ry /= rLen; rz /= rLen;

            float ux = ny * rz - nz * ry;
            float uy = nz * rx - nx * rz;
            float uz = nx * ry - ny * rx;

            float size = baseStarSize + (random.nextFloat() * 0.15f);
            rx *= size; ry *= size; rz *= size;
            ux *= size; uy *= size; uz *= size;

            // --- NEU: XYZ + UV Koordinaten (5 Floats pro Vertex) ---

            // Unten Links
            vertices.add(dx - rx - ux); vertices.add(dy - ry - uy); vertices.add(dz - rz - uz);
            vertices.add(0.0f); vertices.add(0.0f);

            // Unten Rechts
            vertices.add(dx + rx - ux); vertices.add(dy + ry - uy); vertices.add(dz + rz - uz);
            vertices.add(1.0f); vertices.add(0.0f);

            // Oben Rechts
            vertices.add(dx + rx + ux); vertices.add(dy + ry + uy); vertices.add(dz + rz + uz);
            vertices.add(1.0f); vertices.add(1.0f);

            // Oben Links
            vertices.add(dx - rx + ux); vertices.add(dy - ry + uy); vertices.add(dz - rz + uz);
            vertices.add(0.0f); vertices.add(1.0f);

            indices.add(indexOffset);     indices.add(indexOffset + 1); indices.add(indexOffset + 2);
            indices.add(indexOffset + 2); indices.add(indexOffset + 3); indices.add(indexOffset);

            indexOffset += 4;
        }

        float[] vArray = new float[vertices.size()];
        for(int i=0; i<vertices.size(); i++) vArray[i] = vertices.get(i);

        int[] iArray = new int[indices.size()];
        for(int i=0; i<indices.size(); i++) iArray[i] = indices.get(i);

        return new MeshData(vArray, iArray);
    }
}
