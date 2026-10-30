package me.methodial.apothecary;

import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.Region;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.widget.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keyboard focus: kept across Region rebuilds, held by focus scopes, moved by the arrow keys. */
class FocusTest {
    private static UIRoot root(Node content) {
        UIRoot r = new UIRoot(Platform.headless());
        r.resize(400, 300);
        r.setContent(content);
        r.update(0.016f);
        return r;
    }

    private static void frame(UIRoot r) { r.update(0.016f); }

    private static void press(UIRoot r, int key, int mods) {
        r.keyDown(key, 0, mods);
        frame(r);
    }

    private static Node card(String name) {
        return new Node().focusable(true).style(s -> s.size(40, 30)).add(new Text(name));
    }

    @Test
    void regionRebuildKeepsKeyboardFocusInPlace() {
        int[] rebuilds = {0};
        Region region = new Region(r -> {
            rebuilds[0]++;
            for (int i = 0; i < 3; i++) r.add(card("item " + i));
        });
        UIRoot r = root(region);
        press(r, KeyEvent.KEY_TAB, 0);
        press(r, KeyEvent.KEY_TAB, 0);
        assertEquals("item 1", r.focused().accessibleName());

        region.invalidate();
        frame(r);
        assertEquals(2, rebuilds[0]);
        Node focused = r.focused();
        assertEquals("item 1", focused.accessibleName(), "focus moved to the rebuilt element in the same place");
        assertTrue(focused.hasState("focus-visible"), "keyboard focus stays visible");
        assertSame(region.child(1), focused);
    }

    @Test
    void focusScopeTakesFocusHoldsTabAndGivesItBack() {
        boolean[] open = {false};
        Region region = new Region(r -> {
            r.add(card("a"), card("b"));
            if (open[0]) r.add(new Node().focusScope(true).add(new Button("Cancel"), new Button("Buy")));
        });
        UIRoot r = root(region);
        press(r, KeyEvent.KEY_TAB, 0);
        press(r, KeyEvent.KEY_TAB, 0);
        assertEquals("b", r.focused().accessibleName());

        open[0] = true;
        region.invalidate();
        frame(r);
        assertEquals("Cancel", r.focused().accessibleName(), "focus moves into the dialog");
        press(r, KeyEvent.KEY_TAB, 0);
        assertEquals("Buy", r.focused().accessibleName());
        press(r, KeyEvent.KEY_TAB, 0);
        assertEquals("Cancel", r.focused().accessibleName(), "Tab wraps inside the dialog");
        press(r, KeyEvent.KEY_TAB, KeyEvent.MOD_SHIFT);
        assertEquals("Buy", r.focused().accessibleName());

        open[0] = false;
        region.invalidate();
        frame(r);
        assertEquals("b", r.focused().accessibleName(), "focus returns to what opened the dialog");
        assertTrue(r.focused().hasState("focus-visible"));
    }

    @Test
    void arrowKeysMoveAlongGridRowsAndColumns() {
        Node grid = new Node().style(s -> s.cols(3).gap(4));
        List<Node> cards = new ArrayList<>();
        for (int i = 0; i < 6; i++) { Node c = card("c" + i); cards.add(c); grid.add(c); }
        UIRoot r = root(grid);
        press(r, KeyEvent.KEY_RIGHT, 0);
        assertSame(cards.get(0), r.focused(), "an arrow with nothing focused focuses the first element");
        press(r, KeyEvent.KEY_RIGHT, 0);
        assertSame(cards.get(1), r.focused());
        press(r, KeyEvent.KEY_DOWN, 0);
        assertSame(cards.get(4), r.focused(), "down keeps the column");
        press(r, KeyEvent.KEY_LEFT, 0);
        assertSame(cards.get(3), r.focused());
        press(r, KeyEvent.KEY_LEFT, 0);
        assertSame(cards.get(3), r.focused(), "nothing further left: focus stays");
        press(r, KeyEvent.KEY_UP, 0);
        assertSame(cards.get(0), r.focused());
    }

    @Test
    void arrowsPreferElementsInLineOverCloserDiagonalOnes() {
        // A sidebar entry on the left; on the right, a wide search field above a card, as in a shop catalog.
        Node side = card("side").style(s -> s.size(60, 20));
        Node search = card("search").style(s -> s.size(200, 20));
        Node item = card("item").style(s -> s.size(60, 60));
        Node top = card("top").style(s -> s.size(20, 20));
        Node sidebar = new Node().style(s -> s.col().gap(30).w(80)).add(top, side);
        Node catalog = new Node().style(s -> s.col().gap(10).pad(0, 0, 0, 40)).add(search, item);
        UIRoot r = root(new Node().style(s -> s.row().items(me.methodial.apothecary.style.Align.START)).add(sidebar, catalog));
        r.setFocus(item, true);
        press(r, KeyEvent.KEY_UP, 0);
        assertSame(search, r.focused(), "up from a card reaches the field above it, not the sidebar");
        r.setFocus(side, true);
        press(r, KeyEvent.KEY_RIGHT, 0);
        assertSame(item, r.focused(), "right from the sidebar goes across to what lines up with it");
    }

    @Test
    void textFieldKeepsLeftAndRightForItsCaret() {
        TextField field = new TextField("abc");
        Node other = card("other");
        UIRoot r = root(new Node().style(s -> s.row()).add(field, other));
        r.setFocus(field, true);
        press(r, KeyEvent.KEY_RIGHT, 0);
        assertSame(field, r.focused(), "right moved the caret, not the focus");
    }

    @Test
    void escapeLeavesATextFieldButIsNotTakenElsewhere() {
        TextField field = new TextField();
        Node card = card("card");
        UIRoot r = root(new Node().add(field, card));
        r.setFocus(field, true);
        assertTrue(r.keyDown(KeyEvent.KEY_ESCAPE, 0, 0), "escape in a field is handled: it leaves the field");
        assertNull(r.focused());
        r.setFocus(card, true);
        assertFalse(r.keyDown(KeyEvent.KEY_ESCAPE, 0, 0), "escape on a card is left to the screen, which closes");
    }

    @Test
    void accessibleNameComesFromTextThenTooltipUnlessGiven() {
        Node withText = card("Diamond Pickaxe");
        Node iconOnly = new Node().focusable(true).tooltip("Close shop");
        Node named = new Node().add(new Text("ignored")).accessibleName("Search");
        assertEquals("Diamond Pickaxe", withText.accessibleName());
        assertEquals("Close shop", iconOnly.accessibleName());
        assertEquals("Search", named.accessibleName());
        assertEquals("", new Node().accessibleName());
    }

    @Test
    void focusListenerHearsKeyboardMoves() {
        UIRoot r = root(new Node().add(card("x"), card("y")));
        List<String> heard = new ArrayList<>();
        r.onFocusChange((n, keyboard) -> heard.add((n == null ? "none" : n.accessibleName()) + (keyboard ? " (keys)" : "")));
        press(r, KeyEvent.KEY_TAB, 0);
        press(r, KeyEvent.KEY_TAB, 0);
        r.setFocus(null);
        assertEquals(List.of("x (keys)", "y (keys)", "none"), heard);
        assertNull(r.focused());
        assertFalse(heard.isEmpty());
    }
}
