#version 150

#moj_import <apothecary:sdf.glsl>

// Boxes (fill + border, solid/linear/radial paint) and outer/inset box shadows.

uniform vec2 Size;
uniform vec4 Radii;
uniform float BorderWidth;
uniform vec4 BorderColor;
uniform int FillMode;      // 0 solid, 1 linear, 2 radial
uniform vec4 Color0;
uniform vec4 Color1;
uniform vec4 Color2;
uniform vec4 Color3;
uniform vec4 Stops;
uniform int StopCount;
uniform vec4 Gradient;     // linear: dir.xy, length; radial: center.xy (px), radius
uniform int Mode;          // 0 box, 1 outer shadow, 2 inset shadow
uniform vec4 Shadow;       // blur, spread, offset.xy
uniform vec4 ColorModulator;

in vec2 local;
out vec4 fragColor;

vec4 stopColor(int i) {
    return i == 0 ? Color0 : i == 1 ? Color1 : i == 2 ? Color2 : Color3;
}

float stopPos(int i) {
    return i == 0 ? Stops.x : i == 1 ? Stops.y : i == 2 ? Stops.z : Stops.w;
}

vec4 paintAt(vec2 px) {
    if (FillMode == 0 || StopCount <= 1) return premul(Color0);
    float t;
    if (FillMode == 1) {
        t = dot(px - Size * 0.5, Gradient.xy) / max(Gradient.z, 1e-4) + 0.5;
    } else {
        t = length(px - Gradient.xy) / max(Gradient.z, 1e-4);
    }
    t = clamp(t, 0.0, 1.0);
    vec4 c = premul(stopColor(0));
    for (int i = 1; i < 4; i++) {
        if (i >= StopCount) break;
        float a = stopPos(i - 1), b = stopPos(i);
        float k = clamp((t - a) / max(b - a, 1e-4), 0.0, 1.0);
        if (t >= a) c = mix(premul(stopColor(i - 1)), premul(stopColor(i)), k);
    }
    // ordered dither hides gradient banding on 8-bit targets
    c.rgb += (hash12(gl_FragCoord.xy) - 0.5) / 255.0;
    return c;
}

void main() {
    vec2 hs = Size * 0.5;
    vec2 p = local - hs;
    vec4 outc;
    if (Mode == 0) {
        float d = sdRoundBox(p, hs, Radii);
        float cov = coverage(d);
        vec4 fill = paintAt(local);
        if (BorderWidth > 0.0) {
            float inner = coverage(d + BorderWidth);
            outc = fill * inner + premul(BorderColor) * (cov - inner);
        } else {
            outc = fill * cov;
        }
    } else {
        float sigma = max(Shadow.x * 0.5, 0.35);
        float spread = Shadow.y;
        vec2 sp = p - Shadow.zw;
        float box = sdRoundBox(p, hs, Radii);
        if (Mode == 1) {
            float ds = sdRoundBox(sp, hs + spread, max(Radii + spread, 0.0));
            float a = 0.5 - 0.5 * erfApprox(ds / (sigma * 1.41421));
            a *= 1.0 - coverage(box);
            outc = premul(Color0) * a;
        } else {
            float ds = sdRoundBox(sp, max(hs - spread, 0.0), max(Radii - spread, 0.0));
            float a = 0.5 + 0.5 * erfApprox(ds / (sigma * 1.41421));
            a *= coverage(box);
            outc = premul(Color0) * a;
        }
    }
    vec4 c = unpremul(outc) * ColorModulator;
    if (c.a < 0.002) discard;
    fragColor = c;
}
