#version 450

layout(location = 0) in vec2 fragUV;
layout(location = 1) in vec4 fragColor;

layout(location = 0) out vec4 outColor;

void main() {
    // Später kannst du hier sagen: outColor = texture(texSampler, fragUV) * fragColor;
    // Für jetzt nehmen wir einfach das reine Quadrat!
    outColor = fragColor;
}