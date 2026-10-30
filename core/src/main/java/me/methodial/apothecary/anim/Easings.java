package me.methodial.apothecary.anim;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** The standard easing catalogue (CSS keywords, Penner equations, cubic-bezier, steps, springs). */
public final class Easings {
    private Easings() { }

    public static final Easing LINEAR = t -> t;

    public static final Easing EASE = cubicBezier(0.25f, 0.1f, 0.25f, 1f);
    public static final Easing EASE_IN = cubicBezier(0.42f, 0f, 1f, 1f);
    public static final Easing EASE_OUT = cubicBezier(0f, 0f, 0.58f, 1f);
    public static final Easing EASE_IN_OUT = cubicBezier(0.42f, 0f, 0.58f, 1f);

    public static final Easing IN_QUAD = t -> t * t;
    public static final Easing OUT_QUAD = t -> 1 - (1 - t) * (1 - t);
    public static final Easing IN_OUT_QUAD = t -> t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;

    public static final Easing IN_CUBIC = t -> t * t * t;
    public static final Easing OUT_CUBIC = t -> 1 - (float) Math.pow(1 - t, 3);
    public static final Easing IN_OUT_CUBIC = t -> t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;

    public static final Easing IN_QUART = t -> t * t * t * t;
    public static final Easing OUT_QUART = t -> 1 - (float) Math.pow(1 - t, 4);
    public static final Easing IN_OUT_QUART = t -> t < 0.5f ? 8 * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 4) / 2;

    public static final Easing IN_QUINT = t -> t * t * t * t * t;
    public static final Easing OUT_QUINT = t -> 1 - (float) Math.pow(1 - t, 5);
    public static final Easing IN_OUT_QUINT = t -> t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;

    public static final Easing IN_SINE = t -> 1 - (float) Math.cos(t * Math.PI / 2);
    public static final Easing OUT_SINE = t -> (float) Math.sin(t * Math.PI / 2);
    public static final Easing IN_OUT_SINE = t -> -((float) Math.cos(Math.PI * t) - 1) / 2;

    public static final Easing IN_EXPO = t -> t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10);
    public static final Easing OUT_EXPO = t -> t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
    public static final Easing IN_OUT_EXPO = t -> t == 0 ? 0 : t == 1 ? 1
            : t < 0.5f ? (float) Math.pow(2, 20 * t - 10) / 2 : (2 - (float) Math.pow(2, -20 * t + 10)) / 2;

    public static final Easing IN_CIRC = t -> 1 - (float) Math.sqrt(1 - t * t);
    public static final Easing OUT_CIRC = t -> (float) Math.sqrt(1 - (t - 1) * (t - 1));
    public static final Easing IN_OUT_CIRC = t -> t < 0.5f
            ? (1 - (float) Math.sqrt(1 - 4 * t * t)) / 2
            : ((float) Math.sqrt(1 - (float) Math.pow(-2 * t + 2, 2)) + 1) / 2;

    private static final float C1 = 1.70158f, C2 = C1 * 1.525f, C3 = C1 + 1;
    public static final Easing IN_BACK = t -> C3 * t * t * t - C1 * t * t;
    public static final Easing OUT_BACK = t -> 1 + C3 * (float) Math.pow(t - 1, 3) + C1 * (float) Math.pow(t - 1, 2);
    public static final Easing IN_OUT_BACK = t -> t < 0.5f
            ? ((float) Math.pow(2 * t, 2) * ((C2 + 1) * 2 * t - C2)) / 2
            : ((float) Math.pow(2 * t - 2, 2) * ((C2 + 1) * (t * 2 - 2) + C2) + 2) / 2;

    private static final float C4 = (float) (2 * Math.PI / 3), C5 = (float) (2 * Math.PI / 4.5);
    public static final Easing IN_ELASTIC = t -> t == 0 ? 0 : t == 1 ? 1
            : -(float) Math.pow(2, 10 * t - 10) * (float) Math.sin((t * 10 - 10.75) * C4);
    public static final Easing OUT_ELASTIC = t -> t == 0 ? 0 : t == 1 ? 1
            : (float) Math.pow(2, -10 * t) * (float) Math.sin((t * 10 - 0.75) * C4) + 1;
    public static final Easing IN_OUT_ELASTIC = t -> t == 0 ? 0 : t == 1 ? 1 : t < 0.5f
            ? -((float) Math.pow(2, 20 * t - 10) * (float) Math.sin((20 * t - 11.125) * C5)) / 2
            : ((float) Math.pow(2, -20 * t + 10) * (float) Math.sin((20 * t - 11.125) * C5)) / 2 + 1;

    public static final Easing OUT_BOUNCE = Easings::outBounce;
    public static final Easing IN_BOUNCE = t -> 1 - outBounce(1 - t);
    public static final Easing IN_OUT_BOUNCE = t -> t < 0.5f ? (1 - outBounce(1 - 2 * t)) / 2 : (1 + outBounce(2 * t - 1)) / 2;

    /** A gentle, slightly springy default for UI motion. */
    public static final Easing SPRING = spring(170, 26, 1);
    public static final Easing SPRING_BOUNCY = spring(180, 12, 1);

    private static float outBounce(float t) {
        final float n1 = 7.5625f, d1 = 2.75f;
        if (t < 1 / d1) return n1 * t * t;
        if (t < 2 / d1) return n1 * (t -= 1.5f / d1) * t + 0.75f;
        if (t < 2.5 / d1) return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        return n1 * (t -= 2.625f / d1) * t + 0.984375f;
    }

    /** CSS cubic-bezier(x1, y1, x2, y2), solved with Newton-Raphson + bisection fallback. */
    public static Easing cubicBezier(float x1, float y1, float x2, float y2) {
        return t -> {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            float u = t;
            for (int i = 0; i < 8; i++) {
                float x = bezier(u, x1, x2) - t;
                if (Math.abs(x) < 1e-5f) return bezier(u, y1, y2);
                float dx = bezierSlope(u, x1, x2);
                if (Math.abs(dx) < 1e-6f) break;
                u -= x / dx;
            }
            float lo = 0, hi = 1;
            u = t;
            for (int i = 0; i < 24; i++) {
                float x = bezier(u, x1, x2);
                if (Math.abs(x - t) < 1e-5f) break;
                if (x < t) lo = u; else hi = u;
                u = (lo + hi) / 2;
            }
            return bezier(u, y1, y2);
        };
    }

    private static float bezier(float t, float p1, float p2) {
        float it = 1 - t;
        return 3 * it * it * t * p1 + 3 * it * t * t * p2 + t * t * t;
    }

    private static float bezierSlope(float t, float p1, float p2) {
        float it = 1 - t;
        return 3 * it * it * p1 + 6 * it * t * (p2 - p1) + 3 * t * t * (1 - p2);
    }

    /** steps(n, jump-end). */
    public static Easing steps(int n) { return t -> t >= 1 ? 1 : (float) Math.floor(t * n) / n; }

    /**
     * A damped harmonic oscillator from 0 to 1, time-normalized so it settles at t = 1.
     * Underdamped springs overshoot. stiffness ~ 100..400, damping ~ 5..40.
     */
    public static Easing spring(float stiffness, float damping, float mass) {
        float w0 = (float) Math.sqrt(stiffness / mass);
        float zeta = damping / (2 * (float) Math.sqrt(stiffness * mass));
        float settle = settleTime(w0, zeta);
        return t -> {
            if (t >= 1) return 1;
            float time = t * settle;
            return 1 - springDisplacement(w0, zeta, time);
        };
    }

    /** Displacement from target (starting at 1, zero initial velocity). */
    static float springDisplacement(float w0, float zeta, float t) {
        if (zeta < 1) {
            float wd = w0 * (float) Math.sqrt(1 - zeta * zeta);
            return (float) (Math.exp(-zeta * w0 * t) * (Math.cos(wd * t) + (zeta * w0 / wd) * Math.sin(wd * t)));
        } else if (zeta == 1) {
            return (float) (Math.exp(-w0 * t) * (1 + w0 * t));
        } else {
            double s = Math.sqrt(zeta * zeta - 1);
            double r1 = -w0 * (zeta - s), r2 = -w0 * (zeta + s);
            double c2 = -r1 / (r2 - r1), c1 = 1 - c2;
            return (float) (c1 * Math.exp(r1 * t) + c2 * Math.exp(r2 * t));
        }
    }

    /** Time (seconds) until the spring stays within 0.1% of its target. */
    public static float settleTime(float w0, float zeta) {
        float t = 0, dt = 1 / 240f, lastOutside = 0;
        for (int i = 0; i < 240 * 10; i++) {
            t += dt;
            if (Math.abs(springDisplacement(w0, zeta, t)) > 0.001f) lastOutside = t;
            else if (t - lastOutside > 0.5f) break;
        }
        return Math.max(0.05f, lastOutside);
    }

    private static final Map<String, Easing> NAMED = new LinkedHashMap<>();
    static {
        NAMED.put("linear", LINEAR);
        NAMED.put("ease", EASE);
        NAMED.put("ease-in", EASE_IN);
        NAMED.put("ease-out", EASE_OUT);
        NAMED.put("ease-in-out", EASE_IN_OUT);
        String[] fam = {"quad", "cubic", "quart", "quint", "sine", "expo", "circ", "back", "elastic", "bounce"};
        Easing[][] e = {
                {IN_QUAD, OUT_QUAD, IN_OUT_QUAD}, {IN_CUBIC, OUT_CUBIC, IN_OUT_CUBIC}, {IN_QUART, OUT_QUART, IN_OUT_QUART},
                {IN_QUINT, OUT_QUINT, IN_OUT_QUINT}, {IN_SINE, OUT_SINE, IN_OUT_SINE}, {IN_EXPO, OUT_EXPO, IN_OUT_EXPO},
                {IN_CIRC, OUT_CIRC, IN_OUT_CIRC}, {IN_BACK, OUT_BACK, IN_OUT_BACK}, {IN_ELASTIC, OUT_ELASTIC, IN_OUT_ELASTIC},
                {IN_BOUNCE, OUT_BOUNCE, IN_OUT_BOUNCE},
        };
        for (int i = 0; i < fam.length; i++) {
            NAMED.put("ease-in-" + fam[i], e[i][0]);
            NAMED.put("ease-out-" + fam[i], e[i][1]);
            NAMED.put("ease-in-out-" + fam[i], e[i][2]);
        }
        NAMED.put("spring", SPRING);
        NAMED.put("spring-bouncy", SPRING_BOUNCY);
        NAMED.put("step-start", t -> t > 0 ? 1 : 0);
        NAMED.put("step-end", t -> t >= 1 ? 1 : 0);
    }

    /** Every named easing, in a stable order (handy for galleries). */
    public static Map<String, Easing> named() { return java.util.Collections.unmodifiableMap(NAMED); }

    public static Easing byName(String name) { return NAMED.get(name); }

    /** Parses a keyword, cubic-bezier(a,b,c,d), steps(n) or spring(stiffness, damping[, mass]). */
    public static Easing parse(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        Easing named = NAMED.get(s);
        if (named != null) return named;
        int p = s.indexOf('(');
        if (p > 0 && s.endsWith(")")) {
            String fn = s.substring(0, p);
            String[] a = s.substring(p + 1, s.length() - 1).split(",");
            float[] v = new float[a.length];
            for (int i = 0; i < a.length; i++) v[i] = Float.parseFloat(a[i].trim());
            switch (fn) {
                case "cubic-bezier": return cubicBezier(v[0], v[1], v[2], v[3]);
                case "steps": return steps((int) v[0]);
                case "spring": return spring(v[0], v.length > 1 ? v[1] : 20, v.length > 2 ? v[2] : 1);
                default: break;
            }
        }
        throw new IllegalArgumentException("Unknown easing: " + s);
    }
}
