package me.methodial.apothecary.event;

/** A widget's value changed (checkbox, slider, text field, select, tabs...). */
public class ChangeEvent<T> extends UIEvent {
    private final T oldValue, value;

    public ChangeEvent(T oldValue, T value) {
        this.oldValue = oldValue;
        this.value = value;
    }

    public T oldValue() { return oldValue; }
    public T value() { return value; }
}
