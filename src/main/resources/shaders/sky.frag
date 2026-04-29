#version 450

layout(location = 0) in vec3 fragViewDir;
layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PushConstants {
    mat4 invViewProj;
    vec4 sunDir;
    vec4 zenithColor;
    vec4 horizonColor;
    float isUnderwater;
    float globalLight;
    float pad1;
    float pad2;
} push;

void main() {
    // --- UNTERWASSER ---
    if (push.isUnderwater > 0.5) {
        vec3 waterFogColor = vec3(0.05, 0.2, 0.6) * push.globalLight;
        waterFogColor = max(waterFogColor, vec3(0.02, 0.05, 0.15));
        outColor = vec4(waterFogColor, 1.0);
        return;
    }

    // --- NORMALER HIMMEL ---
    vec3 viewDir = normalize(fragViewDir);
    float upFactor = clamp(viewDir.y, 0.0, 1.0);

    // Nur noch der sanfte Farbverlauf von Horizont zu Zenit!
    vec3 skyBase = mix(push.horizonColor.rgb, push.zenithColor.rgb, upFactor);

    outColor = vec4(clamp(skyBase, 0.0, 1.0), 1.0);
}