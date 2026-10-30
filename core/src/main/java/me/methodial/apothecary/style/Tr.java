package me.methodial.apothecary.style;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.anim.Easings;

import java.util.ArrayList;
import java.util.List;

/**
 * Transition presets, grouped the way UIs usually animate:
 * <pre>{@code
 * s.transition(Tr.colors(0.12f), Tr.transform(0.16f, Easings.OUT_BACK), Tr.shadow(0.2f));
 * }</pre>
 */
public final class Tr {
    private Tr() { }

    /** Background, text, border, outline and tint colors. */
    public static TransitionSpec colors(float seconds) { return colors(seconds, Easings.EASE); }
    public static TransitionSpec colors(float seconds, Easing e) { return TransitionSpec.of(seconds, e, Props.COLORS); }

    public static TransitionSpec transform(float seconds) { return transform(seconds, Easings.EASE_OUT); }
    public static TransitionSpec transform(float seconds, Easing e) { return TransitionSpec.of(seconds, e, List.of(Props.TRANSFORM)); }

    public static TransitionSpec shadow(float seconds) { return TransitionSpec.of(seconds, Easings.EASE, List.of(Props.BOX_SHADOW)); }

    public static TransitionSpec opacity(float seconds) { return TransitionSpec.of(seconds, Easings.EASE, List.of(Props.OPACITY)); }

    public static TransitionSpec radius(float seconds, Easing e) { return TransitionSpec.of(seconds, e, List.of(Props.BORDER_RADIUS)); }

    /** Width and height. */
    public static TransitionSpec size(float seconds, Easing e) { return TransitionSpec.of(seconds, e, Props.SIZE); }

    /** Any explicit set of properties. */
    public static TransitionSpec of(float seconds, Easing e, Prop<?>... props) { return TransitionSpec.of(seconds, e, List.of(props)); }

    /** Colors + transform + shadow + opacity: the usual "interactive element" set. */
    public static TransitionSpec interactive(float seconds) {
        List<Prop<?>> ps = new ArrayList<>(Props.COLORS);
        ps.add(Props.TRANSFORM);
        ps.add(Props.BOX_SHADOW);
        ps.add(Props.OPACITY);
        return TransitionSpec.of(seconds, Easings.EASE_OUT, ps);
    }

    /** Every animatable property. */
    public static TransitionSpec all(float seconds) { return TransitionSpec.all(seconds); }
}
