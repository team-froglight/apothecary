package me.methodial.apothecary.theme;

import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Shadow;

import java.util.List;

/**
 * The built-in design tokens every widget is styled with. Re-theme by assigning new values
 * ({@link Themes}); derived tokens (hover/press/soft accents, radius scale) follow automatically.
 * <pre>{@code import static me.methodial.apothecary.theme.Tokens.*;  // then: s.bg(SURFACE).fg(TEXT_DIM)}</pre>
 */
public final class Tokens {
    private Tokens() { }

    // ---------------------------------------------------------------- surfaces & text

    public static final Token<Color> BG = Token.of("bg", Color.hex("#0d0e14"));
    public static final Token<Color> SURFACE = Token.of("surface", Color.hex("#151720"));
    public static final Token<Color> SURFACE_2 = Token.of("surface-2", Color.hex("#1c1f2b"));
    public static final Token<Color> SURFACE_3 = Token.of("surface-3", Color.hex("#252938"));
    public static final Token<Color> SURFACE_HOVER = Token.of("surface-hover", Color.hex("#2b3042"));
    public static final Token<Color> BORDER = Token.of("border", Color.hex("#ffffff12"));
    public static final Token<Color> BORDER_STRONG = Token.of("border-strong", Color.hex("#ffffff24"));
    public static final Token<Color> TEXT = Token.of("text", Color.hex("#e9eaf2"));
    public static final Token<Color> TEXT_DIM = Token.of("text-dim", Color.hex("#a2a6bc"));
    public static final Token<Color> TEXT_FAINT = Token.of("text-faint", Color.hex("#6c7088"));
    /** Overlay tints for hover/press on transparent elements (white on dark themes, black on light). */
    public static final Token<Color> HOVER_TINT = Token.of("hover-tint", Color.hex("#ffffff0f"));
    public static final Token<Color> PRESS_TINT = Token.of("press-tint", Color.hex("#ffffff18"));

    // ---------------------------------------------------------------- brand & status

    public static final Token<Color> ACCENT = Token.of("accent", Color.hex("#8b7bff"));
    public static final Token<Color> ACCENT_HOVER = Token.derived("accent-hover", t -> t.get(ACCENT).lighten(0.16f));
    public static final Token<Color> ACCENT_PRESS = Token.derived("accent-press", t -> t.get(ACCENT).lighten(-0.12f));
    public static final Token<Color> ACCENT_SOFT = Token.derived("accent-soft", t -> t.get(ACCENT).withAlpha(0.18f));
    public static final Token<Color> ACCENT_TEXT = Token.of("accent-text", Color.WHITE);
    public static final Token<Color> ACCENT_2 = Token.of("accent-2", Color.hex("#4fd8c4"));
    public static final Token<Color> SUCCESS = Token.of("success", Color.hex("#43d17a"));
    public static final Token<Color> WARNING = Token.of("warning", Color.hex("#f5b83d"));
    public static final Token<Color> DANGER = Token.of("danger", Color.hex("#f2667a"));
    public static final Token<Color> INFO = Token.of("info", Color.hex("#5aa7ff"));

    // ---------------------------------------------------------------- shape & depth

    public static final Token<Float> RADIUS = Token.of("radius", 5f);
    public static final Token<Float> RADIUS_SM = Token.derived("radius-sm", t -> t.get(RADIUS) * 0.6f);
    public static final Token<Float> RADIUS_LG = Token.derived("radius-lg", t -> t.get(RADIUS) * 1.6f);
    public static final Token<Color> SHADOW_COLOR = Token.of("shadow-color", Color.hex("#000000"));
    public static final Token<List<Shadow>> SHADOW_SM = Token.derived("shadow-sm", t -> List.of(Shadow.of(0, 1, 3, t.get(SHADOW_COLOR).withAlpha(0.4f))));
    public static final Token<List<Shadow>> SHADOW = Token.derived("shadow", t -> List.of(Shadow.of(0, 4, 14, t.get(SHADOW_COLOR).withAlpha(0.56f))));
    public static final Token<List<Shadow>> SHADOW_LG = Token.derived("shadow-lg", t -> List.of(Shadow.of(0, 10, 30, t.get(SHADOW_COLOR).withAlpha(0.7f))));

    // ---------------------------------------------------------------- type

    public static final Token<String> FONT = Token.of("font", "poppins");
    public static final Token<String> MONO = Token.of("mono", "space-mono");

    // ---------------------------------------------------------------- widget specifics

    public static final Token<Color> SELECTION = Token.derived("selection", t -> t.get(ACCENT).withAlpha(0.4f));
    public static final Token<Color> CARET = Token.derived("caret", t -> t.get(TEXT));
    public static final Token<Color> PLACEHOLDER = Token.derived("placeholder", t -> t.get(TEXT_FAINT));
    public static final Token<Color> FIELD_FOCUS_BG = Token.derived("field-focus-bg", t -> t.get(BG));
    public static final Token<Color> TOOLTIP_BG = Token.derived("tooltip-bg", t -> t.get(SURFACE_3).withAlpha(0.96f));
    public static final Token<Color> SCROLLBAR = Token.derived("scrollbar", t -> t.get(TEXT).withAlpha(0.25f));
    public static final Token<Color> BACKDROP = Token.of("backdrop", Color.hex("#05060acc"));
}
