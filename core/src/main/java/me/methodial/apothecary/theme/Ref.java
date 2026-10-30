package me.methodial.apothecary.theme;

import java.util.function.Function;

/**
 * A style value computed from design tokens when the element is styled. Any lambda works:
 * <pre>{@code s.bg(t -> Paint.linear(180, t.get(ACCENT_HOVER), t.get(ACCENT)))}</pre>
 * Tokens are Refs themselves, so {@code s.fg(TEXT_DIM)} just works.
 */
@FunctionalInterface
public interface Ref<T> {
    T resolve(TokenScope tokens);

    default <R> Ref<R> map(Function<? super T, ? extends R> f) { return t -> f.apply(resolve(t)); }

    static <T> Ref<T> of(T constant) { return t -> constant; }
}
