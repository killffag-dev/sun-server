#version 150

in vec3 Position;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

out vec3 vWorldDir;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vWorldDir = normalize(Position);
}