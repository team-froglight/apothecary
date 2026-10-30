package me.methodial.apothecary;

import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.sheet.Media;
import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Keyframes;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Tr;
import me.methodial.apothecary.theme.Theme;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.theme.Tokens;
import me.methodial.apothecary.widget.Text;
import org.junit.jupiter.api.Test;

import static me.methodial.apothecary.sheet.Selector.*;
import static me.methodial.apothecary.style.TrackSize.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {
    private static void advance(UIRoot r, float seconds) {
        for (float t = 0; t < seconds - 1e-4f; t += 0.05f) r.update(Math.min(0.05f, seconds - t));
    }

    private static UIRoot root(Node content, float w, float h) {
        UIRoot r = new UIRoot(Platform.headless());
        r.resize(w, h);
        r.setContent(content);
        r.layoutNow();
        return r;
    }

    private static UIRoot root(Sheet sheet, Node content, float w, float h) {
        UIRoot r = new UIRoot(Platform.headless());
        r.addSheet(sheet);
        r.resize(w, h);
        r.setContent(content);
        r.layoutNow();
        return r;
    }

    // ================================================================== flexbox

    @Test
    void flexRowGrowAndGap() {
        Node a = new Node().style(s -> s.size(50, 20));
        Node b = new Node().style(s -> s.grow(1).h(20));
        Node c = new Node().style(s -> s.grow(2).h(20));
        Node row = new Node().style(s -> s.flex().gap(10).pad(5).h(40)).add(a, b, c);
        root(row, 300, 200);
        assertEquals(300, row.width(), 0.01);
        assertEquals(5, a.x(), 0.01);
        // free = 300 - 10 - 50 - 20 gaps = 220 → b 73.33, c 146.67
        assertEquals(73.33, b.width(), 0.05);
        assertEquals(146.67, c.width(), 0.05);
        assertEquals(5 + 50 + 10, b.x(), 0.01);
    }

    @Test
    void flexJustifyAlignCenter() {
        Node a = new Node().style(s -> s.size(40, 20));
        Node row = new Node().style(s -> s.flex().center().h(100)).add(a);
        root(row, 200, 200);
        assertEquals(80, a.x(), 0.01);
        assertEquals(40, a.y(), 0.01);
    }

    @Test
    void flexWrapLines() {
        Node row = new Node().style(s -> s.flex().wrap().w(100));
        for (int i = 0; i < 5; i++) row.add(new Node().style(s -> s.size(40, 10)));
        root(row, 300, 300);
        assertEquals(0, row.child(2).x(), 0.01);
        assertEquals(10, row.child(2).y(), 0.01);
        assertEquals(30, row.height(), 0.01);
    }

    @Test
    void columnStretchAndAutoHeight() {
        Node a = new Node().style(s -> s.h(30));
        Node col = new Node().style(s -> s.col().pad(4)).add(a, new Node().style(s -> s.h(10)));
        root(col, 120, 300);
        assertEquals(112, a.width(), 0.01);
        assertEquals(48, col.height(), 0.01);
    }

    @Test
    void flexItemsKeepAutomaticMinimumSize() {
        Node col = new Node().style(s -> s.col().h(30));
        for (int i = 0; i < 3; i++) col.add(new Node().style(s -> s.pad(5)).add(new Text("x")));
        root(col, 100, 100);
        assertEquals(10 + 9 * 1.2f, col.child(0).height(), 0.01);
        Node scrolling = new Node().style(s -> s.col().h(30));
        Node a = new Node().style(s -> s.scrollY().basis(50));
        scrolling.add(a);
        root(scrolling, 100, 100);
        assertEquals(30, a.height(), 0.01);
    }

    @Test
    void autoHeightColumnGrowsBeyondAvailableSpace() {
        Node page = new Node().style(s -> s.col());
        Node tall = new Node().style(s -> s.h(150));
        page.add(tall, new Node().style(s -> s.h(100)));
        Node scroller = new Node().style(s -> s.h(120).scrollY()).add(page);
        root(scroller, 100, 300);
        assertEquals(150, tall.height(), 0.01);
        assertEquals(250, page.height(), 0.01);
        assertEquals(130, scroller.maxScrollY(), 0.01);
    }

    @Test
    void marginAutoPushes() {
        Node a = new Node().style(s -> s.size(10));
        Node b = new Node().style(s -> s.size(10).pushRight());
        Node row = new Node().style(s -> s.flex()).add(a, b);
        root(row, 100, 100);
        assertEquals(90, b.x(), 0.01);
    }

    // ================================================================== grid

    @Test
    void gridFrColumnsAndSpans() {
        Node g = new Node().style(s -> s.cols(px(100), fr(1), fr(2)).gap(10).autoRows(px(20)));
        Node wide = new Node().style(s -> s.colSpan(2));
        g.add(new Node(), new Node(), new Node(), wide, new Node());
        root(g, 400, 300);
        assertEquals(93.33, g.child(1).width(), 0.05);
        assertEquals(110, g.child(1).x(), 0.05);
        assertEquals(30, wide.y(), 0.01);
        assertEquals(100 + 10 + 93.33, wide.width(), 0.05);
        assertEquals(30, g.child(4).y(), 0.01);
        assertEquals(213.33, g.child(4).x(), 0.05);
    }

    @Test
    void gridAutoFillAndAreas() {
        Node g = new Node().style(s -> s.cols(fill(px(50))).gap(10));
        for (int i = 0; i < 7; i++) g.add(new Node().style(s -> s.h(10)));
        root(g, 230, 300);
        assertEquals(0, g.child(4).x(), 0.01);
        assertEquals(20, g.child(4).y(), 0.01);

        Node areas = new Node().style(s -> s.cols(px(50), fr(1)).rows(px(20), fr(1)).areas("head head", "side main").h(100));
        Node main = new Node().style(s -> s.area("main"));
        Node head = new Node().style(s -> s.area("head"));
        areas.add(main, head);
        root(areas, 200, 200);
        assertEquals(200, head.width(), 0.01);
        assertEquals(50, main.x(), 0.01);
        assertEquals(20, main.y(), 0.01);
        assertEquals(80, main.height(), 0.01);
    }

    @Test
    void absolutePositioning() {
        Node abs = new Node().style(s -> s.abs().right(10).bottom(5).size(20));
        Node box = new Node().style(s -> s.size(100).border(2, Color.hex("#f00"))).add(abs);
        root(box, 300, 300);
        assertEquals(2 + 96 - 10 - 20, abs.x(), 0.01);
        assertEquals(2 + 96 - 5 - 20, abs.y(), 0.01);
    }

    // ================================================================== text

    @Test
    void textWrapsInsideWidth() {
        Text t = new Text("hello world foo bar");
        Node box = new Node().style(s -> s.w(60)).add(t);
        root(box, 300, 300);
        assertTrue(t.height() >= 9 * 1.2f * 3 - 0.1f, "height " + t.height());
    }

    @Test
    void textBreaksAfterHyphens() {
        var m = Platform.headless().text();
        var r = me.methodial.apothecary.text.TextLayout.layout(m, "ease-in-out-quint", me.methodial.apothecary.render.FontSpec.DEFAULT,
                10, 60, me.methodial.apothecary.style.WhiteSpace.NORMAL, 0, false);
        assertEquals(2, r.lines().size());
        assertEquals("ease-in-", r.lines().get(0).text());
        assertEquals("out-quint", r.lines().get(1).text());
    }

    // ================================================================== sheets, tokens, states

    @Test
    void nestedRulesTokensAndSpecificity() {
        Token<Color> brand = Token.of("brand", Color.hex("#ff0000"));
        Sheet sheet = new Sheet("t")
                .onCls("card", r -> r
                        .style(s -> s.bg(brand).pad(4))
                        .hover(s -> s.pad(8))
                        .partCls("title", p -> p.style(s -> s.fg(Color.hex("#0000ff")))))
                .rule(id("special"), s -> s.pad(2));
        Node title = new Node().cls("title");
        Node card = new Node().cls("card").add(title);
        UIRoot r = root(sheet, card, 100, 100);
        assertEquals(Paint.of(Color.hex("#ff0000")), card.get(Props.BACKGROUND));
        assertEquals(Color.hex("#0000ff"), title.get(Props.COLOR));
        assertEquals(4, card.paddingEdges().top(), 0.01);
        card.state("hover", true);
        r.layoutNow();
        assertEquals(8, card.paddingEdges().top(), 0.01);
        card.id("special");
        r.layoutNow();
        assertEquals(2, card.paddingEdges().top(), 0.01);
        // a theme swaps the token; derived values follow
        r.theme(Theme.of("blue").set(brand, Color.hex("#00ff00")));
        r.layoutNow();
        assertEquals(Paint.of(Color.hex("#00ff00")), card.get(Props.BACKGROUND));
    }

    @Test
    void derivedTokensFollowTheirSource() {
        Node n = new Node().style(s -> s.bg(Tokens.ACCENT_SOFT));
        UIRoot r = root(n, 50, 50);
        Color accent = Tokens.ACCENT.defaultValue(me.methodial.apothecary.theme.TokenScope.DEFAULTS);
        assertEquals(Paint.of(accent.withAlpha(0.18f)), n.get(Props.BACKGROUND));
        r.token(Tokens.ACCENT, Color.hex("#00ff00"));
        r.layoutNow();
        assertEquals(Paint.of(Color.hex("#00ff00").withAlpha(0.18f)), n.get(Props.BACKGROUND));
        // a subtree can override a token for itself only
        Node scoped = new Node().style(s -> s.bg(Tokens.ACCENT)).token(Tokens.ACCENT, Color.hex("#0000ff"));
        r.setContent(new Node().add(n, scoped));
        r.layoutNow();
        assertEquals(Paint.of(Color.hex("#0000ff")), scoped.get(Props.BACKGROUND));
        assertEquals(Paint.of(Color.hex("#00ff00").withAlpha(0.18f)), n.get(Props.BACKGROUND));
    }

    @Test
    void scopedThemeTextResolvesAgainstItsOwnTokens() {
        Text inside = new Text("x");
        Node panel = new Node().add(inside).theme(me.methodial.apothecary.theme.Themes.DAYLIGHT);
        Text outside = new Text("y");
        UIRoot r = root(me.methodial.apothecary.theme.DefaultTheme.sheet(), new Node().add(panel, outside), 100, 100);
        r.theme(me.methodial.apothecary.theme.Themes.AMETHYST);
        advance(r, 1f);
        assertEquals(Color.hex("#1b1d2a"), inside.get(Props.COLOR));   // Daylight text
        assertEquals(Color.hex("#e9eaf2"), outside.get(Props.COLOR));  // Amethyst text
    }

    @Test
    void inlineStateStylesApplyWhileStateIsSet() {
        Node n = new Node().style(s -> s.pad(2)).hover(s -> s.pad(6)).when("selected", s -> s.opacity(0.5f));
        UIRoot r = root(n, 50, 50);
        assertEquals(2, n.paddingEdges().top(), 0.01);
        n.state("hover", true);
        r.layoutNow();
        assertEquals(6, n.paddingEdges().top(), 0.01);
        n.state("selected", true);
        r.layoutNow();
        assertEquals(0.5f, n.get(Props.OPACITY), 0.001);
    }

    @Test
    void mediaRulesFollowRootSize() {
        Sheet sheet = new Sheet("m").style("box", s -> s.w(100)).media(Media.maxWidth(200), m -> m.style("box", s -> s.w(Length.pct(100))));
        Node box = new Node("box");
        UIRoot r = root(sheet, new Node().add(box), 400, 100);
        assertEquals(100, box.width(), 0.01);
        r.resize(150, 100);
        r.layoutNow();
        assertEquals(150, box.width(), 0.01);
    }

    @Test
    void baseStyleHasLowestPriorityAndSurvivesClear() {
        Node n = new Node().cls("x");
        n.baseStyle(s -> s.pad(3).w(40));
        n.style().w(50);
        UIRoot r = root(new Sheet("b").rule(cls("x"), s -> s.pad(5)), n, 200, 200);
        assertEquals(5, n.paddingEdges().top(), 0.01);
        assertEquals(50, n.width(), 0.01);
        n.style().clear();
        r.layoutNow();
        assertEquals(40, n.width(), 0.01);
        r.content().style().clear();
        r.layoutNow();
        assertEquals(200, r.content().height(), 0.01);
    }

    @Test
    void slidingIndicatorAlignsWithTargetInsideBorderedParent() {
        me.methodial.apothecary.widget.SegmentedControl<String> seg = new me.methodial.apothecary.widget.SegmentedControl<String>()
                .option("a", "Alpha").option("b", "Beta");
        seg.style(s -> s.flex().pad(2).border(1, Color.WHITE));
        UIRoot r = root(new Node().add(seg), 300, 100);
        advance(r, 0.5f);
        Node pill = seg.child(0), first = seg.child(1);
        assertEquals(first.x(), pill.x(), 0.3);
        assertEquals(first.y(), pill.y(), 0.3);
        assertEquals(first.width(), pill.width(), 0.3);
    }

    @Test
    void defaultThemeStylesWidgetsAndFollowsThemes() {
        me.methodial.apothecary.widget.Button primary = new me.methodial.apothecary.widget.Button("Go").primary();
        me.methodial.apothecary.widget.Checkbox box = new me.methodial.apothecary.widget.Checkbox("Check", true);
        UIRoot r = root(me.methodial.apothecary.theme.DefaultTheme.sheet(), new Node().add(primary, box), 300, 100);
        assertTrue(primary.get(Props.BACKGROUND) instanceof Paint.Linear, "primary buttons get the accent gradient");
        assertEquals(4, primary.paddingEdges().top(), 0.01);
        Node checkBox = box.child(0);
        assertEquals(Paint.of(r.token(Tokens.ACCENT)), checkBox.get(Props.BACKGROUND));
        r.theme(me.methodial.apothecary.theme.Themes.FOREST);
        advance(r, 1f);
        assertEquals(Paint.of(me.methodial.apothecary.theme.Themes.FOREST.values().get(Tokens.ACCENT) instanceof Color c ? c : null),
                checkBox.get(Props.BACKGROUND));
    }

    // ================================================================== motion

    @Test
    void transitionsInterpolate() {
        Sheet sheet = new Sheet("t").onCls("b", r -> r
                .style(s -> s.opacity(1).transition(Tr.of(1f, Easings.LINEAR, Props.OPACITY)))
                .hover(s -> s.opacity(0)));
        Node b = new Node().cls("b");
        UIRoot r = root(sheet, b, 100, 100);
        r.update(0.016f);
        assertEquals(1f, b.get(Props.OPACITY), 0.001);
        b.state("hover", true);
        r.update(0.016f);
        advance(r, 0.5f);
        assertEquals(0.5f, b.get(Props.OPACITY), 0.03);
        advance(r, 0.6f);
        assertEquals(0f, b.get(Props.OPACITY), 0.001);
    }

    @Test
    void inheritedValuesFollowAnimatingParentAndSettle() {
        Sheet sheet = new Sheet("i")
                .rule(any(), s -> s.transition(Tr.of(0.3f, Easings.LINEAR, Props.COLOR)))
                .onCls("p", r -> r.style(s -> s.fg(Color.BLACK)).variantStyle("on", s -> s.fg(Color.WHITE)));
        Node child = new Text("x");
        Node parent = new Node().cls("p").add(child);
        UIRoot r = root(sheet, parent, 100, 100);
        advance(r, 0.1f);
        parent.cls("on");
        advance(r, 0.15f);
        assertEquals(parent.get(Props.COLOR), child.get(Props.COLOR));
        child.state("hover", true);
        advance(r, 0.05f);
        advance(r, 0.5f);
        assertEquals(Color.WHITE, parent.get(Props.COLOR));
        assertEquals(Color.WHITE, child.get(Props.COLOR));
    }

    @Test
    void keyframesAnimate() {
        Keyframes grow = Keyframes.of("grow").from(s -> s.w(10)).to(s -> s.w(110));
        Node a = new Node().style(s -> s.h(10).animation(grow.play(1f).easing(Easings.LINEAR).forwards()));
        Node wrap = new Node().style(s -> s.flex().itemsStart()).add(a);
        UIRoot r = root(wrap, 300, 100);
        r.update(0.0f);
        advance(r, 0.5f);
        assertEquals(60, a.width(), 1.5);
        advance(r, 1f);
        assertEquals(110, a.width(), 0.5);
    }
}
