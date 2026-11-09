#version 150

// Grading: bloom mixed in, then Complementary Reimagined's tonemap (Lottes 2016,
// modified: contrast 1.05, a lift for dark colours, a path to white, dark desaturation) and its
// saturation and vibrance, and a whisper of vignette. FXAA comes after, in scene_present.

uniform sampler2D Sampler0;   // the lit scene, linear HDR
uniform sampler2D Sampler1;   // the bloom pyramid, summed
uniform float Bloom;
uniform vec4 Frost;           // a frosted rectangle (u0, v0, u1, v1): the bloom's blur there, for a panel over it

in vec2 uv;
out vec4 fragColor;

const float BLOOM_LEVELS = 6.0;
const float TM_EXPOSURE = 1.0;
const float TM_CONTRAST = 1.05;
const float TM_WHITE_PATH = 1.0;
const float TM_DARK_DESATURATION = 0.25;
const float T_SATURATION = 1.0;
const float T_VIBRANCE = 1.0;

float luminance(vec3 c) { return dot(c, vec3(0.2125, 0.7154, 0.0721)); }

vec3 linearToRgb(vec3 color) {
    const vec3 k = vec3(0.055);
    return mix((vec3(1.0) + k) * pow(color, vec3(1.0 / 2.4)) - k, 12.92 * color, lessThan(color, vec3(0.0031308)));
}

vec3 tonemap(vec3 color) {
    color = TM_EXPOSURE * color;
    float initialLuminance = luminance(color);

    vec3 a = vec3(TM_CONTRAST), d = vec3(1.0);
    vec3 hdrMax = vec3(8.0), midIn = vec3(0.25), midOut = vec3(0.25);
    vec3 ad = a * d;
    vec3 hdrMaxA = pow(hdrMax, a), hdrMaxAD = pow(hdrMax, ad);
    vec3 midInA = pow(midIn, a), midInAD = pow(midIn, ad);
    vec3 hm1 = hdrMaxA * midOut, hm2 = hdrMaxAD - midInAD;
    vec3 b = (-midInA + hm1) / (hm2 * midOut);
    vec3 c = (hdrMaxAD * midInA - hm1 * midInAD) / (hm2 * midOut);
    vec3 colorOut = linearToRgb(pow(color, a) / (pow(color, ad) * b + c));

    // keep dark colours readable
    float darkLift = smoothstep(0.1, 0.0, initialLuminance);
    colorOut = mix(colorOut, pow(color, vec3(1.0 / 2.2)), darkLift * 0.75 * max(0.55 - abs(1.05 - TM_CONTRAST), 0.0) / 0.55);
    // the brightest go to white
    float lifted = pow(initialLuminance / 16.0, 2.0 - TM_WHITE_PATH) * 16.0;
    colorOut = mix(colorOut, vec3(1.0), smoothstep(0.0, 16.0, lifted));
    // the darkest lose a little colour
    colorOut = mix(colorOut, vec3(luminance(colorOut)), smoothstep(0.1, 0.0, initialLuminance) * TM_DARK_DESATURATION);
    return clamp(colorOut, 0.0, 1.0);
}

vec3 saturation(vec3 color) {
    float saturationFactor = T_SATURATION + 0.07;
    float grayVibrance = (color.r + color.g + color.b) / 3.0;
    float graySaturation = grayVibrance;
    if (saturationFactor < 1.0) graySaturation = dot(color, vec3(0.299, 0.587, 0.114));
    float mn = min(color.r, min(color.g, color.b)), mx = max(color.r, max(color.g, color.b));
    float sat = (1.0 - (mx - mn)) * (1.0 - mx) * grayVibrance * 5.0;
    vec3 lightness = vec3((mn + mx) * 0.5);
    color = mix(color, mix(color, lightness, 1.0 - T_VIBRANCE), sat);
    color = mix(color, lightness, (1.0 - lightness) * (2.0 - T_VIBRANCE) / 2.0 * abs(T_VIBRANCE - 1.0));
    return color * saturationFactor - graySaturation * (saturationFactor - 1.0);
}

void main() {
    vec3 color = texture(Sampler0, uv).rgb;
    vec3 blur = texture(Sampler1, uv).rgb / BLOOM_LEVELS;
    bool frosted = uv.x >= Frost.x && uv.x <= Frost.z && uv.y >= Frost.y && uv.y <= Frost.w;
    color = frosted ? blur : mix(color, blur, Bloom);

    color = saturation(tonemap(color));

    vec2 v = uv - 0.5;
    color *= 1.0 - dot(v, v) * 0.35;
    fragColor = vec4(color, 1.0);
}
