#version 150

#moj_import <sun:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Softness;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 quadSize = Size + vec2(Softness * 2.0);
    vec2 centerPos = (FragCoord - vec2(0.5)) * quadSize;
    float dist = roundedBoxSDF(centerPos, Size * 0.5, Radius);

    if (dist <= 0.0) {
        discard;
    }

    float alpha = 1.0 - smoothstep(0.0, Softness, dist);
    alpha = alpha * alpha;

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a <= 0.001) {
        discard;
    }

    OutColor = finalColor * ColorModulator;
}