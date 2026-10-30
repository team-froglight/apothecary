#version 150

#moj_import <apothecary:effect.glsl>

// Interactive water ripples around the pointer over a gradient (Color0 → Color1), highlight Color2.
// Params0.x = ring density, Params0.y = idle drift.

void main() {
    vec2 p = local;
    vec2 m = Mouse.x < 0.0 ? Size * vec2(0.5 + 0.3 * sin(Time * 0.7), 0.5 + 0.3 * cos(Time * 0.5)) : Mouse;
    float d = distance(p, m);
    float density = Params0.x == 0.0 ? 0.18 : Params0.x;
    float wave = sin(d * density - Time * 5.0) * exp(-d * 0.012);
    vec2 n = normalize(p - m + 0.0001) * wave;
    vec2 q = uv() + n * 0.02;
    vec3 col = mix(Color0.rgb, Color1.rgb, clamp(q.y + q.x * 0.3, 0.0, 1.0));
    col += Color2.rgb * pow(max(wave, 0.0), 3.0) * 0.6;
    effectOut(vec4(col, 1.0));
}
