#version 450

layout(location = 0) in vec2 fragUV;
layout(location = 1) in vec4 fragColor;
layout(location = 2) in float fragUseTexture;

layout(binding = 0) uniform sampler2D moonAtlas;

layout(location = 0) out vec4 outColor;

void main() {
    if (fragUseTexture > 0.5) {
        vec4 tex = texture(moonAtlas, fragUV);
        if (tex.a < 0.05) discard;
        outColor = tex * fragColor;
    } else {
        outColor = fragColor;
    }
}