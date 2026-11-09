package me.methodial.apothecary;

import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.widget.TextArea;
import me.methodial.apothecary.widget.TextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Caret moves and deletes in the text inputs step over whole characters, emoji included. */
class TextEditTest {
    private static final String SMILE = "😀"; // U+1F600, two chars

    private static UIRoot focus(Node input) {
        UIRoot r = new UIRoot(Platform.headless());
        r.resize(400, 300);
        r.setContent(new Node().add(input));
        r.update(0.016f);
        r.setFocus(input);
        return r;
    }

    private static void key(UIRoot r, int key, int mods) { r.keyDown(key, 0, mods); }

    @Test
    void textFieldNeverSplitsASurrogatePair() {
        TextField field = new TextField("a" + SMILE + "b");
        UIRoot r = focus(field);
        key(r, KeyEvent.KEY_LEFT, 0);               // before b
        key(r, KeyEvent.KEY_LEFT, KeyEvent.MOD_SHIFT);
        assertEquals(SMILE, field.selectedText(), "one step left selects the whole emoji");
        key(r, KeyEvent.KEY_RIGHT, 0);              // collapses to the selection's end, before b
        key(r, KeyEvent.KEY_BACKSPACE, 0);
        assertEquals("ab", field.text());

        field.text(SMILE + SMILE);
        key(r, KeyEvent.KEY_HOME, 0);
        key(r, KeyEvent.KEY_DELETE, 0);
        assertEquals(SMILE, field.text());
    }

    @Test
    void textAreaNeverSplitsASurrogatePair() {
        TextArea area = new TextArea("a" + SMILE);
        UIRoot r = focus(area);
        key(r, KeyEvent.KEY_BACKSPACE, 0);
        assertEquals("a", area.value());

        area.text(SMILE + "b");
        key(r, KeyEvent.KEY_LEFT, 0);
        key(r, KeyEvent.KEY_LEFT, 0);
        key(r, KeyEvent.KEY_DELETE, 0);
        assertEquals("b", area.value());
        key(r, KeyEvent.KEY_RIGHT, 0);
        key(r, KeyEvent.KEY_BACKSPACE, 0);
        assertEquals("", area.value());
    }
}
