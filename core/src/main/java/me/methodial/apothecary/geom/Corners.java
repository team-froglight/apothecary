package me.methodial.apothecary.geom;

/** Per-corner radii in pixels: top-left, top-right, bottom-right, bottom-left (CSS order). */
public record Corners(float topLeft, float topRight, float bottomRight, float bottomLeft) {
    public static final Corners ZERO = new Corners(0, 0, 0, 0);

    public static Corners all(float r) { return new Corners(r, r, r, r); }

    public boolean isZero() { return topLeft == 0 && topRight == 0 && bottomRight == 0 && bottomLeft == 0; }

    /** Shrinks the radii proportionally so adjacent corners never overlap on a w*h box (CSS rule). */
    public Corners fit(float w, float h) {
        float f = 1f;
        if (topLeft + topRight > w) f = Math.min(f, w / (topLeft + topRight));
        if (bottomLeft + bottomRight > w) f = Math.min(f, w / (bottomLeft + bottomRight));
        if (topLeft + bottomLeft > h) f = Math.min(f, h / (topLeft + bottomLeft));
        if (topRight + bottomRight > h) f = Math.min(f, h / (topRight + bottomRight));
        if (f >= 1f) return this;
        f = Math.max(0, f);
        return new Corners(topLeft * f, topRight * f, bottomRight * f, bottomLeft * f);
    }

    public Corners lerp(Corners o, float t) {
        return new Corners(topLeft + (o.topLeft - topLeft) * t, topRight + (o.topRight - topRight) * t,
                bottomRight + (o.bottomRight - bottomRight) * t, bottomLeft + (o.bottomLeft - bottomLeft) * t);
    }

    public Corners shrink(float d) {
        return new Corners(Math.max(0, topLeft - d), Math.max(0, topRight - d),
                Math.max(0, bottomRight - d), Math.max(0, bottomLeft - d));
    }
}
