package me.methodial.apothecary.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A keyframe animation defined in code. Play it with {@code s.animation(PULSE.play(1.2f).infinite())}.
 * <pre>{@code
 * static final Keyframes PULSE = Keyframes.of("pulse")
 *         .from(s -> s.scale(1))
 *         .at(0.5f, s -> s.scale(1.35f).opacity(0.6f))
 *         .to(s -> s.scale(1));
 * }</pre>
 * Properties missing from the first/last frame animate from/to the element's own value.
 */
public final class Keyframes {
    public record Frame(float offset, Style style) { }

    private final String name;
    private final List<Frame> frames = new ArrayList<>();

    private Keyframes(String name) { this.name = name; }

    public static Keyframes of(String name) { return new Keyframes(name); }

    public Keyframes at(float offset, Consumer<Style> body) {
        Style s = new Style();
        body.accept(s);
        frames.add(new Frame(Math.max(0, Math.min(1, offset)), s));
        frames.sort((a, b) -> Float.compare(a.offset(), b.offset()));
        return this;
    }

    public Keyframes from(Consumer<Style> body) { return at(0, body); }

    public Keyframes to(Consumer<Style> body) { return at(1, body); }

    public String name() { return name; }

    public List<Frame> frames() { return Collections.unmodifiableList(frames); }

    /** Plays once over {@code seconds}; chain modifiers like {@code .infinite().alternate()}. */
    public AnimationSpec play(float seconds) { return AnimationSpec.of(this, seconds); }

    @Override public String toString() { return "Keyframes(" + name + ")"; }
}
