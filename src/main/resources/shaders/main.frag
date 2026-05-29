#version 450
layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec3 fragTexCoord;
layout(location = 2) in vec2 fragLight;
layout(location = 3) in float fragFogDist;
layout(location = 4) in vec3 fragWorldPos;
layout(location = 5) in vec2 fragRelXZ;

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
    float isUnderwater;
    float clipY;
} pc;

void main() {
    if (fragWorldPos.y < pc.clipY) {
        discard;
    }

    vec4 textureColor = texture(texSampler, fragTexCoord);
    float alpha = textureColor.a * fragColor.a;

    if (alpha < 0.05) discard;

    vec3 finalColor;

    if (pc.isCloud > 0.5) {
        vec3 cloudColor = textureColor.rgb * fragColor.rgb;

        float timeBlend = clamp((pc.globalLight - 0.05) / 0.95, 0.0, 1.0);

        vec3 dayColor = vec3(1.0, 1.0, 1.0);
        vec3 nightColor = vec3(0.01, 0.01, 0.02);

        vec3 timeColor = mix(nightColor, dayColor, timeBlend);
        finalColor = cloudColor * timeColor;

        float fadeEnd = pc.renderDistance * 4.0;
        float fadeStart = fadeEnd * 0.5;
        float fadeFactor = clamp((fragFogDist - fadeStart) / (fadeEnd - fadeStart), 0.0, 1.0);
        alpha *= (1.0 - fadeFactor);
    } else {
        // Sky light: gentle gamma curve (was 1.8, now 1.5 for less darkness in shade)
        float skyCurve = pow(fragLight.x, 1.5) * pc.globalLight;
        // Block light: slightly softer curve
        float blockCurve = pow(fragLight.y, 1.4);

        // Warmer block light color (torches/glowstone feel)
        vec3 blockLightColor = vec3(1.0, 0.82, 0.6) * blockCurve;

        // Day/night sky color tint
        vec3 dayColor   = vec3(0.95, 0.93, 0.90);
        // Slightly warmer blue for night (moonlight) instead of pure dark
        vec3 nightColor = vec3(0.18, 0.22, 0.40);

        vec3 skyLightColor = mix(nightColor, dayColor, pc.globalLight) * skyCurve;
        vec3 totalLight = max(skyLightColor, blockLightColor);

        // Soft ambient floor so pure-shadow areas aren't pitch black
        totalLight = max(totalLight, vec3(0.03));

        finalColor = textureColor.rgb * fragColor.rgb * totalLight;

        // Subtle contrast enhancement (much softer than before to avoid plastic look)
        // Only apply a small portion of the smoothstep to preserve texture detail
        vec3 contrastColor = smoothstep(0.0, 1.0, finalColor);
        finalColor = mix(finalColor, contrastColor, pc.globalLight * 0.15);

        // Subtle film-grain-like desaturation at night for mood
        float luma = dot(finalColor, vec3(0.299, 0.587, 0.114));
        finalColor = mix(vec3(luma), finalColor, 0.85 + pc.globalLight * 0.15);

        float fogEnd = pc.renderDistance;
        float fogStart = fogEnd * 0.75;

        float distXZ = length(fragRelXZ);
        float fogFactor = clamp((distXZ - fogStart) / (fogEnd - fogStart), 0.0, 1.0);

        vec3 skyDayColorFog = vec3(0.4, 0.7, 1.0) * 1.5;
        vec3 skyNightColorFog = vec3(0.01, 0.01, 0.02) * 1.5;

        float timeBlend = clamp((pc.globalLight - 0.05) / 0.95, 0.0, 1.0);

        vec3 fogColor = mix(skyNightColorFog, skyDayColorFog, timeBlend);
        finalColor = mix(finalColor, fogColor, fogFactor);
    }

    if (pc.isUnderwater > 0.5) {
        vec3 waterFogColor = vec3(0.05, 0.2, 0.6) * pc.globalLight;

        waterFogColor = max(waterFogColor, vec3(0.02, 0.05, 0.15));

        float waterFogStart = 0.0;
        float waterFogEnd = 16.0;

        float waterFogFactor = clamp((fragFogDist - waterFogStart) / (waterFogEnd - waterFogStart), 0.0, 1.0);

        finalColor = mix(finalColor, waterFogColor, waterFogFactor);
    }

    outColor = vec4(finalColor, alpha);
}