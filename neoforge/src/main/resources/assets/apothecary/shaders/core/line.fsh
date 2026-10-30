#version 150

#moj_import <apothecary:sdf.glsl>

// Capsule line. local.x runs along the segment from 0 to Length, local.y across it.

uniform float Length;
uniform float HalfWidth;
uniform vec4 Color0;
uniform vec4 ColorModulator;

in vec2 local;
out vec4 fragColor;

void main() {
    vec2 q = vec2(local.x - clamp(local.x, 0.0, Length), local.y);
    // Lines thinner than a pixel fade instead of aliasing.
    float hw = max(HalfWidth, 0.5);
    float d = length(q) - hw;
    float a = coverage(d) * min(1.0, HalfWidth / 0.5);
    vec4 c = vec4(Color0.rgb, Color0.a * a) * ColorModulator;
    if (c.a < 0.002) discard;
    fragColor = c;
}
