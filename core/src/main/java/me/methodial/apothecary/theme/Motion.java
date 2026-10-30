package me.methodial.apothecary.theme;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.style.Keyframes;

/** Shared easing curves, durations and the library's keyframe animations. */
public final class Motion {
    private Motion() { }

    /** Snappy ease-out with a soft landing (the default for UI motion). */
    public static final Easing OUT = Easings.cubicBezier(0.2f, 0.8f, 0.2f, 1f);
    /** Overshooting "pop" for toggles and knobs. */
    public static final Easing POP = Easings.cubicBezier(0.3f, 1.6f, 0.5f, 1f);
    /** Gentler overshoot for larger surfaces. */
    public static final Easing BOUNCE = Easings.cubicBezier(0.3f, 1.4f, 0.5f, 1f);
    /** Dialog / toast entrance. */
    public static final Easing ENTER = Easings.cubicBezier(0.2f, 1.3f, 0.4f, 1f);

    public static final float FAST = 0.12f, NORMAL = 0.2f, SLOW = 0.32f;

    public static final Keyframes FADE_IN = Keyframes.of("fade-in").from(s -> s.opacity(0)).to(s -> s.opacity(1));
    public static final Keyframes POP_IN = Keyframes.of("pop-in").from(s -> s.opacity(0).translate(0, -4, 0.94f)).to(s -> s.opacity(1).untransformed());
    public static final Keyframes RISE_IN = Keyframes.of("rise-in").from(s -> s.opacity(0).translateY(10)).to(s -> s.opacity(1).untransformed());
    public static final Keyframes SLIDE_FROM_RIGHT = Keyframes.of("slide-from-right").from(s -> s.opacity(0).translateX(12)).to(s -> s.opacity(1).untransformed());
    public static final Keyframes SLIDE_FROM_LEFT = Keyframes.of("slide-from-left").from(s -> s.opacity(0).translateX(-12)).to(s -> s.opacity(1).untransformed());
    public static final Keyframes DIALOG_IN = Keyframes.of("dialog-in").from(s -> s.opacity(0).translate(0, 8, 0.88f)).to(s -> s.opacity(1).untransformed());
    public static final Keyframes TOOLTIP_IN = Keyframes.of("tooltip-in").from(s -> s.opacity(0).translate(0, 2, 0.96f)).to(s -> s.opacity(1).untransformed());
    public static final Keyframes TOAST_IN = Keyframes.of("toast-in").from(s -> s.opacity(0).translate(40, 0, 0.95f)).to(s -> s.opacity(1).untransformed());
    /** A bar shrinking from full to empty along x (toast timers). */
    public static final Keyframes DRAIN = Keyframes.of("drain").from(s -> s.scale(1, 1)).to(s -> s.scale(0, 1));
    /** Indeterminate progress stripe. */
    public static final Keyframes SWEEP = Keyframes.of("indeterminate").from(s -> s.translateRel(-1, 0)).to(s -> s.translateRel(3, 0));

    public static final Keyframes PULSE = Keyframes.of("pulse")
            .from(s -> s.scale(1).opacity(1)).at(0.5f, s -> s.scale(1.35f).opacity(0.6f)).to(s -> s.scale(1).opacity(1));
    public static final Keyframes SPIN = Keyframes.of("spin").from(s -> s.rotate(0)).to(s -> s.rotate(360));
    public static final Keyframes BOUNCE_Y = Keyframes.of("bounce").from(s -> s.translateY(4)).to(s -> s.translateY(-8));
    public static final Keyframes FLOAT = Keyframes.of("float").from(s -> s.translateRotate(0, -3, -4)).to(s -> s.translateRotate(0, 3, 4));
}
