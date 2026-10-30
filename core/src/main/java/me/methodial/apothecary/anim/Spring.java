package me.methodial.apothecary.anim;

import java.util.function.Consumer;

/**
 * A physically simulated value that chases a target (semi-implicit Euler, sub-stepped).
 * Retargeting keeps velocity, so motion stays continuous when interrupted — ideal for drag-release,
 * hover follow and anything interactive.
 * <pre>{@code
 * Spring x = new Spring(0).stiffness(220).damping(14).onUpdate(v -> ...);
 * root.animator().add(x);
 * x.target(100);
 * }</pre>
 */
public class Spring implements Animator.Ticking {
    private float value, velocity, target;
    private float stiffness = 170, damping = 26, mass = 1;
    private float precision = 0.01f;
    private Consumer<Float> onUpdate = v -> { };
    private boolean resting = true;
    private boolean persistent = true;

    public Spring(float initial) {
        this.value = initial;
        this.target = initial;
    }

    public Spring stiffness(float k) { this.stiffness = k; return this; }
    public Spring damping(float c) { this.damping = c; return this; }
    public Spring mass(float m) { this.mass = m; return this; }
    public Spring precision(float p) { this.precision = p; return this; }
    public Spring onUpdate(Consumer<Float> c) { this.onUpdate = c; return this; }
    /** When false, the spring removes itself from the animator once at rest. */
    public Spring persistent(boolean p) { this.persistent = p; return this; }

    public float value() { return value; }
    public float velocity() { return velocity; }
    public float target() { return target; }
    public boolean isResting() { return resting; }

    public Spring target(float t) {
        this.target = t;
        resting = false;
        return this;
    }

    /** Jumps to {@code v} with no motion. */
    public Spring snap(float v) {
        value = target = v;
        velocity = 0;
        resting = true;
        onUpdate.accept(value);
        return this;
    }

    /** Places the spring at {@code v} moving at {@code vel} (e.g. while being dragged), keeping it active. */
    public Spring set(float v, float vel) {
        value = v;
        velocity = vel;
        resting = false;
        onUpdate.accept(value);
        return this;
    }

    /** Adds an impulse (px/s). */
    public Spring kick(float v) {
        velocity += v;
        resting = false;
        return this;
    }

    @Override
    public boolean tick(float dt) {
        if (resting) return persistent;
        dt = Math.min(dt, 0.1f);
        int steps = Math.max(1, (int) Math.ceil(dt / (1 / 240f)));
        float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            float force = -stiffness * (value - target) - damping * velocity;
            velocity += force / mass * h;
            value += velocity * h;
        }
        if (Math.abs(velocity) < precision * 10 && Math.abs(value - target) < precision) {
            value = target;
            velocity = 0;
            resting = true;
        }
        onUpdate.accept(value);
        return persistent || !resting;
    }
}
