package me.methodial.apothecary.style;

import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.render.FontSpec;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Final values for every property of one element, indexed by {@link Prop#id()}. */
public final class ComputedStyle {
    final Object[] values;
    /** Props given explicitly by a rule or inline style (the rest were inherited or initial). */
    final BitSet specified;
    TokenMap tokens = TokenMap.EMPTY;
    private FontSpec font;

    public ComputedStyle() {
        values = new Object[Props.count()];
        specified = new BitSet(values.length);
        for (Prop<?> p : Props.all()) values[p.id()] = p.initial();
    }

    public ComputedStyle(ComputedStyle o) {
        values = Arrays.copyOf(o.values, Math.max(o.values.length, Props.count()));
        specified = (BitSet) o.specified.clone();
        tokens = o.tokens;
        for (int i = o.values.length; i < values.length; i++) values[i] = Props.all().get(i).initial();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Prop<T> p) {
        if (p.id() >= values.length) return p.initial();
        return (T) values[p.id()];
    }

    public Object raw(int id) { return values[id]; }

    public void setRaw(int id, Object v) { values[id] = v; font = null; }

    public <T> void set(Prop<T> p, T v) { values[p.id()] = v; font = null; }

    public boolean isSpecified(Prop<?> p) { return specified.get(p.id()); }

    public void markSpecified(int id) { specified.set(id); }

    public BitSet specifiedSet() { return specified; }

    /** Design-token values visible to this element. */
    public TokenMap tokens() { return tokens; }

    public void setTokens(TokenMap t) { tokens = t; }

    /** Resolves a design token for this element: {@code computed().token(Tokens.ACCENT)}. */
    public <T> T token(me.methodial.apothecary.theme.Token<T> t) { return tokens.get(t); }

    // ------------------------------------------------------------ convenience accessors

    public Display display() { return get(Props.DISPLAY); }
    public boolean isAbsolute() { return get(Props.POSITION) == Position.ABSOLUTE; }
    public Length width() { return get(Props.WIDTH); }
    public Length height() { return get(Props.HEIGHT); }
    public Paint background() { return get(Props.BACKGROUND); }
    public Corners radius() { return get(Props.BORDER_RADIUS); }
    public float opacity() { return get(Props.OPACITY); }
    public Color color() { return get(Props.COLOR); }
    public List<Shadow> shadows() { return get(Props.BOX_SHADOW); }

    public me.methodial.apothecary.geom.Edges border() {
        return new me.methodial.apothecary.geom.Edges(get(Props.BORDER_TOP_WIDTH), get(Props.BORDER_RIGHT_WIDTH),
                get(Props.BORDER_BOTTOM_WIDTH), get(Props.BORDER_LEFT_WIDTH));
    }

    public me.methodial.apothecary.geom.Edges padding(float base) {
        return new me.methodial.apothecary.geom.Edges(get(Props.PADDING_TOP).resolveOr(base, 0), get(Props.PADDING_RIGHT).resolveOr(base, 0),
                get(Props.PADDING_BOTTOM).resolveOr(base, 0), get(Props.PADDING_LEFT).resolveOr(base, 0));
    }

    public LengthEdges margin() {
        return new LengthEdges(get(Props.MARGIN_TOP), get(Props.MARGIN_RIGHT), get(Props.MARGIN_BOTTOM), get(Props.MARGIN_LEFT));
    }

    public FontSpec font() {
        if (font == null) {
            font = new FontSpec(get(Props.FONT_FAMILY), get(Props.FONT_SIZE), get(Props.FONT_WEIGHT), get(Props.FONT_ITALIC), get(Props.LETTER_SPACING));
        }
        return font;
    }

    public boolean sameAs(ComputedStyle o, int id) { return java.util.Objects.equals(values[id], o.values[id]); }
}
