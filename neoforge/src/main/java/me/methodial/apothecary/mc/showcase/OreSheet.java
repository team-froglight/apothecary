package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.mc.screen.UIScreen;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.theme.DefaultTheme;
import me.methodial.apothecary.theme.OreUI;
import me.methodial.apothecary.widget.BlockLogo;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Checkbox;
import me.methodial.apothecary.widget.Select;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Text;
import net.minecraft.network.chat.Component;

/**
 * The Ore UI design sheet rebuilt from Apothecary's widgets, every part where the sheet has it (800 x 450 units), to
 * hold the theme against the sheet: render it in an 800 x 450 window at scale 1 and compare the two.
 */
public final class OreSheet {
    private OreSheet() { }

    public static UIScreen screen() {
        UIScreen s = UIScreen.of(Component.literal("Ore UI sheet"), root -> root.setContent(build()));
        s.sheet(DefaultTheme.sheet()).theme(OreUI.theme()).sheet(OreUI.sheet());
        return s.uiScale(1).backdrop(UIScreen.Backdrop.NONE);
    }

    /** {@code n} at (x, y), {@code w} wide. */
    private static Node at(Node n, float x, float y, float w) {
        return n.style(s -> s.abs().left(x).top(y).w(w));
    }

    private static Node states(Node n, String... states) {
        for (String st : states) n.state(st, true);
        return n;
    }

    public static Node build() {
        Node page = new Node().style(s -> s.size(800, 450).bg(Color.hex("#23232a")));
        page.add(at(new BlockLogo("Design system", OreUI.TITLE, 4.4f), 40, 35, 300));

        // Green and light buttons: focused, hovered, pressed.
        String[][] buttonStates = {{"focus", "focus-visible"}, {"hover"}, {"hover", "active"}};
        float[] buttonY = {83.6f, 121.2f, 161.2f};
        for (int i = 0; i < 3; i++) {
            Button green = new Button("Label");
            green.variant("primary");
            green.title(OreUI.BUTTON_TITLE, OreUI.BUTTON_TITLE_UNIT);
            page.add(states(at(green, 38.6f, buttonY[i], 198), buttonStates[i]));
            page.add(states(at(new Button("Label"), 251.2f, buttonY[i], 197), buttonStates[i]));
        }

        // Dropdowns: resting, hovered, pressed, focused, disabled.
        String[][] dropdownStates = {{}, {"hover"}, {"hover", "active"}, {"focus", "focus-visible"}, {"disabled"}};
        float[] dropdownY = {209.2f, 246.8f, 284.4f, 322f, 358.4f};
        for (int i = 0; i < 5; i++) {
            Select<String> select = new Select<>();
            select.option("x", "Label (X)");
            if (i == 4) select.disabled(true);
            page.add(states(at(select, 41.2f, dropdownY[i], 195), dropdownStates[i]));
        }

        // Sliders: focused, hovered, resting, stepped.
        float[] sliderY = {210f, 241f, 272f, 303f};
        String[][] sliderStates = {{"focus", "focus-visible"}, {"hover"}, {}, {}};
        for (int i = 0; i < 4; i++) page.add(states(at(new Slider(0, 1, 0.5f), 251.2f, sliderY[i], 197), sliderStates[i]));

        // The tooltip, as the root draws one.
        Node tip = new Node("tooltip").cls("tooltip").add(new Text("Tooltip message"));
        tip.add(new Node().cls("tooltip-cap"));
        for (int i = 0; i < 3; i++) tip.add(new Node().cls("tooltip-step", "s" + i));
        page.add(at(tip, 251.2f, 339.2f, 196));

        // Diamonds: checked and not; disabled; pressed; then hovered and focused.
        float[][] diamonds = {{252, 375}, {277, 375}, {330, 375}, {355, 375}, {407, 375}, {432, 375},
                {252, 400}, {277, 400}, {407, 400}, {432, 400}};
        String[][] diamondStates = {{}, {}, {"disabled"}, {"disabled"}, {"active"}, {"active"},
                {"hover"}, {"hover"}, {"focus", "focus-visible"}, {"focus", "focus-visible"}};
        for (int i = 0; i < diamonds.length; i++) {
            Checkbox box = new Checkbox("", i % 2 == 0);
            if (i == 2 || i == 3) box.disabled(true);
            float[] p = diamonds[i];
            page.add(states(box.style(s -> s.abs().left(p[0]).top(p[1])), diamondStates[i]));
        }

        // List rows: label and description, data, side cells; then rows with a handle and a picture.
        float[] rowY = {83.6f, 127.6f, 183.6f, 227.6f, 271.6f};
        int[] cells = {1, 2, 0, 1, 2};
        for (int i = 0; i < 5; i++) page.add(at(row(i >= 2, cells[i], i < 2), 462.4f, rowY[i], 300));

        // A panel with a header and a light dropdown.
        Node panel = new Node().style(s -> s.col().pad(9, 15, 12, 16).gap(5).apply(OreUI.PANEL));
        Select<String> light = new Select<>();
        light.option("x", "Label");
        light.style(s -> s.apply(OreUI.face(OreUI.GENERAL)).h(OreUI.BUTTON_HEIGHT).m(1, 1, 4, 1));
        panel.add(new Text("Header"), light);
        page.add(at(panel, 461.6f, 338, 300));
        return page;
    }

    private static Node row(boolean picture, int cells, boolean data) {
        Node row = new Node().style(s -> s.row().h(38).itemsStretch());
        Node main = new Node().style(s -> s.row().grow().itemsCenter().pad(0, 8).gap(8).apply(OreUI.RAISED));
        if (picture) {
            row.add(new Node().style(s -> s.w(24).center().apply(OreUI.RAISED)).add(new Text("::#")));
            main.add(new Node().style(s -> s.size(52, 30).bg(Color.hex("#c1c1c1"))));
        }
        main.add(new Node().style(s -> s.col().grow().gap(4)).add(new Text("Label"), new Text("Description").style(s -> s.fg(Color.hex("#d6d7da")))));
        if (data) main.add(new Node().style(s -> s.col().gap(4).itemsEnd()).add(new Text("Data"), new Text("Data")));
        row.add(main);
        for (int i = 0; i < cells; i++) {
            row.add(new Node().style(s -> s.w(39).col().center().gap(3).apply(OreUI.RAISED))
                    .add(new Node().style(s -> s.size(14).bg(Color.hex("#b1b2b5"))), new Text("Label").style(s -> s.fontSize(6))));
        }
        return row;
    }
}
