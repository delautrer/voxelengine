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

    // 1. SCHATTEN KNACKIGER MACHEN
    // pow(1.8) statt 1.5 für den Himmel. Dadurch fallen Schatten tagsüber
    // minimal schneller ab und wirken tiefer. Das killt den Matsch-Look.
    float skyCurve = pow(fragLight.x, 1.8) * pc.globalLight;
    float blockCurve = pow(fragLight.y, 1.5);

    // 2. DAS TAGESLICHT DROSSELN
    vec3 blockLightColor = vec3(1.0, 0.85, 0.7) * blockCurve;

    // Hier war das Hauptproblem: 1.0 (Weiß) ist zu aggressiv.
    // Wir machen den Tag ein winziges bisschen dunkler und wärmer (wie Sonnenlicht).
    vec3 dayColor = vec3(0.95, 0.92, 0.88);
    vec3 nightColor = vec3(0.15, 0.20, 0.35);

    vec3 skyLightColor = mix(nightColor, dayColor, pc.globalLight) * skyCurve;

    vec3 totalLight = max(skyLightColor, blockLightColor);
    totalLight = max(totalLight, vec3(0.045));

    // 3. FARBEN KOMBINIEREN
    vec3 finalColor = textureColor.rgb * fragColor.rgb * totalLight;

    // 4. DER TAGES-KONTRAST ("Anti-Weichspüler")
    // Wir nehmen eine S-Kurve (smoothstep), die das Bild knackiger macht.
    // Der Trick: Wir mischen sie umso stärker ein, je heller es draußen ist!
    // Nachts (globalLight = 0) passiert hier gar nichts.
    vec3 contrastColor = smoothstep(0.0, 1.0, finalColor);
    finalColor = mix(finalColor, contrastColor, pc.globalLight * 0.4);

    // WICHTIG: Die Gamma-Korrektur (pow(1.0/1.1)) von vorhin ist komplett WEG,
    // denn die hat alles nur noch blasser gemacht!

    outColor = vec4(finalColor, textureColor.a);
}