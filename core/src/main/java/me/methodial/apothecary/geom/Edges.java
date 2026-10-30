package me.methodial.apothecary.geom;

/** Four edge values in pixels (padding, border widths, ...). CSS order: top, right, bottom, left. */
public record Edges(float top, float right, float bottom, float left) {
    public static final Edges ZERO = new Edges(0, 0, 0, 0);

    public static Edges all(float v) { return new Edges(v, v, v, v); }
    public static Edges of(float vertical, float horizontal) { return new Edges(vertical, horizontal, vertical, horizontal); }

    public float horizontal() { return left + right; }
    public float vertical() { return top + bottom; }

    public boolean isZero() { return top == 0 && right == 0 && bottom == 0 && left == 0; }

    public Edges lerp(Edges o, float t) {
        return new Edges(top + (o.top - top) * t, right + (o.right - right) * t,
                bottom + (o.bottom - bottom) * t, left + (o.left - left) * t);
    }
}
