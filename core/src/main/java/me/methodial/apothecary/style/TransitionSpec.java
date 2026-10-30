package me.methodial.apothecary.style;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.anim.Easings;

import java.util.List;
import java.util.Set;

/**
 * Which properties animate when their value changes, for how long (seconds), with what easing.
 * Build with {@link Tr}: {@code s.transition(Tr.colors(0.12f), Tr.transform(0.2f, Easings.OUT_BACK))}.
 * {@code props == null} means every animatable property.
 */
public record TransitionSpec(Set<Prop<?>> props, float duration, Easing easing, float delay) {
    public static TransitionSpec of(float seconds, Easing easing, List<Prop<?>> props) {
        return new TransitionSpec(Set.copyOf(props), seconds, easing, 0);
    }

    public static TransitionSpec all(float seconds) { return new TransitionSpec(null, seconds, Easings.EASE, 0); }

    public TransitionSpec delay(float seconds) { return new TransitionSpec(props, duration, easing, seconds); }

    public TransitionSpec easing(Easing e) { return new TransitionSpec(props, duration, e, delay); }

    public boolean matches(Prop<?> p) { return props == null || props.contains(p); }
}
