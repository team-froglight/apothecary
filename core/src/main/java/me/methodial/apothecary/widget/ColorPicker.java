package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Props;

/**
 * An HSV color picker drawn with shaders: a saturation/value square ({@code apothecary:hsv_square}),
 * a hue strip ({@code apothecary:hue_strip}), an alpha strip ({@code apothecary:alpha_strip}),
 * a swatch and a hex field. Structure: {@code color-picker > picker-sv > picker-knob}, {@code picker-hue > picker-bar},
 * {@code picker-alpha > picker-bar}, {@code picker-row > (picker-swatch + textfield)}.
 */
public class ColorPicker extends ValueWidget<Color> {
    private float hue, sat, val, alpha = 1;
    private final ShaderView sv, hueStrip, alphaStrip;
    private final Node svKnob, hueBar, alphaBar, swatch;
    private final TextField hex;
    private boolean editingHex;

    public ColorPicker(Color initial) {
        super("color-picker", initial);
        sv = new ShaderView("apothecary:hsv_square");
        sv.cls("picker-sv");
        svKnob = new Node("picker-knob");
        sv.add(svKnob);
        hueStrip = new ShaderView("apothecary:hue_strip");
        hueStrip.cls("picker-hue");
        hueBar = new Node("picker-bar");
        hueStrip.add(hueBar);
        alphaStrip = new ShaderView("apothecary:alpha_strip");
        alphaStrip.cls("picker-alpha");
        alphaBar = new Node("picker-bar");
        alphaStrip.add(alphaBar);
        swatch = new Node("picker-swatch");
        hex = new TextField(initial.toHex());
        hex.maxLength(9).filter(s -> s.matches("#?[0-9a-fA-F]{0,8}"));
        hex.onChange(s -> {
            if (editingHex) return;
            try {
                if (s.matches("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) {
                    editingHex = true;
                    value(Color.parse(s.startsWith("#") ? s : "#" + s));
                }
            } catch (RuntimeException ignored) {
            } finally {
                editingHex = false;
            }
        });
        Node row = new Node("picker-row").add(swatch, hex);
        add(sv, hueStrip, alphaStrip, row);

        sv.params(p -> p.param(0, hue));
        alphaStrip.params(p -> p.color(0, Color.hsv(hue, sat, val, 1)));

        dragArea(sv, (fx, fy) -> { sat = fx; val = 1 - fy; commit(); });
        dragArea(hueStrip, (fx, fy) -> { hue = fx * 360; commit(); });
        dragArea(alphaStrip, (fx, fy) -> { alpha = fx; commit(); });
        fromColor(initial);
        refresh();
    }

    private interface Pick { void at(float fx, float fy); }

    private void dragArea(Node area, Pick pick) {
        java.util.function.Consumer<MouseEvent> handler = e -> {
            float fx = Math.max(0, Math.min(1, e.localX() / Math.max(1, area.width())));
            float fy = Math.max(0, Math.min(1, e.localY() / Math.max(1, area.height())));
            pick.at(fx, fy);
        };
        area.on(EventType.MOUSE_DOWN, handler);
        area.on(EventType.DRAG, handler);
        area.baseStyle(s -> s.cursor(me.methodial.apothecary.style.Cursor.CROSSHAIR));
    }

    private void fromColor(Color c) {
        float[] hsv = c.toHsv();
        if (hsv[1] > 0.001f && hsv[2] > 0.001f) hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        alpha = c.a();
    }

    private void commit() {
        setValue(Color.hsv(hue, sat, val, alpha), true);
        refresh();
    }

    private void refresh() {
        svKnob.style().set(Props.LEFT, Length.percent(sat * 100));
        svKnob.style().set(Props.TOP, Length.percent((1 - val) * 100));
        hueBar.style().set(Props.LEFT, Length.percent(hue / 3.6f));
        alphaBar.style().set(Props.LEFT, Length.percent(alpha * 100));
        swatch.style().set(Props.BACKGROUND, Paint.of(value));
        if (!editingHex) {
            editingHex = true;
            hex.text(value.toHex());
            editingHex = false;
        }
    }

    @Override
    protected void valueChanged(Color old, Color now) {
        if (now == null) return;
        Color mine = Color.hsv(hue, sat, val, alpha);
        if (!mine.equals(now)) fromColor(now);
        if (svKnob != null) refresh();
    }
}
