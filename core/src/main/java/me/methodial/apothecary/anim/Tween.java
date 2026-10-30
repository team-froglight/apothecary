package me.methodial.apothecary.anim;

import java.util.function.Consumer;

/**
 * A float animation from A to B. Start it on an {@link Animator}:
 * <pre>{@code
 * Tween.of(0, 1, 0.4f).easing(Easings.OUT_BACK).onUpdate(v -> node.style().opacity(v)).start(root.animator());
 * }</pre>
 * Supports delay, repeat (with yoyo) and completion callbacks. Also usable inside {@link Timeline}.
 */
public class Tween implements Animator.Ticking {
    private final float from, to, duration;
    private Easing easing = Easings.EASE_OUT;
    private float delay;
    private float elapsed;
    private int repeat;          // extra plays; -1 = forever
    private boolean yoyo;
    private boolean reversedPass;
    private Consumer<Float> onUpdate = v -> { };
    private Runnable onComplete = () -> { };
    private boolean cancelled, finished;

    public Tween(float from, float to, float duration) {
        this.from = from;
        this.to = to;
        this.duration = Math.max(1e-4f, duration);
    }

    public static Tween of(float from, float to, float seconds) { return new Tween(from, to, seconds); }

    public Tween easing(Easing e) { this.easing = e; return this; }
    public Tween delay(float seconds) { this.delay = seconds; return this; }
    public Tween repeat(int times) { this.repeat = times; return this; }
    public Tween forever() { this.repeat = -1; return this; }
    public Tween yoyo(boolean y) { this.yoyo = y; return this; }
    public Tween onUpdate(Consumer<Float> c) { this.onUpdate = c; return this; }
    public Tween onComplete(Runnable r) { this.onComplete = r; return this; }

    public Tween start(Animator a) { a.add(this); return this; }

    public void cancel() { cancelled = true; }
    public boolean isFinished() { return finished; }
    public float duration() { return delay + duration * (repeat < 0 ? Float.POSITIVE_INFINITY : repeat + 1); }

    /** Value at the current time. */
    public float value() {
        float t = Math.max(0, Math.min(1, (elapsed - delay) / duration));
        if (reversedPass) t = 1 - t;
        return from + (to - from) * easing.apply(t);
    }

    @Override
    public boolean tick(float dt) {
        if (cancelled) return false;
        elapsed += dt;
        if (elapsed < delay) return true;
        if (elapsed - delay >= duration) {
            if (repeat != 0) {
                if (repeat > 0) repeat--;
                elapsed -= duration;
                if (yoyo) reversedPass = !reversedPass;
                onUpdate.accept(value());
                return true;
            }
            elapsed = delay + duration;
            onUpdate.accept(value());
            finished = true;
            onComplete.run();
            return false;
        }
        onUpdate.accept(value());
        return true;
    }

    /** Restarts from the beginning (for timelines). */
    public void reset() {
        elapsed = 0;
        reversedPass = false;
        finished = false;
        cancelled = false;
    }
}
