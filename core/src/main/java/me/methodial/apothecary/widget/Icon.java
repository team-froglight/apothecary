package me.methodial.apothecary.widget;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Props;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A stroked vector icon (24-unit grid, Lucide-like) drawn with anti-aliased SDF lines and arcs, so it
 * stays crisp at any size or transform. Color = {@code color}, thickness = {@code stroke-width}, the
 * icon can also be chosen by a sheet rule with {@code s.icon("check")}. Register your own with {@link #register}.
 * Type selector: {@code icon}. Default size = font-size.
 */
public class Icon extends Node {
    /** One drawing command in 24-unit space. */
    public sealed interface Shape {
        record Poly(float[] xy, boolean closed) implements Shape { }
        record Arc(float cx, float cy, float r, float start, float sweep) implements Shape { }
        record Dot(float cx, float cy, float r) implements Shape { }
    }

    private static final Map<String, List<Shape>> ICONS = new LinkedHashMap<>();

    private String name;

    public Icon(String name) {
        super("icon");
        this.name = name;
    }

    public Icon() { this(""); }

    public String name() {
        String fromStyle = get(Props.ICON);
        return fromStyle != null && !fromStyle.isEmpty() ? fromStyle : name;
    }

    public Icon name(String n) { this.name = n; return this; }

    public static Set<String> names() { return Collections.unmodifiableSet(ICONS.keySet()); }

    public static void register(String name, List<Shape> shapes) { ICONS.put(name, List.copyOf(shapes)); }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        float s = get(Props.FONT_SIZE);
        return new float[]{s, s};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        List<Shape> shapes = ICONS.get(name());
        if (shapes == null) return;
        ComputedStyle cs = computed();
        float size = Math.min(r.width(), r.height());
        float k = size / 24f;
        float ox = r.x() + (r.width() - size) / 2, oy = r.y() + (r.height() - size) / 2;
        Color col = cs.color();
        float sw = Math.max(0.75f, cs.get(Props.STROKE_WIDTH) * k);
        for (Shape s : shapes) {
            switch (s) {
                case Shape.Poly p -> {
                    float[] xy = p.xy();
                    float[] t = new float[xy.length + (p.closed() ? 2 : 0)];
                    for (int i = 0; i < xy.length; i += 2) { t[i] = ox + xy[i] * k; t[i + 1] = oy + xy[i + 1] * k; }
                    if (p.closed()) { t[xy.length] = t[0]; t[xy.length + 1] = t[1]; }
                    c.drawPolyline(t, sw, col);
                }
                case Shape.Arc a -> c.drawArc(ox + a.cx() * k, oy + a.cy() * k, a.r() * k, sw, a.start(), a.sweep(), col);
                case Shape.Dot d -> c.fillCircle(ox + d.cx() * k, oy + d.cy() * k, d.r() * k, Paint.of(col));
            }
        }
    }

    // ------------------------------------------------------------------ built-in set

    private static Shape poly(float... xy) { return new Shape.Poly(xy, false); }
    private static Shape closed(float... xy) { return new Shape.Poly(xy, true); }
    private static Shape circle(float cx, float cy, float r) { return new Shape.Arc(cx, cy, r, 0, 360); }
    private static Shape arc(float cx, float cy, float r, float start, float sweep) { return new Shape.Arc(cx, cy, r, start, sweep); }
    private static Shape dot(float cx, float cy, float r) { return new Shape.Dot(cx, cy, r); }

    private static void def(String name, Shape... shapes) { ICONS.put(name, List.of(shapes)); }

    private static float[] star(float cx, float cy, float ro, float ri, int points) {
        float[] xy = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = Math.PI * i / points - Math.PI / 2;
            float r = i % 2 == 0 ? ro : ri;
            xy[i * 2] = cx + (float) Math.cos(a) * r;
            xy[i * 2 + 1] = cy + (float) Math.sin(a) * r;
        }
        return xy;
    }

    static {
        def("check", poly(5, 12.5f, 10, 17.5f, 19, 7));
        def("x", poly(6, 6, 18, 18), poly(18, 6, 6, 18));
        def("close", poly(6, 6, 18, 18), poly(18, 6, 6, 18));
        def("plus", poly(12, 5, 12, 19), poly(5, 12, 19, 12));
        def("minus", poly(5, 12, 19, 12));
        def("chevron-down", poly(6, 9, 12, 15, 18, 9));
        def("chevron-up", poly(6, 15, 12, 9, 18, 15));
        def("chevron-left", poly(15, 6, 9, 12, 15, 18));
        def("chevron-right", poly(9, 6, 15, 12, 9, 18));
        def("arrow-right", poly(5, 12, 19, 12), poly(13, 6, 19, 12, 13, 18));
        def("arrow-left", poly(19, 12, 5, 12), poly(11, 6, 5, 12, 11, 18));
        def("arrow-up", poly(12, 19, 12, 5), poly(6, 11, 12, 5, 18, 11));
        def("arrow-down", poly(12, 5, 12, 19), poly(6, 13, 12, 19, 18, 13));
        def("menu", poly(4, 7, 20, 7), poly(4, 12, 20, 12), poly(4, 17, 20, 17));
        def("search", circle(10.5f, 10.5f, 6.5f), poly(15.5f, 15.5f, 20, 20));
        def("home", poly(3.5f, 11, 12, 4, 20.5f, 11), poly(6, 9.5f, 6, 20, 18, 20, 18, 9.5f), poly(10, 20, 10, 14, 14, 14, 14, 20));
        def("info", circle(12, 12, 9), poly(12, 11, 12, 16.5f), dot(12, 7.75f, 1.25f));
        def("alert", closed(12, 3.5f, 21, 19.5f, 3, 19.5f), poly(12, 9.5f, 12, 13.5f), dot(12, 16.5f, 1.2f));
        def("check-circle", circle(12, 12, 9), poly(8, 12.5f, 11, 15.5f, 16.5f, 9.5f));
        def("x-circle", circle(12, 12, 9), poly(9, 9, 15, 15), poly(15, 9, 9, 15));
        def("star", closed(star(12, 12.5f, 9, 4, 5)));
        def("heart", poly(12, 20, 4.5f, 12.5f), arc(8, 9, 4.2f, 225, 180), poly(12, 7, 12, 7), arc(16, 9, 4.2f, 315, 180), poly(19.5f, 12.5f, 12, 20));
        def("user", circle(12, 8, 4), arc(12, 21, 8, 270, 180));
        def("settings", circle(12, 12, 3), circle(12, 12, 7.5f),
                poly(12, 2, 12, 4.5f), poly(12, 19.5f, 12, 22), poly(2, 12, 4.5f, 12), poly(19.5f, 12, 22, 12),
                poly(4.9f, 4.9f, 6.7f, 6.7f), poly(17.3f, 17.3f, 19.1f, 19.1f), poly(4.9f, 19.1f, 6.7f, 17.3f), poly(17.3f, 6.7f, 19.1f, 4.9f));
        def("sun", circle(12, 12, 4), poly(12, 2.5f, 12, 4.5f), poly(12, 19.5f, 12, 21.5f), poly(2.5f, 12, 4.5f, 12), poly(19.5f, 12, 21.5f, 12),
                poly(5.3f, 5.3f, 6.7f, 6.7f), poly(17.3f, 17.3f, 18.7f, 18.7f), poly(5.3f, 18.7f, 6.7f, 17.3f), poly(17.3f, 6.7f, 18.7f, 5.3f));
        def("moon", arc(12, 12, 8.5f, 20, 290), arc(16.5f, 8, 6.2f, 200, 140));
        def("grid", closed(4, 4, 10, 4, 10, 10, 4, 10), closed(14, 4, 20, 4, 20, 10, 14, 10), closed(4, 14, 10, 14, 10, 20, 4, 20), closed(14, 14, 20, 14, 20, 20, 14, 20));
        def("layout", closed(3.5f, 4, 20.5f, 4, 20.5f, 20, 3.5f, 20), poly(3.5f, 9, 20.5f, 9), poly(9.5f, 9, 9.5f, 20));
        def("columns", closed(3.5f, 4, 20.5f, 4, 20.5f, 20, 3.5f, 20), poly(9.5f, 4, 9.5f, 20), poly(14.5f, 4, 14.5f, 20));
        def("layers", closed(12, 3.5f, 21, 8, 12, 12.5f, 3, 8), poly(3, 12, 12, 16.5f, 21, 12), poly(3, 16, 12, 20.5f, 21, 16));
        def("play", closed(8, 5, 19, 12, 8, 19));
        def("pause", poly(9, 5, 9, 19), poly(15, 5, 15, 19));
        def("refresh", arc(12, 12, 8, 30, 300), poly(15.5f, 3.5f, 16, 6.9f, 12.8f, 8));
        def("trash", poly(4, 6.5f, 20, 6.5f), poly(9, 6.5f, 9, 4, 15, 4, 15, 6.5f), poly(6, 6.5f, 7, 20, 17, 20, 18, 6.5f), poly(10, 10.5f, 10, 16), poly(14, 10.5f, 14, 16));
        def("edit", closed(15.5f, 4.5f, 19.5f, 8.5f, 9, 19, 4.5f, 19.5f, 5, 15), poly(13.5f, 6.5f, 17.5f, 10.5f));
        def("eye", arc(12, 18, 10, 300, 120), arc(12, 6, 10, 120, 120), circle(12, 12, 3));
        def("code", poly(9, 7, 4, 12, 9, 17), poly(15, 7, 20, 12, 15, 17));
        def("zap", closed(13, 2.5f, 4.5f, 13.5f, 11.5f, 13.5f, 10.5f, 21.5f, 19.5f, 10, 12.5f, 10));
        def("droplet", poly(12, 3, 6.6f, 11.2f), arc(12, 14.5f, 6.5f, 236, 248), poly(17.4f, 11.2f, 12, 3));
        def("flask", poly(9.5f, 3.5f, 14.5f, 3.5f), poly(10.5f, 3.5f, 10.5f, 9.5f, 4.8f, 18.4f, 5.8f, 20.5f, 18.2f, 20.5f, 19.2f, 18.4f, 13.5f, 9.5f, 13.5f, 3.5f), poly(7.2f, 15, 16.8f, 15));
        def("leaf", poly(5, 19, 19, 5), arc(12.5f, 11.5f, 7.5f, 315, 180), poly(5.3f, 16.8f, 5, 19));
        def("sparkles", closed(10, 3, 11.6f, 8.4f, 17, 10, 11.6f, 11.6f, 10, 17, 8.4f, 11.6f, 3, 10, 8.4f, 8.4f), poly(18, 14, 18, 20), poly(15, 17, 21, 17));
        def("sliders", poly(4, 7, 20, 7), poly(4, 12, 20, 12), poly(4, 17, 20, 17), dot(9, 7, 2.2f), dot(15, 12, 2.2f), dot(7, 17, 2.2f));
        def("type", poly(5, 7, 5, 4.5f, 19, 4.5f, 19, 7), poly(12, 4.5f, 12, 19.5f), poly(9, 19.5f, 15, 19.5f));
        def("box", closed(12, 3, 20.5f, 7.5f, 20.5f, 16.5f, 12, 21, 3.5f, 16.5f, 3.5f, 7.5f), poly(3.5f, 7.5f, 12, 12, 20.5f, 7.5f), poly(12, 12, 12, 21));
        def("bell", poly(6, 16.5f, 6, 10.5f), arc(12, 10.5f, 6, 270, 180), poly(18, 10.5f, 18, 16.5f), poly(4.5f, 16.5f, 19.5f, 16.5f), arc(12, 18.5f, 2.2f, 90, 180));
        def("palette", arc(12, 12, 9, 0, 300), poly(16.5f, 19.8f, 15.5f, 16.5f, 18.5f, 15, 20.8f, 13.6f),
                dot(8, 10, 1.3f), dot(12, 7.5f, 1.3f), dot(16, 10, 1.3f), dot(8.5f, 14.5f, 1.3f));
        def("cursor", closed(5, 3.5f, 18.5f, 12, 12.5f, 13.5f, 9.5f, 19.5f));
        def("move", poly(12, 3, 12, 21), poly(3, 12, 21, 12), poly(9, 6, 12, 3, 15, 6), poly(9, 18, 12, 21, 15, 18), poly(6, 9, 3, 12, 6, 15), poly(18, 9, 21, 12, 18, 15));
        def("bolt-circle", circle(12, 12, 9), closed(12.5f, 6.5f, 8.5f, 12.5f, 12, 12.5f, 11.5f, 17.5f, 15.5f, 11.5f, 12, 11.5f));
        def("image", closed(3.5f, 5, 20.5f, 5, 20.5f, 19, 3.5f, 19), circle(8.5f, 9.5f, 1.6f), poly(3.5f, 17, 9, 12, 13, 15.5f, 16, 13, 20.5f, 17));
        def("dot", dot(12, 12, 4));
        def("circle", circle(12, 12, 8));
        def("wave", poly(2, 12, 4, 8, 6, 12, 8, 16, 10, 12, 12, 8, 14, 12, 16, 16, 18, 12, 20, 8, 22, 12));
        def("activity", poly(3, 12, 7, 12, 10, 5, 14, 19, 17, 12, 21, 12));
        def("clock", circle(12, 12, 9), poly(12, 7, 12, 12, 15.5f, 14));
        def("copy", closed(8, 8, 20, 8, 20, 20, 8, 20), poly(16, 8, 16, 4, 4, 4, 4, 16, 8, 16));
        def("external", poly(14, 4, 20, 4, 20, 10), poly(20, 4, 11, 13), poly(17, 14, 17, 20, 4, 20, 4, 7, 10, 7));
        def("filter", closed(3.5f, 4.5f, 20.5f, 4.5f, 14, 12.5f, 14, 19.5f, 10, 17.5f, 10, 12.5f));
        def("lock", closed(5, 11, 19, 11, 19, 20.5f, 5, 20.5f), poly(8, 11, 8, 7.5f), arc(12, 7.5f, 4, 270, 180), poly(16, 7.5f, 16, 11));
        def("download", poly(12, 4, 12, 15), poly(7, 10, 12, 15, 17, 10), poly(4, 20, 20, 20));
    }
}
