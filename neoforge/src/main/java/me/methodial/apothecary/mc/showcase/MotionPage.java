package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.anim.Spring;
import me.methodial.apothecary.anim.Timeline;
import me.methodial.apothecary.anim.Tween;
import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Transform;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Icon;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.theme.Tokens;
import static me.methodial.apothecary.style.TrackSize.*;


import static me.methodial.apothecary.mc.showcase.Demo.*;

final class MotionPage {
    private MotionPage() { }

    /** Plots an easing curve and runs a dot along it. */
    static final class EasingGraph extends Node {
        private final Easing easing;
        private final float offset;

        EasingGraph(Easing e, float offset) {
            super("easing-graph");
            this.easing = e;
            this.offset = offset;
        }

        @Override
        protected void paintContent(Canvas c, Rect r) {
            float pad = 4;
            float x0 = r.x() + pad, x1 = r.right() - pad, y0 = r.bottom() - pad - 4, y1 = r.y() + pad + 4;
            Color grid = Color.parse("#ffffff14");
            c.drawLine(x0, y0, x1, y0, 0.75f, grid);
            c.drawLine(x0, y1, x1, y1, 0.75f, grid);
            int n = 48;
            float[] pts = new float[(n + 1) * 2];
            for (int i = 0; i <= n; i++) {
                float t = i / (float) n;
                pts[i * 2] = x0 + (x1 - x0) * t;
                pts[i * 2 + 1] = y0 + (y1 - y0) * easing.apply(t);
            }
            c.drawPolyline(pts, 1.25f, Color.parse("#a194ff"));
            float time = root() == null ? 0 : root().time();
            float cycle = ((time + offset) % 2.2f) / 1.6f;
            float t = Math.min(1, cycle);
            float px = x0 + (x1 - x0) * t, py = y0 + (y1 - y0) * easing.apply(t);
            c.fillCircle(px, py, 2.4f, Paint.of(Color.parse("#4fd8c4")));
            // a bar on the right moving with the eased value
            c.fillRoundRect(new Rect(x1 + 1, py - 1, 2, 2), 1, Paint.of(Color.parse("#4fd8c4aa")));
        }
    }

    static Node build(ShowcaseScreen app) {
        // ---- easing gallery
        Node gallery = new Node().style(st -> st.cols(fill(minmax(px(78), fr(1)))).gap(5));
        String[] names = {"linear", "ease", "ease-in-out", "ease-out-cubic", "ease-in-out-quint", "ease-out-expo",
                "ease-out-circ", "ease-out-back", "ease-in-out-back", "ease-out-elastic", "ease-out-bounce", "spring-bouncy"};
        int i = 0;
        for (String n : names) {
            gallery.add(new Node("easing-cell").add(new EasingGraph(Easings.byName(n), i++ * 0.09f), new Text(n).cls("easing-name")));
        }
        gallery.add(new Node("easing-cell").add(new EasingGraph(Easings.steps(5), 0.3f), new Text("steps(5)").cls("easing-name")));
        gallery.add(new Node("easing-cell").add(new EasingGraph(Easings.cubicBezier(0.7f, -0.4f, 0.4f, 1.4f), 0.5f), new Text("cubic-bezier(.7,-.4,.4,1.4)").cls("easing-name")));

        // ---- spring playground
        Node stage = new Node("spring-stage");
        Node ball = new Node("spring-ball");
        stage.add(ball);
        Spring sx = new Spring(0), sy = new Spring(0);
        Slider stiff = new Slider(20, 600, 180).step(1).showValue(v -> "k " + Math.round(v));
        Slider damp = new Slider(1, 40, 9).step(0.5f).showValue(v -> "c " + String.format("%.1f", v));
        boolean[] dragging = {false};
        float[] grab = {0, 0};
        Runnable home = () -> {
            sx.target(stage.width() / 2 - 9);
            sy.target(stage.height() / 2 - 9);
        };
        stage.on(EventType.MOUNT, e -> {
            app.root().animator().add(dt -> {
                if (!stage.isAttached()) return false;
                sx.stiffness(stiff.value()).damping(damp.value());
                sy.stiffness(stiff.value()).damping(damp.value());
                if (!dragging[0]) {
                    home.run();
                    sx.tick(dt);
                    sy.tick(dt);
                }
                ball.style().set(Props.LEFT, Length.px(sx.value()));
                ball.style().set(Props.TOP, Length.px(sy.value()));
                float speed = (float) Math.hypot(sx.velocity(), sy.velocity());
                float stretch = Math.min(0.35f, speed / 1200f);
                float ang = (float) Math.toDegrees(Math.atan2(sy.velocity(), sx.velocity()));
                ball.style().set(Props.TRANSFORM, Transform.IDENTITY.withRotate(ang).withScale(1 + stretch, 1 - stretch * 0.6f));
                return true;
            });
        });
        ball.on(EventType.MOUSE_DOWN, e -> {
            dragging[0] = true;
            grab[0] = e.localX();
            grab[1] = e.localY();
        });
        ball.on(EventType.DRAG, e -> {
            float[] l = stage.toLocal(e.x(), e.y());
            float nx = l[0] - 9, ny = l[1] - 9;
            // follow the pointer and remember a smoothed velocity for the throw
            sx.set(nx, sx.velocity() * 0.5f + (nx - sx.value()) * 30);
            sy.set(ny, sy.velocity() * 0.5f + (ny - sy.value()) * 30);
        });
        ball.on(EventType.DRAG_END, e -> dragging[0] = false);
        ball.on(EventType.MOUSE_UP, e -> dragging[0] = false);
        Node springCard = card("Spring physics", "Drag and throw the ball. Springs keep velocity when retargeted.",
                stage, control("stiffness", stiff.style(st -> st.w(120))), control("damping", damp.style(st -> st.w(120))),
                row(new Button("Wobbly").cls("sm").action(() -> { stiff.value(120f); damp.value(4f); }),
                        new Button("Gentle").cls("sm").action(() -> { stiff.value(170f); damp.value(26f); }),
                        new Button("Snappy").cls("sm").action(() -> { stiff.value(520f); damp.value(30f); }),
                        new Button("Kick").cls("sm").primary().action(() -> { sx.kick(900 * (Math.random() > 0.5 ? 1 : -1)); sy.kick(-700); })));

        // ---- keyframes
        String[][] kf = {{"a-pulse", "heart", "pulse"}, {"a-spin", "settings", "spin"}, {"a-bounce", "arrow-down", "bounce"},
                {"a-shake", "bell", "shake"}, {"a-float", "leaf", "float"}, {"a-wobble", "droplet", "wobble"},
                {"a-hue", "star", "hue"}, {"a-flip", "zap", "flip"}};
        Node kfGrid = new Node().style(st -> st.cols(fill(minmax(px(48), fr(1)))).gap(5));
        for (String[] k : kf) kfGrid.add(new Node().cls("anim-tile", k[0]).add(new Icon(k[1]), new Text(k[2]).cls("easing-name")));

        // ---- transitions
        Node trans = new Node().style(st -> st.cols(3).gap(6));
        trans.add(transitionTile("lift", "Lift", "transform + shadow"), transitionTile("glow", "Glow", "box-shadow ring"),
                transitionTile("morph", "Morph", "radius + gradient"));

        // ---- timeline / stagger
        Node bars = new Node().style(st -> st.col().gap(3));
        Runnable replay = () -> {
            bars.clear();
            Timeline tl = Timeline.sequence();
            tl.stagger(8, 0.06f, k -> {
                Node bar = new Node("stagger-bar").style(st -> st.w(0).opacity(0));
                bars.add(bar);
                float target = 40 + (float) Math.random() * 160;
                return Tween.of(0, 1, 0.7f).easing(Easings.OUT_BACK).onUpdate(v -> {
                    bar.style().set(Props.WIDTH, Length.px(Math.max(0, target * v)));
                    bar.style().set(Props.OPACITY, Math.min(1, v * 2));
                });
            });
            tl.start(app.root().animator());
        };
        bars.on(EventType.MOUNT, e -> replay.run());
        Node timeline = card("Timelines & stagger", "Timeline.sequence().stagger(8, 0.06s, …) with ease-out-back tweens",
                bars, row(new Button("refresh", "Replay").cls("sm").action(replay)));

        return page().add(
                section("Easing functions", "Standard curves, Penner's equations, cubic-bezier(), steps() and springs.", gallery),
                new Node().cls("cards-wide").add(springCard,
                        card("Keyframe animations", "Keyframes.of(...).at(..) played with .play(1s).infinite().alternate()", kfGrid),
                        card("Transitions", "Hover the tiles — properties interpolate with their own easing", trans),
                        timeline));
    }

    private static Node transitionTile(String cls, String title, String desc) {
        return new Node().cls(cls).style(s -> s.col().center().gap(2).h(54).radius(6).bg(Tokens.SURFACE).border(1, Tokens.BORDER).pointer())
                .add(new Text(title).style(Style::semibold), new Text(desc).cls("faint").style(s -> s.fontSize(6)));
    }
}
