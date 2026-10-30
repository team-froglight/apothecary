#version 150

#moj_import <apothecary:effect.glsl>

// Classic demoscene plasma mapped through a cosine palette. Params0.x = scale, Params0.y = speed.

vec3 palette(float t) {
    vec3 a = vec3(0.5), b = vec3(0.5), c = vec3(1.0), d = vec3(0.0, 0.33, 0.67);
    return a + b * cos(6.28318 * (c * t + d));
}

void main() {
    vec2 p = centered() * (Params0.x == 0.0 ? 3.0 : Params0.x);
    float t = Time * (Params0.y == 0.0 ? 1.0 : Params0.y);
    float v = sin(p.x + t);
    v += sin((p.y + t) * 0.5);
    v += sin((p.x + p.y + t) * 0.5);
    vec2 c = p + vec2(sin(t / 3.0), cos(t / 2.0)) * 2.0;
    v += sin(sqrt(dot(c, c) + 1.0) + t);
    vec3 col = palette(v * 0.25 + t * 0.05);
    effectOut(vec4(col, 1.0));
}
