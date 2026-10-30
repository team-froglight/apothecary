package me.methodial.apothecary.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An observable value. Widgets can bind to signals so state lives outside the UI tree:
 * <pre>{@code
 * Signal<Float> volume = Signal.of(0.5f);
 * new Slider(0, 1).bind(volume);
 * new Text().bind(volume.map(v -> Math.round(v * 100) + "%"));
 * }</pre>
 */
public class Signal<T> {
    /** A cancellable subscription. */
    @FunctionalInterface
    public interface Subscription { void cancel(); }

    private T value;
    private final List<BiConsumer<T, T>> listeners = new ArrayList<>();

    public Signal(T initial) { this.value = initial; }

    public static <T> Signal<T> of(T initial) { return new Signal<>(initial); }

    public T get() { return value; }

    public void set(T v) {
        if (Objects.equals(v, value)) return;
        T old = value;
        value = v;
        for (BiConsumer<T, T> l : new ArrayList<>(listeners)) l.accept(old, v);
    }

    public void update(Function<T, T> f) { set(f.apply(value)); }

    /** Calls {@code l(old, new)} on every change. */
    public Subscription listen(BiConsumer<T, T> l) {
        listeners.add(l);
        return () -> listeners.remove(l);
    }

    /** Calls {@code c} now and on every change. */
    public Subscription observe(Consumer<T> c) {
        c.accept(value);
        return listen((o, n) -> c.accept(n));
    }

    /** A read-only derived signal that tracks this one. */
    public <R> Signal<R> map(Function<T, R> f) {
        Signal<R> out = new Signal<>(f.apply(value));
        listen((o, n) -> out.set(f.apply(n)));
        return out;
    }

    /** Combines two signals into one derived value. */
    public static <A, B, R> Signal<R> combine(Signal<A> a, Signal<B> b, java.util.function.BiFunction<A, B, R> f) {
        Signal<R> out = new Signal<>(f.apply(a.get(), b.get()));
        a.listen((o, n) -> out.set(f.apply(n, b.get())));
        b.listen((o, n) -> out.set(f.apply(a.get(), n)));
        return out;
    }
}
