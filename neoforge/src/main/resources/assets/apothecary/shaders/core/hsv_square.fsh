#version 150

#moj_import <apothecary:effect.glsl>

// Saturation (x) / value (y, top = 1) square for hue Params0.x (degrees).

void main() {
    vec2 p = uv();
    vec3 col = hsv2rgb(vec3(Params0.x / 360.0, p.x, 1.0 - p.y));
    effectOut(vec4(col, 1.0));
}
