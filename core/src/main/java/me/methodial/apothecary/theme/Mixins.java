package me.methodial.apothecary.theme;

import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.style.Tr;

import java.util.function.Consumer;

import static me.methodial.apothecary.theme.Tokens.*;

/**
 * Reusable style chunks. A mixin is just a {@code Consumer<Style>}: write your own the same way.
 * <pre>{@code s.apply(Mixins.CARD, Mixins.stack(6)).pad(10)}</pre>
 */
public final class Mixins {
    private Mixins() { }

    /** Raised surface with a border and large radius. */
    public static final Consumer<Style> CARD = s -> s.bg(SURFACE_2).border(1, BORDER).radius(RADIUS_LG).pad(8).col().gap(5);
    /** Floating panel (popovers, menus). */
    public static final Consumer<Style> PANEL = s -> s.bg(SURFACE_2).border(1, BORDER_STRONG).radius(RADIUS_LG).shadow(SHADOW_LG);
    /** Sunken well for previews and stages. */
    public static final Consumer<Style> WELL = s -> s.bg(Colors.alpha(SHADOW_COLOR, 0.25f)).border(1, BORDER_STRONG).radius(RADIUS_LG);
    /** Pointer cursor + color/transform/shadow transitions. */
    public static final Consumer<Style> INTERACTIVE = s -> s.pointer().transition(Tr.interactive(Motion.FAST));
    /** Keyboard focus ring (use inside a focusVisible rule). */
    public static final Consumer<Style> FOCUS_RING = s -> s.outline(1, ACCENT, 1);
    /** Single line with an ellipsis that may shrink in a row. */
    public static final Consumer<Style> TRUNCATE = s -> s.ellipsis().shrink(1).minW(0);
    /** Fully rounded small label. */
    public static final Consumer<Style> PILL = s -> s.round().padX(6);
    /** Centers content in a flex box. */
    public static final Consumer<Style> CENTERED = s -> s.flex().center();

    /** Vertical stack with a gap. */
    public static Consumer<Style> stack(float gap) { return s -> s.col().gap(gap); }

    /** Horizontal row with a gap (items vertically centered). */
    public static Consumer<Style> inline(float gap) { return s -> s.row().gap(gap); }

    /** Lifts on hover (put in a hover rule): translate up + bigger shadow. */
    public static final Consumer<Style> LIFTED = s -> s.translateY(-2).shadow(SHADOW).borderColor(BORDER_STRONG);
}
