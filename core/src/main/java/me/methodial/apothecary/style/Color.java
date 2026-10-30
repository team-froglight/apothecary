package me.methodial.apothecary.style;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** An sRGB color with straight (non-premultiplied) alpha, stored as packed ARGB. */
public record Color(int argb) {
    public static final Color TRANSPARENT = new Color(0x00000000);
    public static final Color BLACK = new Color(0xFF000000);
    public static final Color WHITE = new Color(0xFFFFFFFF);

    private static final Map<String, Integer> NAMED = new HashMap<>();
    static {
        String[] n = {
                "black", "000000", "white", "ffffff", "red", "ff0000", "green", "008000", "lime", "00ff00",
                "blue", "0000ff", "yellow", "ffff00", "cyan", "00ffff", "aqua", "00ffff", "magenta", "ff00ff",
                "fuchsia", "ff00ff", "gray", "808080", "grey", "808080", "silver", "c0c0c0", "maroon", "800000",
                "olive", "808000", "purple", "800080", "teal", "008080", "navy", "000080", "orange", "ffa500",
                "pink", "ffc0cb", "gold", "ffd700", "coral", "ff7f50", "salmon", "fa8072", "tomato", "ff6347",
                "crimson", "dc143c", "indigo", "4b0082", "violet", "ee82ee", "orchid", "da70d6", "plum", "dda0dd",
                "turquoise", "40e0d0", "skyblue", "87ceeb", "steelblue", "4682b4", "slategray", "708090",
                "khaki", "f0e68c", "beige", "f5f5dc", "ivory", "fffff0", "chocolate", "d2691e", "brown", "a52a2a",
                "tan", "d2b48c", "wheat", "f5deb3", "mintcream", "f5fffa", "lavender", "e6e6fa", "hotpink", "ff69b4",
                "dodgerblue", "1e90ff", "royalblue", "4169e1", "seagreen", "2e8b57", "forestgreen", "228b22",
                "limegreen", "32cd32", "springgreen", "00ff7f", "darkgray", "a9a9a9", "lightgray", "d3d3d3",
                "rebeccapurple", "663399",
        };
        for (int i = 0; i < n.length; i += 2) NAMED.put(n[i], 0xFF000000 | Integer.parseInt(n[i + 1], 16));
    }

    /** {@code Color.hex("#7c5cff")}, {@code "#7c5cff80"} (with alpha), {@code "#fff"}. */
    public static Color hex(String hex) { return parse(hex); }

    /** 0xRRGGBB, fully opaque. */
    public static Color hex(int rgb) { return ofRgb(rgb); }

    public static Color rgb(int r, int g, int b) { return argb(255, r, g, b); }

    public static Color rgba(int r, int g, int b, float a) { return argb(Math.round(clamp01(a) * 255), r, g, b); }

    public static Color argb(int a, int r, int g, int b) {
        return new Color((clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b));
    }

    public static Color ofRgb(int rgb) { return new Color(0xFF000000 | rgb); }

    public static Color ofArgb(int argb) { return new Color(argb); }

    public static Color ofFloats(float r, float g, float b, float a) {
        return argb(Math.round(a * 255), Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** h in degrees, s/l in 0..1. */
    public static Color hsl(float h, float s, float l, float a) {
        h = ((h % 360) + 360) % 360 / 360f;
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;
        return ofFloats(hue(p, q, h + 1 / 3f), hue(p, q, h), hue(p, q, h - 1 / 3f), a);
    }

    /** h in degrees, s/v in 0..1. */
    public static Color hsv(float h, float s, float v, float a) {
        h = ((h % 360) + 360) % 360 / 60f;
        int i = (int) Math.floor(h);
        float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        return switch (i % 6) {
            case 0 -> ofFloats(v, t, p, a);
            case 1 -> ofFloats(q, v, p, a);
            case 2 -> ofFloats(p, v, t, a);
            case 3 -> ofFloats(p, q, v, a);
            case 4 -> ofFloats(t, p, v, a);
            default -> ofFloats(v, p, q, a);
        };
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1 / 6f) return p + (q - p) * 6 * t;
        if (t < 1 / 2f) return q;
        if (t < 2 / 3f) return p + (q - p) * (2 / 3f - t) * 6;
        return p;
    }

    public int alpha() { return (argb >>> 24) & 0xFF; }
    public int red() { return (argb >> 16) & 0xFF; }
    public int green() { return (argb >> 8) & 0xFF; }
    public int blue() { return argb & 0xFF; }

    public float a() { return alpha() / 255f; }
    public float r() { return red() / 255f; }
    public float g() { return green() / 255f; }
    public float b() { return blue() / 255f; }

    public boolean isTransparent() { return alpha() == 0; }

    public Color withAlpha(float a) { return new Color((argb & 0x00FFFFFF) | (clamp255(Math.round(clamp01(a) * 255)) << 24)); }

    public Color multiplyAlpha(float f) {
        if (f >= 1f) return this;
        return new Color((argb & 0x00FFFFFF) | (clamp255(Math.round(alpha() * clamp01(f))) << 24));
    }

    /** Mixes towards white (amount &gt; 0) or black (amount &lt; 0). */
    public Color lighten(float amount) {
        Color target = amount >= 0 ? WHITE : BLACK;
        Color c = lerp(target.withAlpha(a()), Math.abs(amount));
        return c;
    }

    /** Interpolates in premultiplied space so fading to transparent doesn't go grey. */
    public Color lerp(Color o, float t) {
        if (t <= 0) return this;
        if (t >= 1) return o;
        float a0 = a(), a1 = o.a();
        float a = a0 + (a1 - a0) * t;
        if (a <= 0.0001f) return TRANSPARENT;
        float r = (r() * a0 + (o.r() * a1 - r() * a0) * t) / a;
        float g = (g() * a0 + (o.g() * a1 - g() * a0) * t) / a;
        float b = (b() * a0 + (o.b() * a1 - b() * a0) * t) / a;
        return ofFloats(r, g, b, a);
    }

    public float[] toHsv() {
        float r = r(), g = g(), b = b();
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = 60 * (((g - b) / d) % 6);
        else if (max == g) h = 60 * ((b - r) / d + 2);
        else h = 60 * ((r - g) / d + 4);
        if (h < 0) h += 360;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    public String toHex() {
        return alpha() == 255 ? String.format("#%06x", argb & 0xFFFFFF) : String.format("#%06x%02x", argb & 0xFFFFFF, alpha());
    }

    @Override public String toString() { return toHex(); }

    /** Parses #rgb, #rgba, #rrggbb, #rrggbbaa, rgb(), rgba(), hsl(), hsla(), hsv(), named colors and "transparent". */
    public static Color parse(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        if (s.equals("transparent") || s.equals("none")) return TRANSPARENT;
        if (s.startsWith("#")) {
            String h = s.substring(1);
            switch (h.length()) {
                case 3, 4 -> {
                    StringBuilder sb = new StringBuilder();
                    for (char ch : h.toCharArray()) sb.append(ch).append(ch);
                    h = sb.toString();
                }
                default -> { }
            }
            if (h.length() == 6) return ofRgb(Integer.parseInt(h, 16));
            if (h.length() == 8) {
                long v = Long.parseLong(h, 16);
                int rgb = (int) (v >>> 8), a = (int) (v & 0xFF);
                return new Color((a << 24) | rgb);
            }
            throw new IllegalArgumentException("Bad color: " + s);
        }
        int paren = s.indexOf('(');
        if (paren > 0 && s.endsWith(")")) {
            String fn = s.substring(0, paren).trim();
            String[] parts = s.substring(paren + 1, s.length() - 1).split("[,\\s/]+");
            float[] v = new float[parts.length];
            boolean[] pct = new boolean[parts.length];
            int n = 0;
            for (String p : parts) {
                if (p.isBlank()) continue;
                pct[n] = p.endsWith("%");
                String num = p.replace("%", "").replace("deg", "");
                v[n++] = Float.parseFloat(num);
            }
            float alpha = n > 3 ? (pct[3] ? v[3] / 100f : v[3]) : 1f;
            switch (fn) {
                case "rgb", "rgba" -> {
                    float r = pct[0] ? v[0] * 2.55f : v[0], g = pct[1] ? v[1] * 2.55f : v[1], b = pct[2] ? v[2] * 2.55f : v[2];
                    return rgba(Math.round(r), Math.round(g), Math.round(b), alpha);
                }
                case "hsl", "hsla" -> {
                    return hsl(v[0], v[1] / (pct[1] || v[1] > 1 ? 100f : 1f), v[2] / (pct[2] || v[2] > 1 ? 100f : 1f), alpha);
                }
                case "hsv", "hsva", "hsb" -> {
                    return hsv(v[0], v[1] / (pct[1] || v[1] > 1 ? 100f : 1f), v[2] / (pct[2] || v[2] > 1 ? 100f : 1f), alpha);
                }
                default -> throw new IllegalArgumentException("Unknown color function: " + fn);
            }
        }
        Integer named = NAMED.get(s);
        if (named != null) return new Color(named);
        throw new IllegalArgumentException("Bad color: " + s);
    }

    public static boolean looksLikeColor(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.startsWith("#") || s.startsWith("rgb") || s.startsWith("hsl") || s.startsWith("hsv")
                || NAMED.containsKey(s) || s.equals("transparent");
    }

    private static int clamp255(int v) { return v < 0 ? 0 : Math.min(255, v); }
    private static float clamp01(float v) { return v < 0 ? 0 : Math.min(1, v); }
}
