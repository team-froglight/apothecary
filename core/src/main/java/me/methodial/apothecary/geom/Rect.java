package me.methodial.apothecary.geom;

/** An immutable axis-aligned rectangle. */
public record Rect(float x, float y, float width, float height) {
    public static final Rect EMPTY = new Rect(0, 0, 0, 0);

    public float right() { return x + width; }
    public float bottom() { return y + height; }
    public float centerX() { return x + width * 0.5f; }
    public float centerY() { return y + height * 0.5f; }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    public Rect inset(Edges e) {
        return new Rect(x + e.left(), y + e.top(),
                Math.max(0, width - e.left() - e.right()), Math.max(0, height - e.top() - e.bottom()));
    }

    public Rect outset(float d) { return new Rect(x - d, y - d, width + 2 * d, height + 2 * d); }

    public Rect translate(float dx, float dy) { return new Rect(x + dx, y + dy, width, height); }

    public Rect intersect(Rect o) {
        float l = Math.max(x, o.x), t = Math.max(y, o.y);
        float r = Math.min(right(), o.right()), b = Math.min(bottom(), o.bottom());
        return new Rect(l, t, Math.max(0, r - l), Math.max(0, b - t));
    }

    public boolean isEmpty() { return width <= 0 || height <= 0; }
}
