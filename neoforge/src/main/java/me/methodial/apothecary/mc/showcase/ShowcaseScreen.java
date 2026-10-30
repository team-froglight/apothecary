package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.mc.screen.UIScreen;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.widget.Icon;
import me.methodial.apothecary.widget.SlidingIndicator;
import me.methodial.apothecary.widget.Switch;
import me.methodial.apothecary.widget.Text;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** The Apothecary showcase: a sidebar app demonstrating every part of the library. */
public class ShowcaseScreen extends UIScreen {
    record PageDef(String group, String icon, String title, String subtitle, Function<ShowcaseScreen, Node> build) { }

    private static final List<PageDef> PAGES = List.of(
            new PageDef("Start", "home", "Overview", "A portable, stylable UI toolkit for Minecraft", OverviewPage::build),
            new PageDef("Layout", "columns", "Flexbox", "Direction, wrapping, justification and alignment — animated with layout transitions", FlexPage::build),
            new PageDef("Layout", "grid", "Grid", "Tracks, fr units, spans, template areas and auto-fill", GridPage::build),
            new PageDef("Build", "sliders", "Components", "Interactive widgets, all styled by code-defined sheets", ComponentsPage::build),
            new PageDef("Build", "type", "Typography", "Fonts, sizes, weights, wrapping and Minecraft rich text", TypographyPage::build),
            new PageDef("Build", "layers", "Overlays", "Dialogs, toasts, menus and tooltips", OverlaysPage::build),
            new PageDef("Motion", "activity", "Animation", "Easings, springs, keyframes, transitions and timelines", MotionPage::build),
            new PageDef("Motion", "sparkles", "Shaders", "SDF shapes and GPU effects", ShadersPage::build),
            new PageDef("World", "box", "Minecraft", "Items, entities, sprites and vanilla text", MinecraftPage::build),
            new PageDef("World", "palette", "Theming", "Design tokens, themes and scoped themes", ThemingPage::build));

    private static final me.methodial.apothecary.sheet.Sheet SHEET = ShowcaseStyles.build();

    private final List<Node> navItems = new ArrayList<>();
    private final SlidingIndicator navPill = new SlidingIndicator("nav-pill");
    private final Node scroll = new Node("page-scroll");
    private final Text title = new Text(), subtitle = new Text();
    private int current = -1;

    public ShowcaseScreen(Screen parent) {
        super(Component.literal("Apothecary Showcase"));
        parent(parent);
        sheet(SHEET);
        navPill.springs(320, 28);
    }

    @Override
    protected void build(UIRoot ui) {
        Node app = new Node("app");

        Node sidebar = new Node("sidebar");
        Node brand = new Node("brand").add(
                new Node("brand-logo").add(new Icon("flask")),
                new Node().style(s -> s.col()).add(
                        new Text("Apothecary").cls("brand-name"),
                        new Text("UI toolkit · 1.21.1").cls("brand-version")));
        Node nav = new Node("nav").add(navPill);
        String group = null;
        for (int i = 0; i < PAGES.size(); i++) {
            PageDef p = PAGES.get(i);
            if (!p.group().equals(group)) {
                group = p.group();
                nav.add(new Text(group).type("nav-group"));
            }
            int idx = i;
            Node item = new Node("nav-item").add(new Icon(p.icon()), new Text(p.title()));
            item.focusable(true);
            item.onClick(() -> show(idx));
            navItems.add(item);
            nav.add(item);
        }
        Switch sounds = new Switch("UI sounds", true);
        sounds.onChange(on -> me.methodial.apothecary.mc.render.McPlatform.INSTANCE.setSoundsEnabled(on));
        Node footer = new Node("sidebar-footer").add(sounds);
        sidebar.add(brand, nav, footer);

        Node topbar = new Node("topbar").add(
                new Node().style(s -> s.col().grow()).add(title.cls("page-title"), subtitle.cls("page-subtitle")),
                me.methodial.apothecary.widget.Button.icon("arrow-left", "Previous page").ghost().action(() -> show((current - 1 + PAGES.size()) % PAGES.size())),
                me.methodial.apothecary.widget.Button.icon("arrow-right", "Next page").ghost().action(() -> show((current + 1) % PAGES.size())));

        Node main = new Node("main").add(topbar, scroll);
        app.add(sidebar, main);
        ui.setContent(app);
        show(0);
    }

    /** Navigates to a page by index. */
    public void show(int index) {
        if (index == current) return;
        current = index;
        PageDef p = PAGES.get(index);
        for (int i = 0; i < navItems.size(); i++) navItems.get(i).state("selected", i == index);
        navPill.target(navItems.get(index));
        title.text(p.title());
        subtitle.text(p.subtitle());
        Node page = p.build().apply(this);
        scroll.setChildren(page);
        scroll.setScroll(0, 0);
    }

    public void show(String pageTitle) {
        for (int i = 0; i < PAGES.size(); i++) if (PAGES.get(i).title().equals(pageTitle)) show(i);
    }

    UIRoot root() { return ui; }
}
