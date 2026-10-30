package me.methodial.apothecary.style;

/** transform-origin, relative to the element's border box (defaults to the center). */
public record TransformOrigin(Length x, Length y) {
    public static final TransformOrigin CENTER = new TransformOrigin(Length.percent(50), Length.percent(50));

    public float resolveX(float w) { return x.resolveOr(w, w / 2); }
    public float resolveY(float h) { return y.resolveOr(h, h / 2); }
}
