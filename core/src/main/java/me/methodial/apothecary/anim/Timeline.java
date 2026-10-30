package me.methodial.apothecary.anim;

import java.util.ArrayList;
import java.util.List;

/**
 * Composes tweens and callbacks on a time axis.
 * <pre>{@code
 * Timeline.sequence()
 *     .then(Tween.of(0, 1, .3f).onUpdate(a -> ...))
 *     .wait(0.1f)
 *     .with(Tween.of(...))      // starts together with the previous step
 *     .call(() -> ...)
 *     .start(animator);
 * }</pre>
 */
public final class Timeline implements Animator.Ticking {
    private record Entry(float at, Animator.Ticking item, float length) { }

    private final List<Entry> entries = new ArrayList<>();
    private final List<Animator.Ticking> running = new ArrayList<>();
    private float cursor;      // where the next "then" starts
    private float lastStart;   // where the previous step started ("with" aligns to it)
    private float elapsed;
    private int next;
    private boolean loop;
    private Runnable onComplete = () -> { };

    public static Timeline sequence() { return new Timeline(); }

    public Timeline then(Tween t) {
        entries.add(new Entry(cursor, t, t.duration()));
        lastStart = cursor;
        cursor += t.duration();
        return this;
    }

    /** Runs alongside the previous step. */
    public Timeline with(Tween t) {
        entries.add(new Entry(lastStart, t, t.duration()));
        cursor = Math.max(cursor, lastStart + t.duration());
        return this;
    }

    /** Starts at an absolute time from the timeline start. */
    public Timeline at(float seconds, Tween t) {
        entries.add(new Entry(seconds, t, t.duration()));
        cursor = Math.max(cursor, seconds + t.duration());
        return this;
    }

    public Timeline wait(float seconds) {
        cursor += seconds;
        lastStart = cursor;
        return this;
    }

    public Timeline call(Runnable r) {
        entries.add(new Entry(cursor, dt -> { r.run(); return false; }, 0));
        return this;
    }

    /** {@code count} copies of a tween built by {@code factory}, each offset by {@code stagger} seconds. */
    public Timeline stagger(int count, float stagger, java.util.function.IntFunction<Tween> factory) {
        float base = cursor, end = cursor;
        for (int i = 0; i < count; i++) {
            Tween t = factory.apply(i);
            entries.add(new Entry(base + i * stagger, t, t.duration()));
            end = Math.max(end, base + i * stagger + t.duration());
        }
        lastStart = base;
        cursor = end;
        return this;
    }

    public Timeline loop(boolean l) { this.loop = l; return this; }

    public Timeline onComplete(Runnable r) { this.onComplete = r; return this; }

    public float duration() { return cursor; }

    public Timeline start(Animator a) {
        entries.sort((x, y) -> Float.compare(x.at, y.at));
        a.add(this);
        return this;
    }

    @Override
    public boolean tick(float dt) {
        elapsed += dt;
        // Advance what was already running, then start entries whose time has come,
        // catching them up on the part of this frame that passed since their start.
        running.removeIf(t -> !t.tick(dt));
        while (next < entries.size() && entries.get(next).at <= elapsed) {
            Entry e = entries.get(next++);
            if (e.item.tick(elapsed - e.at)) running.add(e.item);
        }
        if (next >= entries.size() && running.isEmpty() && elapsed >= cursor) {
            if (loop) {
                elapsed = 0;
                next = 0;
                for (Entry e : entries) if (e.item instanceof Tween tw) tw.reset();
                return true;
            }
            onComplete.run();
            return false;
        }
        return true;
    }
}
