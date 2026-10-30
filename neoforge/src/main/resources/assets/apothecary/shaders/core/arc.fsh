#version 150

#moj_import <apothecary:sdf.glsl>

// Ring arc with round caps. local = position relative to the center. Angles in radians, 0 = up, clockwise.

uniform float Radius;
uniform float Thickness;
uniform float Start;
uniform float Sweep;
uniform vec4 Color0;
uniform vec4 ColorModulator;

in vec2 local;
out vec4 fragColor;

const float TAU = 6.28318530718;

void main() {
    vec2 p = local;
    float ht = Thickness * 0.5;
    float d;
    if (Sweep >= TAU - 1e-3) {
        d = abs(length(p) - Radius) - ht;
    } else {
        float ang = atan(p.x, -p.y);
        if (ang < 0.0) ang += TAU;
        float rel = mod(ang - Start + 2.0 * TAU, TAU);
        if (rel <= Sweep) {
            d = abs(length(p) - Radius) - ht;
        } else {
            vec2 a = Radius * vec2(sin(Start), -cos(Start));
            vec2 b = Radius * vec2(sin(Start + Sweep), -cos(Start + Sweep));
            d = min(length(p - a), length(p - b)) - ht;
        }
    }
    vec4 c = vec4(Color0.rgb, Color0.a * coverage(d)) * ColorModulator;
    if (c.a < 0.002) discard;
    fragColor = c;
}
