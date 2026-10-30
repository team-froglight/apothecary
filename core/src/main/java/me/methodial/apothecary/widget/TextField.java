package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.text.TextLayout;
import me.methodial.apothecary.theme.Tokens;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A single-line text input: caret, selection (mouse, shift+arrows, double click), clipboard,
 * word navigation (ctrl), undo-free but filterable ({@link #filter}), max length, password mode and
 * placeholder. Colors: text = {@code color}; selection, caret and placeholder come from the
 * {@code SELECTION}, {@code CARET} and {@code PLACEHOLDER} tokens. Type: {@code textfield}.
 */
public class TextField extends ValueWidget<String> {
    private int caret, anchor;
    private float scroll;
    private String placeholder = "";
    private int maxLength = 256;
    private Predicate<String> filter = s -> true;
    private boolean password;
    private Consumer<String> onSubmit;
    private float blinkStart;

    public TextField() { this(""); }

    public TextField(String initial) {
        super("textfield", initial == null ? "" : initial);
        focusable(true);
        caret = anchor = value.length();
        on(EventType.CHAR, e -> {
            if (Character.isISOControl(e.codepoint())) return;
            insert(e.text());
            e.preventDefault();
        });
        on(EventType.KEY_DOWN, e -> { if (handleKey(e)) e.preventDefault(); });
        on(EventType.MOUSE_DOWN, e -> {
            int idx = indexAt(e.localX());
            if (e.clickCount() == 2) selectWord(idx);
            else if (e.clickCount() >= 3) { anchor = 0; caret = value.length(); }
            else {
                caret = idx;
                if (root() == null || !isShiftHeld()) anchor = idx;
            }
            resetBlink();
        });
        on(EventType.DRAG, e -> { caret = indexAt(e.localX()); resetBlink(); });
        on(EventType.FOCUS, e -> resetBlink());
    }

    private boolean shiftHeld;
    private boolean isShiftHeld() { return shiftHeld; }

    public TextField placeholder(String p) { this.placeholder = p == null ? "" : p; return this; }
    public TextField maxLength(int n) { this.maxLength = n; return this; }
    /** Rejects edits whose resulting text doesn't pass (e.g. {@code s -> s.matches("\\d*")}). */
    public TextField filter(Predicate<String> f) { this.filter = f; return this; }
    public TextField password(boolean p) { this.password = p; return this; }
    public TextField onSubmit(Consumer<String> c) { this.onSubmit = c; return this; }

    public String text() { return value; }

    public TextField text(String s) {
        setValue(s == null ? "" : s, true);
        caret = anchor = value.length();
        return this;
    }

    public void selectAll() { anchor = 0; caret = value.length(); }

    public String selectedText() { return value.substring(selStart(), selEnd()); }

    private int selStart() { return Math.min(caret, anchor); }
    private int selEnd() { return Math.max(caret, anchor); }
    private boolean hasSelection() { return caret != anchor; }

    @Override
    protected void valueChanged(String old, String now) {
        caret = Math.min(caret, now.length());
        anchor = Math.min(anchor, now.length());
        markLayoutDirty();
    }

    private void resetBlink() { blinkStart = root() != null ? root().time() : 0; }

    private void replaceSelection(String s) {
        String next = value.substring(0, selStart()) + s + value.substring(selEnd());
        if (next.length() > maxLength || !filter.test(next)) return;
        int pos = selStart() + s.length();
        setValue(next, true);
        caret = anchor = pos;
        resetBlink();
    }

    private void insert(String s) { replaceSelection(s); }

    private boolean handleKey(KeyEvent e) {
        shiftHeld = e.shift();
        boolean word = e.primary();
        int k = e.key();
        if (word && k == KeyEvent.KEY_A) { selectAll(); return true; }
        if (word && (k == KeyEvent.KEY_C || k == KeyEvent.KEY_X)) {
            if (hasSelection() && !password && root() != null) root().platform().setClipboard(selectedText());
            if (k == KeyEvent.KEY_X && hasSelection()) replaceSelection("");
            return true;
        }
        if (word && k == KeyEvent.KEY_V) {
            if (root() != null) insert(root().platform().clipboard().replaceAll("[\\r\\n]+", " "));
            return true;
        }
        switch (k) {
            case KeyEvent.KEY_BACKSPACE -> {
                if (!hasSelection()) anchor = word ? wordLeft(caret) : Math.max(0, caret - 1);
                replaceSelection("");
            }
            case KeyEvent.KEY_DELETE -> {
                if (!hasSelection()) anchor = word ? wordRight(caret) : Math.min(value.length(), caret + 1);
                replaceSelection("");
            }
            case KeyEvent.KEY_LEFT -> move(hasSelection() && !e.shift() ? selStart() : word ? wordLeft(caret) : caret - 1, e.shift());
            case KeyEvent.KEY_RIGHT -> move(hasSelection() && !e.shift() ? selEnd() : word ? wordRight(caret) : caret + 1, e.shift());
            case KeyEvent.KEY_HOME -> move(0, e.shift());
            case KeyEvent.KEY_END -> move(value.length(), e.shift());
            case KeyEvent.KEY_ENTER, KeyEvent.KEY_KP_ENTER -> {
                if (onSubmit != null) onSubmit.accept(value);
                else return false;
            }
            case KeyEvent.KEY_ESCAPE, KeyEvent.KEY_TAB -> { return false; }
            default -> {
                // Swallow other keys so typing doesn't trigger game keybinds (e.g. inventory "E").
                return k >= 32 && k < 256;
            }
        }
        resetBlink();
        return true;
    }

    private void move(int to, boolean extend) {
        caret = Math.max(0, Math.min(value.length(), to));
        if (!extend) anchor = caret;
    }

    private int wordLeft(int i) {
        i = Math.max(0, i - 1);
        while (i > 0 && Character.isWhitespace(value.charAt(i))) i--;
        while (i > 0 && !Character.isWhitespace(value.charAt(i - 1))) i--;
        return i;
    }

    private int wordRight(int i) {
        int n = value.length();
        while (i < n && Character.isWhitespace(value.charAt(i))) i++;
        while (i < n && !Character.isWhitespace(value.charAt(i))) i++;
        return i;
    }

    private void selectWord(int idx) {
        anchor = wordLeft(Math.min(value.length(), idx + 1));
        if (anchor > idx) anchor = idx;
        caret = wordRight(idx);
    }

    private String shown() { return password ? "•".repeat(value.length()) : value; }

    private TextMeasurer m() { return root().platform().text(); }

    private int indexAt(float localX) {
        if (root() == null || computed() == null) return 0;
        Rect c = contentBox();
        return TextLayout.hitIndex(m(), shown(), computed().font(), localX - c.x() + scroll);
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        float lh = computed() == null ? 9 : (root() != null ? m().lineHeight(computed().font()) : computed().font().size());
        return new float[]{80, lh};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        ComputedStyle cs = computed();
        FontSpec font = cs.font();
        TextMeasurer m = c.text();
        String s = shown();
        float caretX = m.width(s.substring(0, caret), font);
        // keep the caret in view
        if (caretX - scroll > r.width() - 2) scroll = caretX - r.width() + 2;
        if (caretX - scroll < 0) scroll = caretX;
        float total = m.width(s, font);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - r.width() + 2)));
        float lh = m.lineHeight(font);
        float y = r.y() + (r.height() - lh) / 2;

        c.pushClip(new Rect(r.x() - 1, r.y() - 2, r.width() + 2, r.height() + 4));
        float x0 = r.x() - scroll;
        if (hasSelection() && isFocused()) {
            float a = m.width(s.substring(0, selStart()), font), b = m.width(s.substring(0, selEnd()), font);
            c.fillRect(new Rect(x0 + a, y - 1, b - a, lh + 2), cs.token(Tokens.SELECTION));
        }
        if (s.isEmpty() && !placeholder.isEmpty()) {
            c.drawText(placeholder, r.x(), y, font, cs.token(Tokens.PLACEHOLDER), false, cs.get(Props.TEXT_DECORATION));
        } else {
            c.drawText(s, x0, y, font, cs.color(), cs.get(Props.TEXT_SHADOW), cs.get(Props.TEXT_DECORATION));
        }
        if (isFocused()) {
            float t = (root().time() - blinkStart) % 1.0f;
            if (t < 0.55f) c.fillRect(new Rect(x0 + caretX - 0.5f, y - 1, 1, lh + 2), cs.token(Tokens.CARET));
        }
        c.popClip();
    }
}
