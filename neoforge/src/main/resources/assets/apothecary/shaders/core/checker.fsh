#version 150

#moj_import <apothecary:effect.glsl>

// Checkerboard: Color0 / Color1, cell size Params0.x (default 6px).

void main() {
    float s = Params0.x == 0.0 ? 6.0 : Params0.x;
    vec2 c = floor(local / s);
    effectOut(mix(Color0, Color1, mod(c.x + c.y, 2.0)));
}
