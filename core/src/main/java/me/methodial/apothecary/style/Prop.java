package me.methodial.apothecary.style;

/**
 * A typed style property. Built-ins live in {@link Props}; libraries can add their own with
 * {@link Props#register} and read them back with {@code node.get(prop)}.
 *
 * @param <T> value type
 */
public final class Prop<T> {
    /** Linear interpolation for transitions/animations; null means the property changes discretely. */
    @FunctionalInterface
    public interface Lerp<T> { T lerp(T a, T b, float t); }

    /** What a change to this property invalidates. */
    public enum Affects { PAINT, LAYOUT }

    private final String name;
    private final int id;
    private final T initial;
    private final boolean inherited;
    private final Affects affects;
    private final Lerp<T> lerp;

    Prop(String name, int id, T initial, boolean inherited, Affects affects, Lerp<T> lerp) {
        this.name = name;
        this.id = id;
        this.initial = initial;
        this.inherited = inherited;
        this.affects = affects;
        this.lerp = lerp;
    }

    public String name() { return name; }
    public int id() { return id; }
    public T initial() { return initial; }
    public boolean inherited() { return inherited; }
    public Affects affects() { return affects; }
    public boolean animatable() { return lerp != null; }

    @SuppressWarnings("unchecked")
    public T interpolate(Object a, Object b, float t) {
        if (lerp == null) return (T) (t < 0.5f ? a : b);
        return lerp.lerp((T) a, (T) b, t);
    }

    @Override public String toString() { return name; }
}
