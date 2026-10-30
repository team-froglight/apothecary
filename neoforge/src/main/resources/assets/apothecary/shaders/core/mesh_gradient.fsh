#version 150

#moj_import <apothecary:effect.glsl>

// Animated four-point mesh gradient (Color0..3) with soft warping. Params0.x = speed.

void main() {
    vec2 p = uv();
    float t = Time * (Params0.x == 0.0 ? 0.35 : Params0.x);
    // warp the domain so the blend feels fluid
    p += 0.08 * vec2(sin(p.y * 6.0 + t * 1.3), cos(p.x * 5.0 - t * 1.1));
    vec2 c0 = vec2(0.2 + 0.15 * sin(t), 0.2 + 0.1 * cos(t * 1.2));
    vec2 c1 = vec2(0.8 + 0.1 * cos(t * 0.9), 0.25 + 0.15 * sin(t * 1.4));
    vec2 c2 = vec2(0.25 + 0.12 * cos(t * 1.1), 0.8 + 0.1 * sin(t * 0.8));
    vec2 c3 = vec2(0.78 + 0.12 * sin(t * 1.3), 0.78 + 0.12 * cos(t));
    float w0 = 1.0 / pow(distance(p, c0) + 0.05, 2.2);
    float w1 = 1.0 / pow(distance(p, c1) + 0.05, 2.2);
    float w2 = 1.0 / pow(distance(p, c2) + 0.05, 2.2);
    float w3 = 1.0 / pow(distance(p, c3) + 0.05, 2.2);
    vec3 col = (Color0.rgb * w0 + Color1.rgb * w1 + Color2.rgb * w2 + Color3.rgb * w3) / (w0 + w1 + w2 + w3);
    col += (hash12(local) - 0.5) / 128.0;
    effectOut(vec4(col, 1.0));
}
