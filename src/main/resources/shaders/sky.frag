#version 450

layout(location = 0) in vec3 fragViewDir;
layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PushConstants {
    mat4 invViewProj;   // 0-63
    vec4 sunDir;        // 64-79
    vec4 zenithColor;   // 80-95
    vec4 horizonColor;  // 96-111
    float isUnderwater; // 112
    float globalLight;  // 116
    float pad1;         // 120
    float pad2;         // 124
} push;

void main() {
    // --- UNTERWASSER-HIMMEL ---
    if (push.isUnderwater > 0.5) {
        vec3 waterFogColor = vec3(0.05, 0.2, 0.6) * push.globalLight;
        waterFogColor = max(waterFogColor, vec3(0.02, 0.05, 0.15));
        outColor = vec4(waterFogColor, 1.0);
        return;
    }

    // --- NORMALER HIMMEL ---
    vec3 viewDir = normalize(fragViewDir);

    // HIER GEÄNDERT: .xyz angehängt, weil push.sunDir jetzt ein vec4 ist
    vec3 sDir = normalize(push.sunDir.xyz);

    // 1. Himmelsbasis
    float upFactor = clamp(viewDir.y, 0.0, 1.0);

    // HIER GEÄNDERT: .rgb angehängt, weil es vec4 Farben sind
    vec3 skyBase = mix(push.horizonColor.rgb, push.zenithColor.rgb, upFactor);

    // 2. Quadratische Sonne (Stabilere Basis)
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