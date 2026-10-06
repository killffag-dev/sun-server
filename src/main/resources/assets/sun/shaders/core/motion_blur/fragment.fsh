#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform mat4 GameModelView;
uniform mat4 GamePrevModelView;
uniform mat4 GameProjection;
uniform mat4 GamePrevProjection;
uniform mat4 InverseGameModelView;
uniform mat4 InverseGameProjection;
uniform vec3 CameraPos;
uniform vec3 PrevCameraPos;
uniform vec2 ViewRes;
uniform float BlendFactor;
uniform int MotionBlurSamples;
uniform int BlurAlgorithm;
uniform int UseDepth;

out vec4 OutColor;

vec3 reproject(vec3 screenPos) {
    vec3 ndc = screenPos * 2.0 - 1.0;

    vec4 viewPos4 = InverseGameProjection * vec4(ndc, 1.0);
    vec3 viewPos = viewPos4.xyz / viewPos4.w;

    vec3 worldPos = (InverseGameModelView * vec4(viewPos, 1.0)).xyz + (CameraPos - PrevCameraPos);
    vec4 prevProj = GamePrevProjection * (GamePrevModelView * vec4(worldPos, 1.0));

    return (prevProj.xyz / prevProj.w) * 0.5 + 0.5;
}

vec2 clampLength(vec2 velocity) {
    float lenSq = dot(velocity, velocity);
    return (lenSq > 0.16) ? velocity * (0.4 * inversesqrt(lenSq)) : velocity;
}

float ditherNoise(vec2 pos) {
    return fract(52.9829189 * fract(0.06711056 * pos.x + 0.00583715 * pos.y));
}

void main() {
    float depth = texture(Sampler1, TexCoord).x;
    // фикс на руку игрока: у неё своя глубина, блюрить её не нужно
    if (depth < 0.56) {
        OutColor = texture(Sampler0, TexCoord);
        return;
    }

    float dilatedDepth = depth;
    vec2 texelSize = 1.0 / ViewRes;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float d = texture(Sampler1, TexCoord + vec2(float(x), float(y)) * texelSize).x;
            dilatedDepth = min(dilatedDepth, d);
        }
    }

    vec2 velocity = TexCoord - reproject(vec3(TexCoord, UseDepth == 1 ? dilatedDepth : 1.0)).xy;
    velocity = clampLength(velocity);

    float speed = length(velocity);
    int dynamicSamples = clamp(int(ceil(speed * float(MotionBlurSamples))), 4, MotionBlurSamples);

    vec2 baseStep = (BlendFactor * velocity) / float(dynamicSamples);
    vec3 colorSum = vec3(0.0);
    vec2 seed = TexCoord * ViewRes;
    float centerOffset = BlurAlgorithm == 0 ? 0.0 : -(float(dynamicSamples) * 0.5);

    for (int i = 0; i < dynamicSamples; ++i) {
        float fi = float(i);
        float jitter = ditherNoise(seed + vec2(fi, fi * 1.4));
        vec2 pos = TexCoord + (fi + centerOffset + jitter) * baseStep;
        vec3 sampleColor = texture(Sampler0, pos).rgb;
        colorSum += sampleColor * sampleColor;
    }

    OutColor = vec4(sqrt(colorSum / float(dynamicSamples)), 1.0) * FragColor.a;
}