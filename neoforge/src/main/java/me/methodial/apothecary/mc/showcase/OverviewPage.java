package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Dialog;
import me.methodial.apothecary.widget.Icon;
import me.methodial.apothecary.widget.ShaderView;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.widget.Toast;
import me.methodial.apothecary.theme.Motion;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class OverviewPage {
    private OverviewPage() { }

    static Node build(ShowcaseScreen app) {
        ShaderView aurora = new ShaderView("apothecary:aurora");
        aurora.color(0, Color.parse("#8b7bff")).color(1, Color.parse("#4fd8c4")).color(2, Color.parse("#ff7ac8"))
                .color(3, Color.parse("#0b0a18")).param(0, 1.0f).param(1, 0.03f);

        Node hero = new Node("hero").add(
                aurora,
                new Node("hero-scrim"),
                new Text("Apothecary").cls("hero-title"),
                new Text("Flexbox and grid layout, code-first styling with design tokens and themes, transitions, "
                        + "keyframes, springs and SDF shaders — in a portable core with a NeoForge 1.21.1 backend.").cls("hero-lead"),
                new Node().cls("hero-actions").add(
                        new Button("sliders", "Explore components").primary().action(() -> app.show("Components")),
                        new Button("sparkles", "Shaders").action(() -> app.show("Shaders")),
                        new Button("info", "About").ghost().action(() -> new Dialog("flask", "About Apothecary")
                                .text("Everything on screen is rendered by Apothecary: layout runs in a pure-Java core, "
                                        + "styles are Sheets built in Java from design tokens, and shapes are drawn with signed distance field shaders.")
                                .text("Every widget is styled by a Sheet written in Java; swap a Theme and the whole UI follows.")
                                .button("Nice", "primary", () -> Toast.show(app.root(), "success", "Glad you like it", "Toasts stack in the corner.", 4))
                                .open(app.root()))));

        String[][] features = {
                {"columns", "Flexbox & Grid", "Full flexbox algorithm and grid layout with fr, minmax, auto-fill and areas."},
                {"code", "Styles in code", "Typed fluent styles, nested rule builders, selectors, states, tokens and media rules."},
                {"activity", "Motion", "Transitions, keyframes, springs, tweens, timelines and layout (FLIP) animations."},
                {"sparkles", "Shaders", "Anti-aliased SDF boxes, borders, gradients, blurred shadows and GPU effects."},
                {"box", "Portable core", "No Minecraft code in the core: swap the Canvas backend for any version or loader."},
                {"palette", "Themable", "Design tokens with derived values: change the accent, everything follows."},
        };
        Node grid = new Node().cls("cards");
        for (int i = 0; i < features.length; i++) {
            String[] f = features[i];
            Node card = card(null, null,
                    new Node().cls("feature-icon").add(new Icon(f[0])),
                    new Text(f[1]).cls("card-title"),
                    new Text(f[2]).cls("card-desc")).cls("feature", "hoverable");
            float delay = 0.12f + i * 0.07f;
            card.style(st -> st.animation(Motion.RISE_IN.play(0.5f).easing(Motion.OUT).delay(delay).backwards()));
            grid.add(card);
        }

        Node quick = section("Quick start", "Trees, styles and rules are all plain Java.",
                card(null, null,
                        code("col(s(x -> x.gap(6).pad(10)), h2(\"Inventory\"), row(gap(4), button(\"Save\", this::save).primary()));"),
                        code("sheet.on(\"chip\", r -> r.style(s -> s.pad(2, 6).round().bg(SURFACE_3)).hover(s -> s.bg(ACCENT_SOFT)));"),
                        code("ui.theme(Themes.OCEAN.with(Themes.COMPACT));   // every widget re-skins, colors animate")));

        return page().add(hero, section("What's inside", null, grid), quick);
    }
}
