#version 450

layout(location = 0) in vec3 inPosition;

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    vec4 color;
} pcs;

layout(location = 0) out vec4 outColor;

void main() {
    gl_Position = pcs.mvp * vec4(inPosition, 1.0);
    outColor = pcs.color;
}