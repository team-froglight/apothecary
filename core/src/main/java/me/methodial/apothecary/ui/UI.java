package me.methodial.apothecary.ui;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.style.TrackEntry;
import me.methodial.apothecary.theme.Theme;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.theme.TypeScale;
import me.methodial.apothecary.theme.TypeStyle;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Icon;
import me.methodial.apothecary.widget.Text;

import java.util.function.Consumer;

/**
 * Terse, declarative tree building. {@code import static me.methodial.apothecary.ui.UI.*;}
 * <pre>{@code
 * col(s(x -> x.gap(6).pad(10)).card(),
 *     h2("Inventory"),
 *     muted("Drag items to reorder."),
 *     row(gap(4),
 *         button("check", "Save", this::save).primary(),
 *         button("Cancel", this::close).ghost()));
 * }</pre>
 * Arguments can be child {@link Node}s, {@link Part}s ({@link #s}, {@link #hover}, {@link #click},
 * {@link #tip}...), class-name strings ({@code "card hoverable"}), or iterables/arrays of those.
 */
public final class UI {
    private UI() { }

    /** Something that configures a node when building. */
    @FunctionalInterface
    public interface Part {
        void applyTo(Node n);

        /** Chains another part. */
        default Part and(Part next) { return n -> { applyTo(n); next.applyTo(n); }; }
    }

    // ------------------------------------------------------------------ containers

    public static Node box(Object... parts) { return build(new Node(), parts); }

    /** A node of a custom type (styled by sheet rules for that type): {@code el("card-header", ...)}. */
    public static Node el(String type, Object... parts) { return build(new Node(type), parts); }

    /** Flex row, items vertically centered. */
    public static Node row(Object... parts) { return build(new Node().style(Style::row), parts); }

    /** Flex column. */
    public static Node col(Object... parts) { return build(new Node().style(Style::col), parts); }

    /** Column with a gap. */
    public static Node stack(float gap, Object... parts) { return build(new Node().style(s -> s.col().gap(gap)), parts); }

    /** Row with a gap. */
    public static Node line(float gap, Object... parts) { return build(new Node().style(s -> s.row().gap(gap)), parts); }

    /** Grid with {@code n} equal columns. */
    public static Node grid(int columns, Object... parts) { return build(new Node().style(s -> s.cols(columns)), parts); }

    /** Grid with explicit tracks: {@code grid(tracks(px(60), fr(1)), ...)}. */
    public static Node grid(TrackEntry[] columns, Object... parts) { return build(new Node().style(s -> s.cols(columns)), parts); }

    public static TrackEntry[] tracks(TrackEntry... t) { return t; }

    /** Pushes siblings apart in a row/column. */
    public static Node spacer() { return new Node("spacer").style(s -> s.grow(1)); }

    public static Node divider() { return new Node("divider"); }

    // ------------------------------------------------------------------ text

    public static Text text(String s) { return new Text(s); }

    public static Text text(String s, TypeStyle type) { return new Text(s).style(x -> x.type(type)); }

    public static Text h1(String s) { return text(s, TypeScale.H1); }
    public static Text h2(String s) { return text(s, TypeScale.H2); }
    public static Text h3(String s) { return text(s, TypeScale.H3); }
    public static Text title(String s) { return text(s, TypeScale.TITLE); }
    public static Text muted(String s) { return new Text(s).cls("muted"); }
    public static Text faint(String s) { return new Text(s).cls("faint"); }
    public static Text caps(String s) { return text(s, TypeScale.CAPS); }
    public static Text small(String s) { return text(s, TypeScale.SMALL); }
    /** Inline code chip. */
    public static Text code(String s) { return (Text) new Text(s).type("code"); }

    public static Icon icon(String name) { return new Icon(name); }

    public static Icon icon(String name, float size) { return (Icon) new Icon(name).style(s -> s.fontSize(size)); }

    // ------------------------------------------------------------------ widgets

    public static Button button(String label, Runnable action) { return new Button(label).action(action); }

    public static Button button(String icon, String label, Runnable action) { return new Button(icon, label).action(action); }

    public static Button iconButton(String icon, String tooltip, Runnable action) { return Button.icon(icon, tooltip).action(action); }

    // ------------------------------------------------------------------ parts

    /** Inline style: {@code s(x -> x.pad(8).bg(SURFACE))}. */
    public static Part s(Consumer<Style> edit) { return n -> n.style(edit); }

    /** Same as {@link #s}, for readability. */
    public static Part style(Consumer<Style> edit) { return s(edit); }

    /** Base (lowest-priority) style. */
    public static Part base(Consumer<Style> edit) { return n -> n.baseStyle(edit); }

    public static Part gap(float g) { return n -> n.style(s -> s.gap(g)); }

    public static Part pad(float p) { return n -> n.style(s -> s.pad(p)); }

    public static Part grow() { return n -> n.style(Style::grow); }

    /** Style while hovered. */
    public static Part hover(Consumer<Style> edit) { return n -> n.hover(edit); }

    /** Style while pressed. */
    public static Part pressed(Consumer<Style> edit) { return n -> n.active(edit); }

    /** Style while the node has a state. */
    public static Part when(String state, Consumer<Style> edit) { return n -> n.when(state, edit); }

    public static Part cls(String... names) { return n -> n.cls(names); }

    public static Part id(String id) { return n -> n.id(id); }

    public static Part click(Runnable r) { return n -> n.onClick(r); }

    public static Part click(Consumer<MouseEvent> r) { return n -> n.onClick(r); }

    public static Part rightClick(Consumer<MouseEvent> r) { return n -> n.on(EventType.CONTEXT_MENU, r); }

    public static Part tip(String text) { return n -> n.tooltip(text); }

    public static Part focusable() { return n -> n.focusable(true); }

    public static <T> Part token(Token<T> t, T value) { return n -> n.token(t, value); }

    public static Part theme(Theme t) { return n -> n.theme(t); }

    /** Runs arbitrary setup on the node. */
    public static Part with(Consumer<Node> c) { return c::accept; }

    // ------------------------------------------------------------------ building

    /** Applies parts to a node and returns it. */
    public static <N extends Node> N build(N n, Object... parts) {
        for (Object p : parts) {
            if (p == null) continue;
            if (p instanceof Node c) n.add(c);
            else if (p instanceof Part part) part.applyTo(n);
            else if (p instanceof String cls) n.cls(cls.replace(".", " ").trim());
            else if (p instanceof Iterable<?> it) for (Object o : it) build(n, o);
            else if (p instanceof Object[] arr) build(n, arr);
            else throw new IllegalArgumentException("Unsupported UI part: " + p);
        }
        return n;
    }
}
