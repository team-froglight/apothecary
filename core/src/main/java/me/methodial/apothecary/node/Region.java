package me.methodial.apothecary.node;

import me.methodial.apothecary.widget.ValueWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A part of the tree rebuilt from code whenever its state changes, the retained-mode counterpart of an
 * immediate-mode "recompose". The builder fills the region from scratch; {@link #invalidate} schedules
 * one rebuild for the next frame (however many times it is called), {@link #refresh} rebuilds now.
 * <p>
 * Nodes that must survive rebuilds (a text field being typed in, a scroll container) are created
 * through {@link #keep}: the same instance comes back on every rebuild, keeping its text, caret, focus
 * and scroll position.
 * <pre>{@code
 * Region list = new Region(r -> {
 *     r.add(r.keep("search", () -> new TextField().placeholder("Search")));
 *     for (Item i : filtered()) r.add(row(text(i.name())));
 * });
 * // later, after the data changes:
 * list.invalidate();
 * }</pre>
 * Type selector: {@code region}.
 */
public class Region extends Node {
    private final Consumer<Region> builder;
    private Map<Object, Node> kept = new HashMap<>();
    private Map<Object, Node> building;
    private boolean invalid;

    public Region(Consumer<Region> builder) {
        super("region");
        this.builder = builder;
        refresh();
    }

    /** Rebuilds now. */
    public Region refresh() {
        invalid = false;
        building = new HashMap<>();
        try {
            clear();
            builder.accept(this);
        } finally {
            kept = building;
            building = null;
        }
        return this;
    }

    /** Rebuilds once, before the next layout. */
    public Region invalidate() {
        invalid = true;
        return this;
    }

    public boolean isInvalid() { return invalid; }

    /**
     * The node kept under {@code key}: created on first use, then the same instance on every rebuild
     * that asks for it. Nodes no rebuild asked for are forgotten.
     */
    @SuppressWarnings("unchecked")
    public <N extends Node> N keep(Object key, Supplier<N> create) {
        Node n = kept.get(key);
        if (n == null && building != null) n = building.get(key);
        if (n == null) n = create.get();
        else n.removeFromParent();
        if (building != null) building.put(key, n);
        else kept.put(key, n);
        return (N) n;
    }

    /**
     * A kept input controlled by the app: it shows {@code value} (set quietly when it differs) and reports
     * edits to {@code onChange}, which is replaced on every rebuild so it always sees the current state.
     * The same instance survives rebuilds, so typing, caret and focus are never interrupted.
     * <pre>{@code r.add(r.bound("name", TextField::new, draft.name, v -> { draft.name = v; r.invalidate(); }));}</pre>
     */
    @SuppressWarnings("unchecked")
    public <T, N extends ValueWidget<T>> N bound(Object key, Supplier<N> create, T value, Consumer<T> onChange) {
        N n = keep(key, () -> {
            N created = create.get();
            created.onChange(v -> {
                Object h = created.userDataFor(Region.class);
                if (h instanceof Consumer<?> c) ((Consumer<T>) c).accept(v);
            });
            return created;
        });
        n.userDataFor(Region.class, onChange);
        if (!java.util.Objects.equals(n.value(), value)) n.setQuietly(value);
        return n;
    }

    @Override
    protected void onFrame(float dt) {
        if (invalid) refresh();
    }
}
