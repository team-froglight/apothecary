#version 150

// Shared vertex stage for every Apothecary shader: UV0 carries the fragment's position in the
// shape's own (untransformed) pixel space, so SDFs stay exact under rotation and scaling.

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 local;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    local = UV0;
}
