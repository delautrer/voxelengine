#version 450
layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec4 inColor;
layout(location = 2) in vec2 inTexCoord;
layout(location = 3) in float inTexLayer;
layout(location = 4) in vec2 inLight;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec3 fragTexCoord;
layout(location = 2) out vec2 fragLight;
layout(location = 3) out float fragFogDist;
layout(location = 4) out vec3 fragWorldPos;
layout(location = 5) out vec2 fragRelXZ; // <--- NEU FÜR NEBEL

layout(push_constant) uniform PushConstants {
    mat4 mvp;                 // 0..15 (64 bytes)
    float globalLight;        // 16 (byte 64)
    float renderDistance;     // 17 (byte 68)
    float skyR;               // 18 (byte 72 - also playerSkyLight when isFirstPerson)
    float skyG;               // 19 (byte 76 - also playerBlockLight when isFirstPerson)
    float skyB;               // 20 (byte 80)
    float camX;               // 21 (byte 84)
    float camY;               // 22 (byte 88)
    float camZ;               // 23 (byte 92)
    float offsetX;            // 24 (byte 96)
    float offsetY;            // 25 (byte 100)
    float offsetZ;            // 26 (byte 104)
    float isCloud;            // 27 (byte 108)
    float isUnderwater;       // 28 (byte 112)
    float clipY;              // 29 (byte 116)
    float useVertexColorOnly; // 30 (byte 120)
    float isFirstPerson;      // 31 (byte 124)
} pc;

void main() {
    // 1. Berechne die Position RELATIV zur Kamera exakt im Shader
    vec3 relPos = inPosition + vec3(pc.offsetX, pc.offsetY, pc.offsetZ);

    // 2. Nutze die neue mvp (welche keine Translation mehr hat!)
    vec4 clipPos = pc.mvp * vec4(relPos, 1.0);
    gl_Position = clipPos;

    fragColor = inColor;
    fragTexCoord = vec3(inTexCoord, inTexLayer);
    
    if (pc.isFirstPerson > 0.5) {
        fragLight = vec2(pc.skyR, pc.skyG);
    } else {
        fragLight = inLight;
    }
    
    fragFogDist = clipPos.w;

    // Absolute Weltposition für das clipY wiederherstellen (Y ist klein genug)
    fragWorldPos = relPos + vec3(pc.camX, pc.camY, pc.camZ);

    // Die präzise XZ-Distanz zur Kamera für den Nebel weitergeben
    fragRelXZ = relPos.xz;
}