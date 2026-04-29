package de.delautrer.game.world.sky;

import de.delautrer.engine.graphics.MeshData;

public class CelestialSystem {

    public MeshData generateCelestialMesh() {
        float distance = 90.0f;
        float size = 2.4f;

        // Ein simples Viereck auf der +X Achse, das zum Mittelpunkt schaut
        float[] vertices = {
                // X,          Y,     Z,         U,    V
                distance,  size, -size,      0.0f, 0.0f, // Oben Links
                distance, -size, -size,      1.0f, 0.0f, // Unten Links
                distance, -size,  size,      1.0f, 1.0f, // Unten Rechts
                distance,  size,  size,      0.0f, 1.0f  // Oben Rechts
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        return new MeshData(vertices, indices);
    }
}