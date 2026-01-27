#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float BlurRadius;
uniform float VignetteIntensity;
uniform float FadeIntensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    // Sample original color (used when fading in)
    vec4 originalColor = texture(DiffuseSampler, texCoord);

    // Early exit if no effect needed
    if (FadeIntensity <= 0.0) {
        fragColor = originalColor;
        return;
    }

    vec4 blurredColor = vec4(0.0);
    float totalWeight = 0.0;

    // Gaussian blur with intensity-scaled radius
    float effectiveRadius = BlurRadius * FadeIntensity;
    int samples = int(max(1.0, effectiveRadius));
    for (int x = -samples; x <= samples; x++) {
        for (int y = -samples; y <= samples; y++) {
            vec2 offset = vec2(float(x), float(y)) / InSize;
            float weight = exp(-float(x*x + y*y) / (effectiveRadius * 0.5 + 0.001));
            blurredColor += texture(DiffuseSampler, texCoord + offset) * weight;
            totalWeight += weight;
        }
    }

    blurredColor /= totalWeight;

    // Vignette effect with fade
    vec2 center = texCoord - vec2(0.5);
    float dist = length(center);
    float vignette = 1.0 - smoothstep(0.3, 0.8, dist) * VignetteIntensity * FadeIntensity;

    // Mix between original and effect based on fade intensity
    vec4 effectColor = vec4(blurredColor.rgb * vignette, 1.0);
    fragColor = mix(originalColor, effectColor, FadeIntensity);
}
