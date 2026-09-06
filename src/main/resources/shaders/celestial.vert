#version 450

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inUV;

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    vec4 color;
    float phaseIndex;
    float useTexture;
} pcs;

layout(location = 0) out vec2 fragUV;
layout(location = 1) out vec4 fragColor;
layout(location = 2) out float fragUseTexture;

void main() {
    gl_Position = pcs.mvp * vec4(inPosition, 1.0);
    if (pcs.useTexture > 0.5) {
        fragUV = vec2((pcs.phaseIndex + inUV.x) / 8.0, inUV.y);
    } else {
        fragUV = inUV;
    }
    fragColor = pcs.color;
    fragUseTexture = pcs.useTexture;
}