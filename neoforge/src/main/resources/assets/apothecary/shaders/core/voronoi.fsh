#version 150

#moj_import <apothecary:effect.glsl>

// Animated Voronoi cells with glowing edges. Color0 = cell base, Color1 = edge glow. Params0.x = cell scale.

void main() {
    vec2 p = centered() * (Params0.x == 0.0 ? 5.0 : Params0.x);
    vec2 i = floor(p), f = fract(p);
    float d1 = 8.0, d2 = 8.0;
    float id = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(float(x), float(y));
            vec2 o = hash22(i + g);
            o = 0.5 + 0.45 * sin(Time * 0.8 + 6.2831 * o);
            float d = length(g + o - f);
            if (d < d1) { d2 = d1; d1 = d; id = hash12(i + g); }
            else if (d < d2) d2 = d;
        }
    }
    float edge = smoothstep(0.0, 0.08, d2 - d1);
    vec3 base = Color0.rgb * (0.55 + 0.45 * id);
    vec3 col = mix(Color1.rgb * 1.4, base, edge);
    col += Color1.rgb * 0.25 * (1.0 - d1);
    effectOut(vec4(col, 1.0));
}
