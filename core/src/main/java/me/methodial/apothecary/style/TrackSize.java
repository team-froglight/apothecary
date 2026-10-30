package me.methodial.apothecary.style;

import java.util.List;

/**
 * One grid track sizing function. Build track lists with the static helpers:
 * <pre>{@code
 * s.cols(px(60), fr(1), fr(2));
 * s.cols(fill(minmax(px(40), fr(1))));     // repeat(auto-fill, minmax(40px, 1fr))
 * s.cols(repeat(3, fr(1)));
 * }</pre>
 */
public sealed interface TrackSize extends TrackEntry {
    /** A px / % / vw length. */
    record Fixed(Length length) implements TrackSize { }

    /** A flexible {@code fr} track. */
    record Flex(float fr) implements TrackSize { }

    /** auto: sized to its items. */
    record Auto() implements TrackSize { }

    record MinContent() implements TrackSize { }

    record MaxContent() implements TrackSize { }

    /** minmax(min, max). {@code max} may be a Flex track. */
    record MinMax(TrackSize min, TrackSize max) implements TrackSize { }

    /** fit-content(limit): content size clamped to {@code limit}. */
    record FitContent(Length limit) implements TrackSize { }

    TrackSize AUTO = new Auto();

    static TrackSize px(float v) { return new Fixed(Length.px(v)); }
    static TrackSize pct(float v) { return new Fixed(Length.pct(v)); }
    static TrackSize fr(float v) { return new Flex(v); }
    static TrackSize auto() { return AUTO; }
    static TrackSize minContent() { return new MinContent(); }
    static TrackSize maxContent() { return new MaxContent(); }
    static TrackSize minmax(TrackSize min, TrackSize max) { return new MinMax(min, max); }
    static TrackSize fitContent(float px) { return new FitContent(Length.px(px)); }

    /** repeat(count, tracks...). */
    static TrackEntry repeat(int count, TrackSize... tracks) { return new Repeat(count, List.of(tracks)); }
    /** repeat(auto-fill, tracks...): as many as fit. */
    static TrackEntry fill(TrackSize... tracks) { return new Repeat(Repeat.AUTO_FILL, List.of(tracks)); }
    /** repeat(auto-fit, tracks...): like fill, but empty tracks collapse. */
    static TrackEntry fit(TrackSize... tracks) { return new Repeat(Repeat.AUTO_FIT, List.of(tracks)); }

    /** The sizing function used for the track's minimum (base size). */
    default TrackSize minSizing() {
        if (this instanceof MinMax m) return m.min();
        if (this instanceof Flex) return AUTO;
        return this;
    }

    /** The sizing function used for the track's growth limit. */
    default TrackSize maxSizing() {
        if (this instanceof MinMax m) return m.max();
        return this;
    }

    default boolean isFlexible() { return maxSizing() instanceof Flex; }

    default float flexFactor() { return maxSizing() instanceof Flex f ? f.fr() : 0; }

    /** repeat(count, tracks). count &lt;= 0 means auto-fill (-1) or auto-fit (-2). */
    record Repeat(int count, List<TrackSize> tracks) implements TrackEntry {
        public static final int AUTO_FILL = -1, AUTO_FIT = -2;
    }
}
