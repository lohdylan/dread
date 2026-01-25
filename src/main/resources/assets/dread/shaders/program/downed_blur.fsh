#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float BlurRadius;
uniform float VignetteIntensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 blurredColor = vec4(0.0);
    float totalWeight = 0.0;

    // Gaussian blur
    int samples = int(BlurRadius);
    for (int x = -samples; x <= samples; x++) {
        for (int y = -samples; y <= samples; y++) {
            vec2 offset = vec2(float(x), float(y)) / InSize;
            float weight = exp(-float(x*x + y*y) / (BlurRadius * 0.5));
            blurredColor += texture(DiffuseSampler, texCoord + offset) * weight;
            totalWeight += weight;
        }
    }

    blurredColor /= totalWeight;

    // Vignette effect
    vec2 center = texCoord - vec2(0.5);
    float dist = length(center);
    float vignette = 1.0 - smoothstep(0.3, 0.8, dist) * VignetteIntensity;

    fragColor = vec4(blurredColor.rgb * vignette, 1.0);
}
