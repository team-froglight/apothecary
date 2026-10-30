package me.methodial.apothecary.anim;

/** Maps linear progress t in [0,1] to eased progress (may overshoot for back/elastic/spring). */
@FunctionalInterface
public interface Easing {
    float apply(float t);

    default Easing reversed() { return t -> 1 - apply(1 - t); }

    /** Runs this easing forward then backward within one cycle (0 → 1 → 0). */
    default Easing yoyo() { return t -> t < 0.5f ? apply(t * 2) : apply(2 - t * 2); }
}
