package me.methodial.apothecary.theme;

import java.util.function.Function;

/**
 * A named, typed design value (color, radius, shadow, font...). Themes assign values to tokens;
 * styles reference tokens, and re-resolve whenever a theme changes (with transitions).
 * <pre>{@code
 * Token<Color> BRAND = Token.of("brand", Color.hex("#ff5c8a"));
 * Token<Color> BRAND_SOFT = Token.derived("brand-soft", t -> t.get(BRAND).withAlpha(0.2f));
 * }</pre>
 */
public final class Token<T> implements Ref<T> {
    private final String name;
    private final T fallback;
    private final Function<TokenScope, T> derive;

    private Token(String name, T fallback, Function<TokenScope, T> derive) {
        this.name = name;
        this.fallback = fallback;
        this.derive = derive;
    }

    /** A token with a default value. */
    public static <T> Token<T> of(String name, T fallback) { return new Token<>(name, fallback, null); }

    /** A token whose default is computed from other tokens (until a theme sets it explicitly). */
    public static <T> Token<T> derived(String name, Function<TokenScope, T> derive) { return new Token<>(name, null, derive); }

    public String name() { return name; }

    /** Value when no theme or scope sets this token. */
    public T defaultValue(TokenScope scope) { return derive != null ? derive.apply(scope) : fallback; }

    @Override public T resolve(TokenScope tokens) { return tokens.get(this); }

    @Override public String toString() { return "token(" + name + ")"; }
}
