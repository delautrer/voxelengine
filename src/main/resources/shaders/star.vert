#version 450

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inUV; // NEU

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    float alpha;
    float time;
} pcs;

layout(location = 0) out float fragAlpha;
layout(location = 1) out vec2 fragUV;
layout(location = 2) out vec3 fragPos;
layout(location = 3) out float fragTime;

void main() {
    gl_Position = pcs.mvp * vec4(inPosition, 1.0);
    fragAlpha = pcs.alpha;
    fragUV = inUV;
    fragPos = inPosition;
    fragTime = pcs.time;
}