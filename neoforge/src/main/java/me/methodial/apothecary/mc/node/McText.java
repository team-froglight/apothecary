package me.methodial.apothecary.mc.node;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.TextAlign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Vanilla rich text: a {@link Component} with its own colors, formatting, fonts, translations and
 * click-less hover styling, wrapped to the box width. Scaled by {@code font-size} (9 = vanilla size);
 * the style color ({@code fg}) is used where the component has none. Type selector: {@code text}.
 */
public class McText extends Node {
    private Component text;

    public McText(Component text) {
        super("text");
        this.text = text;
    }

    public McText text(Component c) {
        this.text = c;
        markLayoutDirty();
        return this;
    }

    @Override public boolean isLeaf() { return true; }

    /** Same pixel-snapped scale the canvas draws vanilla text with. */
    private float scale() { return me.methodial.apothecary.mc.render.McFonts.vanillaScale(get(Props.FONT_SIZE), me.methodial.apothecary.mc.render.McFonts.INSTANCE.deviceScale()); }

    private List<FormattedCharSequence> lines(float width) {
        Font f = Minecraft.getInstance().font;
        int w = Float.isNaN(width) ? Integer.MAX_VALUE / 4 : Math.max(1, (int) Math.floor(width / scale()));
        return f.split(text, w);
    }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        Font f = Minecraft.getInstance().font;
        List<FormattedCharSequence> ls = lines(availWidth);
        float w = 0;
        for (FormattedCharSequence l : ls) w = Math.max(w, f.getSplitter().stringWidth(l));
        float lh = get(Props.FONT_SIZE) * get(Props.LINE_HEIGHT);
        return new float[]{(float) Math.ceil(w * scale()), Math.max(1, ls.size()) * lh};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        McCanvas mc = c.backend(McCanvas.class);
        if (mc == null) return;
        ComputedStyle cs = computed();
        float s = scale();
        float lh = cs.get(Props.FONT_SIZE) * cs.get(Props.LINE_HEIGHT);
        int alpha = Math.round(cs.color().a() * c.opacity() * 255);
        if (alpha < 5) return;
        int argb = (alpha << 24) | (cs.color().argb() & 0xFFFFFF);
        boolean shadow = cs.get(Props.TEXT_SHADOW);
        TextAlign align = cs.get(Props.TEXT_ALIGN);
        Font f = Minecraft.getInstance().font;
        List<FormattedCharSequence> ls = lines(r.width());
        float fs = cs.get(Props.FONT_SIZE);
        float y = r.y() + (lh - fs) / 2;
        for (FormattedCharSequence l : ls) {
            float w = f.getSplitter().stringWidth(l) * s;
            float x = align == TextAlign.CENTER ? r.x() + (r.width() - w) / 2 : align == TextAlign.RIGHT ? r.right() - w : r.x();
            mc.drawVanilla(l, x, y, fs, 0, argb, shadow);
            y += lh;
        }
        mc.afterVanilla3D();
    }
}
