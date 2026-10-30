package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.state.Signal;
import me.methodial.apothecary.widget.Accordion;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Checkbox;
import me.methodial.apothecary.widget.NumberStepper;
import me.methodial.apothecary.widget.ProgressBar;
import me.methodial.apothecary.widget.RadioGroup;
import me.methodial.apothecary.widget.SegmentedControl;
import me.methodial.apothecary.widget.Select;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Spinner;
import me.methodial.apothecary.widget.Switch;
import me.methodial.apothecary.widget.Tabs;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.widget.TextField;
import me.methodial.apothecary.widget.Toast;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class ComponentsPage {
    private ComponentsPage() { }

    static Node build(ShowcaseScreen app) {
        // ---- buttons
        Node buttons = card("Buttons", "Variants, sizes, icons and states",
                row(new Button("Primary").primary(), new Button("Secondary"), new Button("Outline").variant("outline"),
                        new Button("Ghost").ghost(), new Button("Danger").danger()),
                row((Node) new Button("check", "Small").cls("sm"), new Button("star", "Default"), (Node) new Button("zap", "Large").primary().cls("lg")),
                row(Button.icon("heart", "Like"), Button.icon("settings", "Settings"), Button.icon("trash", "Delete").danger(),
                        (Node) new Button("Pill").cls("pill", "outline"), new Button("Disabled").disabled(true),
                        new Button("download", "Export").trailingIcon("chevron-down")));

        // ---- text input
        TextField name = new TextField().placeholder("Your name");
        Text greeting = new Text("Hello, stranger!").cls("muted");
        name.onChange(v -> greeting.text(v.isBlank() ? "Hello, stranger!" : "Hello, " + v + "!"));
        TextField digits = new TextField().placeholder("Digits only").filter(t -> t.matches("\\d*")).maxLength(8);
        TextField pw = new TextField("hunter2").password(true);
        TextField submit = new TextField().placeholder("Press Enter to submit");
        submit.onSubmit(v -> {
            Toast.show(app.root(), "info", "Submitted", v.isBlank() ? "(empty)" : v, 3);
            submit.text("");
        });
        Node inputs = card("Text fields", "Selection, clipboard, filters, password mode",
                name, greeting, digits, pw, submit);

        // ---- toggles
        Signal<Boolean> notify = Signal.of(true);
        Checkbox all = new Checkbox("Select all");
        Checkbox a = new Checkbox("Diamonds", true), b = new Checkbox("Emeralds"), c = new Checkbox("Netherite", true);
        Runnable sync = () -> {
            int n = (a.checked() ? 1 : 0) + (b.checked() ? 1 : 0) + (c.checked() ? 1 : 0);
            all.value(n == 3);
            all.indeterminate(n > 0 && n < 3);
        };
        for (Checkbox cb : new Checkbox[]{a, b, c}) cb.onChange(v -> sync.run());
        all.onChange(v -> { if (!all.hasState("indeterminate")) for (Checkbox cb : new Checkbox[]{a, b, c}) cb.value(v); });
        sync.run();
        Switch sw1 = new Switch("Notifications");
        sw1.bind(notify);
        Text notifyState = new Text().bind(notify.map(v -> v ? "Notifications are on" : "Notifications are off"));
        notifyState.cls("faint", "small");
        RadioGroup<String> difficulty = new RadioGroup<String>().option("peaceful", "Peaceful").option("easy", "Easy")
                .option("normal", "Normal").option("hard", "Hard");
        difficulty.value("normal");
        Node toggles = card("Toggles", "Checkbox (with indeterminate), switch bound to a Signal, radio group",
                new Node().style(st -> st.row().wrap().gap(10, 14).alignItems(me.methodial.apothecary.style.Align.START)).add(
                        col(all, new Node().style(s -> s.col().gap(4).padLeft(10)).add(a, b, c)),
                        col(sw1, new Switch("Dark mode", true), new Switch("Disabled").disabled(true), notifyState),
                        difficulty));

        // ---- sliders & progress
        Signal<Float> volume = Signal.of(0.65f);
        Slider vol = new Slider(0, 1);
        vol.bind(volume);
        ((Slider) vol).showValue(v -> Math.round(v * 100) + "%");
        Text volText = new Text().bind(volume.map(v -> "Volume " + Math.round(v * 100) + "%"));
        volText.cls("muted");
        ProgressBar linked = new ProgressBar(0.65f);
        volume.listen((o, n) -> linked.value(n));
        Slider steps = new Slider(0, 10, 4).step(1).showValue(null);
        ProgressBar loading = new ProgressBar(0);
        ProgressBar indeterminate = new ProgressBar(0).indeterminate();
        Spinner spin = new Spinner();
        Spinner gauge = new Spinner();
        Node sliders = card("Sliders & progress", "Drag, click, arrow keys or scroll while focused",
                volText, vol, linked, steps,
                row(new Button("play", "Simulate load").cls("sm").action(() -> {
                    loading.value(0f);
                    float[] p = {0, 0};
                    app.root().animator().add(dt -> {
                        p[1] += dt;
                        if (p[1] < 0.12f) return true;
                        p[1] = 0;
                        p[0] = Math.min(1, p[0] + (float) Math.random() * 0.12f);
                        loading.value(p[0]);
                        gauge.progress(p[0]);
                        return p[0] < 1;
                    });
                }), spin, gauge.progress(0.0f)),
                loading, indeterminate);

        // ---- selects, stepper, segmented
        Select<String> biome = new Select<String>().option("plains", "Plains", "leaf").option("desert", "Desert", "sun")
                .option("ocean", "Ocean", "droplet").option("nether", "Nether", "zap").option("end", "The End", "sparkles");
        Text biomeText = new Text("Selected: plains").cls("muted");
        biome.onChange(v -> biomeText.text("Selected: " + v));
        NumberStepper stack = new NumberStepper(1, 64, 16).step(1);
        SegmentedControl<String> view = new SegmentedControl<String>().option("day", "Day").option("week", "Week").option("month", "Month");
        Node pickers = card("Pickers", "Dropdowns, steppers and segmented controls",
                control("Biome", biome), biomeText, control("Stack", stack), control("View", view));

        // ---- tabs
        Tabs tabs = new Tabs()
                .tab("box", "Inventory", () -> col(new Text("Tabs swap panels with a direction-aware slide."), new Text("The underline springs between tabs.").cls("muted")))
                .tab("zap", "Effects", () -> col(new Text("Panels are built lazily and cached."), new ProgressBar(0.4f)))
                .tab("settings", "Settings", () -> col(new Switch("Auto-save", true), new Checkbox("Show coordinates")));
        Node tabsCard = card("Tabs", null, tabs);

        // ---- accordion
        Node acc = card("Accordion", "Height animates between 0 and the content's natural size",
                new Accordion("What is Apothecary?", new Text("A UI toolkit with a pure-Java layout and style engine.")).open(true),
                new Accordion("How do I theme it?", new Text("Apply a Theme (token values) to the UI or any subtree, or add your own Sheet of rules.")),
                new Accordion("Does it support shaders?", new Text("Yes — boxes, lines and arcs are SDF shaders, and ShaderView runs custom effects."),
                        new Button("sparkles", "Take me there").cls("sm").action(() -> app.show("Shaders"))));

        // ---- badges
        Node badges = card("Badges & keys", null,
                row(badge("New", null), badge("Stable", "success"), badge("Beta", "warning"), badge("Deprecated", "danger"), badge("v0.1", "info")),
                row(new Text("Press"), (Node) new Text("Tab").type("kbd"), new Text("to move focus,"), (Node) new Text("Esc").type("kbd"), new Text("to close.")));

        return page().add(
                new Node().cls("cards-wide").add(buttons, inputs, toggles, sliders, pickers, tabsCard, acc, badges));
    }
}
