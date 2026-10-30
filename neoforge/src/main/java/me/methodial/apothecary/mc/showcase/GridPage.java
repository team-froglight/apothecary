package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Align;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.GridAutoFlow;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.theme.Mixins;
import me.methodial.apothecary.widget.Select;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Switch;
import me.methodial.apothecary.widget.Text;

import java.util.function.Consumer;

import static me.methodial.apothecary.mc.showcase.Demo.*;
import static me.methodial.apothecary.style.TrackSize.*;

final class GridPage {
    private GridPage() { }

    private static Node cell(String label, int color, Consumer<Style> extra) {
        Color bg = Color.hex(PALETTE[color % PALETTE.length]);
        return new Node("grid-cell").style(s -> s.bg(bg).apply(extra)).add(new Text(label));
    }

    private static Node cell(String label, int color) { return cell(label, color, s -> { }); }

    static Node build(ShowcaseScreen app) {
        // ---- template areas
        Node areas = new Node("grid-demo").style(st -> st.cols(px(60), fr(1), fr(1)).rows(auto(), px(40), px(30), auto())
                .areas("head head head", "side main main", "side a b", "foot foot foot"));
        String[][] parts = {{"head", "header"}, {"side", "sidebar"}, {"main", "main"}, {"a", "stat"}, {"b", "stat"}, {"foot", "footer"}};
        for (int i = 0; i < parts.length; i++) {
            String area = parts[i][0];
            areas.add(cell(parts[i][1], i, st -> st.area(area)));
        }

        // ---- responsive auto-fill
        Node gallery = new Node("grid-demo");
        for (int i = 0; i < 18; i++) gallery.add(cell(String.valueOf(i + 1), i, st -> st.h(26)));
        Slider min = new Slider(20, 90, 36).step(1).showValue(v -> Math.round(v) + "px");
        Switch fit = new Switch("fit() instead of fill()", false);
        Text code = code("");
        Runnable applyGallery = () -> {
            int m = Math.round(min.value());
            gallery.style(st -> st.cols(fit.isOn() ? fit(minmax(px(m), fr(1))) : fill(minmax(px(m), fr(1)))));
            code.text("s.cols(" + (fit.isOn() ? "fit" : "fill") + "(minmax(px(" + m + "), fr(1))))");
        };
        min.onChange(v -> applyGallery.run());
        fit.onChange(v -> applyGallery.run());
        applyGallery.run();

        // ---- spans & dense packing
        Node spans = new Node("grid-demo").style(st -> st.cols(5).autoRows(px(22)));
        int[][] spec = {{2, 1}, {1, 1}, {1, 2}, {1, 1}, {3, 1}, {1, 1}, {1, 1}, {2, 1}, {1, 1}, {2, 2}, {1, 1}, {1, 1}};
        for (int i = 0; i < spec.length; i++) {
            int cs = spec[i][0], rs = spec[i][1];
            String label = cs == 1 && rs == 1 ? String.valueOf(i + 1) : "colSpan " + cs + (rs > 1 ? " · rowSpan " + rs : "");
            spans.add(cell(label, i, st -> st.colSpan(cs).rowSpan(rs)));
        }
        Switch dense = new Switch("dense()", false);
        dense.onChange(on -> spans.style().set(Props.GRID_AUTO_FLOW, on ? GridAutoFlow.ROW_DENSE : GridAutoFlow.ROW));

        // ---- item alignment
        Node align = new Node("grid-demo").style(st -> st.cols(3).rows(px(34), px(34)).pad(4).apply(Mixins.WELL));
        for (int i = 0; i < 6; i++) align.add(cell(String.valueOf(i + 1), i + 2, st -> st.minH(12).minW(16)));
        Select<Align> ji = alignSelect(), ai = alignSelect();
        ji.onChange(v -> align.style(st -> st.justifyItems(v)));
        ai.onChange(v -> align.style(st -> st.items(v)));

        return page().add(
                section("Template areas", "areas(...) names regions; children just call area(\"main\").",
                        card(null, null, areas, code("s.cols(px(60), fr(1), fr(1)).areas(\"head head head\", \"side main main\", \"side a b\", \"foot foot foot\")"))),
                section("Responsive tracks", "fill(minmax(...)) picks the column count from the available width.",
                        card(null, null, control("min width", min.style(st -> st.w(120))), fit, gallery, code)),
                section("Spans & packing", "Items can span tracks; dense() back-fills holes.",
                        card(null, null, dense, spans)),
                section("Alignment", "justifyItems / items position items inside their cells.",
                        card(null, null, row(control("justify", ji), control("align", ai)), align)));
    }

    private static Select<Align> alignSelect() {
        Select<Align> s = new Select<>();
        for (Align a : new Align[]{Align.STRETCH, Align.START, Align.CENTER, Align.END}) s.option(a, a.name().toLowerCase());
        return s;
    }
}
