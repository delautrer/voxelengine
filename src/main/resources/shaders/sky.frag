#version 450

layout(location = 0) in vec3 fragViewDir;
layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PushConstants {
    mat4 invViewProj;
    vec3 sunDir;
    vec3 zenithColor;
    vec3 horizonColor;
} push;

void main() {
    vec3 viewDir = normalize(fragViewDir);
    vec3 sDir = normalize(push.sunDir);

    // 1. Himmelsbasis
    float upFactor = clamp(viewDir.y, 0.0, 1.0);
    vec3 skyBase = mix(push.horizonColor, push.zenithColor, upFactor);

    // 2. Quadratische Sonne (Stabilere Basis)
    // Wir nutzen einen festen Vektor für das Kreuzprodukt, der nicht mit der Sonne kollidiert
    vec3 ortho = (abs(sDir.y) > 0.9) ? vec3(1, 0, 0) : vec3(0, 1, 0);
    vec3 right = normalize(cross(ortho, sDir));
    vec3 upLocal = cross(sDir, right);

    vec3 diff = viewDir - sDir;
    float dx = abs(dot(diff, right));
    float dy = abs(dot(diff, upLocal));

    // Die Sonne ist nachts einfach weg (smoothstep am Horizont)
    float sunVisibility = smoothstep(-0.05, 0.05, sDir.y);
    float sunDisc = (1.0 - smoothstep(0.02, 0.022, max(dx, dy))) * sunVisibility;

    // 3. Dynamische Farben & Glow
    float sunHeight = clamp(sDir.y, 0.0, 1.0);
    // Sonne wird mittags weißer, abends gelber
    vec3 sunColor = mix(vec3(1.0, 0.6, 0.2), vec3(1.0, 1.0, 0.95), pow(sunHeight, 0.5));

    float sunDot = clamp(dot(viewDir, sDir), 0.0, 1.0);
    float glowIntensity = mix(0.7, 0.05, sunHeight) * sunVisibility;
    vec3 glowColor = mix(vec3(1.0, 0.4, 0.1), vec3(0.8, 0.9, 1.0), sunHeight);
    float sunGlow = pow(sunDot, 16.0) * glowIntensity;

    // 4. Finales Mixing
    vec3 finalColor = skyBase + (glowColor * sunGlow);
    finalColor = mix(finalColor, sunColor, sunDisc);

    outColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}