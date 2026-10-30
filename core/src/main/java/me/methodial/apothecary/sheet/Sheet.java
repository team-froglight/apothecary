package me.methodial.apothecary.sheet;

import me.methodial.apothecary.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A set of style rules written in code. Rules apply to every element their {@link Selector} matches,
 * cascade by specificity, and react to state changes (hover, focus...) with transitions.
 * <pre>{@code
 * Sheet sheet = new Sheet("my-mod")
 *     .on("chip", r -> r
 *         .style(s -> s.row().gap(3).pad(2, 6).round().bg(SURFACE_3).fg(TEXT_DIM))
 *         .hover(s -> s.fg(TEXT))
 *         .variant("active", v -> v.style(s -> s.bg(ACCENT).fg(ACCENT_TEXT)))
 *         .part("icon", p -> p.style(s -> s.fontSize(8))));
 * ui.addSheet(sheet);
 * }</pre>
 */
public final class Sheet {
    public record Rule(Selector selector, Style style, int order, Media media) { }

    private final String name;
    private final List<Rule> rules = new ArrayList<>();
    private Media currentMedia;
    private int version;

    public Sheet(String name) { this.name = name; }

    public String name() { return name; }
    public List<Rule> rules() { return Collections.unmodifiableList(rules); }
    /** Bumped on every change so live UIs restyle. */
    public int version() { return version; }

    /** A single rule. */
    public Sheet rule(Selector selector, Consumer<Style> body) {
        Style s = new Style();
        body.accept(s);
        rules.add(new Rule(selector, s, rules.size(), currentMedia));
        version++;
        return this;
    }

    /** A rule scope for elements of {@code type}, with nested states, variants and parts. */
    public Sheet on(String type, Consumer<Rules> body) { return on(Selector.is(type), body); }

    /** A rule scope for any selector. */
    public Sheet on(Selector selector, Consumer<Rules> body) {
        body.accept(new Rules(this, selector));
        return this;
    }

    /** Rules for elements with a class: {@code sheet.onCls("card", r -> ...)}. */
    public Sheet onCls(String cls, Consumer<Rules> body) { return on(Selector.cls(cls), body); }

    /** Quick single rule for a type. */
    public Sheet style(String type, Consumer<Style> body) { return rule(Selector.is(type), body); }

    /** Rules that only apply while the root's size matches. */
    public Sheet media(Media media, Consumer<Sheet> body) {
        Media outer = currentMedia;
        currentMedia = outer == null ? media : outer.and(media);
        body.accept(this);
        currentMedia = outer;
        return this;
    }

    /** Appends every rule of another sheet (they keep their relative order). */
    public Sheet include(Sheet other) {
        for (Rule r : other.rules) rules.add(new Rule(r.selector(), r.style(), rules.size(), r.media()));
        version++;
        return this;
    }

    /** Replaces this sheet's rules (rebuild a theme at runtime; open UIs restyle). */
    public void replaceWith(Sheet o) {
        rules.clear();
        include(o);
    }

    public void clear() {
        rules.clear();
        version++;
    }

    @Override public String toString() { return "Sheet(" + name + ", " + rules.size() + " rules)"; }
}
