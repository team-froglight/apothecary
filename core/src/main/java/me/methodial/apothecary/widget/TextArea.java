package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.text.TextLayout;
import me.methodial.apothecary.theme.Tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A multi-line text input. Lines wrap at the box width (at spaces when possible) and Enter inserts a line
 * break ({@link #singleParagraph} turns breaks off, for long one-line values such as data strings). Caret,
 * selection, clipboard and up/down/home/end navigation work on the wrapped lines; the view scrolls to keep
 * the caret visible. Height = {@link #rows} lines unless sized by style. Type selector: {@code textfield}
 * with class {@code textarea}, so it shares the text field look.
 */
public class TextArea extends ValueWidget<String> {
    private int caret, anchor;
    private float scrollY;
    private int rows = 3;
    private String placeholder = "";
    private int maxLength = 4096;
    private boolean singleParagraph;
    private Predicate<String> filter = s -> true;
    private Consumer<String> onSubmit;
    private float blinkStart;
    private boolean shiftHeld;
    /** Wrapped lines from the last paint: start and end offsets into the value. */
    private final List<int[]> lines = new ArrayList<>();
    private float lastWidth = -1;
    private String lastWrapped;

    public TextArea() { this(""); }

    public TextArea(String initial) {
        super("textfield", initial == null ? "" : initial);
        cls("textarea");
        focusable(true);
        caret = anchor = value.length();
        on(EventType.CHAR, e -> {
            if (Character.isISOControl(e.codepoint())) return;
            replaceSelection(e.text());
            e.preventDefault();
        });
        on(EventType.KEY_DOWN, e -> { if (handleKey(e)) e.preventDefault(); });
        on(EventType.MOUSE_DOWN, e -> {
            int idx = indexAt(e.localX(), e.localY());
            caret = idx;
            if (!shiftHeld) anchor = idx;
            if (e.clickCount() >= 2) { anchor = 0; caret = value.length(); }
            resetBlink();
        });
        on(EventType.DRAG, e -> { caret = indexAt(e.localX(), e.localY()); resetBlink(); });
        on(EventType.FOCUS, e -> resetBlink());
        on(EventType.SCROLL, e -> {
            float lh = lineHeight();
            float max = Math.max(0, lines.size() * lh - contentBox().height());
            float next = Math.max(0, Math.min(max, scrollY - e.deltaY() * lh));
            if (next != scrollY) { scrollY = next; e.preventDefault(); }
        });
    }

    public TextArea rows(int r) { this.rows = Math.max(1, r); markLayoutDirty(); return this; }
    public TextArea placeholder(String p) { this.placeholder = p == null ? "" : p; return this; }
    public TextArea maxLength(int n) { this.maxLength = n; return this; }
    public TextArea filter(Predicate<String> f) { this.filter = f; return this; }
    /** Ctrl+Enter (or Enter, with {@link #singleParagraph}) calls this. */
    public TextArea onSubmit(Consumer<String> c) { this.onSubmit = c; return this; }
    /** No line breaks: Enter submits, pasted breaks become spaces; long text still wraps for display. */
    public TextArea singleParagraph(boolean on) { this.singleParagraph = on; return this; }

    public TextArea text(String s) {
        setValue(s == null ? "" : s, true);
        caret = anchor = value.length();
        return this;
    }

    @Override
    protected void valueChanged(String old, String now) {
        caret = Math.min(caret, now.length());
        anchor = Math.min(anchor, now.length());
        lastWrapped = null;
    }

    private void resetBlink() { blinkStart = root() != null ? root().time() : 0; }

    private int selStart() { return Math.min(caret, anchor); }
    private int selEnd() { return Math.max(caret, anchor); }
    private boolean hasSelection() { return caret != anchor; }

    private void replaceSelection(String s) {
        if (singleParagraph) s = s.replaceAll("[\\r\\n]+", " ");
        String next = value.substring(0, selStart()) + s + value.substring(selEnd());
        if (next.length() > maxLength || !filter.test(next)) return;
        int pos = selStart() + s.length();
        setValue(next, true);
        caret = anchor = pos;
        resetBlink();
    }

    private boolean handleKey(KeyEvent e) {
        shiftHeld = e.shift();
        boolean word = e.primary();
        int k = e.key();
        if (word && k == KeyEvent.KEY_A) { anchor = 0; caret = value.length(); return true; }
        if (word && (k == KeyEvent.KEY_C || k == KeyEvent.KEY_X)) {
            if (hasSelection() && root() != null) root().platform().setClipboard(value.substring(selStart(), selEnd()));
            if (k == KeyEvent.KEY_X && hasSelection()) replaceSelection("");
            return true;
        }
        if (word && k == KeyEvent.KEY_V) {
            if (root() != null) replaceSelection(root().platform().clipboard().replace("\r", ""));
            return true;
        }
        switch (k) {
            case KeyEvent.KEY_BACKSPACE -> {
                if (!hasSelection()) anchor = Math.max(0, caret - 1);
                replaceSelection("");
            }
            case KeyEvent.KEY_DELETE -> {
                if (!hasSelection()) anchor = Math.min(value.length(), caret + 1);
                replaceSelection("");
            }
            case KeyEvent.KEY_LEFT -> move(hasSelection() && !e.shift() ? selStart() : caret - 1, e.shift());
            case KeyEvent.KEY_RIGHT -> move(hasSelection() && !e.shift() ? selEnd() : caret + 1, e.shift());
            case KeyEvent.KEY_UP -> move(vertical(-1), e.shift());
            case KeyEvent.KEY_DOWN -> move(vertical(1), e.shift());
            case KeyEvent.KEY_HOME -> move(word ? 0 : lineStart(lineOf(caret)), e.shift());
            case KeyEvent.KEY_END -> move(word ? value.length() : lineEnd(lineOf(caret)), e.shift());
            case KeyEvent.KEY_ENTER, KeyEvent.KEY_KP_ENTER -> {
                if ((word || singleParagraph) && onSubmit != null) onSubmit.accept(value);
                else if (!singleParagraph) replaceSelection("\n");
                else return false;
            }
            case KeyEvent.KEY_ESCAPE, KeyEvent.KEY_TAB -> { return false; }
            default -> { return k >= 32 && k < 256; }
        }
        resetBlink();
        return true;
    }

    private void move(int to, boolean extend) {
        caret = Math.max(0, Math.min(value.length(), to));
        if (!extend) anchor = caret;
    }

    // ------------------------------------------------------------------ wrapped lines

    private TextMeasurer m() { return root().platform().text(); }

    private float lineHeight() {
        ComputedStyle cs = computed();
        if (cs == null || root() == null) return 9;
        return m().lineHeight(cs.font()) + 1;
    }

    private void wrap(TextMeasurer m, FontSpec font, float width) {
        if (value.equals(lastWrapped) && width == lastWidth) return;
        lastWrapped = value;
        lastWidth = width;
        lines.clear();
        int start = 0;
        while (true) {
            int nl = value.indexOf('\n', start);
            int end = nl < 0 ? value.length() : nl;
            wrapParagraph(m, font, width, start, end);
            if (nl < 0) break;
            start = nl + 1;
        }
    }

    private void wrapParagraph(TextMeasurer m, FontSpec font, float width, int start, int end) {
        if (start == end) { lines.add(new int[]{start, end}); return; }
        int s = start;
        while (s < end) {
            int e = s + 1;
            int lastSpace = -1;
            while (e <= end && m.width(value.substring(s, e), font) <= width) {
                if (value.charAt(e - 1) == ' ') lastSpace = e;
                e++;
            }
            e--;
            if (e >= end) { lines.add(new int[]{s, end}); return; }
            if (e <= s) e = s + 1;
            int cut = lastSpace > s ? lastSpace : e;
            lines.add(new int[]{s, cut});
            s = cut;
        }
    }

    private int lineOf(int index) {
        for (int i = lines.size() - 1; i >= 0; i--) if (index >= lines.get(i)[0]) return i;
        return 0;
    }

    private int lineStart(int line) { return lines.isEmpty() ? 0 : lines.get(line)[0]; }

    private int lineEnd(int line) { return lines.isEmpty() ? value.length() : lines.get(line)[1]; }

    private int vertical(int dir) {
        if (lines.isEmpty() || root() == null) return caret;
        int line = lineOf(caret), target = line + dir;
        if (target < 0) return 0;
        if (target >= lines.size()) return value.length();
        FontSpec font = computed().font();
        float x = m().width(value.substring(lineStart(line), caret), font);
        int[] l = lines.get(target);
        return l[0] + TextLayout.hitIndex(m(), value.substring(l[0], l[1]), font, x);
    }

    private int indexAt(float lx, float ly) {
        if (root() == null || computed() == null || lines.isEmpty()) return value.length();
        Rect c = contentBox();
        int line = (int) Math.floor((ly - c.y() + scrollY) / lineHeight());
        line = Math.max(0, Math.min(lines.size() - 1, line));
        int[] l = lines.get(line);
        return l[0] + TextLayout.hitIndex(m(), value.substring(l[0], l[1]), computed().font(), lx - c.x());
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        return new float[]{120, rows * lineHeight()};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        ComputedStyle cs = computed();
        FontSpec font = cs.font();
        TextMeasurer m = c.text();
        float lh = lineHeight();
        wrap(m, font, Math.max(4, r.width() - 2));
        // Keep the caret line in view.
        int cl = lineOf(caret);
        if (cl * lh - scrollY < 0) scrollY = cl * lh;
        if ((cl + 1) * lh - scrollY > r.height()) scrollY = (cl + 1) * lh - r.height();
        scrollY = Math.max(0, Math.min(scrollY, Math.max(0, lines.size() * lh - r.height())));

        c.pushClip(new Rect(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2));
        if (value.isEmpty() && !placeholder.isEmpty()) {
            c.drawText(placeholder, r.x(), r.y(), font, cs.token(Tokens.PLACEHOLDER), false, cs.get(Props.TEXT_DECORATION));
        }
        float glyph = m.lineHeight(font);
        for (int i = 0; i < lines.size(); i++) {
            float y = r.y() + i * lh - scrollY;
            if (y + lh < r.y() || y > r.bottom()) continue;
            int[] l = lines.get(i);
            String text = value.substring(l[0], l[1]);
            if (hasSelection() && isFocused()) {
                int a = Math.max(selStart(), l[0]), b = Math.min(selEnd(), l[1]);
                if (a < b || (a == b && selStart() <= l[0] && selEnd() > l[1])) {
                    float x0 = m.width(value.substring(l[0], a), font), x1 = m.width(value.substring(l[0], b), font);
                    c.fillRect(new Rect(r.x() + x0, y - 0.5f, Math.max(2, x1 - x0), glyph + 1), cs.token(Tokens.SELECTION));
                }
            }
            if (!text.isEmpty()) c.drawText(text, r.x(), y, font, cs.color(), cs.get(Props.TEXT_SHADOW), cs.get(Props.TEXT_DECORATION));
            if (isFocused() && i == cl) {
                float t = (root().time() - blinkStart) % 1.0f;
                float cx = m.width(value.substring(l[0], Math.min(caret, l[1])), font);
                if (t < 0.55f) c.fillRect(new Rect(r.x() + cx - 0.5f, y - 0.5f, 1, glyph + 1), cs.token(Tokens.CARET));
            }
        }
        c.popClip();
    }
}
