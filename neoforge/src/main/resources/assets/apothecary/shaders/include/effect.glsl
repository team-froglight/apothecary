// Uniform contract shared by every effect shader (see ShaderParams in apothecary-core).
// Write your own effect: include this, compute a color, and finish with effectOut(color).

#moj_import <apothecary:sdf.glsl>

uniform vec2 Size;        // px
uniform float Time;       // seconds
uniform vec2 Mouse;       // local px, -1 when the pointer is outside
uniform vec4 Params0;
uniform vec4 Params1;
uniform vec4 Color0;
uniform vec4 Color1;
uniform vec4 Color2;
uniform vec4 Color3;
uniform float Radius;     // rounded mask radius, px
uniform vec4 ColorModulator;

in vec2 local;
out vec4 fragColor;

// 0..1 across the effect, y down.
vec2 uv() { return local / max(Size, vec2(1.0)); }

// Aspect-corrected coordinates centered on the effect, y up, short side = 1.
vec2 centered() {
    vec2 p = (local - Size * 0.5) / min(Size.x, Size.y);
    return vec2(p.x, -p.y);
}

void effectOut(vec4 color) {
    float m = coverage(sdRoundBox(local - Size * 0.5, Size * 0.5, vec4(Radius)));
    vec4 c = vec4(color.rgb, color.a * m) * ColorModulator;
    // Reference every contract uniform so drivers keep them (avoids "could not find uniform" log spam).
    if (Params1.w < -1e30) c += vec4(Time, Mouse, Params0.x) + Params1 + Color0 + Color1 + Color2 + Color3;
    if (c.a < 0.002) discard;
    fragColor = c;
}
