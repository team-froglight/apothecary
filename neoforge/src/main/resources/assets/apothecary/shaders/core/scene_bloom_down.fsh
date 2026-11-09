#version 150

// Bloom, going down: a 13-tap box-of-boxes (Jimenez, "Next Generation Post Processing in Call of
// Duty: Advanced Warfare"), half the size each level. Clamped so a stray bright pixel can't flare.

uniform sampler2D Sampler0;
uniform vec2 TexelSize;

in vec2 uv;
out vec4 fragColor;

vec3 tap(vec2 o) { return min(texture(Sampler0, uv + o * TexelSize).rgb, vec3(32.0)); }

void main() {
    vec3 a = tap(vec2(-2.0, 2.0)), b = tap(vec2(0.0, 2.0)), c = tap(vec2(2.0, 2.0));
    vec3 d = tap(vec2(-2.0, 0.0)), e = tap(vec2(0.0, 0.0)), f = tap(vec2(2.0, 0.0));
    vec3 g = tap(vec2(-2.0, -2.0)), h = tap(vec2(0.0, -2.0)), i = tap(vec2(2.0, -2.0));
    vec3 j = tap(vec2(-1.0, 1.0)), k = tap(vec2(1.0, 1.0)), l = tap(vec2(-1.0, -1.0)), m = tap(vec2(1.0, -1.0));
    vec3 color = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
    fragColor = vec4(color, 1.0);
}
