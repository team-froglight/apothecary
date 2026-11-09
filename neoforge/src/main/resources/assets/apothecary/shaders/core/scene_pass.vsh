#version 150

// A full-target quad for the scene's passes: positions arrive in clip space already.

in vec3 Position;
in vec2 UV0;

out vec2 uv;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    uv = UV0;
}
