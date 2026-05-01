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
    mat4 mvp;
    float globalLight;
    float renderDistance;
    float fogMultiplier;
    float camX;
    float camY;
    float camZ;
    float offsetX;
    float offsetY;
    float offsetZ;
    float isCloud;
    float isUnderwater;
    float clipY;
} pc;

void main() {
    // 1. Berechne die Position RELATIV zur Kamera exakt im Shader
    vec3 relPos = inPosition + vec3(pc.offsetX - pc.camX, pc.offsetY - pc.camY, pc.offsetZ - pc.camZ);

    // 2. Nutze die neue mvp (welche keine Translation mehr hat!)
    vec4 clipPos = pc.mvp * vec4(relPos, 1.0);
    gl_Position = clipPos;

    fragColor = inColor;
    fragTexCoord = vec3(inTexCoord, inTexLayer);
    fragLight = inLight;
    fragFogDist = clipPos.w;

    // Absolute Weltposition für das clipY wiederherstellen (Y ist klein genug)
    fragWorldPos = inPosition + vec3(pc.offsetX, pc.offsetY, pc.offsetZ);

    // Die präzise XZ-Distanz zur Kamera für den Nebel weitergeben
    fragRelXZ = relPos.xz;
}