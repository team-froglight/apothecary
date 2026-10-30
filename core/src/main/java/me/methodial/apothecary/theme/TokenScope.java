package me.methodial.apothecary.theme;

/** Looks up design-token values visible to an element (its own, its ancestors', the theme's, or defaults). */
public interface TokenScope {
    <T> T get(Token<T> token);

    /** Scope with no overrides: every token resolves to its derived or fallback value. */
    TokenScope DEFAULTS = new TokenScope() {
        @Override public <T> T get(Token<T> token) { return token.defaultValue(this); }
    };
}
