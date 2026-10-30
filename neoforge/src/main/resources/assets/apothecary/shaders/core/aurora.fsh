#version 150

#moj_import <apothecary:effect.glsl>

// Drifting aurora curtains over a dark base. Color0..2 = curtain colors, Color3 = base.
// Params0.x = speed (default 1 when 0), Params0.y = grain amount.

void main() {
    vec2 p = uv();
    float speed = Params0.x == 0.0 ? 1.0 : Params0.x;
    float t = Time * 0.15 * speed;
    vec3 col = Color3.rgb;
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        vec4 c = i == 0 ? Color0 : i == 1 ? Color1 : Color2;
        float wave = fbm(vec2(p.x * (1.6 + fi * 0.4) + t * (1.0 + fi * 0.3), fi * 4.1 + t * 0.5));
        float center = 0.25 + fi * 0.22 + (wave - 0.5) * 0.55;
        float band = exp(-pow((p.y - center) * (5.0 - fi), 2.0));
        float shimmer = 0.65 + 0.35 * sin(p.x * 22.0 + t * 9.0 + fi * 2.0 + wave * 6.0);
        col += c.rgb * band * shimmer * c.a * 0.85;
    }
    // soft vignette + grain
    vec2 q = p - 0.5;
    col *= 1.0 - dot(q, q) * 0.9;
    col += (hash12(local + fract(Time) * 100.0) - 0.5) * Params0.y;
    effectOut(vec4(col, Color3.a));
}
