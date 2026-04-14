#version 450

layout(location = 0) out vec3 fragViewDir;

// WIR NUTZEN JETZT PUSH CONSTANTS (Kein UBO mehr!)
layout(push_constant) uniform PushConstants {
    mat4 invViewProj;
    vec3 sunDir;
    vec3 zenithColor;
    vec3 horizonColor;
} push;

vec2 positions[6] = vec2[](
vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(-1.0,  1.0),
vec2(-1.0,  1.0), vec2(1.0, -1.0), vec2( 1.0,  1.0)
);

void main() {
    vec2 pos = positions[gl_VertexIndex];
    gl_Position = vec4(pos, 0.9999, 1.0);

    // Die Mathematik ist jetzt unzerstörbar:
    vec4 target = push.invViewProj * vec4(pos.x, pos.y, 0.5, 1.0);
    fragViewDir = normalize(target.xyz / target.w);
}