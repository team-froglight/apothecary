package me.methodial.apothecary.style;

import me.methodial.apothecary.geom.Edges;

/** Four lengths (margins may be auto or percentages). CSS order: top, right, bottom, left. */
public record LengthEdges(Length top, Length right, Length bottom, Length left) {
    public static final LengthEdges ZERO = all(Length.ZERO);
    public static final LengthEdges AUTO = all(Length.AUTO);

    public static LengthEdges all(Length l) { return new LengthEdges(l, l, l, l); }
    public static LengthEdges px(float v) { return all(Length.px(v)); }
    public static LengthEdges px(float vertical, float horizontal) {
        return new LengthEdges(Length.px(vertical), Length.px(horizontal), Length.px(vertical), Length.px(horizontal));
    }
    public static LengthEdges px(float t, float r, float b, float l) {
        return new LengthEdges(Length.px(t), Length.px(r), Length.px(b), Length.px(l));
    }

    /** Resolves to pixels against the containing block width (as CSS does for every side); auto becomes 0. */
    public Edges resolve(float base) {
        return new Edges(top.resolveOr(base, 0), right.resolveOr(base, 0), bottom.resolveOr(base, 0), left.resolveOr(base, 0));
    }

    public LengthEdges lerp(LengthEdges o, float t) {
        return new LengthEdges(top.lerp(o.top, t), right.lerp(o.right, t), bottom.lerp(o.bottom, t), left.lerp(o.left, t));
    }
}
