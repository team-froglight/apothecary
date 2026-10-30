package me.methodial.apothecary.sheet;

import me.methodial.apothecary.style.Style;

import java.util.function.Consumer;

/**
 * Nested rule builder for one selector (created by {@link Sheet#on}). Every method adds rules and
 * returns {@code this}, so a component's whole look reads as one chain:
 * <pre>{@code
 * sheet.on("switch", r -> r
 *     .style(s -> s.row().gap(5).pointer())
 *     .disabled(s -> s.opacity(.45f))
 *     .part("switch-track", t -> t.style(s -> s.size(20, 11).round().bg(SURFACE_3)))
 *     .part("switch-knob", k -> k.style(s -> s.size(7).round().bg(TEXT_DIM)))
 *     .when(CHECKED, c -> c
 *         .part("switch-track", t -> t.style(s -> s.bg(ACCENT)))
 *         .part("switch-knob", k -> k.style(s -> s.translateX(9).bg(Color.WHITE)))));
 * }</pre>
 */
public final class Rules {
    private final Sheet sheet;
    private final Selector selector;

    Rules(Sheet sheet, Selector selector) {
        this.sheet = sheet;
        this.selector = selector;
    }

    public Selector selector() { return selector; }

    /** Style for the scope itself. */
    public Rules style(Consumer<Style> body) {
        sheet.rule(selector, body);
        return this;
    }

    // ------------------------------------------------------------------ states of this element

    /** Style while this element has {@code state}. */
    public Rules state(String state, Consumer<Style> body) {
        sheet.rule(selector.state(state), body);
        return this;
    }

    public Rules hover(Consumer<Style> body) { return state(Selector.HOVER, body); }
    public Rules active(Consumer<Style> body) { return state(Selector.ACTIVE, body); }
    public Rules focus(Consumer<Style> body) { return state(Selector.FOCUS, body); }
    /** Keyboard focus only (Tab), the usual place for focus rings. */
    public Rules focusVisible(Consumer<Style> body) { return state(Selector.FOCUS_VISIBLE, body); }
    public Rules disabled(Consumer<Style> body) { return state(Selector.DISABLED, body); }
    public Rules checked(Consumer<Style> body) { return state(Selector.CHECKED, body); }
    public Rules selected(Consumer<Style> body) { return state(Selector.SELECTED, body); }
    public Rules open(Consumer<Style> body) { return state(Selector.OPEN, body); }
    /** Style while being removed by {@code UIRoot.removeAnimated} (exit animations). */
    public Rules closing(Consumer<Style> body) { return state(Selector.CLOSING, body); }

    /** Nested rules that apply while this element has {@code state} (parts can react to the parent's state). */
    public Rules when(String state, Consumer<Rules> body) {
        body.accept(new Rules(sheet, selector.state(state)));
        return this;
    }

    // ------------------------------------------------------------------ variants & structure

    /** Elements of this scope that also have class {@code cls}: buttons' "primary", "ghost"... */
    public Rules variant(String cls, Consumer<Rules> body) {
        body.accept(new Rules(sheet, selector.withCls(cls)));
        return this;
    }

    /** Shorthand for a variant with only a base style. */
    public Rules variantStyle(String cls, Consumer<Style> body) {
        sheet.rule(selector.withCls(cls), body);
        return this;
    }

    /** Descendants of a type ({@code part("checkbox-box", ...)}). */
    public Rules part(String type, Consumer<Rules> body) { return find(Selector.is(type), body); }

    /** Descendants with a class. */
    public Rules partCls(String cls, Consumer<Rules> body) { return find(Selector.cls(cls), body); }

    /** Shorthand: descendants of a type with only a base style. */
    public Rules partStyle(String type, Consumer<Style> body) {
        sheet.rule(selector.find(Selector.is(type)), body);
        return this;
    }

    /** Descendants matching any selector. */
    public Rules find(Selector descendant, Consumer<Rules> body) {
        body.accept(new Rules(sheet, selector.find(descendant)));
        return this;
    }

    /** Direct children matching a selector. */
    public Rules child(Selector child, Consumer<Rules> body) {
        body.accept(new Rules(sheet, selector.child(child)));
        return this;
    }

    /** Refine this scope with any selector change, e.g. {@code r.refine(sel -> sel.firstChild(), ...)}. */
    public Rules refine(java.util.function.UnaryOperator<Selector> f, Consumer<Rules> body) {
        body.accept(new Rules(sheet, f.apply(selector)));
        return this;
    }
}
