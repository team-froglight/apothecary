#version 150

#moj_import <apothecary:effect.glsl>

// Parallax starfield over a nebula. Color0 = nebula tint, Color1 = deep space. Params0.x = speed.

void main() {
    vec2 p = centered();
    float speed = Params0.x == 0.0 ? 1.0 : Params0.x;
    vec3 col = Color1.rgb;
    col += Color0.rgb * pow(fbm(p * 2.2 + vec2(Time * 0.02 * speed, 0.0)), 2.5) * 1.6;
    for (int layer = 0; layer < 3; layer++) {
        float fl = float(layer);
        float scale = 12.0 + fl * 14.0;
        vec2 q = p * scale + vec2(Time * (0.3 + fl * 0.5) * speed, fl * 13.0);
        vec2 cell = floor(q), f = fract(q) - 0.5;
        float h = hash12(cell + fl * 7.0);
        if (h > 0.86) {
            vec2 o = hash22(cell) - 0.5;
            float d = length(f - o * 0.6);
            float twinkle = 0.6 + 0.4 * sin(Time * (2.0 + h * 5.0) + h * 50.0);
            col += vec3(0.9, 0.95, 1.0) * smoothstep(0.08 - fl * 0.015, 0.0, d) * twinkle * (1.0 - fl * 0.25);
        }
    }
    effectOut(vec4(col, 1.0));
}
