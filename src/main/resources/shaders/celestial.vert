#version 450

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inUV;

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    vec4 color; // RGBA für Sonne oder Mond
} pcs;

layout(location = 0) out vec2 fragUV;
layout(location = 1) out vec4 fragColor;

void main() {
    gl_Position = pcs.mvp * vec4(inPosition, 1.0);
    fragUV = inUV;
    fragColor = pcs.color;
}