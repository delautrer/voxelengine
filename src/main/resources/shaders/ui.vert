#version 450

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inTexCoord;

layout(location = 0) out vec2 fragTexCoord;
layout(location = 1) out vec3 fragColor;

layout(push_constant) uniform PushConstants {
    mat4 ortho;
} pcs;

void main() {
    gl_Position = pcs.ortho * vec4(inPosition.x, inPosition.y, 0.0, 1.0);
    fragTexCoord = inTexCoord;
    fragColor = inColor;
}
