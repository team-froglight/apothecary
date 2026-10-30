package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.ChangeEvent;
import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.state.Signal;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base for widgets holding a value (checkbox, slider, text field...). Fires {@link EventType#CHANGE}
 * and keeps an optional {@link Signal} in sync in both directions.
 */
public abstract class ValueWidget<T> extends Node {
    protected T value;
    private Signal<T> bound;
    private Signal.Subscription sub;
    private boolean syncing;

    protected ValueWidget(String type, T initial) {
        super(type);
        this.value = initial;
        on(EventType.MOUNT, e -> subscribe());
        on(EventType.UNMOUNT, e -> unsubscribe());
    }

    public T value() { return value; }

    /** Sets the value without firing change listeners (controlled inputs: the app owns the value). */
    public ValueWidget<T> setQuietly(T v) {
        setValue(v, false);
        return this;
    }

    /** Sets the value (fires change listeners when it differs). */
    public ValueWidget<T> value(T v) {
        setValue(v, true);
        return this;
    }

    protected void setValue(T v, boolean notify) {
        v = sanitize(v);
        if (Objects.equals(v, value)) return;
        T old = value;
        value = v;
        valueChanged(old, v);
        if (bound != null && !syncing) {
            syncing = true;
            bound.set(v);
            syncing = false;
        }
        if (notify) emit(EventType.CHANGE, new ChangeEvent<>(old, v));
    }

    /** Clamp/normalize incoming values. */
    protected T sanitize(T v) { return v; }

    /** Update visuals for a new value. */
    protected abstract void valueChanged(T old, T now);

    @SuppressWarnings("unchecked")
    public ValueWidget<T> onChange(Consumer<T> c) {
        on(EventType.CHANGE, e -> { if (e.target() == this) c.accept(((ChangeEvent<T>) e).value()); });
        return this;
    }

    /** Two-way binding: the widget shows the signal's value and writes user edits back. */
    public ValueWidget<T> bind(Signal<T> s) {
        unsubscribe();
        bound = s;
        setValue(s.get(), false);
        if (isAttached()) subscribe();
        return this;
    }

    private void subscribe() {
        if (bound == null || sub != null) return;
        sub = bound.observe(v -> {
            if (syncing) return;
            syncing = true;
            setValue(v, true);
            syncing = false;
        });
    }

    private void unsubscribe() {
        if (sub != null) { sub.cancel(); sub = null; }
    }

    protected void sound(String id) {
        if (root() != null) root().platform().playSound(id);
    }
}
