package me.methodial.apothecary.geom;

/**
 * An immutable 2D affine transform.
 * <pre>
 * | a c tx |
 * | b d ty |
 * | 0 0 1  |
 * </pre>
 */
public record Affine(float a, float b, float c, float d, float tx, float ty) {
    public static final Affine IDENTITY = new Affine(1, 0, 0, 1, 0, 0);

    public static Affine translation(float x, float y) { return new Affine(1, 0, 0, 1, x, y); }
    public static Affine scaling(float sx, float sy) { return new Affine(sx, 0, 0, sy, 0, 0); }

    public static Affine rotation(float radians) {
        float cos = (float) Math.cos(radians), sin = (float) Math.sin(radians);
        return new Affine(cos, sin, -sin, cos, 0, 0);
    }

    /** this * o : applies {@code o} first, then {@code this}. */
    public Affine multiply(Affine o) {
        return new Affine(
                a * o.a + c * o.b, b * o.a + d * o.b,
                a * o.c + c * o.d, b * o.c + d * o.d,
                a * o.tx + c * o.ty + tx, b * o.tx + d * o.ty + ty);
    }

    public Affine translate(float x, float y) { return multiply(translation(x, y)); }
    public Affine scale(float sx, float sy) { return multiply(scaling(sx, sy)); }
    public Affine rotate(float radians) { return multiply(rotation(radians)); }

    public float mapX(float x, float y) { return a * x + c * y + tx; }
    public float mapY(float x, float y) { return b * x + d * y + ty; }

    public boolean isIdentity() { return this.equals(IDENTITY); }
    public boolean isTranslationOnly() { return a == 1 && b == 0 && c == 0 && d == 1; }

    public Affine invert() {
        float det = a * d - b * c;
        if (Math.abs(det) < 1e-9f) return IDENTITY;
        float id = 1f / det;
        return new Affine(d * id, -b * id, -c * id, a * id,
                (c * ty - d * tx) * id, (b * tx - a * ty) * id);
    }

    /** Axis-aligned bounds of {@code r} after this transform. */
    public Rect mapBounds(Rect r) {
        float x0 = r.x(), y0 = r.y(), x1 = r.right(), y1 = r.bottom();
        float ax = mapX(x0, y0), ay = mapY(x0, y0), bx = mapX(x1, y0), by = mapY(x1, y0);
        float cx = mapX(x1, y1), cy = mapY(x1, y1), dx = mapX(x0, y1), dy = mapY(x0, y1);
        float minX = Math.min(Math.min(ax, bx), Math.min(cx, dx)), maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
        float minY = Math.min(Math.min(ay, by), Math.min(cy, dy)), maxY = Math.max(Math.max(ay, by), Math.max(cy, dy));
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    /** Uniform scale factor estimate (for picking text raster sizes etc). */
    public float approxScale() { return (float) Math.sqrt(Math.abs(a * d - b * c)); }
}
