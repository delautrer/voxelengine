#version 450
layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec3 fragTexCoord;
layout(location = 2) in vec2 fragLight;
layout(location = 3) in float fragFogDist;
layout(location = 4) in vec3 fragWorldPos;

layout(binding = 0) uniform sampler2DArray texSampler;

layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PushConstants {
    mat4 mvp;
    float globalLight;
    float renderDistance;
    float fogMultiplier;
    float camX;
    float camY;
    float camZ;
    float offsetX;
    float offsetY;
    float offsetZ;
    float isCloud;
} pc;

void main() {
    vec4 textureColor = texture(texSampler, fragTexCoord);

    // ==========================================
    // HIER IST DER FIX FÜRS WASSER:
    // Wir mischen Textur-Alpha (1.0) mit Java-Alpha (0.7 bei Wasser)
    // ==========================================
    float alpha = textureColor.a * fragColor.a;

    if (alpha < 0.05) discard;

    vec3 finalColor;

    if (pc.isCloud > 0.5) {
        vec3 cloudColor = textureColor.rgb * fragColor.rgb;
        vec3 dayColor = vec3(1.0, 1.0, 1.0);
        vec3 nightColor = vec3(0.15, 0.20, 0.35);
        vec3 timeColor = mix(nightColor, dayColor, pc.globalLight);
        finalColor = cloudColor * timeColor;

        float fadeEnd = pc.renderDistance * 4.0;
        float fadeStart = fadeEnd * 0.5;

        float fadeFactor = clamp((fragFogDist - fadeStart) / (fadeEnd - fadeStart), 0.0, 1.0);
        alpha *= (1.0 - fadeFactor);

    } else {
        float skyCurve = pow(fragLight.x, 1.8) * pc.globalLight;
        float blockCurve = pow(fragLight.y, 1.5);

        vec3 blockLightColor = vec3(1.0, 0.85, 0.7) * blockCurve;
        vec3 dayColor = vec3(0.95, 0.92, 0.88);
        vec3 nightColor = vec3(0.15, 0.20, 0.35);

        vec3 skyLightColor = mix(nightColor, dayColor, pc.globalLight) * skyCurve;
        vec3 totalLight = max(skyLightColor, blockLightColor);
        totalLight = max(totalLight, vec3(0.045));

        finalColor = textureColor.rgb * fragColor.rgb * totalLight;

        vec3 contrastColor = smoothstep(0.0, 1.0, finalColor);
        finalColor = mix(finalColor, contrastColor, pc.globalLight * 0.4);

        float fogEnd = pc.renderDistance;
        float fogStart = fogEnd * 0.75;

        float distXZ = length(fragWorldPos.xz - vec2(pc.camX, pc.camZ));
        float fogFactor = clamp((distXZ - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
        fogFactor *= pc.fogMultiplier;

        vec3 skyDayColorFog = vec3(0.5, 0.7, 1.0);
        vec3 skyNightColorFog = vec3(0.02, 0.02, 0.05);
        vec3 fogColor = mix(skyNightColorFog, skyDayColorFog, pc.globalLight);

        finalColor = mix(finalColor, fogColor, fogFactor);
    }

    outColor = vec4(finalColor, alpha);
}