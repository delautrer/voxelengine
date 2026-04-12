#version 450
layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec4 inColor;
layout(location = 2) in vec2 inTexCoord;
layout(location = 3) in float inTexLayer;
layout(location = 4) in vec2 inLight;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec3 fragTexCoord;
layout(location = 2) out vec2 fragLight;

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    float globalLight;
} pc;

void main() {
    gl_Position = pc.mvp * vec4(inPosition, 1.0);
    fragColor = inColor;
    fragTexCoord = vec3(inTexCoord, inTexLayer);
    fragLight = inLight;
}