package me.methodial.apothecary;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.widget.TextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Presses and clicks through a painted tree: what the modifiers do, where popups open. */
class PointerTest {
    private final TestCanvas canvas = new TestCanvas();

    private UIRoot root(Node content) {
        UIRoot r = new UIRoot(Platform.headless());
        r.resize(400, 300);
        r.setContent(content);
        frame(r);
        return r;
    }

    // Painted, so hit-testing sees where everything is.
    private void frame(UIRoot r) { r.render(canvas, 0.016f); }

    private static void click(UIRoot r, float x, float y, int mods) {
        r.mouseDown(x, y, MouseEvent.LEFT, mods);
        r.mouseUp(x, y, MouseEvent.LEFT);
    }

    @Test
    void onlyAShiftClickExtendsTheSelection() {
        TextField field = new TextField("hello world");
        field.style(s -> s.size(200, 20));
        UIRoot r = root(new Node().add(field));
        Rect b = field.globalBounds();
        float y = b.y() + b.height() / 2;
        r.setFocus(field);
        // Shift+Left selects a character; shift is then let go, which the field must not miss.
        r.keyDown(KeyEvent.KEY_LEFT, 0, KeyEvent.MOD_SHIFT);
        assertEquals("d", field.selectedText());

        click(r, b.x() + 20, y, 0);
        assertEquals("", field.selectedText(), "a plain click after a shift chord only places the caret");
        click(r, b.x() + 60, y, KeyEvent.MOD_SHIFT);
        assertFalse(field.selectedText().isEmpty(), "a shift-click selects from the caret to the click");
    }

    @Test
    void aPopupAtTheCursorStaysWhereItOpened() {
        Node area = new Node().style(s -> s.size(300, 200));
        Node menu = new Node().style(s -> s.size(40, 30));
        UIRoot r = root(new Node().add(area));
        area.on(EventType.CONTEXT_MENU, e -> r.showPopup(menu, area, UIRoot.Placement.CURSOR, null));
        r.mouseDown(50, 60, MouseEvent.RIGHT);
        r.mouseUp(50, 60, MouseEvent.RIGHT);
        frame(r);
        assertTrue(r.isOpen(menu));
        assertEquals(50, menu.globalBounds().x(), 0.01f);
        assertEquals(60, menu.globalBounds().y(), 0.01f);

        // Presses that keep it open (on the menu, on its anchor) don't drag it along.
        click(r, 70, 75, 0);
        frame(r);
        click(r, 200, 150, 0);
        frame(r);
        assertTrue(r.isOpen(menu));
        assertEquals(50, menu.globalBounds().x(), 0.01f);
        assertEquals(60, menu.globalBounds().y(), 0.01f);
    }
}
