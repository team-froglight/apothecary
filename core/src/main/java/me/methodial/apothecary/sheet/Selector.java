package me.methodial.apothecary.sheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches elements, built in code. Immutable; every method returns a new selector.
 * <pre>{@code
 * is("button").withCls("primary").hover()             // button.primary:hover
 * is("check-mark").in(is("checkbox").checked())       // checkbox:checked check-mark
 * cls("card").child(is("text"))                       // .card > text
 * }</pre>
 * Specificity follows CSS: ids &gt; classes/states &gt; types; later rules win ties.
 */
public final class Selector {
    /** Common state names (any string works with {@link #state(String)}). */
    public static final String HOVER = "hover", ACTIVE = "active", FOCUS = "focus", FOCUS_VISIBLE = "focus-visible",
            FOCUS_WITHIN = "focus-within", DISABLED = "disabled", CHECKED = "checked", SELECTED = "selected", OPEN = "open",
            CLOSING = "closing", INDETERMINATE = "indeterminate", DRAGGING = "dragging", HIGHLIGHTED = "highlighted";

    private enum Combinator { DESCENDANT, CHILD }

    /** One compound part, e.g. {@code button.primary:hover}. */
    private record Compound(String type, String id, List<String> classes, List<String> states, List<int[]> nth,
                            List<Compound> nots, boolean root) {
        static Compound empty() { return new Compound(null, null, List.of(), List.of(), List.of(), List.of(), false); }

        Compound withType(String t) { return new Compound(t, id, classes, states, nth, nots, root); }
        Compound withId(String i) { return new Compound(type, i, classes, states, nth, nots, root); }
        Compound withClass(String c) { return new Compound(type, id, plus(classes, c), states, nth, nots, root); }
        Compound withState(String s) { return new Compound(type, id, classes, plus(states, s), nth, nots, root); }
        Compound withNth(int[] n) { return new Compound(type, id, classes, states, plus(nth, n), nots, root); }
        Compound withNot(Compound c) { return new Compound(type, id, classes, states, nth, plus(nots, c), root); }
        Compound asRoot() { return new Compound(type, id, classes, states, nth, nots, true); }

        private static <T> List<T> plus(List<T> l, T v) {
            List<T> out = new ArrayList<>(l);
            out.add(v);
            return List.copyOf(out);
        }

        boolean matches(Styleable e) {
            if (type != null && !type.equals(e.typeName())) return false;
            if (id != null && !id.equals(e.id())) return false;
            for (String c : classes) if (!e.classes().contains(c)) return false;
            for (String s : states) if (!e.hasState(s)) return false;
            if (root && e.styleParent() != null) return false;
            for (int[] n : nth) {
                int idx = n[2] == 1 ? e.siblingCount() - e.indexInParent() : e.indexInParent() + 1;
                if (!nthMatches(n[0], n[1], idx)) return false;
            }
            for (Compound c : nots) if (c.matches(e)) return false;
            return true;
        }

        int specificity() {
            int s = (id != null ? 10000 : 0) + (classes.size() + states.size() + nth.size() + (root ? 1 : 0)) * 100 + (type != null ? 1 : 0);
            for (Compound c : nots) s += c.specificity();
            return s;
        }

        private static boolean nthMatches(int a, int b, int idx) {
            if (a == 0) return idx == b;
            int n = idx - b;
            return n % a == 0 && n / a >= 0;
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder(type == null ? (classes.isEmpty() && id == null && states.isEmpty() ? "*" : "") : type);
            if (id != null) sb.append('#').append(id);
            for (String c : classes) sb.append('.').append(c);
            for (String s : states) sb.append(':').append(s);
            return sb.toString();
        }
    }

    private final List<Compound> parts;          // left to right
    private final List<Combinator> combinators;  // between parts
    private final int specificity;

    private Selector(List<Compound> parts, List<Combinator> combinators) {
        this.parts = List.copyOf(parts);
        this.combinators = List.copyOf(combinators);
        int s = 0;
        for (Compound c : parts) s += c.specificity();
        this.specificity = s;
    }

    // ------------------------------------------------------------------ factories

    /** Elements of a type ({@code Node#type}). */
    public static Selector is(String type) { return new Selector(List.of(Compound.empty().withType(type)), List.of()); }

    /** Elements with a class. */
    public static Selector cls(String name) { return new Selector(List.of(Compound.empty().withClass(name)), List.of()); }

    public static Selector id(String id) { return new Selector(List.of(Compound.empty().withId(id)), List.of()); }

    public static Selector any() { return new Selector(List.of(Compound.empty()), List.of()); }

    /** The root element (where UI-wide tokens and defaults live). */
    public static Selector root() { return new Selector(List.of(Compound.empty().asRoot()), List.of()); }

    // ------------------------------------------------------------------ refinements (apply to the rightmost part)

    private Selector last(java.util.function.UnaryOperator<Compound> f) {
        List<Compound> p = new ArrayList<>(parts);
        p.set(p.size() - 1, f.apply(p.get(p.size() - 1)));
        return new Selector(p, combinators);
    }

    public Selector and(String type) { return last(c -> c.withType(type)); }
    public Selector andCls(String name) { return last(c -> c.withClass(name)); }
    /** Adds a class requirement: {@code is("button").withCls("primary")}. */
    public Selector withCls(String name) { return andCls(name); }
    public Selector state(String s) { return last(c -> c.withState(s)); }
    public Selector hover() { return state(HOVER); }
    public Selector active() { return state(ACTIVE); }
    public Selector focus() { return state(FOCUS); }
    public Selector focusVisible() { return state(FOCUS_VISIBLE); }
    public Selector disabled() { return state(DISABLED); }
    public Selector checked() { return state(CHECKED); }
    public Selector selected() { return state(SELECTED); }
    public Selector open() { return state(OPEN); }
    public Selector closing() { return state(CLOSING); }
    public Selector not(Selector simple) { return last(c -> c.withNot(simple.parts.get(simple.parts.size() - 1))); }
    public Selector notState(String s) { return last(c -> c.withNot(Compound.empty().withState(s))); }
    public Selector firstChild() { return last(c -> c.withNth(new int[]{0, 1, 0})); }
    public Selector lastChild() { return last(c -> c.withNth(new int[]{0, 1, 1})); }
    /** :nth-child(a n + b), 1-based. */
    public Selector nth(int a, int b) { return last(c -> c.withNth(new int[]{a, b, 0})); }
    public Selector odd() { return nth(2, 1); }
    public Selector even() { return nth(2, 0); }

    // ------------------------------------------------------------------ combinators

    /** This selector, anywhere inside {@code ancestor}. */
    public Selector in(Selector ancestor) { return join(ancestor, Combinator.DESCENDANT, this); }

    /** This selector, as a direct child of {@code parent}. */
    public Selector childOf(Selector parent) { return join(parent, Combinator.CHILD, this); }

    /** {@code descendant} anywhere inside this. */
    public Selector find(Selector descendant) { return join(this, Combinator.DESCENDANT, descendant); }

    /** {@code child} directly inside this. */
    public Selector child(Selector child) { return join(this, Combinator.CHILD, child); }

    private static Selector join(Selector left, Combinator c, Selector right) {
        List<Compound> p = new ArrayList<>(left.parts);
        p.addAll(right.parts);
        List<Combinator> cs = new ArrayList<>(left.combinators);
        cs.add(c);
        cs.addAll(right.combinators);
        return new Selector(p, cs);
    }

    // ------------------------------------------------------------------ matching

    public int specificity() { return specificity; }

    /** The rightmost part's type (or null), used to bucket rules. */
    public String keyType() { return parts.get(parts.size() - 1).type(); }

    public boolean matches(Styleable e) { return matchFrom(parts.size() - 1, e); }

    private boolean matchFrom(int i, Styleable e) {
        if (!parts.get(i).matches(e)) return false;
        if (i == 0) return true;
        Combinator comb = combinators.get(i - 1);
        Styleable p = e.styleParent();
        if (comb == Combinator.CHILD) return p != null && matchFrom(i - 1, p);
        while (p != null) {
            if (matchFrom(i - 1, p)) return true;
            p = p.styleParent();
        }
        return false;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(combinators.get(i - 1) == Combinator.CHILD ? " > " : " ");
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
