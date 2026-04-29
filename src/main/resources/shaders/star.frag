#version 450

layout(location = 0) in float fragAlpha;
layout(location = 1) in vec2 fragUV;
layout(location = 2) in vec3 fragPos;
layout(location = 3) in float fragTime;

layout(location = 0) out vec4 outColor;

// Eine simple Pseudo-Zufallsfunktion basierend auf Koordinaten
float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    // 1. Distanz zur Mitte des Vierecks (0.5, 0.5) berechnen
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(fragUV, center);

    // Ecken wegschneiden -> Es wird ein perfekter Kreis!
    if (dist > 0.5) {
        discard;
    }

    // 2. Weicher Glow (innen hell, außen sanft auslaufend)
    float glow = smoothstep(0.5, 0.0, dist);

    // 3. Jeder Stern bekommt einen eigenen "Seed" durch seine Position
    float seed = rand(fragPos.xy + fragPos.z);

    // 4. Asynchrones Funkeln berechnen
    float twinkle = clamp(0.3 + 0.8 * sin(fragTime * 150.0 * seed + seed * 100.0), 0.0, 1.0);

    // 5. Leichte Farbvariation für Realismus (Blau, Weiß, Orange)
    vec3 baseColor = vec3(1.0, 1.0, 1.0);
    if (seed > 0.8) {
        baseColor = vec3(0.8, 0.95, 1.0); // Leicht bläulich
    } else if (seed < 0.2) {
        baseColor = vec3(1.0, 0.9, 0.8);  // Leicht gelblich/orange
    }

    // Finaler Mix: Umgebung-Alpha * Glow * Funkeln
    float finalAlpha = fragAlpha * glow * twinkle;
    outColor = vec4(baseColor, finalAlpha);
}