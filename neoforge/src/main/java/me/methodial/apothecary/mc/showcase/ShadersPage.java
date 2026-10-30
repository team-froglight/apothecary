package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.Transform;
import me.methodial.apothecary.widget.ColorPicker;
import me.methodial.apothecary.widget.SegmentedControl;
import me.methodial.apothecary.widget.ShaderView;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.style.Style;
import static me.methodial.apothecary.style.TrackSize.*;

import java.util.List;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class ShadersPage {
    private ShadersPage() { }

    private static Node tile(String name, ShaderView view) {
        return new Node("effect-tile").add(view, new Text(name).cls("effect-name"));
    }

    static Node build(ShowcaseScreen app) {
        Node effects = new Node().style(st -> st.cols(fill(minmax(px(110), fr(1)))).gap(6));
        effects.add(
                tile("aurora", new ShaderView("apothecary:aurora").color(0, Color.parse("#4fd8c4")).color(1, Color.parse("#8b7bff"))
                        .color(2, Color.parse("#ff7ac8")).color(3, Color.parse("#07060f")).param(1, 0.04f)),
                tile("plasma", new ShaderView("apothecary:plasma").param(0, 3).param(1, 1)),
                tile("mesh gradient", new ShaderView("apothecary:mesh_gradient").color(0, Color.parse("#ff7a90")).color(1, Color.parse("#8b7bff"))
                        .color(2, Color.parse("#4fd8c4")).color(3, Color.parse("#ffe073"))),
                tile("ripple · hover me", new ShaderView("apothecary:ripple").color(0, Color.parse("#0f2a4a")).color(1, Color.parse("#1c6b8a"))
                        .color(2, Color.parse("#bff4ff"))),
                tile("voronoi", new ShaderView("apothecary:voronoi").color(0, Color.parse("#24204a")).color(1, Color.parse("#8b7bff")).param(0, 4)),
                tile("starfield", new ShaderView("apothecary:starfield").color(0, Color.parse("#6b3fa0")).color(1, Color.parse("#05040c"))));

        // ---- SDF playground
        Node shape = new Node().style(st -> st.size(90, 60));
        Slider radius = new Slider(0, 30, 10).step(0.5f).showValue(v -> Math.round(v) + "px");
        Slider border = new Slider(0, 8, 1.5f).step(0.5f).showValue(null);
        Slider blur = new Slider(0, 40, 16).step(1).showValue(null);
        Slider offset = new Slider(-12, 12, 6).step(1).showValue(null);
        Slider rotate = new Slider(-45, 45, 0).step(1).showValue(v -> Math.round(v) + "°");
        SegmentedControl<String> fill = new SegmentedControl<String>().option("solid", "solid").option("linear", "linear").option("radial", "radial");
        ColorPicker picker = new ColorPicker(Color.parse("#8b7bff"));
        Runnable apply = () -> {
            Color c = picker.value();
            Paint p = switch (fill.value()) {
                case "linear" -> Paint.linear(135, c, c.lerp(Color.parse("#4fd8c4"), 0.8f));
                case "radial" -> new Paint.Radial(0.3f, 0.25f, 1f, List.of(new Paint.Stop(c.lighten(0.4f), 0), new Paint.Stop(c, 0.45f), new Paint.Stop(c.lighten(-0.6f), 1)));
                default -> Paint.of(c);
            };
            shape.style().set(Props.BACKGROUND, p);
            shape.style().set(Props.BORDER_RADIUS, Corners.all(radius.value()));
            shape.style().borderW(border.value()).set(Props.BORDER_COLOR, Color.parse("#ffffffaa"));
            shape.style().set(Props.BOX_SHADOW, List.of(
                    new Shadow(0, offset.value(), blur.value(), 0, c.withAlpha(0.85f), false),
                    new Shadow(0, 0, 0, 0, Color.TRANSPARENT, true)));
            shape.style().set(Props.TRANSFORM, Transform.rotate(rotate.value()));
        };
        for (Slider sl : new Slider[]{radius, border, blur, offset, rotate}) sl.onChange(v -> apply.run());
        fill.onChange(v -> apply.run());
        picker.onChange(v -> apply.run());
        apply.run();

        Node stage = new Node("sdf-stage").add(shape);
        Node controls = new Node().style(st -> st.col().gap(4).grow()).add(
                control("radius", radius.style(Style::grow)), control("border", border.style(Style::grow)),
                control("shadow blur", blur.style(Style::grow)), control("offset", offset.style(Style::grow)),
                control("rotate", rotate.style(Style::grow)), control("fill", fill));
        Node playground = card("Signed distance fields", "One shader draws every box: per-corner radii, inner borders, "
                        + "gradients and Gaussian shadows, anti-aliased at any scale or rotation",
                new Node().style(st -> st.flex().gap(8).itemsStart()).add(
                        new Node().style(st -> st.col().gap(6).grow()).add(stage, controls),
                        picker));

        // ---- primitives
        Node prims = new Node().style(st -> st.row().gap(10).h(60)).add(new Primitives());

        return page().add(
                section("Effect shaders", "ShaderView runs any registered effect; uniforms: Size, Time, Mouse, Params, Colors.", effects),
                section("SDF playground", null, playground),
                section("Primitives", "Lines, polylines and arcs are SDF capsules and rings.", card(null, null, prims)),
                section("Write your own", null, card(null, null,
                        code("ApothecaryShaders.registerEffect(ResourceLocation.parse(\"mymod:fire\"));"),
                        code("// assets/mymod/shaders/core/fire.fsh:  #moj_import <apothecary:effect.glsl>  …  effectOut(color);"),
                        code("new ShaderView(\"mymod:fire\").params(p -> p.param(0, intensity));"))));
    }

    /** Draws a few primitives directly on the canvas. */
    static final class Primitives extends Node {
        Primitives() {
            super("primitives");
            style(st -> st.grow().h(60));
        }

        @Override
        protected void paintContent(me.methodial.apothecary.render.Canvas c, me.methodial.apothecary.geom.Rect r) {
            float t = root() == null ? 0 : root().time();
            float cy = r.centerY();
            // arcs: a gauge and a spinner-like sweep
            c.drawArc(r.x() + 24, cy, 18, 4, 0, 360, Color.parse("#ffffff14"));
            c.drawArc(r.x() + 24, cy, 18, 4, 225, 270 * (0.5f + 0.5f * (float) Math.sin(t)), Color.parse("#8b7bff"));
            c.drawArc(r.x() + 70, cy, 14, 2, t * 200, 90, Color.parse("#4fd8c4"));
            c.drawArc(r.x() + 70, cy, 8, 2, -t * 260, 200, Color.parse("#ff7ac8"));
            // sine polyline
            int n = 60;
            float[] pts = new float[n * 2];
            float x0 = r.x() + 100, w = Math.max(40, r.width() - 110);
            for (int i = 0; i < n; i++) {
                float k = i / (float) (n - 1);
                pts[i * 2] = x0 + k * w;
                pts[i * 2 + 1] = cy + (float) Math.sin(k * 12 + t * 3) * 12 * (float) Math.sin(k * 3.1416f);
            }
            c.drawPolyline(pts, 2f, Color.parse("#ffb86b"));
            // star burst of thin lines
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 + t * 0.5;
                float len = 10 + 4 * (float) Math.sin(t * 2 + i);
                c.drawLine(x0 + w / 2, cy, x0 + w / 2 + (float) Math.cos(a) * len, cy + (float) Math.sin(a) * len, 0.75f, Color.parse("#ffffff66"));
            }
        }
    }
}
