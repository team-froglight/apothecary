package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.state.Signal;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Prop;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.TextAlign;
import me.methodial.apothecary.style.TextOverflow;
import me.methodial.apothecary.style.VerticalAlign;
import me.methodial.apothecary.text.TextLayout;

/**
 * A block of text. Font, size, color, alignment, wrapping, max lines and ellipsis all come from styles
 * (inherited from ancestors unless set here). Type selector: {@code text}.
 */
public class Text extends Node {
    private String text;
    private TextLayout.Result cached;
    private float cachedWidth = -2;
    private Signal.Subscription binding;

    public Text() { this(""); }

    public Text(String text) {
        super("text");
        this.text = text == null ? "" : text;
    }

    // Covariant overrides keep chains typed: Text t = new Text("x").cls("muted");
    @Override public Text cls(String... names) { super.cls(names); return this; }
    @Override public Text style(java.util.function.Consumer<me.methodial.apothecary.style.Style> edit) { super.style(edit); return this; }

    public String text() { return text; }

    public Text text(String t) {
        if (t == null) t = "";
        if (!t.equals(text)) {
            text = t;
            cached = null;
            markLayoutDirty();
        }
        return this;
    }

    /** Shows the signal's value (via toString) and follows changes while attached. */
    public Text bind(Signal<?> s) {
        on(EventType.MOUNT, e -> { if (binding == null) binding = s.observe(v -> text(String.valueOf(v))); });
        on(EventType.UNMOUNT, e -> { if (binding != null) { binding.cancel(); binding = null; } });
        if (isAttached() && binding == null) binding = s.observe(v -> text(String.valueOf(v)));
        else text(String.valueOf(s.get()));
        return this;
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected void onStyleChanged(Prop<?> p) {
        if (p.affects() == Prop.Affects.LAYOUT) cached = null;
    }

    private TextMeasurer measurer() { return root() != null ? root().platform().text() : (t, f) -> t.length() * 6f; }

    private float lineHeight(ComputedStyle cs) {
        return measurer().lineHeight(cs.font()) * cs.get(Props.LINE_HEIGHT);
    }

    protected TextLayout.Result layoutText(float maxWidth) {
        ComputedStyle cs = computed();
        if (cs == null) return new TextLayout.Result(java.util.List.of(), 0, 0);
        float key = Float.isNaN(maxWidth) ? -1 : maxWidth;
        if (cached != null && Math.abs(cachedWidth - key) < 0.01f) return cached;
        String t = TextLayout.transform(text, cs.get(Props.TEXT_TRANSFORM));
        cached = TextLayout.layout(measurer(), t, cs.font(), lineHeight(cs), maxWidth, cs.get(Props.WHITE_SPACE),
                cs.get(Props.MAX_LINES), cs.get(Props.TEXT_OVERFLOW) == TextOverflow.ELLIPSIS);
        cachedWidth = key;
        return cached;
    }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        TextLayout.Result r = layoutText(availWidth);
        return new float[]{(float) Math.ceil(r.width()), r.height()};
    }

    @Override
    protected void paintContent(Canvas c, Rect content) {
        ComputedStyle cs = computed();
        if (text.isEmpty()) return;
        TextLayout.Result r = layoutText(content.width());
        FontSpec font = cs.font();
        TextAlign align = cs.get(Props.TEXT_ALIGN);
        VerticalAlign va = cs.get(Props.VERTICAL_ALIGN);
        float lh = r.lineHeight();
        float glyphH = measurer().lineHeight(font);
        float y = content.y();
        if (va == VerticalAlign.MIDDLE) y += (content.height() - r.height()) / 2;
        else if (va == VerticalAlign.BOTTOM) y += content.height() - r.height();
        boolean clip = r.width() > content.width() + 0.5f && cs.get(Props.TEXT_OVERFLOW) == TextOverflow.CLIP;
        if (clip) c.pushClip(new Rect(content.x(), content.y() - 2, content.width(), content.height() + 4));
        for (TextLayout.Line line : r.lines()) {
            float x = switch (align) {
                case CENTER -> content.x() + (content.width() - line.width()) / 2;
                case RIGHT -> content.x() + content.width() - line.width();
                default -> content.x();
            };
            c.drawText(line.text(), x, y + (lh - glyphH) / 2, font, cs.color(), cs.get(Props.TEXT_SHADOW), cs.get(Props.TEXT_DECORATION));
            y += lh;
        }
        if (clip) c.popClip();
    }
}
