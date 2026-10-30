package me.methodial.apothecary.logo;

import java.util.HashMap;
import java.util.Map;

/**
 * The block alphabet of logo titles, after the Minecraft logo: squat capitals five blocks tall, strokes one
 * block thick with square corners (Latin, digits, punctuation, Ukrainian and Russian Cyrillic). Lowercase
 * draws as capitals; anything missing draws as {@code ?}. Drawn by {@link LogoPainter}.
 */
public final class LogoFont {
    /** Blocks per letter, top to bottom. */
    public static final int HEIGHT = 5;
    /** Blocks between letters. */
    public static final int SPACING = 1;

    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    private LogoFont() { }

    private static void g(char c, String... rows) {
        if (rows.length != HEIGHT) throw new IllegalArgumentException("Bad logo glyph '" + c + "'");
        for (String r : rows) if (r.length() != rows[0].length()) throw new IllegalArgumentException("Bad logo glyph '" + c + "'");
        GLYPHS.put(c, rows);
    }

    static {
        g('A', "####", "#..#", "####", "#..#", "#..#");
        g('B', "###.", "#..#", "###.", "#..#", "###.");
        g('C', "####", "#...", "#...", "#...", "####");
        g('D', "###.", "#..#", "#..#", "#..#", "###.");
        g('E', "####", "#...", "###.", "#...", "####");
        g('F', "####", "#...", "###.", "#...", "#...");
        g('G', "####", "#...", "#.##", "#..#", "####");
        g('H', "#..#", "#..#", "####", "#..#", "#..#");
        g('I', "#", "#", "#", "#", "#");
        g('J', "...#", "...#", "...#", "#..#", "####");
        g('K', "#..#", "#.#.", "##..", "#.#.", "#..#");
        g('L', "#...", "#...", "#...", "#...", "####");
        g('M', "#...#", "##.##", "#.#.#", "#...#", "#...#");
        g('N', "#..#", "##.#", "#.##", "#..#", "#..#");
        g('O', "####", "#..#", "#..#", "#..#", "####");
        g('P', "####", "#..#", "####", "#...", "#...");
        g('Q', "####", "#..#", "#..#", "#.##", "####");
        g('R', "####", "#..#", "###.", "#..#", "#..#");
        g('S', "####", "#...", "####", "...#", "####");
        g('T', "#####", "..#..", "..#..", "..#..", "..#..");
        g('U', "#..#", "#..#", "#..#", "#..#", "####");
        g('V', "#...#", "#...#", ".#.#.", ".#.#.", "..#..");
        g('W', "#...#", "#...#", "#.#.#", "##.##", "#...#");
        g('X', "#..#", "#..#", ".##.", "#..#", "#..#");
        g('Y', "#...#", ".#.#.", "..#..", "..#..", "..#..");
        g('Z', "####", "...#", ".##.", "#...", "####");
        g('0', "####", "#..#", "#..#", "#..#", "####");
        g('1', "##.", ".#.", ".#.", ".#.", "###");
        g('2', "####", "...#", "####", "#...", "####");
        g('3', "####", "...#", ".###", "...#", "####");
        g('4', "#..#", "#..#", "####", "...#", "...#");
        g('5', "####", "#...", "####", "...#", "####");
        g('6', "####", "#...", "####", "#..#", "####");
        g('7', "####", "...#", "..#.", ".#..", ".#..");
        g('8', "####", "#..#", "####", "#..#", "####");
        g('9', "####", "#..#", "####", "...#", "####");
        g('!', "#", "#", "#", ".", "#");
        g('?', "####", "...#", ".##.", "....", ".#..");
        g('.', ".", ".", ".", ".", "#");
        g(',', ".", ".", ".", "#", "#");
        g(':', ".", "#", ".", "#", ".");
        g('\'', "#", "#", ".", ".", ".");
        g('-', "...", "...", "###", "...", "...");
        g('+', "...", ".#.", "###", ".#.", "...");
        g('/', "..#", "..#", ".#.", "#..", "#..");
        g('×', "...", "#.#", ".#.", "#.#", "...");
        g('#', ".#.#.", "#####", ".#.#.", "#####", ".#.#.");
        g(' ', "..", "..", "..", "..", "..");
        g('Б', "####", "#...", "###.", "#..#", "###.");
        g('Г', "####", "#...", "#...", "#...", "#...");
        g('Ґ', "...#", "####", "#...", "#...", "#...");
        g('Д', ".###.", ".#.#.", ".#.#.", "#####", "#...#");
        g('Є', "####", "#...", "##..", "#...", "####");
        g('Ж', "#.#.#", "#.#.#", ".###.", "#.#.#", "#.#.#");
        g('И', "#..#", "#.##", "##.#", "#..#", "#..#");
        g('Ї', "#.#", "...", ".#.", ".#.", ".#.");
        g('Й', ".##.", "#..#", "#.##", "##.#", "#..#");
        g('Л', ".###", ".#.#", ".#.#", ".#.#", "##.#");
        g('П', "####", "#..#", "#..#", "#..#", "#..#");
        g('У', "#..#", "#..#", "####", "...#", "####");
        g('Ф', ".###.", "#.#.#", "#.#.#", ".###.", "..#..");
        g('Ц', "#..#.", "#..#.", "#..#.", "#####", "....#");
        g('Ш', "#.#.#", "#.#.#", "#.#.#", "#.#.#", "#####");
        g('Щ', "#.#.#.", "#.#.#.", "#.#.#.", "######", ".....#");
        g('Ь', "#...", "#...", "###.", "#..#", "###.");
        g('Ю', "#.###", "#.#.#", "###.#", "#.#.#", "#.###");
        g('Я', "####", "#..#", ".###", "#..#", "#..#");
        g('Ы', "#...#", "#...#", "###.#", "#.#.#", "###.#");
        g('Э', "####", "...#", ".###", "...#", "####");
        g('Ъ', "##...", ".#...", ".###.", ".#..#", ".###.");
        // Cyrillic letters shaped like Latin ones share their glyphs.
        String alias = "АAВBЕEЁEКKМMНHОOРPСCТTХXІIЗ3Ч4";
        for (int i = 0; i < alias.length(); i += 2) GLYPHS.put(alias.charAt(i), GLYPHS.get(alias.charAt(i + 1)));
    }

    private static String[] glyph(char c) {
        String[] g = GLYPHS.get(Character.toUpperCase(c));
        return g != null ? g : GLYPHS.get('?');
    }

    /** Whether {@code c} has its own glyph (rather than drawing as {@code ?}). */
    public static boolean has(char c) { return GLYPHS.containsKey(Character.toUpperCase(c)); }

    /** Width of one character, in blocks. */
    public static int width(char c) { return glyph(c)[0].length(); }

    /** Whether the block at column x, row y (0 = top) of c is solid; false outside the glyph. */
    public static boolean solid(char c, int x, int y) {
        String[] g = glyph(c);
        return y >= 0 && y < HEIGHT && x >= 0 && x < g[0].length() && g[y].charAt(x) == '#';
    }

    /** Width of text in blocks, letter spacing included. */
    public static int width(CharSequence text) {
        if (text.isEmpty()) return 0;
        int w = 0;
        for (int i = 0; i < text.length(); i++) w += width(text.charAt(i)) + SPACING;
        return w - SPACING;
    }
}
