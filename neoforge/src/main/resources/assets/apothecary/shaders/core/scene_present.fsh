#version 150

// The finished scene onto the screen: FXAA 3.11 (Complementary Reimagined's port of Simon
// Rodriguez's write-up) over the graded picture, then dither against banding. When the scene
// was rendered bigger than its rectangle, the linear filter averages it down on the way.

uniform sampler2D Sampler0;   // the graded scene
uniform vec2 TexelSize;

in vec2 uv;
out vec4 fragColor;

const float QUALITY[12] = float[12](1.0, 1.0, 1.0, 1.0, 1.0, 1.5, 2.0, 2.0, 2.0, 2.0, 4.0, 8.0);

float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
vec3 at(vec2 p) { return texture(Sampler0, p).rgb; }

vec3 fxaa(vec2 texCoord) {
    const float edgeThresholdMin = 0.03125, edgeThresholdMax = 0.0625, subpixelQuality = 0.75;
    const int iterations = 12;
    vec2 view = TexelSize;
    vec3 color = at(texCoord);

    float lumaCenter = luma(color);
    float lumaDown = luma(at(texCoord + vec2(0.0, -view.y)));
    float lumaUp = luma(at(texCoord + vec2(0.0, view.y)));
    float lumaLeft = luma(at(texCoord + vec2(-view.x, 0.0)));
    float lumaRight = luma(at(texCoord + vec2(view.x, 0.0)));
    float lumaMin = min(lumaCenter, min(min(lumaDown, lumaUp), min(lumaLeft, lumaRight)));
    float lumaMax = max(lumaCenter, max(max(lumaDown, lumaUp), max(lumaLeft, lumaRight)));
    float lumaRange = lumaMax - lumaMin;
    if (lumaRange <= max(edgeThresholdMin, lumaMax * edgeThresholdMax)) return color;

    float lumaDownLeft = luma(at(texCoord + vec2(-view.x, -view.y)));
    float lumaUpRight = luma(at(texCoord + vec2(view.x, view.y)));
    float lumaUpLeft = luma(at(texCoord + vec2(-view.x, view.y)));
    float lumaDownRight = luma(at(texCoord + vec2(view.x, -view.y)));
    float lumaDownUp = lumaDown + lumaUp, lumaLeftRight = lumaLeft + lumaRight;
    float lumaLeftCorners = lumaDownLeft + lumaUpLeft, lumaDownCorners = lumaDownLeft + lumaDownRight;
    float lumaRightCorners = lumaDownRight + lumaUpRight, lumaUpCorners = lumaUpRight + lumaUpLeft;

    float edgeHorizontal = abs(-2.0 * lumaLeft + lumaLeftCorners) + abs(-2.0 * lumaCenter + lumaDownUp) * 2.0
            + abs(-2.0 * lumaRight + lumaRightCorners);
    float edgeVertical = abs(-2.0 * lumaUp + lumaUpCorners) + abs(-2.0 * lumaCenter + lumaLeftRight) * 2.0
            + abs(-2.0 * lumaDown + lumaDownCorners);
    bool isHorizontal = edgeHorizontal >= edgeVertical;

    float luma1 = isHorizontal ? lumaDown : lumaLeft;
    float luma2 = isHorizontal ? lumaUp : lumaRight;
    float gradient1 = luma1 - lumaCenter, gradient2 = luma2 - lumaCenter;
    bool is1Steepest = abs(gradient1) >= abs(gradient2);
    float gradientScaled = 0.25 * max(abs(gradient1), abs(gradient2));
    float stepLength = isHorizontal ? view.y : view.x;
    float lumaLocalAverage;
    if (is1Steepest) {
        stepLength = -stepLength;
        lumaLocalAverage = 0.5 * (luma1 + lumaCenter);
    } else {
        lumaLocalAverage = 0.5 * (luma2 + lumaCenter);
    }

    vec2 currentUv = texCoord;
    if (isHorizontal) currentUv.y += stepLength * 0.5; else currentUv.x += stepLength * 0.5;
    vec2 offset = isHorizontal ? vec2(view.x, 0.0) : vec2(0.0, view.y);
    vec2 uv1 = currentUv - offset, uv2 = currentUv + offset;
    float lumaEnd1 = luma(at(uv1)) - lumaLocalAverage;
    float lumaEnd2 = luma(at(uv2)) - lumaLocalAverage;
    bool reached1 = abs(lumaEnd1) >= gradientScaled, reached2 = abs(lumaEnd2) >= gradientScaled;
    if (!reached1) uv1 -= offset;
    if (!reached2) uv2 += offset;
    if (!(reached1 && reached2)) {
        for (int i = 2; i < iterations; i++) {
            if (!reached1) lumaEnd1 = luma(at(uv1)) - lumaLocalAverage;
            if (!reached2) lumaEnd2 = luma(at(uv2)) - lumaLocalAverage;
            reached1 = abs(lumaEnd1) >= gradientScaled;
            reached2 = abs(lumaEnd2) >= gradientScaled;
            if (!reached1) uv1 -= offset * QUALITY[i];
            if (!reached2) uv2 += offset * QUALITY[i];
            if (reached1 && reached2) break;
        }
    }

    float distance1 = isHorizontal ? (texCoord.x - uv1.x) : (texCoord.y - uv1.y);
    float distance2 = isHorizontal ? (uv2.x - texCoord.x) : (uv2.y - texCoord.y);
    bool isDirection1 = distance1 < distance2;
    float distanceFinal = min(distance1, distance2);
    float pixelOffset = -distanceFinal / (distance1 + distance2) + 0.5;
    bool isLumaCenterSmaller = lumaCenter < lumaLocalAverage;
    bool correctVariation = ((isDirection1 ? lumaEnd1 : lumaEnd2) < 0.0) != isLumaCenterSmaller;
    float finalOffset = correctVariation ? pixelOffset : 0.0;

    float lumaAverage = (1.0 / 12.0) * (2.0 * (lumaDownUp + lumaLeftRight) + lumaLeftCorners + lumaRightCorners);
    float subPixelOffset1 = clamp(abs(lumaAverage - lumaCenter) / lumaRange, 0.0, 1.0);
    float subPixelOffset2 = (-2.0 * subPixelOffset1 + 3.0) * subPixelOffset1 * subPixelOffset1;
    finalOffset = max(finalOffset, subPixelOffset2 * subPixelOffset2 * subpixelQuality);

    vec2 finalUv = texCoord;
    if (isHorizontal) finalUv.y += finalOffset * stepLength; else finalUv.x += finalOffset * stepLength;
    return at(finalUv);
}

void main() {
    vec3 color = fxaa(uv);
    float n = fract(52.9829189 * fract(0.06711056 * gl_FragCoord.x + 0.00583715 * gl_FragCoord.y));
    fragColor = vec4(color + (n - 0.5) / 255.0, 1.0);
}
