#version 150

#moj_import <apothecary:effect.glsl>

// Horizontal hue spectrum.

void main() {
    effectOut(vec4(hsv2rgb(vec3(uv().x, 1.0, 1.0)), 1.0));
}
