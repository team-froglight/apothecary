package me.methodial.apothecary.text;

import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.TextTransform;
import me.methodial.apothecary.style.WhiteSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Breaks text into lines: word wrapping, hard breaks, long-word splitting, max lines and ellipsis. */
public final class TextLayout {
    public record Line(String text, float width) { }

    public record Result(List<Line> lines, float width, float lineHeight) {
        public float height() { return lines.size() * lineHeight; }
    }

    private TextLayout() { }

    public static String transform(String text, TextTransform t) {
        return switch (t) {
            case NONE -> text;
            case UPPERCASE -> text.toUpperCase(Locale.ROOT);
            case LOWERCASE -> text.toLowerCase(Locale.ROOT);
            case CAPITALIZE -> {
                StringBuilder sb = new StringBuilder(text.length());
                boolean start = true;
                for (char c : text.toCharArray()) {
                    sb.append(start ? Character.toUpperCase(c) : c);
                    start = Character.isWhitespace(c);
                }
                yield sb.toString();
            }
        };
    }

    /**
     * @param maxWidth wrap width, NaN for none
     * @param maxLines 0 = unlimited
     */
    public static Result layout(TextMeasurer m, String text, FontSpec font, float lineHeight, float maxWidth,
                                WhiteSpace ws, int maxLines, boolean ellipsis) {
        List<Line> lines = new ArrayList<>();
        boolean wrap = (ws == WhiteSpace.NORMAL || ws == WhiteSpace.PRE_WRAP) && !Float.isNaN(maxWidth);
        boolean keepNewlines = ws == WhiteSpace.PRE || ws == WhiteSpace.PRE_WRAP || ws == WhiteSpace.NORMAL;
        String src = ws == WhiteSpace.NORMAL || ws == WhiteSpace.NOWRAP ? collapse(text, ws == WhiteSpace.NORMAL) : text;
        String[] paragraphs = keepNewlines ? src.split("\n", -1) : new String[]{src.replace('\n', ' ')};
        boolean truncated = false;
        outer:
        for (String para : paragraphs) {
            if (!wrap) {
                lines.add(new Line(para, m.width(para, font)));
            } else {
                wrapParagraph(m, para, font, maxWidth, lines);
            }
            if (maxLines > 0 && lines.size() > maxLines) { truncated = true; break outer; }
        }
        if (maxLines > 0 && lines.size() > maxLines) {
            truncated = true;
            while (lines.size() > maxLines) lines.remove(lines.size() - 1);
        }
        if (ellipsis && !lines.isEmpty() && !Float.isNaN(maxWidth)) {
            int last = lines.size() - 1;
            Line l = lines.get(last);
            if (truncated || l.width > maxWidth + 0.01f) lines.set(last, ellipsize(m, l.text, font, maxWidth, truncated));
            for (int i = 0; i < last; i++) {
                if (lines.get(i).width > maxWidth + 0.01f) lines.set(i, ellipsize(m, lines.get(i).text, font, maxWidth, false));
            }
        }
        float w = 0;
        for (Line l : lines) w = Math.max(w, l.width);
        return new Result(lines, w, lineHeight);
    }

    private static String collapse(String s, boolean keepNewlines) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean space = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' && keepNewlines) { sb.append('\n'); space = false; continue; }
            if (Character.isWhitespace(c)) {
                if (!space) sb.append(' ');
                space = true;
            } else {
                sb.append(c);
                space = false;
            }
        }
        return sb.toString();
    }

    private static void wrapParagraph(TextMeasurer m, String para, FontSpec font, float maxWidth, List<Line> out) {
        if (para.isEmpty()) { out.add(new Line("", 0)); return; }
        // Break opportunities: spaces, and after hyphens inside words ("ease-in-out" → "ease-" | "in-" | "out").
        List<String> pieces = new ArrayList<>();
        List<Boolean> spaced = new ArrayList<>();   // piece starts a new word (needs a space before it)
        for (String word : para.split(" ", -1)) {
            int start = 0;
            boolean first = true;
            for (int i = 1; i < word.length() - 1; i++) {
                if (word.charAt(i) == '-' && word.charAt(i - 1) != '-') {
                    pieces.add(word.substring(start, i + 1));
                    spaced.add(first);
                    first = false;
                    start = i + 1;
                }
            }
            pieces.add(word.substring(start));
            spaced.add(first);
        }
        StringBuilder line = new StringBuilder();
        float lineW = 0;
        for (int k = 0; k < pieces.size(); k++) {
            String piece = pieces.get(k);
            if (line.isEmpty()) {
                float pw = m.width(piece, font);
                if (pw > maxWidth && piece.length() > 1 && maxWidth > 0) {
                    lineW = breakLongWord(m, piece, font, maxWidth, out, line);
                } else {
                    line.append(piece);
                    lineW = pw;
                }
                continue;
            }
            String candidate = line + (spaced.get(k) ? " " : "") + piece;
            float cw = m.width(candidate, font);
            if (cw <= maxWidth + 0.01f) {
                line.setLength(0);
                line.append(candidate);
                lineW = cw;
            } else {
                out.add(new Line(line.toString().stripTrailing(), m.width(line.toString().stripTrailing(), font)));
                line.setLength(0);
                float pw = m.width(piece, font);
                if (pw > maxWidth && piece.length() > 1 && maxWidth > 0) {
                    lineW = breakLongWord(m, piece, font, maxWidth, out, line);
                } else {
                    line.append(piece);
                    lineW = pw;
                }
            }
        }
        out.add(new Line(line.toString(), lineW));
    }

    /** Splits a word longer than the line; leaves the tail in {@code line} and returns its width. */
    private static float breakLongWord(TextMeasurer m, String word, FontSpec font, float maxWidth, List<Line> out, StringBuilder line) {
        int start = 0;
        while (start < word.length()) {
            int end = start + 1;
            while (end < word.length() && m.width(word.substring(start, end + 1), font) <= maxWidth) end++;
            String part = word.substring(start, end);
            if (end >= word.length()) {
                line.append(part);
                return m.width(part, font);
            }
            out.add(new Line(part, m.width(part, font)));
            start = end;
        }
        return 0;
    }

    private static Line ellipsize(TextMeasurer m, String text, FontSpec font, float maxWidth, boolean forceDots) {
        String dots = "…";
        float dw = m.width(dots, font);
        if (!forceDots && m.width(text, font) <= maxWidth) return new Line(text, m.width(text, font));
        int end = text.length();
        while (end > 0 && m.width(text.substring(0, end), font) + dw > maxWidth) end--;
        String s = text.substring(0, end).stripTrailing() + dots;
        return new Line(s, m.width(s, font));
    }

    /** Index of the character boundary nearest to {@code x} on a single line. */
    public static int hitIndex(TextMeasurer m, String line, FontSpec font, float x) {
        if (x <= 0) return 0;
        float prev = 0;
        for (int i = 1; i <= line.length(); i++) {
            float w = m.width(line.substring(0, i), font);
            if (x < (prev + w) / 2) return i - 1;
            prev = w;
        }
        return line.length();
    }
}
