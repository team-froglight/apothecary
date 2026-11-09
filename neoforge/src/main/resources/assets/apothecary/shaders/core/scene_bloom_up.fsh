#version 150

// Bloom, coming back up: a 3x3 tent over the smaller level, added onto the larger one.

uniform sampler2D Sampler0;
uniform vec2 TexelSize;

in vec2 uv;
out vec4 fragColor;

vec4 tap(vec2 o) { return texture(Sampler0, uv + o * TexelSize); }

void main() {
    vec4 color = tap(vec2(0.0)) * 4.0
            + (tap(vec2(-1.0, 0.0)) + tap(vec2(1.0, 0.0)) + tap(vec2(0.0, -1.0)) + tap(vec2(0.0, 1.0))) * 2.0
            + tap(vec2(-1.0, -1.0)) + tap(vec2(1.0, -1.0)) + tap(vec2(-1.0, 1.0)) + tap(vec2(1.0, 1.0));
    fragColor = color / 16.0;
}
