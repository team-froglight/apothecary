package me.methodial.apothecary.style;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.theme.Ref;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.theme.TypeStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A block of style declarations, written in code. Every setter returns {@code this}:
 * <pre>{@code
 * s.row().center().gap(4).pad(4, 9).radius(RADIUS).bg(SURFACE_3).border(1, BORDER_STRONG).fg(TEXT)
 *  .transition(Tr.colors(0.12f), Tr.transform(0.14f));
 * }</pre>
 * Values can be constants, design {@link Token}s ({@code bg(SURFACE)}) or {@link Ref}s computed from tokens
 * ({@code bg(t -> Paint.linear(180, t.get(ACCENT_HOVER), t.get(ACCENT)))}). Reusable chunks are plain
 * {@code Consumer<Style>} mixins: {@code s.apply(Mixins.card, Mixins.focusRing)}.
 */
public class Style {
    private final Map<Prop<?>, Object> values = new LinkedHashMap<>();
    private final Map<Token<?>, Object> tokens = new LinkedHashMap<>();
    private Runnable onChange;

    public Style() { }

    public void onChange(Runnable r) { this.onChange = r; }

    private Style changed() {
        if (onChange != null) onChange.run();
        return this;
    }

    // ================================================================== core

    /** Raw values: constants, {@link Ref}s or {@link Keyword}s. */
    public Map<Prop<?>, Object> values() { return Collections.unmodifiableMap(values); }

    /** Token values scoped to elements this style applies to (and their descendants). */
    public Map<Token<?>, Object> tokens() { return Collections.unmodifiableMap(tokens); }

    public boolean isEmpty() { return values.isEmpty() && tokens.isEmpty(); }

    public <T> Style set(Prop<T> p, T value) { values.put(p, value); return changed(); }

    /** Sets a property to a value computed from design tokens. */
    public <T> Style set(Prop<T> p, Ref<? extends T> value) { values.put(p, value); return changed(); }

    @SuppressWarnings("unchecked")
    public <T> T get(Prop<T> p) {
        Object v = values.get(p);
        return v instanceof Ref || v instanceof Keyword ? null : (T) v;
    }

    public Style remove(Prop<?> p) { values.remove(p); return changed(); }

    public Style inherit(Prop<?> p) { values.put(p, Keyword.INHERIT); return changed(); }

    public Style initial(Prop<?> p) { values.put(p, Keyword.INITIAL); return changed(); }

    public Style clear() { values.clear(); tokens.clear(); return changed(); }

    /** Scopes a token value to this element's subtree: {@code s.token(ACCENT, Color.hex("#f25"))}. */
    public <T> Style token(Token<T> t, T value) { tokens.put(t, value); return changed(); }

    public <T> Style token(Token<T> t, Ref<? extends T> value) { tokens.put(t, value); return changed(); }

    /** Copies everything from {@code o} over this one. */
    public Style merge(Style o) {
        values.putAll(o.values);
        tokens.putAll(o.tokens);
        return changed();
    }

    /** Applies reusable style functions (mixins). */
    @SafeVarargs
    public final Style apply(Consumer<Style>... mixins) {
        for (Consumer<Style> m : mixins) m.accept(this);
        return this;
    }

    /** Applies {@code body} only when {@code condition} holds (handy in builders). */
    public Style when(boolean condition, Consumer<Style> body) {
        if (condition) body.accept(this);
        return this;
    }

    private Style each(List<Prop<?>> props, Object v) {
        for (Prop<?> p : props) values.put(p, v);
        return changed();
    }

    // ================================================================== display & flex

    public Style display(Display d) { return set(Props.DISPLAY, d); }
    public Style block() { return display(Display.BLOCK); }
    public Style flex() { return display(Display.FLEX); }
    public Style grid() { return display(Display.GRID); }
    public Style hidden() { return display(Display.NONE); }
    /** Flex row (items centered vertically, the common case). */
    public Style row() { flex(); set(Props.FLEX_DIRECTION, FlexDirection.ROW); return set(Props.ALIGN_ITEMS, Align.CENTER); }
    /** Flex column. */
    public Style col() { flex(); return set(Props.FLEX_DIRECTION, FlexDirection.COLUMN); }
    public Style column() { return col(); }
    public Style direction(FlexDirection d) { flex(); return set(Props.FLEX_DIRECTION, d); }
    public Style wrap() { return set(Props.FLEX_WRAP, FlexWrap.WRAP); }
    public Style nowrapFlex() { return set(Props.FLEX_WRAP, FlexWrap.NOWRAP); }
    public Style wrap(FlexWrap w) { return set(Props.FLEX_WRAP, w); }

    public Style justify(Justify j) { return set(Props.JUSTIFY_CONTENT, j); }
    public Style items(Align a) { return set(Props.ALIGN_ITEMS, a); }
    public Style alignItems(Align a) { return items(a); }
    public Style self(Align a) { return set(Props.ALIGN_SELF, a); }
    public Style alignSelf(Align a) { return self(a); }
    public Style content(Justify j) { return set(Props.ALIGN_CONTENT, j); }
    public Style alignContent(Justify j) { return content(j); }
    public Style justifyItems(Align a) { return set(Props.JUSTIFY_ITEMS, a); }
    public Style justifySelf(Align a) { return set(Props.JUSTIFY_SELF, a); }
    /** Centers children on both axes (switches to flex if the element isn't already flex). */
    public Style center() {
        if (!(values.get(Props.DISPLAY) instanceof Display d) || d != Display.FLEX) flex();
        justify(Justify.CENTER);
        return items(Align.CENTER);
    }
    public Style between() { return justify(Justify.SPACE_BETWEEN); }
    public Style around() { return justify(Justify.SPACE_AROUND); }
    public Style evenly() { return justify(Justify.SPACE_EVENLY); }
    public Style justifyStart() { return justify(Justify.START); }
    public Style justifyEnd() { return justify(Justify.END); }
    public Style justifyCenter() { return justify(Justify.CENTER); }
    public Style itemsStart() { return items(Align.START); }
    public Style itemsCenter() { return items(Align.CENTER); }
    public Style itemsEnd() { return items(Align.END); }
    public Style itemsStretch() { return items(Align.STRETCH); }

    public Style grow(float g) { return set(Props.FLEX_GROW, g); }
    public Style grow() { return grow(1); }
    public Style shrink(float s) { return set(Props.FLEX_SHRINK, s); }
    public Style noShrink() { return shrink(0); }
    public Style basis(float px) { return set(Props.FLEX_BASIS, Length.px(px)); }
    public Style basis(Length l) { return set(Props.FLEX_BASIS, l); }
    /** flex: g 1 0 — share space proportionally. */
    public Style flex(float grow) { set(Props.FLEX_GROW, grow); set(Props.FLEX_SHRINK, 1f); return set(Props.FLEX_BASIS, Length.ZERO); }
    public Style order(int o) { return set(Props.ORDER, o); }
    public Style gap(float g) { set(Props.ROW_GAP, Length.px(g)); return set(Props.COLUMN_GAP, Length.px(g)); }
    public Style gap(float row, float col) { set(Props.ROW_GAP, Length.px(row)); return set(Props.COLUMN_GAP, Length.px(col)); }

    // ================================================================== grid

    /** grid-template-columns: {@code cols(px(60), fr(1))}, {@code cols(fill(minmax(px(40), fr(1))))}. */
    public Style cols(TrackEntry... tracks) { grid(); return set(Props.GRID_TEMPLATE_COLUMNS, List.of(tracks)); }
    /** n equal columns. */
    public Style cols(int n) { return cols(TrackSize.repeat(n, TrackSize.fr(1))); }
    public Style rows(TrackEntry... tracks) { grid(); return set(Props.GRID_TEMPLATE_ROWS, List.of(tracks)); }
    public Style areas(String... rows) { grid(); return set(Props.GRID_TEMPLATE_AREAS, GridAreas.of(rows)); }
    public Style autoRows(TrackSize... t) { return set(Props.GRID_AUTO_ROWS, List.of(t)); }
    public Style autoCols(TrackSize... t) { return set(Props.GRID_AUTO_COLUMNS, List.of(t)); }
    public Style flow(GridAutoFlow f) { return set(Props.GRID_AUTO_FLOW, f); }
    public Style dense() { return flow(GridAutoFlow.ROW_DENSE); }
    /** Place in a named template area. */
    public Style area(String name) { set(Props.GRID_COLUMN, GridPlacement.area(name)); return set(Props.GRID_ROW, GridPlacement.area(name)); }
    public Style colSpan(int n) { return set(Props.GRID_COLUMN, GridPlacement.span(n)); }
    public Style rowSpan(int n) { return set(Props.GRID_ROW, GridPlacement.span(n)); }
    /** Grid column lines (1-based, negative from the end): {@code gridCol(1, -1)} spans everything. */
    public Style gridCol(int start, int end) { return set(Props.GRID_COLUMN, GridPlacement.lines(start, end)); }
    public Style gridRow(int start, int end) { return set(Props.GRID_ROW, GridPlacement.lines(start, end)); }
    public Style gridColumn(GridPlacement p) { return set(Props.GRID_COLUMN, p); }
    public Style gridRow(GridPlacement p) { return set(Props.GRID_ROW, p); }

    // ================================================================== size

    public Style w(float px) { return set(Props.WIDTH, Length.px(px)); }
    public Style h(float px) { return set(Props.HEIGHT, Length.px(px)); }
    public Style w(Length l) { return set(Props.WIDTH, l); }
    public Style h(Length l) { return set(Props.HEIGHT, l); }
    public Style width(float px) { return w(px); }
    public Style height(float px) { return h(px); }
    public Style wPct(float pct) { return w(Length.pct(pct)); }
    public Style hPct(float pct) { return h(Length.pct(pct)); }
    public Style size(float px) { w(px); return h(px); }
    public Style size(float w, float h) { w(w); return h(h); }
    /** 100% x 100%. */
    public Style full() { wPct(100); return hPct(100); }
    public Style fullW() { return wPct(100); }
    public Style fullH() { return hPct(100); }
    public Style minW(float px) { return set(Props.MIN_WIDTH, Length.px(px)); }
    public Style minH(float px) { return set(Props.MIN_HEIGHT, Length.px(px)); }
    public Style maxW(float px) { return set(Props.MAX_WIDTH, Length.px(px)); }
    public Style maxH(float px) { return set(Props.MAX_HEIGHT, Length.px(px)); }
    public Style maxW(Length l) { return set(Props.MAX_WIDTH, l); }
    public Style aspect(float ratio) { return set(Props.ASPECT_RATIO, ratio); }

    // ================================================================== spacing

    public Style pad(float all) { return pad(all, all, all, all); }
    public Style pad(float v, float h) { return pad(v, h, v, h); }
    public Style pad(float t, float r, float b, float l) {
        values.put(Props.PADDING_TOP, Length.px(t)); values.put(Props.PADDING_RIGHT, Length.px(r));
        values.put(Props.PADDING_BOTTOM, Length.px(b)); values.put(Props.PADDING_LEFT, Length.px(l));
        return changed();
    }
    public Style padX(float h) { values.put(Props.PADDING_LEFT, Length.px(h)); return set(Props.PADDING_RIGHT, Length.px(h)); }
    public Style padY(float v) { values.put(Props.PADDING_TOP, Length.px(v)); return set(Props.PADDING_BOTTOM, Length.px(v)); }
    public Style pt(float v) { return set(Props.PADDING_TOP, Length.px(v)); }
    public Style pr(float v) { return set(Props.PADDING_RIGHT, Length.px(v)); }
    public Style pb(float v) { return set(Props.PADDING_BOTTOM, Length.px(v)); }
    public Style pl(float v) { return set(Props.PADDING_LEFT, Length.px(v)); }
    public Style padTop(float v) { return set(Props.PADDING_TOP, Length.px(v)); }
    public Style padRight(float v) { return set(Props.PADDING_RIGHT, Length.px(v)); }
    public Style padBottom(float v) { return set(Props.PADDING_BOTTOM, Length.px(v)); }
    public Style padLeft(float v) { return set(Props.PADDING_LEFT, Length.px(v)); }

    public Style m(float all) { return m(all, all, all, all); }
    public Style m(float v, float h) { return m(v, h, v, h); }
    public Style m(float t, float r, float b, float l) {
        values.put(Props.MARGIN_TOP, Length.px(t)); values.put(Props.MARGIN_RIGHT, Length.px(r));
        values.put(Props.MARGIN_BOTTOM, Length.px(b)); values.put(Props.MARGIN_LEFT, Length.px(l));
        return changed();
    }
    public Style mx(float h) { values.put(Props.MARGIN_LEFT, Length.px(h)); return set(Props.MARGIN_RIGHT, Length.px(h)); }
    public Style my(float v) { values.put(Props.MARGIN_TOP, Length.px(v)); return set(Props.MARGIN_BOTTOM, Length.px(v)); }
    public Style mt(float v) { return set(Props.MARGIN_TOP, Length.px(v)); }
    public Style mr(float v) { return set(Props.MARGIN_RIGHT, Length.px(v)); }
    public Style mb(float v) { return set(Props.MARGIN_BOTTOM, Length.px(v)); }
    public Style ml(float v) { return set(Props.MARGIN_LEFT, Length.px(v)); }
    /** margin-left: auto — pushes this item (and following ones) to the end of a row. */
    public Style pushRight() { return set(Props.MARGIN_LEFT, Length.AUTO); }
    /** margin-top: auto — pushes to the bottom of a column. */
    public Style pushDown() { return set(Props.MARGIN_TOP, Length.AUTO); }
    public Style mxAuto() { values.put(Props.MARGIN_LEFT, Length.AUTO); return set(Props.MARGIN_RIGHT, Length.AUTO); }

    // ================================================================== position

    public Style abs() { return set(Props.POSITION, Position.ABSOLUTE); }
    public Style absolute() { return abs(); }
    public Style rel() { return set(Props.POSITION, Position.RELATIVE); }
    public Style top(float px) { return set(Props.TOP, Length.px(px)); }
    public Style right(float px) { return set(Props.RIGHT, Length.px(px)); }
    public Style bottom(float px) { return set(Props.BOTTOM, Length.px(px)); }
    public Style left(float px) { return set(Props.LEFT, Length.px(px)); }
    public Style left(Length l) { return set(Props.LEFT, l); }
    public Style top(Length l) { return set(Props.TOP, l); }
    public Style inset(float all) { return inset(all, all, all, all); }
    public Style inset(float t, float r, float b, float l) {
        values.put(Props.TOP, Length.px(t)); values.put(Props.RIGHT, Length.px(r));
        values.put(Props.BOTTOM, Length.px(b)); values.put(Props.LEFT, Length.px(l));
        return changed();
    }
    /** Absolutely positioned over the whole parent (inset 0). */
    public Style cover() { abs(); return inset(0); }
    public Style z(int z) { return set(Props.Z_INDEX, z); }
    public Style zIndex(int z) { return z(z); }

    // ================================================================== overflow

    public Style overflow(Overflow o) { values.put(Props.OVERFLOW_X, o); return set(Props.OVERFLOW_Y, o); }
    public Style clip() { return overflow(Overflow.HIDDEN); }
    public Style scroll() { return overflow(Overflow.SCROLL); }
    public Style scrollY() { values.put(Props.OVERFLOW_X, Overflow.HIDDEN); return set(Props.OVERFLOW_Y, Overflow.SCROLL); }
    public Style scrollX() { values.put(Props.OVERFLOW_Y, Overflow.HIDDEN); return set(Props.OVERFLOW_X, Overflow.SCROLL); }
    public Style scrollbar(float width) { return set(Props.SCROLLBAR_WIDTH, width); }
    public Style scrollbar(float width, Ref<Color> color) { set(Props.SCROLLBAR_WIDTH, width); return set(Props.SCROLLBAR_COLOR, color); }
    /** A square scrollbar: the thumb a raised block of {@code color}, framed, in a groove of {@code track}. */
    public Style blockyScrollbar(float width, Ref<Color> color, Ref<Color> track) {
        scrollbar(width, color);
        set(Props.SCROLLBAR_TRACK_COLOR, track);
        return set(Props.SCROLLBAR_BLOCKY, true);
    }
    public Style smoothScroll(float seconds) { return set(Props.SCROLL_SMOOTHNESS, seconds); }

    // ================================================================== background, border, shadow

    public Style bg(Color c) { return set(Props.BACKGROUND, Paint.of(c)); }
    public Style bg(Paint p) { return set(Props.BACKGROUND, p); }
    /** Background color from a token or token math: {@code bg(SURFACE)}, {@code bg(alpha(DANGER, .13f))}. */
    public Style bg(Ref<Color> c) { return set(Props.BACKGROUND, (Ref<Paint>) t -> Paint.of(c.resolve(t))); }
    /** Background paint computed from tokens: {@code bgPaint(t -> Paint.radial(t.get(ACCENT), t.get(BG)))}. */
    public Style bgPaint(Ref<? extends Paint> p) { return set(Props.BACKGROUND, p); }
    public Style background(Color c) { return bg(c); }
    public Style background(Paint p) { return bg(p); }
    /** A top-to-bottom gradient of token colors: {@code gradient(180, ACCENT_HOVER, ACCENT)}. */
    @SafeVarargs
    public final Style gradient(float angle, Ref<Color>... colors) {
        return set(Props.BACKGROUND, (Ref<Paint>) t -> {
            Color[] cs = new Color[colors.length];
            for (int i = 0; i < cs.length; i++) cs[i] = colors[i].resolve(t);
            return Paint.linear(angle, cs);
        });
    }

    public Style border(float width, Color c) { borderW(width); return set(Props.BORDER_COLOR, c); }
    public Style border(float width, Ref<Color> c) { borderW(width); return set(Props.BORDER_COLOR, c); }
    public Style borderW(float width) { return each(Props.BORDER_WIDTHS, width); }
    public Style borderColor(Color c) { return set(Props.BORDER_COLOR, c); }
    public Style borderColor(Ref<Color> c) { return set(Props.BORDER_COLOR, c); }
    /** Only the bottom edge (dividers, tab bars). */
    public Style borderBottom(float width, Ref<Color> c) {
        borderW(0);
        values.put(Props.BORDER_BOTTOM_WIDTH, width);
        return set(Props.BORDER_COLOR, c);
    }
    public Style borderTop(float width, Ref<Color> c) {
        borderW(0);
        values.put(Props.BORDER_TOP_WIDTH, width);
        return set(Props.BORDER_COLOR, c);
    }
    public Style borderRight(float width, Ref<Color> c) {
        borderW(0);
        values.put(Props.BORDER_RIGHT_WIDTH, width);
        return set(Props.BORDER_COLOR, c);
    }
    /** Each edge its own width, one color (0 leaves an edge open). */
    public Style borders(float top, float right, float bottom, float left, Ref<Color> c) {
        values.put(Props.BORDER_TOP_WIDTH, top);
        values.put(Props.BORDER_RIGHT_WIDTH, right);
        values.put(Props.BORDER_BOTTOM_WIDTH, bottom);
        values.put(Props.BORDER_LEFT_WIDTH, left);
        return set(Props.BORDER_COLOR, c);
    }
    public Style noBorder() { return borderW(0); }

    public Style radius(float r) { return set(Props.BORDER_RADIUS, Corners.all(r)); }
    public Style radius(Ref<Float> r) { return set(Props.BORDER_RADIUS, (Ref<Corners>) t -> Corners.all(r.resolve(t))); }
    public Style radius(float tl, float tr, float br, float bl) { return set(Props.BORDER_RADIUS, new Corners(tl, tr, br, bl)); }
    /** Only the top corners (bottom square). */
    public Style radiusTop(Ref<Float> r) { return set(Props.BORDER_RADIUS, (Ref<Corners>) t -> { float v = r.resolve(t); return new Corners(v, v, 0, 0); }); }
    public Style radiusBottom(Ref<Float> r) { return set(Props.BORDER_RADIUS, (Ref<Corners>) t -> { float v = r.resolve(t); return new Corners(0, 0, v, v); }); }
    /** Fully rounded ends (pills, circles). */
    public Style round() { return radius(999); }

    public Style shadow(Shadow... s) { return set(Props.BOX_SHADOW, List.of(s)); }
    public Style shadow(Ref<List<Shadow>> s) { return set(Props.BOX_SHADOW, s); }
    /** One shadow whose color comes from a token: {@code shadow(0, 2, 8, ACCENT.map(c -> c.withAlpha(.4f)))}. */
    public Style shadow(float x, float y, float blur, Ref<Color> color) {
        return set(Props.BOX_SHADOW, (Ref<List<Shadow>>) t -> List.of(new Shadow(x, y, blur, 0, color.resolve(t), false)));
    }
    /** A focus-ring style halo (spread, no blur). */
    public Style ring(float width, Ref<Color> color) {
        return set(Props.BOX_SHADOW, (Ref<List<Shadow>>) t -> List.of(new Shadow(0, 0, 0, width, color.resolve(t), false)));
    }
    /**
     * A hard-edged bevel drawn inside the box: a {@code light} band along the top and a {@code depth} band
     * along the bottom (blocky, Minecraft-style buttons and panels). Negative widths swap the sides, for
     * recessed wells. Replaces the shadow list.
     */
    public Style bevel(float light, Ref<Color> lightColor, float depth, Ref<Color> depthColor) {
        return set(Props.BOX_SHADOW, (Ref<List<Shadow>>) t -> List.of(
                Shadow.inset(0, light, 0, lightColor.resolve(t)), Shadow.inset(0, -depth, 0, depthColor.resolve(t))));
    }

    /** {@link #bevel} plus a hard drop shadow below the box. */
    public Style bevel(float light, Ref<Color> lightColor, float depth, Ref<Color> depthColor, float drop, Ref<Color> dropColor) {
        return set(Props.BOX_SHADOW, (Ref<List<Shadow>>) t -> List.of(Shadow.of(0, drop, 0, dropColor.resolve(t)),
                Shadow.inset(0, light, 0, lightColor.resolve(t)), Shadow.inset(0, -depth, 0, depthColor.resolve(t))));
    }

    public Style noShadow() { return set(Props.BOX_SHADOW, Shadow.NONE); }

    public Style outline(float width, Ref<Color> c) { set(Props.OUTLINE_WIDTH, width); return set(Props.OUTLINE_COLOR, c); }
    public Style outline(float width, Ref<Color> c, float offset) { outline(width, c); return set(Props.OUTLINE_OFFSET, offset); }
    public Style opacity(float o) { return set(Props.OPACITY, o); }
    public Style tint(Color c) { return set(Props.TINT, c); }
    public Style tint(Ref<Color> c) { return set(Props.TINT, c); }

    // ================================================================== transform

    public Style transform(Transform t) { return set(Props.TRANSFORM, t); }
    public Style translate(float x, float y) { return transform(Transform.translate(x, y)); }
    public Style translateX(float x) { return transform(Transform.translate(x, 0)); }
    public Style translateY(float y) { return transform(Transform.translate(0, y)); }
    /** Translate and uniformly scale in one go (entrance/exit animations). */
    public Style translate(float x, float y, float scale) { return transform(Transform.translate(x, y).withScale(scale, scale)); }
    /** Translate by fractions of the element's own size (1 = 100%). */
    public Style translateRel(float fx, float fy) { return transform(Transform.IDENTITY.withTranslate(Length.pct(fx * 100), Length.pct(fy * 100))); }
    /** Translate then rotate. */
    public Style translateRotate(float x, float y, float degrees) { return transform(Transform.translate(x, y).withRotate(degrees)); }
    public Style scale(float s) { return transform(Transform.scale(s)); }
    public Style scale(float sx, float sy) { return transform(Transform.IDENTITY.withScale(sx, sy)); }
    public Style rotate(float degrees) { return transform(Transform.rotate(degrees)); }
    public Style skewX(float degrees) { return transform(Transform.IDENTITY.withSkew(degrees, 0)); }
    /** No transform. */
    public Style untransformed() { return transform(Transform.IDENTITY); }
    /** transform-origin as fractions of the box (0..1): {@code origin(0, 0.5f)} = left center. */
    public Style origin(float fx, float fy) { return set(Props.TRANSFORM_ORIGIN, new TransformOrigin(Length.pct(fx * 100), Length.pct(fy * 100))); }

    // ================================================================== interaction

    public Style cursor(Cursor c) { return set(Props.CURSOR, c); }
    /** cursor: pointer. */
    public Style pointer() { return cursor(Cursor.POINTER); }
    public Style pointerEvents(PointerEvents p) { return set(Props.POINTER_EVENTS, p); }
    /** Clicks pass through this element (children still receive them). */
    public Style passThrough() { return pointerEvents(PointerEvents.NONE); }
    public Style visible(boolean v) { return set(Props.VISIBILITY, v ? Visibility.VISIBLE : Visibility.HIDDEN); }

    // ================================================================== text

    public Style fg(Color c) { return set(Props.COLOR, c); }
    public Style fg(Ref<Color> c) { return set(Props.COLOR, c); }
    public Style color(Color c) { return fg(c); }
    public Style font(String family) { return set(Props.FONT_FAMILY, family); }
    public Style font(Ref<String> family) { return set(Props.FONT_FAMILY, family); }
    public Style fontSize(float px) { return set(Props.FONT_SIZE, px); }
    public Style weight(FontWeight w) { return set(Props.FONT_WEIGHT, w); }
    public Style bold() { return weight(FontWeight.BOLD); }
    public Style semibold() { return weight(FontWeight.SEMIBOLD); }
    public Style medium() { return weight(FontWeight.MEDIUM); }
    public Style regular() { return weight(FontWeight.NORMAL); }
    public Style italic() { return set(Props.FONT_ITALIC, true); }
    public Style lineHeight(float f) { return set(Props.LINE_HEIGHT, f); }
    /** Letter spacing in px. */
    public Style tracking(float px) { return set(Props.LETTER_SPACING, px); }
    public Style textAlign(TextAlign a) { return set(Props.TEXT_ALIGN, a); }
    public Style textCenter() { return textAlign(TextAlign.CENTER); }
    public Style textRight() { return textAlign(TextAlign.RIGHT); }
    public Style verticalAlign(VerticalAlign v) { return set(Props.VERTICAL_ALIGN, v); }
    public Style whiteSpace(WhiteSpace w) { return set(Props.WHITE_SPACE, w); }
    public Style nowrap() { return whiteSpace(WhiteSpace.NOWRAP); }
    /** Single line, truncated with "…". */
    public Style ellipsis() { set(Props.TEXT_OVERFLOW, TextOverflow.ELLIPSIS); return nowrap(); }
    /** At most {@code n} lines, truncated with "…". */
    public Style lines(int n) { set(Props.MAX_LINES, n); return set(Props.TEXT_OVERFLOW, n > 0 ? TextOverflow.ELLIPSIS : TextOverflow.CLIP); }
    public Style upper() { return set(Props.TEXT_TRANSFORM, TextTransform.UPPERCASE); }
    public Style lower() { return set(Props.TEXT_TRANSFORM, TextTransform.LOWERCASE); }
    public Style capitalize() { return set(Props.TEXT_TRANSFORM, TextTransform.CAPITALIZE); }
    public Style underline() { return set(Props.TEXT_DECORATION, new TextDecoration(true, false)); }
    public Style strike() { return set(Props.TEXT_DECORATION, new TextDecoration(false, true)); }
    /** Minecraft-style hard text shadow. */
    public Style textShadow(boolean on) { return set(Props.TEXT_SHADOW, on); }
    /** Applies a typography preset (size, weight, line height, tracking...). */
    public Style type(TypeStyle t) { t.applyTo(this); return this; }
    /** Vector icon stroke width (24-unit space). */
    public Style stroke(float w) { return set(Props.STROKE_WIDTH, w); }
    /** Picks the icon drawn by {@code Icon} nodes. */
    public Style icon(String name) { return set(Props.ICON, name); }
    public Style objectFit(ObjectFit f) { return set(Props.OBJECT_FIT, f); }

    // ================================================================== motion

    /** Replaces the transition list: {@code transition(Tr.colors(.12f), Tr.transform(.2f, Easings.OUT_BACK))}. */
    public Style transition(TransitionSpec... specs) { return set(Props.TRANSITION, List.of(specs)); }
    /** Shorthand: these properties over {@code seconds} with an ease curve. */
    public Style transition(float seconds, Easing e, Prop<?>... props) { return transition(Tr.of(seconds, e, props)); }
    /** Appends to the transition list instead of replacing it. */
    public Style alsoTransition(TransitionSpec spec) {
        Object cur = values.get(Props.TRANSITION);
        List<TransitionSpec> list = new ArrayList<>();
        if (cur instanceof List<?> l) for (Object o : l) list.add((TransitionSpec) o);
        list.add(spec);
        return set(Props.TRANSITION, List.copyOf(list));
    }
    public Style noTransition() { return set(Props.TRANSITION, List.of()); }

    /** Runs keyframe animations: {@code animation(SPIN.play(1.6f).infinite())}. */
    public Style animation(AnimationSpec... specs) { return set(Props.ANIMATION, List.of(specs)); }
    public Style noAnimation() { return set(Props.ANIMATION, List.of()); }

    /** Animates position changes caused by layout (items glide to new places). */
    public Style layoutTransition(float seconds, Easing easing) {
        return set(Props.LAYOUT_TRANSITION, List.of(new TransitionSpec(null, seconds, easing, 0)));
    }
}
