package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.theme.Theme;
import me.methodial.apothecary.theme.Themes;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.theme.Tokens;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Checkbox;
import me.methodial.apothecary.widget.ColorPicker;
import me.methodial.apothecary.widget.ProgressBar;
import me.methodial.apothecary.widget.Select;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Switch;
import me.methodial.apothecary.widget.Text;

import java.util.ArrayList;
import java.util.List;

import static me.methodial.apothecary.mc.showcase.Demo.*;
import static me.methodial.apothecary.style.TrackSize.*;

final class ThemingPage {
    private ThemingPage() { }

    private static final List<Theme> PRESETS = List.of(Themes.AMETHYST, Themes.OCEAN, Themes.FOREST, Themes.NETHER, Themes.DAYLIGHT);

    private static Color value(Theme t, Token<Color> token) {
        Object v = t.values().get(token);
        return v instanceof Color c ? c : token.defaultValue(me.methodial.apothecary.theme.TokenScope.DEFAULTS);
    }

    /** The same little form, to show off a theme. */
    private static Node preview() {
        return stack(
                row(new Button("Primary").primary(), new Button("Default"), new Button("Ghost").ghost()),
                row(new Switch("Switch", true), new Checkbox("Check", true)),
                new ProgressBar(0.6f));
    }

    private static Node stack(Node... n) { return new Node().style(s -> s.col().gap(6)).add(n); }

    static Node build(ShowcaseScreen app) {
        UIRoot ui = app.root();

        // ---- presets (applied to the whole UI)
        Node presets = new Node().style(st -> st.cols(fill(minmax(px(70), fr(1)))).gap(5));
        List<Node> swatches = new ArrayList<>();
        for (Theme theme : PRESETS) {
            Node colors = new Node("swatch-colors");
            for (Token<Color> k : List.of(Tokens.BG, Tokens.SURFACE_3, Tokens.ACCENT, Tokens.ACCENT_2, Tokens.TEXT)) {
                Color c = value(theme, k);
                colors.add(new Node("swatch-color").style(st -> st.bg(c)));
            }
            Node sw = new Node("theme-swatch").add(colors, new Text(theme.name()).style(st -> st.fontSize(7)));
            swatches.add(sw);
            sw.onClick(() -> {
                ui.theme(theme);
                for (Node o : swatches) o.state("selected", o == sw);
            });
            presets.add(sw);
        }
        swatches.get(0).state("selected", true);

        // ---- single-token knobs
        ColorPicker accent = new ColorPicker(Color.hex("#8b7bff"));
        accent.onChange(c -> ui.token(Tokens.ACCENT, c));   // hover/press/soft accents are derived and follow
        Slider radius = new Slider(0, 12, 5).step(0.5f).showValue(v -> Math.round(v * 10) / 10f + "px");
        radius.onChange(v -> ui.token(Tokens.RADIUS, v));   // small/large radii are derived from RADIUS
        Select<String> font = new Select<String>().option("poppins", "Poppins").option("default", "Minecraft")
                .option("space-mono", "Space Mono").option("silkscreen", "Silkscreen").option("uniform", "Unifont");
        font.onChange(f -> ui.token(Tokens.FONT, f));

        Node knobs = card("Tweak tokens", "ui.token(ACCENT, color) — derived tokens (hover, press, soft, radius scale) follow, colors animate",
                new Node().style(st -> st.flex().gap(10).itemsStart()).add(
                        accent,
                        new Node().style(st -> st.col().gap(6).grow()).add(
                                control("radius", radius.style(st -> st.grow())),
                                control("font", font),
                                new Text("Preview").cls("caps"),
                                preview())));

        // ---- scoped themes: the same widgets under different themes, side by side
        Node scoped = new Node().style(st -> st.cols(fill(minmax(px(150), fr(1)))).gap(6));
        for (Theme theme : List.of(Themes.OCEAN, Themes.NETHER, Themes.DAYLIGHT.with(Themes.SOFT))) {
            Node panel = new Node().cls("card").add(new Text(theme.name()).cls("card-title"), preview());
            panel.theme(theme);          // tokens apply to this subtree only
            scoped.add(panel);
        }

        return page().add(
                section("Presets", "A theme is just a set of token values: ui.theme(Themes.OCEAN).", presets),
                knobs.style(st -> st.gridCol(1, -1)),
                section("Scoped themes", "node.theme(theme) re-skins one subtree; everything else keeps the UI theme.", scoped),
                section("Your own design system", null, card(null, null,
                        code("Token<Color> BRAND = Token.of(\"brand\", Color.hex(\"#ff5c8a\"));"),
                        code("Token<Color> BRAND_SOFT = Token.derived(\"brand-soft\", t -> t.get(BRAND).withAlpha(.2f));"),
                        code("sheet.on(\"chip\", r -> r.style(s -> s.pad(2, 6).round().bg(BRAND_SOFT).fg(BRAND)).hover(s -> s.bg(BRAND)));"),
                        code("ui.theme(Themes.DAYLIGHT.with(Theme.of(\"Brand\").set(ACCENT, Color.hex(\"#ff5c8a\"))));"))));
    }
}
