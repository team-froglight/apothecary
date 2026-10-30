#version 150

#moj_import <apothecary:effect.glsl>

// Alpha ramp of Color0 over a checkerboard.

void main() {
    vec2 c = floor(local / 4.0);
    float k = mod(c.x + c.y, 2.0);
    vec3 checker = mix(vec3(0.85), vec3(0.6), k);
    vec3 col = mix(checker, Color0.rgb, uv().x);
    effectOut(vec4(col, 1.0));
}
