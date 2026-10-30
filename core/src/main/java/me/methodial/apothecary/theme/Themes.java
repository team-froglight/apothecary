package me.methodial.apothecary.theme;

import me.methodial.apothecary.style.Color;

import static me.methodial.apothecary.theme.Tokens.*;

/**
 * Ready-made themes. Each sets only base tokens; derived ones (accent-hover, radius scale, shadows...)
 * follow. Compose them: {@code ui.theme(Themes.OCEAN.with(Themes.COMPACT))}.
 */
public final class Themes {
    private Themes() { }

    private static Color c(String hex) { return Color.hex(hex); }

    /** Dark chrome shared by the dark palettes. */
    private static Theme dark(String name) {
        return Theme.of(name)
                .set(TEXT, c("#e9eaf2")).set(TEXT_DIM, c("#a2a6bc")).set(TEXT_FAINT, c("#6c7088"))
                .set(BORDER, c("#ffffff12")).set(BORDER_STRONG, c("#ffffff24"))
                .set(HOVER_TINT, c("#ffffff0f")).set(PRESS_TINT, c("#ffffff18"))
                .set(SHADOW_COLOR, c("#000000")).set(BACKDROP, c("#05060acc"));
    }

    public static final Theme AMETHYST = dark("Amethyst")
            .set(BG, c("#0d0e14")).set(SURFACE, c("#151720")).set(SURFACE_2, c("#1c1f2b")).set(SURFACE_3, c("#252938"))
            .set(SURFACE_HOVER, c("#2b3042")).set(ACCENT, c("#8b7bff")).set(ACCENT_2, c("#4fd8c4"));

    public static final Theme OCEAN = dark("Ocean")
            .set(BG, c("#081218")).set(SURFACE, c("#0d1a22")).set(SURFACE_2, c("#12232d")).set(SURFACE_3, c("#1a303c"))
            .set(SURFACE_HOVER, c("#213b49")).set(TEXT, c("#e3f3f7")).set(TEXT_DIM, c("#97b6c0")).set(TEXT_FAINT, c("#5e7d88"))
            .set(ACCENT, c("#2ec4d6")).set(ACCENT_2, c("#5a8cff"));

    public static final Theme FOREST = dark("Forest")
            .set(BG, c("#0b120d")).set(SURFACE, c("#111a13")).set(SURFACE_2, c("#17241a")).set(SURFACE_3, c("#203223"))
            .set(SURFACE_HOVER, c("#273d2b")).set(TEXT, c("#e8f2e6")).set(TEXT_DIM, c("#a3b9a0")).set(TEXT_FAINT, c("#6a8068"))
            .set(ACCENT, c("#5fcf6a")).set(ACCENT_2, c("#e2c55a"));

    public static final Theme NETHER = dark("Nether")
            .set(BG, c("#140a0a")).set(SURFACE, c("#1d0f0f")).set(SURFACE_2, c("#271414")).set(SURFACE_3, c("#341b1a"))
            .set(SURFACE_HOVER, c("#432221")).set(TEXT, c("#f6e7e3")).set(TEXT_DIM, c("#c4a29b")).set(TEXT_FAINT, c("#8a6a64"))
            .set(ACCENT, c("#ff6b3d")).set(ACCENT_2, c("#ffcc4d"));

    public static final Theme DAYLIGHT = Theme.of("Daylight")
            .set(BG, c("#eef0f6")).set(SURFACE, c("#ffffff")).set(SURFACE_2, c("#f6f7fb")).set(SURFACE_3, c("#e8ebf3"))
            .set(SURFACE_HOVER, c("#dde1ec")).set(TEXT, c("#1b1d2a")).set(TEXT_DIM, c("#545a72")).set(TEXT_FAINT, c("#8a90a6"))
            .set(BORDER, c("#0000001a")).set(BORDER_STRONG, c("#0000002e"))
            .set(HOVER_TINT, c("#0000000c")).set(PRESS_TINT, c("#00000014"))
            .set(ACCENT, c("#6a52ff")).set(ACCENT_2, c("#00a896"))
            .set(SHADOW_COLOR, c("#1b1d2a")).set(BACKDROP, c("#1b1d2a66"))
            .set(TOOLTIP_BG, c("#fffffff5")).set(FIELD_FOCUS_BG, c("#ffffff"));

    /** Smaller corner radii; layer on top of a palette. */
    public static final Theme COMPACT = Theme.of("Compact").set(RADIUS, 2f);

    /** Rounder everything. */
    public static final Theme SOFT = Theme.of("Soft").set(RADIUS, 8f);
}
