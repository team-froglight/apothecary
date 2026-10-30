package me.methodial.apothecary.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A set of token values. Apply to a whole UI ({@code ui.theme(theme)}) or to any subtree
 * ({@code node.theme(theme)}). Values may be constants or {@link Ref}s computed from other tokens.
 * <pre>{@code
 * Theme ocean = Theme.of("Ocean").set(ACCENT, Color.hex("#2ec4d6")).set(BG, Color.hex("#081218"));
 * Theme compact = Theme.of("Compact").set(RADIUS, 2f);
 * ui.theme(ocean.with(compact));
 * }</pre>
 */
public final class Theme {
    private final String name;
    private final Map<Token<?>, Object> values = new LinkedHashMap<>();
    /**
     * Inherited styles re-declared where the theme is applied, so text inside a themed subtree resolves
     * against this theme's tokens instead of inheriting the outer theme's already-resolved colors.
     */
    private java.util.function.Consumer<me.methodial.apothecary.style.Style> scopeStyle =
            s -> s.fg(Tokens.TEXT).font(Tokens.FONT).scrollbar(3, Tokens.SCROLLBAR);

    private Theme(String name) { this.name = name; }

    /** Replaces the inherited styles re-applied at the theme's boundary (for custom token sets). */
    public Theme scopeStyle(java.util.function.Consumer<me.methodial.apothecary.style.Style> s) {
        this.scopeStyle = s;
        return this;
    }

    public java.util.function.Consumer<me.methodial.apothecary.style.Style> scopeStyle() { return scopeStyle; }

    public static Theme of(String name) { return new Theme(name); }

    public String name() { return name; }

    public <T> Theme set(Token<T> token, T value) {
        values.put(token, value);
        return this;
    }

    /** Sets a token to a value computed from other tokens. */
    public <T> Theme set(Token<T> token, Ref<? extends T> value) {
        values.put(token, value);
        return this;
    }

    /** A copy of this theme with {@code other}'s values layered on top. */
    public Theme with(Theme other) {
        Theme t = new Theme(name + "+" + other.name);
        t.values.putAll(values);
        t.values.putAll(other.values);
        t.scopeStyle = scopeStyle;
        return t;
    }

    public Map<Token<?>, Object> values() { return Collections.unmodifiableMap(values); }

    @Override public String toString() { return "Theme(" + name + ", " + values.size() + " tokens)"; }
}
