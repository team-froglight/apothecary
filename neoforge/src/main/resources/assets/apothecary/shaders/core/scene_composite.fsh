#version 150

// The scene's lighting pass, done after the fact from the camera's colour and depth: world
// positions and normals rebuilt from depth, the sun's shadow map sampled soft, ambient occlusion,
// the overworld sky with the sun's texture and Unbound's volumetric clouds, haze, and light shafts through
// the shadow map. Out comes linear light, brighter than white where it should be, for the bloom
// and the tonemap. The sky, its time-of-day factors and the light and ambient colours are
// Complementary Unbound's (clear weather), as are the shadow filter's sample pattern.

uniform sampler2D Sampler0;   // scene colour, as vanilla drew it
uniform sampler2D Sampler1;   // scene depth
uniform sampler2D Sampler2;   // shadow map depth, from the sun (or the moon)
uniform sampler2D Sampler3;   // Complementary's noise.png, for the clouds
uniform sampler2D Sampler4;   // the sun's picture
uniform sampler2D Sampler5;   // the moon's phases, 4 by 2, as vanilla's moon_phases.png
uniform sampler2D Sampler6;   // point lights: per row, position and range, then colour
uniform sampler2D Sampler7;   // materials: red where water, green where lava, blue where light-emitting

uniform mat4 InvViewProj;
uniform mat4 ShadowMat;
uniform vec3 CameraPos;
uniform vec3 SunDir;
uniform vec3 SkyColor;
uniform vec2 ScreenSize;
uniform float Time;
uniform float SunSize;
uniform float CloudHeight;
uniform vec2 Fog;             // haze start, end (blocks)
uniform vec2 ShadowInfo;      // shadow map size (texels), its depth range (blocks)
uniform mat4 SkyInvViewProj;  // the sky's look, a perspective one even when the scene's is orthographic
uniform float AoScale;        // half a block in pixels: at the camera (orthographic) or over distance (perspective)
uniform float Ortho;          // 1 for an orthographic camera
uniform vec3 ShadowCenter;    // the middle of the shadowed world, where light shafts are worth marching
uniform float MoonPhase;      // 0 full .. 4 new .. 7
uniform float LightCount;
uniform float CloudScale;     // above 1: smaller clouds, for a little world

in vec2 uv;
out vec4 fragColor;

// ------------------------------------------------------------------ helpers

float pow2(float x) { return x * x; }
float max0(float x) { return max(x, 0.0); }
float clamp01(float x) { return clamp(x, 0.0, 1.0); }
float sqrt1(float x) { return x * (2.0 - x); }
float sqrt2(float x) { x = 1.0 - x; x *= x; x *= x; return 1.0 - x; }
float sqrt3(float x) { x = 1.0 - x; x *= x; x *= x; x *= x; return 1.0 - x; }
float smoothstep1(float x) { return x * x * (3.0 - 2.0 * x); }
float GetLuminance(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
float ign(vec2 p) { return fract(52.9829189 * fract(0.06711056 * p.x + 0.00583715 * p.y)); }

// ------------------------------------------------------------------ time of day

float SdotU, sunFactor, sunVisibility, sunVisibility2, shadowTime;
float noonFactor, invNoonFactor, invNoonFactor2, nightFactor;
vec3 lightDir;

void timeOfDay() {
    SdotU = SunDir.y;
    sunFactor = SdotU < 0.0 ? clamp(SdotU + 0.375, 0.0, 0.75) / 0.75 : clamp(SdotU + 0.03125, 0.0, 0.0625) / 0.0625;
    sunVisibility = clamp(SdotU + 0.0625, 0.0, 0.125) / 0.125;
    sunVisibility2 = sunVisibility * sunVisibility;
    shadowTime = pow2(abs(sunVisibility - 0.5) * 2.0);
    noonFactor = sqrt(max(SdotU, 0.0));
    invNoonFactor = 1.0 - noonFactor;
    invNoonFactor2 = invNoonFactor * invNoonFactor;
    nightFactor = max(-SdotU, 0.0);
    lightDir = SdotU >= 0.0 ? SunDir : -SunDir;   // the moon lights the night
}

// ------------------------------------------------------------------ colours (skyColors.glsl, lightAndAmbientColors.glsl)

vec3 dayUpSkyColor, dayMiddleSkyColor, dayDownSkyColor, sunsetDownSkyColorP;
vec3 nightUpSkyColor, nightMiddleSkyColor, nightDownSkyColor;
vec3 lightColor, ambientColor, shaftColor;

void colors() {
    vec3 skyColorM = sqrt(SkyColor);
    vec3 noonUpSkyColor = pow(skyColorM, vec3(2.9)) * vec3(0.85, 0.92, 0.81);
    vec3 noonMiddleSkyColor = skyColorM * vec3(1.4, 1.3, 1.3) + noonUpSkyColor * 0.25;
    vec3 noonDownSkyColor = skyColorM * vec3(0.9) + noonUpSkyColor * 0.25;
    vec3 sunsetUpSkyColor = SkyColor * vec3(0.72, 0.522, 0.47);
    vec3 sunsetMiddleSkyColor = SkyColor * vec3(1.8, 1.3, 1.2);
    sunsetDownSkyColorP = vec3(1.45, 0.86, 0.5);
    vec3 sunsetDownSkyColor = sunsetDownSkyColorP * 0.5 + 0.25 * sunsetMiddleSkyColor;
    dayUpSkyColor = mix(noonUpSkyColor, sunsetUpSkyColor, invNoonFactor2);
    dayMiddleSkyColor = mix(noonMiddleSkyColor, sunsetMiddleSkyColor, invNoonFactor2);
    dayDownSkyColor = mix(noonDownSkyColor, sunsetDownSkyColor, invNoonFactor2);
    vec3 nightColFactor = 0.9 * vec3(0.07, 0.14, 0.24) + SkyColor;
    nightUpSkyColor = pow(nightColFactor, vec3(0.90)) * 0.45;
    nightMiddleSkyColor = sqrt(nightUpSkyColor) * 0.65;
    nightDownSkyColor = nightMiddleSkyColor * vec3(0.82, 0.82, 0.88);

    const float vsBrightness = 0.5;
    vec3 noonLight = vec3(0.65, 0.55, 0.375) * 2.05;
    vec3 noonAmbient = pow(SkyColor, vec3(0.75)) * 0.85;
    vec3 sunsetLight = pow(vec3(0.64, 0.45, 0.3), vec3(1.5 + invNoonFactor)) * 5.0;
    vec3 sunsetAmbient = noonAmbient * vec3(1.21, 0.92, 0.76) * 0.95;
    vec3 nightLight = 0.9 * vec3(0.15, 0.14, 0.20) * (0.4 + vsBrightness * 0.4);
    vec3 nightAmbient = 0.9 * vec3(0.09, 0.12, 0.17) * (1.55 + vsBrightness * 0.77);
    lightColor = mix(nightLight, mix(sunsetLight, noonLight, noonFactor), sunVisibility2);
    ambientColor = mix(nightAmbient, mix(sunsetAmbient, noonAmbient, noonFactor), sunVisibility2);

    vec3 noonShaft = vec3(0.4, 0.75, 1.3);
    vec3 sunsetShaft = pow(vec3(0.62, 0.39, 0.24), vec3(1.5 + invNoonFactor)) * 6.8;
    vec3 nightShaft = vec3(0.08, 0.12, 0.23);
    shaftColor = mix(nightShaft, mix(sunsetShaft, noonShaft, noonFactor * noonFactor), sunVisibility2);
}

// ------------------------------------------------------------------ the sky (sky.glsl, GetSky)

vec3 GetSky(float VdotU, float VdotS, float dither, bool doGlare, bool doGround) {
    float nightFactorSqrt2 = sqrt2(nightFactor);
    float nightFactorM = sqrt2(nightFactorSqrt2) * 0.4;
    float VdotSM1 = pow2(max(VdotS, 0.0));
    float VdotSM2 = pow2(VdotSM1);
    float VdotSM3 = pow2(pow2(max(-VdotS, 0.0)));
    float VdotSML = sunVisibility > 0.5 ? VdotS : -VdotS;
    float VdotUmax0 = max(VdotU, 0.0);

    vec3 upColor = mix(nightUpSkyColor * (1.5 - 0.5 * nightFactorSqrt2 + nightFactorM * VdotSM3 * 1.5), dayUpSkyColor, sunFactor);
    vec3 middleColor = mix(nightMiddleSkyColor * (3.0 - 2.0 * nightFactorSqrt2), dayMiddleSkyColor * (1.0 + VdotSM2 * 0.3), sunFactor);
    vec3 downColor = mix(nightDownSkyColor, dayDownSkyColor, (sunFactor + sunVisibility) * 0.5);

    float VdotUM1 = pow2(1.0 - VdotUmax0);
    VdotUM1 = pow(VdotUM1, (1.0 - VdotSM2 * 0.4) * 1.15);
    vec3 finalSky = mix(upColor, middleColor, VdotUM1);

    float VdotUM2 = pow2(1.0 - abs(VdotU));
    VdotUM2 = VdotUM2 * VdotUM2 * (3.0 - 2.0 * VdotUM2);
    VdotUM2 *= (0.7 - nightFactorM + VdotSM1 * (0.3 + nightFactorM)) * invNoonFactor * sunFactor;
    finalSky = mix(finalSky, sunsetDownSkyColorP * (1.0 + VdotSM1 * 0.3), VdotUM2);

    float VdotUM3 = min(max0(-VdotU + 0.08) / 0.35, 1.0);
    VdotUM3 = smoothstep1(VdotUM3);
    vec3 scatteredGroundMixer = vec3(VdotUM3 * VdotUM3, sqrt1(VdotUM3), sqrt3(VdotUM3));
    scatteredGroundMixer = mix(vec3(VdotUM3), scatteredGroundMixer, 0.75);
    finalSky = mix(finalSky, downColor, scatteredGroundMixer);

    if (doGround) finalSky *= smoothstep1(pow2(1.0 + min(VdotU, 0.0)));

    if (doGlare && 0.0 < VdotSML) {
        float glareScatter = 3.0 * (2.0 - clamp01(VdotS * 1000.0));
        float VdotSM4 = pow(abs(VdotS), glareScatter);
        float visfactor = 0.075;
        float glare = visfactor / (1.0 - (1.0 - visfactor) * VdotSM4) - visfactor;
        glare *= 0.7;
        vec3 glareColor = mix(vec3(0.38, 0.4, 0.5) * 0.3, vec3(1.5, 0.7, 0.3) + vec3(0.0, 0.5, 0.5) * noonFactor, sunVisibility);
        finalSky += glareColor * glare * shadowTime;
    }

    return max(finalSky + (dither - 0.5) / 128.0, vec3(0.0));
}

// ------------------------------------------------------------------ the sun

/** A billboard of {@code size} (half-width, as a tangent) facing back along {@code dir}: where {@code V} lands on it, 0..1, or -1. */
vec2 onBillboard(vec3 V, vec3 dir, float size) {
    float facing = dot(V, dir);
    if (facing <= 0.0) return vec2(-1.0);
    vec3 side = normalize(cross(dir, abs(dir.y) > 0.99 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0)));
    vec3 lift = cross(side, dir);
    vec2 q = vec2(dot(V, side), dot(V, lift)) / facing / size * 0.5 + 0.5;
    return any(lessThan(q, vec2(0.0))) || any(greaterThan(q, vec2(1.0))) ? vec2(-1.0) : q;
}

/** The moon, opposite the sun: its phase's cell of the sheet, added on, two thirds the sun's size as vanilla's is. */
vec3 moonDisk(vec3 V) {
    vec2 q = onBillboard(V, -SunDir, SunSize * 0.667);
    if (q.x < 0.0) return vec3(0.0);
    float phase = floor(MoonPhase + 0.5);
    vec2 cell = vec2(mod(phase, 4.0), floor(phase / 4.0));
    vec3 c = texture(Sampler5, (cell + vec2(q.x, 1.0 - q.y)) / vec2(4.0, 2.0)).rgb;
    return pow(c, vec3(2.2)) * 1.6 * (1.0 - 0.85 * sunVisibility);
}

// ------------------------------------------------------------------ stars (stars.glsl, NIGHT_STAR_AMOUNT 2)

float GetStarNoise(vec2 pos) { return fract(sin(dot(pos, vec2(12.9898, 4.1414))) * 43758.54953); }

vec3 GetStars(vec3 V, float VdotU, float VdotS) {
    if (VdotU < 0.0) return vec3(0.0);
    vec3 starCoord3 = V / (V.y + length(V.xz) * 0.5);
    vec2 starCoord = starCoord3.xz + vec2(0.006 * Time / 72.0, 0.0);
    starCoord *= 0.2;
    starCoord = floor(starCoord * 1024.0) / 1024.0;
    float star = GetStarNoise(starCoord) * GetStarNoise(starCoord + 0.1) * GetStarNoise(starCoord + 0.23);
    star = max0(star - 0.7);
    star *= star;
    star *= min(VdotU * 3.0, 1.0) * max0(1.0 - pow(abs(VdotS) * 1.002, 100.0));
    star *= pow2(pow2(invNoonFactor2)) * (1.0 - 0.5 * sunVisibility);
    return 40.0 * star * vec3(0.38, 0.4, 0.5);
}

vec3 sunDisk(vec3 V) {
    vec2 q = onBillboard(V, SunDir, SunSize);
    if (q.x < 0.0) return vec3(0.0);
    vec3 c = texture(Sampler4, vec2(q.x, 1.0 - q.y)).rgb;
    return pow(c, vec3(2.2)) * 4.0 * sunVisibility;
}

// ------------------------------------------------------------------ clouds (Unbound's volumetric clouds)

float min1(float x) { return min(x, 1.0); }
float pow1_5(float x) { return x * sqrt(x); }

/** commonFunctions.glsl's Noise3D: two slices of the 2D noise, offset per z, blended. */
float Noise3D(vec3 p) {
    p.z = fract(p.z) * 128.0;
    float iz = floor(p.z), fz = fract(p.z);
    vec2 aOff = vec2(23.0, 29.0) * iz / 128.0;
    vec2 bOff = vec2(23.0, 29.0) * (iz + 1.0) / 128.0;
    return mix(texture(Sampler3, p.xy + aOff).r, texture(Sampler3, p.xy + bOff).r, fz);
}

const float cloudStretch = 16.0;           // CLOUD_QUALITY 2
const float cloudTallness = cloudStretch * 2.0;
const float cloudNarrowness = 0.00012;

float GetCloudNoise(vec3 tracePos, float cloudAltitude, float cloudPlayerPosY) {
    vec3 tracePosM = tracePos * cloudNarrowness;
    float wind = 0.0006 * Time;
    float noise = 0.0, currentPersist = 1.0, total = 0.0;
    const int sampleCount = 4;
    const float persistance = 0.5;
    float noiseMult = 1.14;
    for (int i = 0; i < sampleCount; i++) {
        noise += Noise3D(tracePosM - vec3(0.0, 0.0, wind)) * currentPersist;
        total += currentPersist;
        tracePosM *= 3.0;
        wind *= 0.5;
        currentPersist *= persistance;
    }
    noise = pow2(noise / total);
    noiseMult *= 0.8 + 0.1 * clamp01(-cloudPlayerPosY / cloudTallness);
    noise *= noiseMult;
    float threshold = clamp(abs(cloudAltitude - tracePos.y) / cloudStretch, 0.001, 0.999);
    threshold = pow2(pow2(pow2(threshold)));
    return noise - (threshold * 0.2 + 0.25);
}

vec4 GetVolumetricClouds(float cloudAltitude, vec3 nPlayerPos, float VdotS, float VdotU, float dither, vec3 cameraPos) {
    const float distanceThreshold = 4000.0;
    vec4 volumetricClouds = vec4(0.0);
    float higherPlaneAltitude = cloudAltitude + cloudStretch;
    float lowerPlaneAltitude = cloudAltitude - cloudStretch;
    float lowerPlaneDistance = (lowerPlaneAltitude - cameraPos.y) / nPlayerPos.y;
    float higherPlaneDistance = (higherPlaneAltitude - cameraPos.y) / nPlayerPos.y;
    float minPlaneDistance = max(min(lowerPlaneDistance, higherPlaneDistance), 0.0);
    float maxPlaneDistance = max(lowerPlaneDistance, higherPlaneDistance);
    if (maxPlaneDistance < 0.0) return vec4(0.0);
    float planeDistanceDif = maxPlaneDistance - minPlaneDistance;

    vec3 cloudAmbientColor = ambientColor * (sunVisibility2 * (0.55 + vec3(0.2, 0.1, 0.0) * noonFactor) + 0.35);
    vec3 cloudLightColor = lightColor * (1.3 + vec3(0.1, -0.1, -0.3) * noonFactor);

    const float stepMult = 32.0;
    int sampleCount = min(int(planeDistanceDif / stepMult + dither + 1.0), 64);
    vec3 traceAdd = nPlayerPos * stepMult;
    vec3 tracePos = cameraPos + minPlaneDistance * nPlayerPos;
    tracePos += traceAdd * dither;
    tracePos.y -= traceAdd.y;

    float VdotSM1 = max0(sunVisibility > 0.5 ? VdotS : -VdotS);
    float VdotSM2 = pow2(VdotSM1) * abs(sunVisibility - 0.5) * 2.0;
    float VdotSM3 = VdotSM2 * 2.5;
    float VdotSM4 = pow(VdotSM1, 100.0) * sunVisibility;
    vec3 cloudSkyColor = GetSky(VdotU, VdotS, dither, true, false);

    for (int i = 0; i < sampleCount; i++) {
        tracePos += traceAdd;
        if (abs(tracePos.y - cloudAltitude) > cloudStretch) break;
        vec3 cloudPlayerPos = tracePos - cameraPos;
        float lTracePosXZ = length(cloudPlayerPos.xz);
        if (lTracePosXZ > distanceThreshold) break;

        float cloudNoise = GetCloudNoise(tracePos, cloudAltitude, cloudPlayerPos.y);
        if (cloudNoise > 0.00001) {
            float opacityFactor = min1(cloudNoise * 8.0);
            float cloudShading = 1.0 - (higherPlaneAltitude - tracePos.y) / cloudTallness;
            cloudShading *= 1.0 + 0.2 * VdotSM3 * (1.0 - opacityFactor) + VdotSM4;
            vec3 colorSample = cloudAmbientColor * (0.4 + 0.6 * cloudShading) + cloudLightColor * cloudShading;
            float distanceRatio = (distanceThreshold - lTracePosXZ) / distanceThreshold;
            float cloudDistanceFactor = clamp(distanceRatio, 0.0, 0.8) * 1.25;
            float cloudFogFactor = pow2(pow1_5(clamp(distanceRatio, 0.0, 1.0))) * 0.69;
            colorSample = mix(cloudSkyColor, colorSample, cloudFogFactor * 0.69);
            volumetricClouds.rgb = mix(volumetricClouds.rgb, colorSample, 1.0 - min1(volumetricClouds.a));
            volumetricClouds.a += opacityFactor * pow(cloudDistanceFactor, 0.5 + 10.0 * pow(abs(VdotSM1), 90.0));
            if (volumetricClouds.a > 0.9) {
                volumetricClouds.a = 1.0;
                break;
            }
        }
    }
    return volumetricClouds;
}

// ------------------------------------------------------------------ positions and normals

vec3 worldAt(vec2 p, float depth) {
    vec4 w = InvViewProj * vec4(vec3(p, depth) * 2.0 - 1.0, 1.0);
    return w.xyz / w.w;
}

/** From the depth around the pixel, taking each axis's nearer neighbour so edges stay sharp. */
vec3 normalAt(vec2 p, float d, vec3 pos) {
    vec2 px = 1.0 / ScreenSize;
    float dr = texture(Sampler1, p + vec2(px.x, 0.0)).r, dl = texture(Sampler1, p - vec2(px.x, 0.0)).r;
    float du = texture(Sampler1, p + vec2(0.0, px.y)).r, dd = texture(Sampler1, p - vec2(0.0, px.y)).r;
    vec3 dx = abs(dr - d) < abs(dl - d) ? worldAt(p + vec2(px.x, 0.0), dr) - pos : pos - worldAt(p - vec2(px.x, 0.0), dl);
    vec3 dy = abs(du - d) < abs(dd - d) ? worldAt(p + vec2(0.0, px.y), du) - pos : pos - worldAt(p - vec2(0.0, px.y), dd);
    vec3 n = normalize(cross(dx, dy));
    return dot(n, CameraPos - pos) < 0.0 ? -n : n;
}

// ------------------------------------------------------------------ shadows (shadowSampling.glsl's pattern)

vec3 shadowCoord(vec3 world) {
    vec4 s = ShadowMat * vec4(world, 1.0);
    return s.xyz / s.w * 0.5 + 0.5;
}

bool inShadowMap(vec3 sp) { return all(greaterThan(sp, vec3(0.0))) && all(lessThan(sp, vec3(1.0))); }

vec2 offsetDist(float x, int s) {
    float n = fract(x * 2.427) * 3.1415;
    return vec2(cos(n), sin(n)) * 1.4 * x / float(s);
}

float sunShadow(vec3 pos, vec3 n, float NdotL) {
    vec3 sp = shadowCoord(pos + n * 0.03);
    if (!inShadowMap(sp)) return 1.0;
    float bias = (0.02 + 0.06 * (1.0 - NdotL)) / ShadowInfo.y;
    float noise = ign(gl_FragCoord.xy);
    float radius = 1.3875 / ShadowInfo.x;
    const int SAMPLES = 6;
    float lit = 0.0;
    for (int i = 0; i < SAMPLES; i++) {
        vec2 o = offsetDist(noise + float(i), SAMPLES) * radius;
        lit += step(sp.z - bias, texture(Sampler2, sp.xy + o).r);
        lit += step(sp.z - bias, texture(Sampler2, sp.xy - o).r);
    }
    return lit / float(SAMPLES * 2);
}

// ------------------------------------------------------------------ ambient occlusion

float occlusion(vec3 pos, vec3 n, float dist) {
    float radius = clamp(Ortho > 0.5 ? AoScale : AoScale / max(dist, 0.1), 2.0, 48.0);
    float noise = ign(gl_FragCoord.xy + 17.0) * 6.2831853;
    float occ = 0.0;
    const int SAMPLES = 8;
    for (int i = 0; i < SAMPLES; i++) {
        float a = noise + float(i) * 2.3999632;
        float r = sqrt((float(i) + 0.5) / float(SAMPLES)) * radius;
        vec2 q = uv + vec2(cos(a), sin(a)) * r / ScreenSize;
        float sd = texture(Sampler1, q).r;
        if (sd >= 1.0) continue;
        vec3 d = worldAt(q, sd) - pos;
        float l = length(d);
        if (l < 1e-4) continue;
        occ += max0(dot(n, d / l) - 0.15) * (1.0 - smoothstep(0.25, 0.75, l));
    }
    return clamp01(1.0 - occ / float(SAMPLES) * 1.6);
}

// ------------------------------------------------------------------ block light

/** Light from lanterns, lava and the like: each point's colour, falling off over its range, wrapped softly round corners. */
vec3 blockLight(vec3 pos, vec3 n) {
    vec3 sum = vec3(0.0);
    int count = int(LightCount);
    for (int i = 0; i < 128; i++) {
        if (i >= count) break;
        vec4 at = texelFetch(Sampler6, ivec2(0, i), 0);
        vec3 d = at.xyz - pos;
        float dist = length(d);
        if (dist >= at.w) continue;
        float falloff = 1.0 - dist / at.w;
        falloff *= falloff;
        float wrap = dist < 1e-3 ? 1.0 : max0(dot(n, d / dist)) * 0.6 + 0.4;
        sum += texelFetch(Sampler6, ivec2(1, i), 0).rgb * falloff * wrap;
    }
    // as a lightmap does: lights side by side brighten towards a ceiling, never piling up into daylight
    return sum / (1.0 + dot(sum, vec3(0.333)));
}

// ------------------------------------------------------------------ light shafts

/** Light scattered toward the eye along the ray, wherever the shadow map says the sun gets in. */
vec3 shafts(vec3 V, float dist, float VdotL) {
    const int STEPS = 10;
    // the stretch of the ray through the shadowed world: up to the surface, or past the middle for the sky
    float reach = ShadowInfo.y * 0.5;
    float end = min(dist, dot(ShadowCenter - CameraPos, V) + reach * 0.5);
    float begin = max(0.0, end - reach);
    float noise = ign(gl_FragCoord.xy + 31.0);
    float lit = 0.0;
    for (int i = 0; i < STEPS; i++) {
        vec3 p = CameraPos + V * (begin + (float(i) + noise) / float(STEPS) * (end - begin));
        vec3 sp = shadowCoord(p);
        if (!inShadowMap(sp)) continue;
        vec2 edge = min(sp.xy, 1.0 - sp.xy);
        float w = smoothstep(0.0, 0.2, min(edge.x, edge.y));
        lit += w * step(sp.z, texture(Sampler2, sp.xy).r);
    }
    lit /= float(STEPS);
    float g = 0.65;
    float phase = (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * VdotL, 1.5) * 0.01;
    return shaftColor * lit * phase * ((end - begin) / max(reach, 1.0)) * (0.35 + 0.65 * invNoonFactor);
}

// ------------------------------------------------------------------ liquids (Unbound's look)

/** Two drifting layers of noise, crossing: the water's swell. */
float waveHeight(vec2 p) {
    float t = Time;
    return texture(Sampler3, p * 0.045 + vec2(t * 0.0045, t * 0.0030)).r * 0.65
         + texture(Sampler3, p * 0.11 + vec2(-t * 0.0060, t * 0.0075)).g * 0.35;
}

/** Streaks pouring down a fall: noise stretched tall, sliding down with time. */
float fallHeight(float across, float y) {
    float t = Time;
    return texture(Sampler3, vec2(across * 0.09, y * 0.012 + t * 0.055)).r * 0.6
         + texture(Sampler3, vec2(across * 0.23 + 0.37, y * 0.03 + t * 0.11)).g * 0.4;
}

/** A level surface's normal, tilted by the waves; a falling one's, by the streaks pouring down it. */
vec3 waterNormal(vec3 pos, vec3 n, out float streak) {
    streak = 0.0;
    if (n.y < 0.7) {
        vec3 side = normalize(cross(vec3(0.0, 1.0, 0.0), n));
        float across = dot(pos, side);
        const float e = 0.12;
        float h = fallHeight(across, pos.y);
        streak = h;
        float du = (fallHeight(across + e, pos.y) - h) / e, dv = (fallHeight(across, pos.y + e) - h) / e;
        return normalize(n - side * du * 0.2 - vec3(0.0, 1.0, 0.0) * dv * 0.08);
    }
    const float e = 0.15;
    float h = waveHeight(pos.xz);
    vec2 slope = vec2(waveHeight(pos.xz + vec2(e, 0.0)) - h, waveHeight(pos.xz + vec2(0.0, e)) - h) / e;
    return normalize(vec3(-slope.x * 0.2, 1.0, -slope.y * 0.2));
}

/**
 * Unbound's water. The scene keeps what lies behind it (the water only wrote its depth), so here it is seen through
 * the water bent by the ripples, its reds swallowed and fading into the water's own blue-teal, with the sky mirrored
 * at a glance and the sun glinting on the ripples. {@code amount} is how much water there is (a fall thins out below);
 * {@code behind} is the sky, where nothing stands behind it.
 */
vec3 shadeWater(vec3 pos, vec3 n, vec3 V, float shadow, float dither, vec3 lighting, vec3 behind, float depth, float amount) {
    float streak;
    vec3 nw = waterNormal(pos, n, streak);
    bool falling = n.y < 0.7;
    // what's behind, refracted: the ripples' tilt shifts where it's read, unless that lands on something in front
    vec3 d = nw - n;
    vec2 q = uv + vec2(d.x + d.z, d.y + d.z * 0.5) * (falling ? 0.06 : 0.035);
    if (texture(Sampler1, q).r < depth - 1e-5 || texture(Sampler7, q).r <= 0.004) q = uv;
    vec4 back = texture(Sampler0, q);
    vec3 seen = pow(back.rgb / max(back.a, 1e-3), vec3(2.2)) * lighting;
    seen = mix(behind, seen, back.a);
    vec3 light = lightColor * shadow * shadowTime + ambientColor;
    vec3 body = vec3(0.02, 0.13, 0.2) * light;   // Unbound's blue-teal
    vec3 col = mix(seen * vec3(0.12, 0.42, 0.62), body, falling ? 0.62 : 0.55);
    // the ripples' light and shade, as the sky lights their slopes
    col *= 1.0 + 2.0 * (d.x * 0.6 + d.z * 0.4 + d.y);
    vec3 R = reflect(V, nw);
    float NdotV = max0(dot(normalize(n + nw), -V));
    float fresnel = 0.08 + 0.8 * pow(1.0 - NdotV, 4.0);
    vec3 sky = GetSky(max(R.y, 0.02), dot(R, SunDir), dither, false, false);
    col = mix(col, sky * vec3(0.65, 0.82, 0.95), fresnel);
    vec3 H = normalize(lightDir - V);
    col += lightColor * pow(max0(dot(nw, H)), 400.0) * 24.0 * shadow * shadowTime * fresnel;
    return mix(seen, col, amount);
}

/** A light-emitting surface: unlit, and its bright parts (a lantern's flame, glowstone's crystals) glowing. */
vec3 shadeLight(vec3 col, vec3 albedo) {
    float lum = dot(albedo, vec3(0.299, 0.587, 0.114));
    return max(col, albedo * (0.8 + 3.2 * smoothstep(0.35, 0.85, lum)));
}

/** Lava: its own light, in slow-stirring brighter veins. */
vec3 shadeLava(vec3 albedo, vec3 pos) {
    float t = Time;
    float stir = texture(Sampler3, pos.xz * 0.06 + vec2(t * 0.002, -t * 0.0015)).b
               * texture(Sampler3, pos.xz * 0.15 + vec2(-t * 0.003, t * 0.0025) + pos.y * 0.05).r;
    return albedo * (2.2 + 4.0 * stir * stir);
}

// ------------------------------------------------------------------ the pass

void main() {
    timeOfDay();
    colors();

    float depth = texture(Sampler1, uv).r;
    vec4 sky = SkyInvViewProj * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec3 skyV = normalize(sky.xyz / sky.w);
    vec3 V = skyV;                                 // the sky's ray; a surface's below
    if (depth < 1.0) V = normalize(worldAt(uv, depth) - CameraPos);
    float VdotU = V.y, VdotS = dot(V, SunDir), VdotL = dot(V, lightDir);
    float dither = ign(gl_FragCoord.xy + 5.0);

    // how much of the pixel the surfaces cover: less where something translucent (a fall fading out) lies over the sky
    float cover = depth < 1.0 ? texture(Sampler0, uv).a : 0.0;
    vec3 col = vec3(0.0), behind = vec3(0.0);
    float dist = Fog.y * 4.0;
    if (cover < 0.999) {
        float skyUp = skyV.y, skySun = dot(skyV, SunDir);
        col = GetSky(skyUp, skySun, dither, true, true) + GetStars(skyV, skyUp, skySun) + sunDisk(skyV) + moonDisk(skyV);
        vec4 c = vec4(0.0);
        if (CloudHeight > -1e8) {
            // The camera's own ray through this pixel (an orthographic one's are parallel, looking down; the sky's
            // lens is only for its colour). Cloud space is the world scaled up, so the clouds come out smaller, and an
            // orthographic ray starts its march near the world rather than at its far-off near plane.
            vec3 near = worldAt(uv, 0.0);
            vec3 ray = normalize(worldAt(uv, 1.0) - near);
            // an orthographic ray starts just above the cloud layer, wherever it meets it: a sea of clouds to the edges
            float top = CloudHeight + 16.0 / CloudScale;
            vec3 from = Ortho > 0.5 ? near + ray * ((top - near.y) / min(ray.y, -1e-4) - 1.0) : CameraPos;
            if (Ortho < 0.5 || ray.y < -1e-4) c = GetVolumetricClouds(CloudHeight * CloudScale, ray, dot(ray, SunDir), ray.y, dither, from * CloudScale);
        }
        col = mix(col, c.rgb, c.a);
        behind = col;
    }
    if (depth < 1.0) {
        vec3 pos = worldAt(uv, depth);
        dist = length(pos - CameraPos);
        vec3 n = normalAt(uv, depth, pos);
        vec3 albedo = pow(texture(Sampler0, uv).rgb / max(cover, 1e-3), vec3(2.2));

        float NdotL = max0(dot(n, lightDir));
        float shadow = NdotL > 0.0 ? sunShadow(pos, n, NdotL) : 0.0;
        float ao = occlusion(pos, n, dist);
        // sunlight where it reaches, sky light from above everywhere, as Complementary mixes them
        vec3 sceneLighting = lightColor * NdotL * shadow * shadowTime + ambientColor * (0.75 + 0.25 * n.y) * ao
                + blockLight(pos, n) * (0.5 + 0.5 * ao);
        col = albedo * sceneLighting;
        vec4 material = texture(Sampler7, uv);
        if (material.r > 0.004) {
            col = shadeWater(pos, n, V, shadow, dither, sceneLighting, behind, depth, clamp01(material.a));
            cover = 1.0;   // the water saw to what lies behind it
        }
        else if (material.g > 0.004) col = shadeLava(albedo, pos);
        else if (material.b > 0.004) col = shadeLight(col, albedo);

        float haze = smoothstep(Fog.x, Fog.y, dist);
        col = mix(col, GetSky(VdotU, VdotS, dither, false, true), haze);
        col = mix(behind, col, cover);
    }

    col += shafts(V, dist, VdotL);
    fragColor = vec4(col, 1.0);
}
