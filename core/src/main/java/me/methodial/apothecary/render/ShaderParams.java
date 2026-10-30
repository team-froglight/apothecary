package me.methodial.apothecary.render;

import me.methodial.apothecary.style.Color;

/**
 * Inputs to a custom effect shader. Backends expose these as uniforms:
 * {@code Size} (vec2, px), {@code Time} (float, seconds), {@code Mouse} (vec2, local px, -1 when outside),
 * {@code Params0}/{@code Params1} (vec4 each), {@code Color0..Color3} (vec4, straight alpha) and {@code Opacity}.
 */
public final class ShaderParams {
    public final float[] params = new float[8];
    public final Color[] colors = {Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK};
    public float time;
    /** Corner radius the effect is clipped to (rounded SDF mask), px. */
    public float radius;
    public float mouseX = -1, mouseY = -1;

    public ShaderParams param(int i, float v) { params[i] = v; return this; }

    public ShaderParams color(int i, Color c) { colors[i] = c; return this; }
}
