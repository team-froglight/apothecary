package me.methodial.apothecary.style;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.anim.Easings;

/**
 * Runs a {@link Keyframes} animation on an element. Immutable; modifiers return copies:
 * <pre>{@code
 * SPIN.play(1.6f).easing(Easings.LINEAR).infinite()
 * RISE_IN.play(0.5f).delay(0.1f).backwards()
 * }</pre>
 * iterations: {@link Float#POSITIVE_INFINITY} for infinite.
 */
public record AnimationSpec(Keyframes keyframes, float duration, Easing easing, float delay, float iterations,
                            Direction direction, FillMode fillMode, boolean paused) {
    public enum Direction { NORMAL, REVERSE, ALTERNATE, ALTERNATE_REVERSE }
    public enum FillMode { NONE, FORWARDS, BACKWARDS, BOTH }

    public static AnimationSpec of(Keyframes k, float seconds) {
        return new AnimationSpec(k, seconds, Easings.EASE, 0, 1, Direction.NORMAL, FillMode.NONE, false);
    }

    public AnimationSpec easing(Easing e) { return new AnimationSpec(keyframes, duration, e, delay, iterations, direction, fillMode, paused); }
    public AnimationSpec delay(float s) { return new AnimationSpec(keyframes, duration, easing, s, iterations, direction, fillMode, paused); }
    public AnimationSpec repeat(float times) { return new AnimationSpec(keyframes, duration, easing, delay, times, direction, fillMode, paused); }
    public AnimationSpec infinite() { return repeat(Float.POSITIVE_INFINITY); }
    public AnimationSpec direction(Direction d) { return new AnimationSpec(keyframes, duration, easing, delay, iterations, d, fillMode, paused); }
    public AnimationSpec alternate() { return direction(Direction.ALTERNATE); }
    public AnimationSpec reverse() { return direction(Direction.REVERSE); }
    public AnimationSpec fill(FillMode f) { return new AnimationSpec(keyframes, duration, easing, delay, iterations, direction, f, paused); }
    /** Hold the first frame during the delay (no flash of the final state before an entrance). */
    public AnimationSpec backwards() { return fill(FillMode.BACKWARDS); }
    /** Keep the last frame after finishing. */
    public AnimationSpec forwards() { return fill(FillMode.FORWARDS); }
    public AnimationSpec both() { return fill(FillMode.BOTH); }
    public AnimationSpec paused(boolean p) { return new AnimationSpec(keyframes, duration, easing, delay, iterations, direction, fillMode, p); }

    /** Identity used to decide whether a running animation restarts when styles recompute. */
    public String key() {
        return keyframes.name() + "@" + System.identityHashCode(keyframes) + "/" + duration + "/" + delay + "/" + iterations + "/" + direction;
    }
}
