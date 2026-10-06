#version 150

in vec3 vWorldDir;

uniform float Time;
uniform float Speed;
uniform float Scale;
uniform float Intensity;
uniform int Mode;
uniform int Octaves;

out vec4 OutColor;

float hash3(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float valueNoise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float n000 = hash3(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);

    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);

    return mix(nxy0, nxy1, u.z);
}

float fbm3D(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    int oct = clamp(Octaves, 1, 4);
    for (int i = 0; i < 4; i++) {
        if (i >= oct) break;
        value += amplitude * valueNoise3D(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

vec3 renderAqua(vec3 dir) {
    vec3 p = dir * Scale * 3.0 + vec3(Time * Speed * 0.2, 0.0, Time * Speed * 0.1);
    float n = fbm3D(p);

    vec3 deep = vec3(0.0, 0.08, 0.18);
    vec3 shallow = vec3(0.05, 0.45, 0.55);
    return mix(deep, shallow, n) * Intensity;
}

vec3 renderMagma(vec3 dir) {
    vec3 p = dir * Scale * 3.0 - vec3(0.0, Time * Speed * 0.3, 0.0);
    float n = fbm3D(p);

    vec3 dark = vec3(0.05, 0.0, 0.0);
    vec3 hot = vec3(1.0, 0.35, 0.02);
    float glow = smoothstep(0.4, 0.9, n);
    return mix(dark, hot, glow) * Intensity;
}

void main() {
    vec3 dir = normalize(vWorldDir);
    vec3 color;
    if (Mode == 0) {
        color = renderAqua(dir);
    } else {
        color = renderMagma(dir);
    }

    OutColor = vec4(color, 1.0);
}