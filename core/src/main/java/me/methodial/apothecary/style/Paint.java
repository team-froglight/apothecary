package me.methodial.apothecary.style;

import java.util.List;

/** What fills a shape: a solid color or a gradient. Renderers support up to {@link #MAX_STOPS} gradient stops. */
public sealed interface Paint permits Paint.Solid, Paint.Linear, Paint.Radial {
    int MAX_STOPS = 4;
    Paint NONE = new Solid(Color.TRANSPARENT);

    record Stop(Color color, float position) {
        public Stop lerp(Stop o, float t) { return new Stop(color.lerp(o.color, t), position + (o.position - position) * t); }
    }

    record Solid(Color color) implements Paint { }

    /** Linear gradient; angle in degrees, CSS convention (0 = to top, 90 = to right, 180 = to bottom). */
    record Linear(float angle, List<Stop> stops) implements Paint { }

    /** Radial gradient centered at (cx, cy) in 0..1 box space, radius as a fraction of the farthest corner. */
    record Radial(float cx, float cy, float radius, List<Stop> stops) implements Paint { }

    static Paint of(Color c) { return new Solid(c); }

    static Paint linear(float angle, Color... colors) { return new Linear(angle, evenStops(colors)); }

    static Paint radial(Color... colors) { return new Radial(0.5f, 0.5f, 1f, evenStops(colors)); }

    static List<Stop> evenStops(Color... colors) {
        Stop[] s = new Stop[colors.length];
        for (int i = 0; i < colors.length; i++) s[i] = new Stop(colors[i], colors.length == 1 ? 0 : i / (float) (colors.length - 1));
        return List.of(s);
    }

    default boolean isVisible() {
        return switch (this) {
            case Solid s -> !s.color().isTransparent();
            case Linear l -> l.stops().stream().anyMatch(st -> !st.color().isTransparent());
            case Radial r -> r.stops().stream().anyMatch(st -> !st.color().isTransparent());
        };
    }

    /** Any paint seen as a list of stops (solid = one stop). */
    default List<Stop> stops() {
        return switch (this) {
            case Solid s -> List.of(new Stop(s.color(), 0));
            case Linear l -> l.stops();
            case Radial r -> r.stops();
        };
    }

    default Paint multiplyAlpha(float f) {
        if (f >= 1) return this;
        return switch (this) {
            case Solid s -> new Solid(s.color().multiplyAlpha(f));
            case Linear l -> new Linear(l.angle(), l.stops().stream().map(st -> new Stop(st.color().multiplyAlpha(f), st.position())).toList());
            case Radial r -> new Radial(r.cx(), r.cy(), r.radius(), r.stops().stream().map(st -> new Stop(st.color().multiplyAlpha(f), st.position())).toList());
        };
    }

    default Paint lerp(Paint o, float t) {
        if (t <= 0) return this;
        if (t >= 1) return o;
        if (this instanceof Solid a && o instanceof Solid b) return new Solid(a.color().lerp(b.color(), t));
        // Promote both sides to the same gradient kind and blend stop-by-stop.
        List<Stop> sa = resample(stops(), Math.max(stops().size(), o.stops().size()));
        List<Stop> sb = resample(o.stops(), sa.size());
        Stop[] mixed = new Stop[sa.size()];
        for (int i = 0; i < mixed.length; i++) mixed[i] = sa.get(i).lerp(sb.get(i), t);
        if (this instanceof Radial || o instanceof Radial) {
            Radial ra = this instanceof Radial r ? r : (o instanceof Radial r2 ? r2 : null);
            Radial rb = o instanceof Radial r ? r : ra;
            ra = this instanceof Radial r ? r : rb;
            return new Radial(ra.cx() + (rb.cx() - ra.cx()) * t, ra.cy() + (rb.cy() - ra.cy()) * t,
                    ra.radius() + (rb.radius() - ra.radius()) * t, List.of(mixed));
        }
        float a0 = this instanceof Linear l ? l.angle() : (o instanceof Linear l2 ? l2.angle() : 180);
        float a1 = o instanceof Linear l ? l.angle() : a0;
        return new Linear(a0 + (a1 - a0) * t, List.of(mixed));
    }

    private static List<Stop> resample(List<Stop> stops, int n) {
        if (stops.size() == n) return stops;
        Stop[] out = new Stop[n];
        for (int i = 0; i < n; i++) {
            float pos = n == 1 ? 0 : i / (float) (n - 1);
            out[i] = new Stop(sample(stops, pos), pos);
        }
        return List.of(out);
    }

    static Color sample(List<Stop> stops, float pos) {
        if (stops.size() == 1 || pos <= stops.get(0).position()) return stops.get(0).color();
        for (int i = 1; i < stops.size(); i++) {
            Stop a = stops.get(i - 1), b = stops.get(i);
            if (pos <= b.position()) {
                float span = b.position() - a.position();
                return a.color().lerp(b.color(), span <= 0 ? 1 : (pos - a.position()) / span);
            }
        }
        return stops.get(stops.size() - 1).color();
    }
}
