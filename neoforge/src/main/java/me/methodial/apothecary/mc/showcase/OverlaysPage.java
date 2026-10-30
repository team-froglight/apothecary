package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Dialog;
import me.methodial.apothecary.widget.Icon;
import me.methodial.apothecary.widget.Menu;
import me.methodial.apothecary.widget.ProgressBar;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.widget.TextField;
import me.methodial.apothecary.widget.Toast;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.theme.Mixins;
import me.methodial.apothecary.theme.Tokens;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class OverlaysPage {
    private OverlaysPage() { }

    static Node build(ShowcaseScreen app) {
        UIRoot ui = app.root();

        // ---- dialogs
        Node dialogs = card("Dialogs", "Modal layer with backdrop, focus trap, Esc to close",
                row(new Button("Confirm").primary().action(() -> new Dialog("flask", "Brew this potion?")
                                .text("It will consume 1 Nether Wart and 3 Water Bottles.")
                                .button("Cancel", "ghost", null)
                                .button("Brew", "primary", () -> Toast.show(ui, "success", "Brewing started", "Ready in 20 seconds.", 4))
                                .open(ui)),
                        new Button("trash", "Destructive").danger().action(() -> new Dialog("alert", "Delete world?")
                                .text("This permanently deletes \"New World\". This can't be undone.")
                                .button("Keep it", "ghost", null)
                                .button("Delete", "danger", () -> Toast.show(ui, "error", "World deleted", "Just kidding — nothing happened.", 4))
                                .dismissOnBackdrop(false)
                                .open(ui)),
                        new Button("edit", "Form").action(() -> {
                            TextField field = new TextField().placeholder("Waypoint name");
                            Slider r = new Slider(0, 255, 120).step(1).showValue(null);
                            new Dialog("edit", "New waypoint")
                                    .body(new Text("Name").cls("muted"), field, new Text("Beacon hue").cls("muted"), r)
                                    .button("Cancel", "ghost", null)
                                    .button("Create", "primary", () -> Toast.show(ui, "success", "Waypoint created",
                                            field.text().isBlank() ? "Unnamed" : field.text(), 3))
                                    .open(ui);
                        })));

        // ---- toasts
        Node toasts = card("Toasts", "Stack bottom-right, slide in, drain a timer and slide out",
                row(new Button("info", "Info").action(() -> Toast.show(ui, "info", "Did you know?", "Toasts are plain nodes in the toast layer.", 4)),
                        new Button("check-circle", "Success").action(() -> Toast.show(ui, "success", "Saved", "Your settings were saved.", 3)),
                        new Button("alert", "Warning").action(() -> Toast.show(ui, "warning", "Low durability", "Your pickaxe is about to break.", 4)),
                        new Button("x-circle", "Error").action(() -> Toast.show(ui, "error", "Connection lost", "Retrying in 5 seconds…", 5))),
                row(new Button("Burst ×5").ghost().action(() -> {
                    for (int i = 0; i < 5; i++) {
                        int n = i;
                        ui.animator().schedule(i * 0.15f, () -> Toast.show(ui, n % 2 == 0 ? "info" : "success", "Toast #" + (n + 1), null, 2.5f + n * 0.3f));
                    }
                })));

        // ---- menus
        Button dropdown = new Button("Actions");
        dropdown.trailingIcon("chevron-down");
        dropdown.action(() -> {
            Menu m = new Menu().header("World")
                    .item("edit", "Rename", "F2", () -> Toast.show(ui, "info", "Rename", null, 2))
                    .item("copy", "Duplicate", "Ctrl+D", () -> Toast.show(ui, "info", "Duplicate", null, 2))
                    .item("download", "Export", null, () -> Toast.show(ui, "info", "Export", null, 2))
                    .separator()
                    .item("trash", "Delete", "Del", () -> Toast.show(ui, "error", "Delete", null, 2));
            m.show(ui, dropdown, UIRoot.Placement.BELOW, null);
        });
        Node area = new Node().style(st -> st.h(50).flex().center().apply(Mixins.WELL))
                .add(new Text("Right-click anywhere here").cls("muted"));
        area.on(EventType.CONTEXT_MENU, e -> new Menu()
                .item("refresh", "Reload chunks", "F3+A", () -> Toast.show(ui, "info", "Reloading chunks", null, 2))
                .item("eye", "Toggle hitboxes", "F3+B", () -> Toast.show(ui, "info", "Hitboxes", null, 2))
                .separator()
                .item("settings", "Settings…", null, () -> app.show("Theming"))
                .showAtCursor(ui, area));
        Node menus = card("Menus", "Dropdown anchored to a button, context menu at the cursor; arrows + Enter work",
                row(dropdown), area);

        // ---- tooltips
        Node rich = new Node().style(st -> st.col().gap(3).w(120)).add(
                new Node().style(st -> st.row().gap(4)).add(new Icon("flask").style(st -> st.fg(Tokens.ACCENT).fontSize(10)),
                        new Text("Potion of Swiftness").style(Style::semibold)),
                new Text("Speed II · 1:30").cls("muted"),
                new ProgressBar(0.7f));
        Node tips = card("Tooltips", "Delayed, follow the cursor, flip at screen edges; any node can be a tooltip",
                row(Button.icon("heart", "Plain text tooltip"),
                        Button.icon("star", "Tooltips can be long and wrap automatically when they exceed their maximum width."),
                        new Button("flask", "Rich tooltip").tooltip(() -> rich),
                        new Node().style(st -> st.pad(4, 8).radius(4).bg(Tokens.SURFACE_3))
                                .add(new Text("Hover this box")).tooltip("Any element can have a tooltip.")));

        return page().add(new Node().cls("cards-wide").add(dialogs, toasts, menus, tips));
    }
}
