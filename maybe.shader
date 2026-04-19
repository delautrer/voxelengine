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
    if (textureColor.a < 0.1) discard;

    // 1. LICHT (Linearer und weicher)
    float light = max(fragLight.x * pc.globalLight, fragLight.y);
    // Nur eine leichte Kurve für Tiefe, kein "Schlamm-Effekt"
    float finalLight = pow(light, 1.4);
    finalLight = max(finalLight, 0.15);

    // 2. FARBE KOMBINIEREN
    vec3 color = textureColor.rgb * fragColor.rgb * finalLight;

    // 3. SUBTILER KONTRAST-FIX (S-Kurve)
    // Wir nutzen eine ganz sanfte Kontrastanhebung
    color = smoothstep(-0.05, 1.05, color);

    // 4. NATÜRLICHE SÄTTIGUNG
    // Wir lassen die Sättigung fast original (1.0),
    // damit es nicht so grau/matschig wirkt.
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, 0.95);

    // 5. GAMMA KORREKTUR (Der Standard für Monitore)
    // Macht das Bild wieder etwas "leichter" und freundlicher
    color = pow(color, vec3(1.0 / 1.2));

    outColor = vec4(clamp(color, 0.0, 1.0), textureColor.a);
}