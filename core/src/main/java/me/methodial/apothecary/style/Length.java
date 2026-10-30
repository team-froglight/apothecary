package me.methodial.apothecary.style;


/** A CSS-like length: pixels, percentage of a reference size, viewport units, fr (grid only) or auto. */
public record Length(float value, Unit unit) {
    public enum Unit { PX, PERCENT, VW, VH, FR, AUTO }

    public static final Length AUTO = new Length(0, Unit.AUTO);
    public static final Length ZERO = new Length(0, Unit.PX);

    /** Viewport size used for vw/vh; updated by the root before each layout pass. */
    static float viewportW = 0, viewportH = 0;

    public static void setViewport(float w, float h) { viewportW = w; viewportH = h; }

    public static Length px(float v) { return new Length(v, Unit.PX); }
    public static Length percent(float v) { return new Length(v, Unit.PERCENT); }
    public static Length pct(float v) { return new Length(v, Unit.PERCENT); }
    public static Length fr(float v) { return new Length(v, Unit.FR); }
    public static Length vw(float v) { return new Length(v, Unit.VW); }
    public static Length vh(float v) { return new Length(v, Unit.VH); }

    public boolean isAuto() { return unit == Unit.AUTO; }
    public boolean isFr() { return unit == Unit.FR; }
    public boolean isPercent() { return unit == Unit.PERCENT; }

    /** Whether this resolves without a reference size. */
    public boolean isAbsolute() { return unit == Unit.PX || unit == Unit.VW || unit == Unit.VH; }

    /** Resolves to pixels; returns NaN when auto, fr, or a percentage of an unknown (NaN) base. */
    public float resolve(float base) {
        return switch (unit) {
            case PX -> value;
            case PERCENT -> Float.isNaN(base) ? Float.NaN : value * base / 100f;
            case VW -> value * viewportW / 100f;
            case VH -> value * viewportH / 100f;
            case FR, AUTO -> Float.NaN;
        };
    }

    /** Resolves, falling back to {@code fallback} when the result is undefined. */
    public float resolveOr(float base, float fallback) {
        float v = resolve(base);
        return Float.isNaN(v) ? fallback : v;
    }

    public Length lerp(Length o, float t) {
        if (unit == o.unit) return new Length(value + (o.value - value) * t, unit);
        if (isAbsolute() && o.isAbsolute()) {
            float a = resolve(0), b = o.resolve(0);
            return px(a + (b - a) * t);
        }
        return t < 0.5f ? this : o;
    }


    @Override public String toString() {
        return switch (unit) {
            case AUTO -> "auto";
            case PX -> fmt(value) + "px";
            case PERCENT -> fmt(value) + "%";
            case FR -> fmt(value) + "fr";
            case VW -> fmt(value) + "vw";
            case VH -> fmt(value) + "vh";
        };
    }

    private static String fmt(float v) { return v == (int) v ? Integer.toString((int) v) : Float.toString(v); }
}
