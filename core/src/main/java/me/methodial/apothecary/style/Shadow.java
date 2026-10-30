package me.methodial.apothecary.style;

import java.util.ArrayList;
import java.util.List;

/** One box-shadow layer. {@code inset} shadows are drawn inside the box (over the background). */
public record Shadow(float offsetX, float offsetY, float blur, float spread, Color color, boolean inset) {
    public static final List<Shadow> NONE = List.of();

    public static Shadow of(float ox, float oy, float blur, Color color) { return new Shadow(ox, oy, blur, 0, color, false); }

    /** A shadow drawn inside the box (bevels, recessed wells). */
    public static Shadow inset(float ox, float oy, float blur, Color color) { return new Shadow(ox, oy, blur, 0, color, true); }

    public Shadow lerp(Shadow o, float t) {
        return new Shadow(offsetX + (o.offsetX - offsetX) * t, offsetY + (o.offsetY - offsetY) * t,
                blur + (o.blur - blur) * t, spread + (o.spread - spread) * t, color.lerp(o.color, t), t < 0.5f ? inset : o.inset);
    }

    private Shadow invisible() { return new Shadow(offsetX, offsetY, blur, spread, color.withAlpha(0), inset); }

    /** Blends two shadow lists layer by layer; a missing layer fades in or out. */
    public static List<Shadow> lerp(List<Shadow> a, List<Shadow> b, float t) {
        int n = Math.max(a.size(), b.size());
        List<Shadow> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Shadow sa = i < a.size() ? a.get(i) : b.get(i).invisible();
            Shadow sb = i < b.size() ? b.get(i) : a.get(i).invisible();
            out.add(sa.lerp(sb, t));
        }
        return out;
    }
}
