#version 450
layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec3 fragTexCoord;
layout(location = 2) in vec2 fragLight;

layout(binding = 0) uniform sampler2DArray texSampler;

layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    float globalLight;
} pc;

void main() {
    vec4 textureColor = texture(texSampler, fragTexCoord);
    if (textureColor.a < 0.1) {
        discard;
    }

    float sky = fragLight.x * pc.globalLight;
    float block = fragLight.y;

    float finalLight = max(sky, block);

    finalLight = max(finalLight, 0.045);

    outColor = textureColor * fragColor * vec4(vec3(finalLight), 1.0);
}