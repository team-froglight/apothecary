package me.methodial.apothecary.anim;

import java.util.ArrayList;
import java.util.List;

/** Drives anything time-based. Each UI root owns one; {@link #tick(float)} is called once per frame. */
public final class Animator {
    /** Something advanced every frame; return false when finished to be removed. */
    @FunctionalInterface
    public interface Ticking {
        boolean tick(float dt);
    }

    private final List<Ticking> active = new ArrayList<>();
    private final List<Ticking> added = new ArrayList<>();
    private float time;

    public <T extends Ticking> T add(T t) {
        added.add(t);
        return t;
    }

    public void remove(Ticking t) {
        active.remove(t);
        added.remove(t);
    }

    /** Seconds since this animator started. */
    public float time() { return time; }

    public boolean isIdle() { return active.isEmpty() && added.isEmpty(); }

    public void tick(float dt) {
        time += dt;
        if (!added.isEmpty()) {
            active.addAll(added);
            added.clear();
        }
        for (int i = 0; i < active.size(); i++) {
            Ticking t = active.get(i);
            boolean keep;
            try {
                keep = t.tick(dt);
            } catch (RuntimeException e) {
                keep = false;
                e.printStackTrace();
            }
            if (!keep) {
                active.remove(i--);
            }
        }
    }

    /** Runs {@code r} after {@code seconds}. */
    public Ticking schedule(float seconds, Runnable r) {
        float[] left = {seconds};
        return add(dt -> {
            left[0] -= dt;
            if (left[0] <= 0) { r.run(); return false; }
            return true;
        });
    }

    /** Runs {@code r} every {@code seconds} until cancelled with {@link #remove}. */
    public Ticking every(float seconds, Runnable r) {
        float[] acc = {0};
        return add(dt -> {
            acc[0] += dt;
            while (acc[0] >= seconds) { acc[0] -= seconds; r.run(); }
            return true;
        });
    }
}
