package me.methodial.apothecary.theme;

import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.style.Tr;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static me.methodial.apothecary.sheet.Selector.*;
import static me.methodial.apothecary.theme.Colors.alpha;
import static me.methodial.apothecary.theme.Motion.*;
import static me.methodial.apothecary.theme.Tokens.*;

/**
 * Minecraft's Ore UI, after the "Minecraft – Ore UI Style" Figma kit: its color tokens and component construction,
 * at one Minecraft pixel per kit pixel-block (the kit is drawn at 4×).
 * <ul>
 *   <li><b>Buttons</b>: a 1 px near-black frame around a face with a 1 px stroke along its right and bottom, a 1 px
 *   lighter "secondary" stroke just inside its top and left, and a 2 px depth band below in the palette's shadow
 *   color. Pressing drops the depth and sinks the face; keyboard focus adds a white frame.</li>
 *   <li><b>Palettes</b> ({@link Palette}): {@link #COLORED} (green, the main action), {@link #GENERAL} (light gray),
 *   {@link #GENERAL_DARK}, {@link #WARNING}, {@link #DESTRUCTIVE} and {@link #REALMS}.</li>
 *   <li><b>Fields and wells</b>: a frame around a dark well shaded along its top.</li>
 * </ul>
 * <pre>{@code
 * screen.theme(OreUI.theme()).sheet(OreUI.sheet());
 * button("Buy", buy).primary();          // Colored
 * button("Cancel", close);               // General
 * box(cls("ore-panel"), ...);            // a popup-style panel
 * }</pre>
 * Button variants: {@code primary}/{@code success} (Colored), {@code dark} (General Dark), {@code warning},
 * {@code danger} (Destructive), {@code realms}, {@code ghost}. Classes: {@code ore-panel}, {@code ore-raised},
 * {@code ore-well}, {@code ore-chip}, {@code gold}. Everything is tokens, so a {@link Theme} re-skins it.
 */
public final class OreUI {
    private OreUI() { }

    // ------------------------------------------------------------------ metrics, as measured on the design sheet

    /** The depth band under a raised face (buttons, knobs), in units: the kit's 8 px at 4 px a unit. */
    public static final float DEPTH = 2;
    /** A button's face, its edge lines included: the kit's 89 px. */
    public static final float BUTTON_HEIGHT = 22;
    /** Text fields, frame included: the kit's Textbox, 126 px. */
    public static final float FIELD_HEIGHT = 31;
    /** The checkbox and radio diamond: a square this size turned 45 degrees, 15 across. */
    public static final float DIAMOND = 10.5f;

    /** Headings in the sheet's heavy pixel capitals: white with a thin dark shadow ({@link me.methodial.apothecary.widget.BlockLogo}). */
    public static final me.methodial.apothecary.logo.LogoStyle TITLE = me.methodial.apothecary.logo.LogoStyle.flat(0xFFFFFF, 0x121216);
    /** A green button's label in the same capitals, its shadow dark green. */
    public static final me.methodial.apothecary.logo.LogoStyle BUTTON_TITLE = me.methodial.apothecary.logo.LogoStyle.flat(0xFFFFFF, 0x2C6A16);
    /** Block size of a green button's label: whole units so it lands on pixels, 11 tall (the sheet's is 8.5). */
    public static final float BUTTON_TITLE_UNIT = 2;

    // ------------------------------------------------------------------ kit tokens

    /** The kit's "Background": the frame around every control, and the darkest surface. */
    public static final Token<Color> OUTLINE = Token.of("ore-background", Color.hex("#1e1e1f"));
    /** "Selection": the focus frame. */
    public static final Token<Color> SELECTION_FRAME = Token.of("ore-selection", Color.hex("#ffffff"));
    /** "Text/Text Highlight": highlights and money. */
    public static final Token<Color> GOLD = Token.of("ore-text-highlight", Color.hex("#d9af4e"));
    /** "Background Disabled": the frame of disabled fields. */
    public static final Token<Color> DISABLED = Token.of("ore-background-disabled", Color.hex("#5a5a5a"));
    /** Text field well and its top shade. */
    public static final Token<Color> WELL_BG = Token.of("ore-well", Color.hex("#313233"));
    public static final Token<Color> WELL_SHADE = Token.of("ore-well-shade", Color.hex("#242425"));
    /** A hovered well: text fields lighten under the pointer while the frame and top shade stay. */
    public static final Token<Color> WELL_HOVER = Token.of("ore-well-hover", Color.hex("#3e3f41"));
    /** Overlays behind panels, menus and popups. */
    public static final Token<Color> OVERLAY_PANEL = Token.of("ore-overlay-panel", Color.hex("#00000066"));
    public static final Token<Color> OVERLAY_MENU = Token.of("ore-overlay-menu", Color.hex("#00000080"));
    public static final Token<Color> OVERLAY_POPUP = Token.of("ore-overlay-popup", Color.hex("#000000b2"));

    /** Older names, kept for code written against the first version of this theme. */
    public static final Token<Color> BEVEL_LIGHT = Token.of("ore-stroke-secondary", Color.hex("#ffffff33"));
    public static final Token<Color> BEVEL_DARK = Token.of("ore-bevel-dark", Color.hex("#00000073"));

    /**
     * One of the kit's button color sets. {@code pressed} falls back to {@code hover} where the kit has none.
     */
    public record Palette(String name, Token<Color> bg, Token<Color> hover, Token<Color> pressed, Token<Color> stroke,
                          Token<Color> strokeHover, Token<Color> secondary, Token<Color> secondaryHover,
                          Token<Color> shadow, Token<Color> text) {
        static Palette of(String name, String bg, String hover, String pressed, String stroke, String strokeHover,
                          String secondary, String secondaryHover, String shadow, String text) {
            String p = "ore-" + name + "-";
            return new Palette(name, Token.of(p + "background", Color.hex(bg)), Token.of(p + "background-hover", Color.hex(hover)),
                    Token.of(p + "background-pressed", Color.hex(pressed)), Token.of(p + "stroke", Color.hex(stroke)),
                    Token.of(p + "stroke-hover", Color.hex(strokeHover)), Token.of(p + "stroke-secondary", Color.hex(secondary)),
                    Token.of(p + "stroke-hover-secondary", Color.hex(secondaryHover)), Token.of(p + "shadow", Color.hex(shadow)),
                    Token.of(p + "text", Color.hex(text)));
        }
    }

    // The kit's color sets, token for token. Hovered and pressed faces are darker, as in the kit.
    public static final Palette COLORED = Palette.of("colored", "#387e24", "#275b1a", "#1c4a12", "#50913d", "#6b935f",
            "#ffffff33", "#ffffff66", "#1d4d13", "#ffffff");
    public static final Palette GENERAL = Palette.of("general", "#d0d1d5", "#b1b2b6", "#b1b2b6", "#e3e3e5", "#e3e3e5",
            "#ffffff33", "#ffffff33", "#59595b", "#1e1e20");
    /** The kit's navigation buttons (NavigationButtonsHorizontal). */
    public static final Palette GENERAL_DARK = Palette.of("general-dark", "#515453", "#585959", "#585959", "#636562", "#636562",
            "#ffffff33", "#ffffff33", "#59595b", "#ffffff");
    /** A selected navigation button: sunk, without depth, darker, with a white line under its label. */
    public static final Palette GENERAL_DARK_SELECTED = Palette.of("general-dark-selected", "#373a39", "#444545", "#444545",
            "#4b4e4c", "#4b4e4c", "#ffffff33", "#ffffff33", "#373a39", "#ffffff");
    /**
     * The kit's cards and pagination items: flat, a light line along the top and left and a dark one along the right
     * and bottom; darker when hovered.
     */
    public static final Palette CARD = Palette.of("card", "#48494a", "#2f2f2f", "#2f2f2f", "#373737", "#262626",
            "#6d6d6e", "#3f3f3f", "#373737", "#ffffff");
    /** A selected card. */
    public static final Palette CARD_SELECTED = Palette.of("card-selected", "#6d6d6e", "#6d6d6e", "#6d6d6e", "#8e8e91", "#8e8e91",
            "#b1b2b5", "#b1b2b5", "#373737", "#ffffff");
    /** Popup header and action bars. */
    public static final Palette POPUP_BAR = Palette.of("popup-bar", "#585959", "#585959", "#585959", "#636562", "#636562",
            "#ffffff33", "#ffffff33", "#373a39", "#ffffff");
    /** Dropdowns: the text box's well, its dark line along the top. */
    public static final Palette DROPDOWN = Palette.of("dropdown", "#313233", "#3c3d3f", "#2a2b2c", "#313233", "#3c3d3f",
            "#242425", "#242425", "#1e1e1f", "#ffffff");
    /** The kit's disabled text box: a light well in a grey frame, with grey text. */
    public static final Palette DISABLED_FACE = Palette.of("disabled", "#c2c4c6", "#c2c4c6", "#c2c4c6", "#c2c4c6", "#c2c4c6",
            "#8f9194", "#8f9194", "#5a5a5a", "#3f4143");
    /** The popup body. */
    public static final Token<Color> POPUP_BG = Token.of("ore-popup-content", Color.hex("#373a39"));
    public static final Palette WARNING = Palette.of("warning", "#e1b922", "#c09a0d", "#c09a0d", "#e7c85b", "#e7c85b",
            "#ffffff33", "#ffffff33", "#b87919", "#1e1e20");
    public static final Palette DESTRUCTIVE = Palette.of("destructive", "#c93636", "#c02d2d", "#ad1d1d", "#d04a4b", "#d98180",
            "#ffffff33", "#ffffff66", "#ad1d1d", "#ffffff");
    public static final Palette REALMS = Palette.of("realms", "#7345e5", "#5d2cc6", "#4a1cac", "#8e4aeb", "#9242e2",
            "#fab0ff33", "#fab0ff66", "#4a1cac", "#ffffff");

    private static Theme theme;
    private static Sheet sheet;

    /** Ore UI's values for the shared {@link Tokens}. */
    public static synchronized Theme theme() {
        if (theme == null) {
            theme = Theme.of("Ore UI")
                    .set(BG, Color.hex("#23232a"))
                    .set(SURFACE, Color.hex("#313233"))
                    .set(SURFACE_2, Color.hex("#48494a"))
                    .set(SURFACE_3, Color.hex("#48494a"))
                    .set(SURFACE_HOVER, Color.hex("#585959"))
                    .set(BORDER, Color.hex("#1e1e1f"))
                    .set(BORDER_STRONG, Color.hex("#1e1e1f"))
                    .set(TEXT, Color.hex("#ffffff"))
                    .set(TEXT_DIM, Color.hex("#ffffffbf"))
                    .set(TEXT_FAINT, Color.hex("#ffffff73"))
                    .set(HOVER_TINT, Color.hex("#ffffff14"))
                    .set(PRESS_TINT, Color.hex("#ffffff22"))
                    .set(ACCENT, Color.hex("#387e24"))
                    .set(ACCENT_HOVER, Color.hex("#275b1a"))
                    .set(ACCENT_PRESS, Color.hex("#1c4a12"))
                    .set(ACCENT_SOFT, Color.hex("#9ddf7733"))
                    .set(ACCENT_TEXT, Color.hex("#ffffff"))
                    .set(ACCENT_2, Color.hex("#9ddf77"))
                    // Notice colors: readable on the dark surfaces.
                    .set(SUCCESS, Color.hex("#9ddf77"))
                    .set(Tokens.WARNING, Color.hex("#ffec72"))
                    .set(DANGER, Color.hex("#ff8a8a"))
                    .set(INFO, Color.hex("#89afff"))
                    .set(RADIUS, 0f)
                    .set(SHADOW_SM, (Ref<List<Shadow>>) t -> List.of(Shadow.of(0, 2, 0, Color.hex("#00000066"))))
                    .set(SHADOW, (Ref<List<Shadow>>) t -> List.of(Shadow.of(0, 3, 0, Color.hex("#00000080"))))
                    .set(SHADOW_LG, (Ref<List<Shadow>>) t -> List.of(Shadow.of(0, 4, 0, Color.hex("#000000b2"))))
                    .set(FONT, "minecraft")
                    .set(MONO, "minecraft")
                    .set(SELECTION, Color.hex("#ffffff55"))
                    .set(PLACEHOLDER, Color.hex("#b1b2b5"))
                    .set(TOOLTIP_BG, Color.hex("#2e6be5"))
                    .set(FIELD_FOCUS_BG, Color.hex("#313233"))
                    .set(SCROLLBAR, Color.hex("#ffffff40"))
                    .set(BACKDROP, Color.hex("#000000b2"));
        }
        return theme;
    }

    // ------------------------------------------------------------------ construction

    /**
     * The shadow layers that build an Ore face: the frame (and the depth's frame below it), the depth band, the
     * secondary stroke 1 px inside the top and left, and the stroke along the right and bottom.
     */
    private static Ref<List<Shadow>> faceShadows(Palette p, boolean depth, boolean hot, boolean focus, boolean frame) {
        return t -> {
            List<Shadow> out = new ArrayList<>();
            Color outline = OUTLINE.resolve(t);
            float below = depth ? DEPTH : 0;
            if (focus) {
                Color white = SELECTION_FRAME.resolve(t);
                out.add(new Shadow(0, 0, 0, 2, white, false));
                if (depth) out.add(new Shadow(0, below, 0, 2, white, false));
            }
            if (frame) {
                out.add(new Shadow(0, 0, 0, 1, outline, false));
                if (depth) out.add(new Shadow(0, below, 0, 1, outline, false));
            }
            if (depth) out.add(Shadow.of(0, below, 0, p.shadow().resolve(t)));
            // Measured from the kit's renders: the secondary line runs along the top and left edges of the face,
            // right against the frame, and the stroke covers the right and bottom edges over it.
            Color secondary = (hot ? p.secondaryHover() : p.secondary()).resolve(t);
            out.add(Shadow.inset(0, 1, 0, secondary));
            out.add(Shadow.inset(1, 0, 0, secondary));
            Color stroke = (hot ? p.strokeHover() : p.stroke()).resolve(t);
            out.add(Shadow.inset(-1, 0, 0, stroke));
            out.add(Shadow.inset(0, -1, 0, stroke));
            return out;
        };
    }

    /** A framed Ore face in palette {@code p} with its depth band: buttons, knobs, handles. */
    public static Consumer<Style> face(Palette p) {
        return s -> s.bg(p.bg()).fg(p.text()).borderW(0).shadow(faceShadows(p, true, false, false, true));
    }

    /**
     * The kit's states for any clickable element drawn as a face in palette {@code p} (default, hover, pressed,
     * focused, disabled), as buttons have them. Leave 1 unit around and 4 below it for the frame and depth.
     */
    public static void interactive(me.methodial.apothecary.sheet.Rules r, Palette p) { buttonStates(r, p); }

    /** The pressed face of palette {@code p}, held: no depth, hover strokes, sunk into its place (selected items). */
    public static Consumer<Style> pressedFace(Palette p) {
        return s -> s.bg(p.pressed()).fg(p.text()).borderW(0).translateY(DEPTH).shadow(faceShadows(p, false, true, false, true));
    }

    /** A framed face without depth: raised rows, headers, fills. */
    public static Consumer<Style> flatFace(Palette p) {
        return s -> s.bg(p.bg()).fg(p.text()).borderW(0).shadow(faceShadows(p, false, false, false, true));
    }

    /** A face filled with {@code color}, framed, with the dark depth band (compatibility with the first version). */
    public static Consumer<Style> face(Ref<Color> color) {
        return s -> s.bg(color).borderW(0).shadow(t -> List.of(new Shadow(0, 0, 0, 1, OUTLINE.resolve(t), false),
                new Shadow(0, 2, 0, 1, OUTLINE.resolve(t), false), Shadow.of(0, 2, 0, BEVEL_DARK.resolve(t)),
                Shadow.inset(0, 1, 0, BEVEL_LIGHT.resolve(t)), Shadow.inset(0, -1, 0, BEVEL_DARK.resolve(t))));
    }

    /**
     * The kit's states for a list row drawn as a face in palette {@code p}: as on the sheet, a row lies flat, with no
     * depth band, so pressing darkens it in place; hover lightens it and keyboard focus frames it in white.
     */
    public static void row(me.methodial.apothecary.sheet.Rules r, Palette p) { rowStates(r, p); }

    /**
     * The kit's card, whose text is half the size of a button's, so in text pixels its lines are twice as thick: a
     * 2-unit frame, a 2-unit light line all round with a 2-unit dark one over its right and bottom, and keyboard focus
     * a 2-unit white frame outside. Hovered and pressed, darker. Leave 2 units around it (4 for the focus frame).
     */
    public static void card(me.methodial.apothecary.sheet.Rules r) {
        r.style(s -> s.bg(CARD.bg()).fg(CARD.text()).borderW(0).shadow(cardShadows(false, false)))
                .hover(s -> s.bg(CARD.hover()).shadow(cardShadows(true, false)))
                .active(s -> s.bg(CARD.pressed()).untransformed().shadow(cardShadows(true, false)))
                .focusVisible(s -> s.shadow(cardShadows(false, true)).outline(0, SELECTION_FRAME))
                .refine(sel -> sel.focusVisible().hover(), v -> v.style(s -> s.shadow(cardShadows(true, true))));
    }

    private static Ref<List<Shadow>> cardShadows(boolean hot, boolean focus) {
        return t -> {
            List<Shadow> out = new ArrayList<>();
            if (focus) out.add(new Shadow(0, 0, 0, 4, SELECTION_FRAME.resolve(t), false));
            out.add(new Shadow(0, 0, 0, 2, OUTLINE.resolve(t), false));
            Color light = (hot ? CARD.secondaryHover() : CARD.secondary()).resolve(t);
            Color dark = (hot ? CARD.strokeHover() : CARD.stroke()).resolve(t);
            // Later layers paint over earlier ones: the light line all round, then the dark one over its right and bottom.
            out.add(Shadow.inset(0, 2, 0, light));
            out.add(Shadow.inset(2, 0, 0, light));
            out.add(Shadow.inset(-2, 0, 0, light));
            out.add(Shadow.inset(0, -2, 0, light));
            out.add(Shadow.inset(-2, 0, 0, dark));
            out.add(Shadow.inset(0, -2, 0, dark));
            return out;
        };
    }

    /** A panel as the kit's popup body: framed, flat. */
    public static final Consumer<Style> PANEL = s -> s.bg(POPUP_BG).border(1, OUTLINE).noShadow();
    /** A recessed well (fields, lists, item grids): framed, shaded along its top. */
    public static final Consumer<Style> WELL = s -> s.bg(WELL_BG).border(1, OUTLINE).shadow(topShade(OUTLINE, WELL_SHADE));

    /**
     * A well's 2-unit shade under its top frame line. Inset shadows paint over the border, so the shade runs 3 deep
     * and the frame's top line is laid back over it.
     */
    private static Ref<List<Shadow>> topShade(Ref<Color> frame, Ref<Color> shade) {
        return t -> List.of(Shadow.inset(0, 3, 0, shade.resolve(t)), Shadow.inset(0, 1, 0, frame.resolve(t)));
    }
    /** The kit's text box: the well, its 1-unit frame and 2-unit shade along the top. */
    public static final Consumer<Style> FIELD = WELL;
    /** The kit's disabled text box. */
    public static final Consumer<Style> FIELD_DISABLED = s -> s.bg(DISABLED_FACE.bg()).fg(DISABLED_FACE.text()).opacity(1)
            .border(1, DISABLED_FACE.shadow()).shadow(topShade(DISABLED_FACE.shadow(), DISABLED_FACE.secondary()));
    /** A diamond's face and the rim just inside its frame. */
    private record Diamond(Color face, Color rim) {
        Diamond(String face, String rim) { this(Color.hex(face), Color.hex(rim)); }
    }

    private static final Diamond DIAMOND_OFF = new Diamond("#8c8d90", "#a5a6a8");
    private static final Diamond DIAMOND_OFF_HOVER = new Diamond("#b1b2b5", "#c6c7c9");
    private static final Diamond DIAMOND_OFF_PRESSED = new Diamond("#575759", "#6e6e70");
    private static final Diamond DIAMOND_ON = new Diamond("#3c8527", "#5f9c4e");
    private static final Diamond DIAMOND_ON_HOVER = new Diamond("#52a535", "#6db857");
    private static final Diamond DIAMOND_ON_PRESSED = new Diamond("#2a641c", "#457738");
    private static final Diamond DIAMOND_DISABLED = new Diamond("#d0d1d4", "#909194");

    /** The diamond's frame (or, focused, the white frame around it) and its rim. */
    private static Ref<List<Shadow>> diamondShadows(Diamond d, boolean focus) {
        return t -> {
            List<Shadow> out = new ArrayList<>();
            if (focus) out.add(new Shadow(0, 0, 0, 3, SELECTION_FRAME.resolve(t), false));
            out.add(new Shadow(0, 0, 0, focus ? 2 : 1, OUTLINE.resolve(t), false));
            out.add(new Shadow(0, 0, 0, 1, d.rim(), true));
            return out;
        };
    }

    private static Consumer<Style> diamond(Diamond d, boolean focus) {
        return s -> s.size(DIAMOND).m(3).noShrink().center().radius(0).borderW(0).borderColor(OUTLINE).bg(d.face()).rotate(45)
                .outline(0, SELECTION_FRAME).shadow(diamondShadows(d, focus));
    }

    /** The white diamond in a checked one: white, its lower half grey once turned. */
    private static final Consumer<Style> DIAMOND_MARK = s -> s.size(4).radius(0).borderW(0).bg(Colors.WHITE)
            .fg(Color.TRANSPARENT).shadow(t -> List.of(Shadow.inset(-1, 0, 0, Color.hex("#d0d2d5")),
                    Shadow.inset(0, -1, 0, Color.hex("#d0d2d5"))));

    /** A disabled control: the kit's disabled text box. */
    public static final Consumer<Style> DISABLED_CONTROL = s -> s.apply(FIELD_DISABLED).untransformed();
    /** A raised row or card on a panel: the kit's card face. */
    public static final Consumer<Style> RAISED = flatFace(CARD);
    /** The kit's focus: a white frame. */
    public static final Consumer<Style> FOCUS = s -> s.outline(1, SELECTION_FRAME, 0);

    // ------------------------------------------------------------------ the sheet

    /** The Ore UI look for every built-in widget. Layer it over the default sheet. */
    public static synchronized Sheet sheet() {
        if (sheet == null) sheet = build();
        return sheet;
    }

    public static Sheet build() {
        Sheet sheet = new Sheet("apothecary:ore-ui");
        // Minecraft's own line spacing: 9 rows per line, so every text box is a whole number of pixels.
        // Scrollbars as Minecraft would draw them: square, a raised General Dark block in a dark groove.
        sheet.rule(root(), s -> s.font(FONT).fontSize(9).lineHeight(1).fg(TEXT).blockyScrollbar(5, GENERAL_DARK.bg(), alpha(OUTLINE, 0.7f)));

        sheet.onCls("ore-panel", r -> r.style(PANEL));
        sheet.onCls("ore-raised", r -> r.style(RAISED));
        sheet.onCls("ore-well", r -> r.style(WELL));
        sheet.onCls("ore-inset", r -> r.style(s -> s.bg(OVERLAY_PANEL)));
        sheet.onCls("ore-chip", r -> r.style(s -> s.row().gap(3).pad(1, 4).bg(OVERLAY_MENU).border(1, OUTLINE)));
        sheet.rule(cls("gold"), s -> s.fg(GOLD));
        sheet.rule(cls("card"), PANEL);
        sheet.rule(cls("panel"), PANEL);

        buttons(sheet);
        controls(sheet);
        overlays(sheet);

        sheet.on("item-slot", r -> r
                .style(s -> s.size(20).pad(2).apply(WELL).transition(Tr.colors(0.08f)))
                .hover(s -> s.bg(SURFACE_2).borderColor(SELECTION_FRAME).untransformed()));
        sheet.style("divider", s -> s.h(1).noShrink().bg(OUTLINE));
        sheet.on("badge", r -> r.style(s -> s.radius(0).fontSize(9).regular()));
        return sheet;
    }

    /** One palette's states on the rules of a button (or variant). */
    private static void buttonStates(me.methodial.apothecary.sheet.Rules r, Palette p) {
        r.style(s -> s.bg(p.bg()).fg(p.text()).borderW(0).shadow(faceShadows(p, true, false, false, true)))
                // Hovered: the hover background, and the hover stroke and secondary line; the depth stays.
                .hover(s -> s.bg(p.hover()).shadow(faceShadows(p, true, true, false, true)))
                .refine(sel -> sel.focusVisible().hover(), v -> v.style(s -> s.shadow(faceShadows(p, true, true, true, true))))
                // Pressed: the depth goes and the face sinks into its place.
                .active(s -> s.bg(p.pressed()).translateY(DEPTH).shadow(faceShadows(p, false, true, false, true)))
                // The face draws the white focus frame; drop the default theme's accent outline.
                .focusVisible(s -> s.shadow(faceShadows(p, true, false, true, true)).outline(0, SELECTION_FRAME))
                // Disabled: as the kit, the hover face without depth, its label at half strength.
                .disabled(s -> s.bg(p.hover()).fg(alpha(p.text(), 0.5f)).opacity(1).untransformed()
                        .shadow(faceShadows(p, false, false, false, true)));
    }

    /** One palette's states on the rules of a list row (or variant): flat, see {@link #row}. */
    private static void rowStates(me.methodial.apothecary.sheet.Rules r, Palette p) {
        r.style(s -> s.bg(p.bg()).fg(p.text()).borderW(0).shadow(faceShadows(p, false, false, false, true)))
                .hover(s -> s.bg(p.hover()).shadow(faceShadows(p, false, true, false, true)))
                .refine(sel -> sel.focusVisible().hover(), v -> v.style(s -> s.shadow(faceShadows(p, false, true, true, true))))
                .active(s -> s.bg(p.pressed()).shadow(faceShadows(p, false, true, false, true)))
                .focusVisible(s -> s.shadow(faceShadows(p, false, false, true, true)).outline(0, SELECTION_FRAME))
                .disabled(s -> s.fg(alpha(p.text(), 0.5f)).opacity(1).untransformed()
                        .shadow(faceShadows(p, false, false, false, true)));
    }

    private static void buttons(Sheet sheet) {
        sheet.on("button", r -> {
            // The frame and depth are drawn outside the box: margins keep neighbours clear of them.
            // 9 px text like all other text; symmetric padding centers the capitals in the face between its strokes.
            r.style(s -> s.row().center().gap(4).pad(7, 9, 6, 9).m(1, 1, 1 + DEPTH, 1).radius(0).fontSize(9).lineHeight(1).regular().pointer().noShrink()
                    .transition(Tr.colors(0.06f), Tr.transform(0.05f, OUT), Tr.opacity(FAST)));
            buttonStates(r, GENERAL);
            // Green buttons: the kit's white label with its text shadow.
            r.variant("primary", v -> { buttonStates(v, COLORED); v.style(s -> s.textShadow(true)); });
            r.variant("success", v -> { buttonStates(v, COLORED); v.style(s -> s.textShadow(true)); });
            r.variant("dark", v -> buttonStates(v, GENERAL_DARK));
            r.variant("warning", v -> buttonStates(v, WARNING));
            r.variant("gold", v -> buttonStates(v, WARNING));
            r.variant("danger", v -> buttonStates(v, DESTRUCTIVE));
            r.variant("realms", v -> buttonStates(v, REALMS));
            r.variant("ghost", v -> v
                    .style(s -> s.bg(Color.TRANSPARENT).noShadow().fg(TEXT_DIM).pad(4, 6).m(0))
                    .hover(s -> s.bg(HOVER_TINT).fg(TEXT))
                    .active(s -> s.bg(PRESS_TINT).untransformed().noShadow())
                    .focusVisible(FOCUS));
            r.variant("outline", v -> v
                    .style(s -> s.bg(Color.TRANSPARENT).border(1, TEXT_DIM).noShadow().fg(TEXT).pad(4, 9))
                    .hover(s -> s.bg(HOVER_TINT).borderColor(SELECTION_FRAME))
                    .active(s -> s.untransformed().noShadow()));
            r.variantStyle("sm", s -> s.pad(3, 6));
            r.variantStyle("lg", s -> s.pad(8, 14).fontSize(13.5f));
            r.variantStyle("icon-only", s -> s.pad(4));
            r.partCls("button-icon", p -> p.style(s -> s.fontSize(9).stroke(2.4f)));
        });
    }

    private static void controls(Sheet sheet) {
        // Text field: the kit's Textbox, a dark well shaded along its top; focus frames it in white. Every state
        // re-applies the field: the default theme underneath recolors the frame on hover and adds a ring on focus.
        sheet.on("textfield", r -> r
                .style(s -> s.minW(60).h(FIELD_HEIGHT).padX(6).radius(0).apply(FIELD).fg(TEXT))
                .hover(FIELD)
                .focus(s -> s.apply(FIELD).outline(1, SELECTION_FRAME, 0))
                .disabled(FIELD_DISABLED)
                .variantStyle("invalid", s -> s.apply(FIELD).outline(1, DANGER, 0)));
        sheet.rule(cls("textarea"), s -> s.h(Length.AUTO).padY(5));

        // Checkbox and radio: the sheet's diamond, a framed square turned 45 degrees with a lighter rim; checked, green
        // with a small white diamond in it. Lighter on hover, darker pressed, the white frame on keyboard focus.
        sheet.on("checkbox", r -> r
                .style(s -> s.gap(8))
                .part("checkbox-box", b -> b.style(diamond(DIAMOND_OFF, false)))
                .partCls("check-mark", m -> m.style(s -> s.apply(DIAMOND_MARK).opacity(0).transition(Tr.opacity(0.08f))))
                .when(HOVER, h -> h.partStyle("checkbox-box", diamond(DIAMOND_OFF_HOVER, false)))
                .when(ACTIVE, a -> a.partStyle("checkbox-box", diamond(DIAMOND_OFF_PRESSED, false)))
                .when(CHECKED, c -> c.partStyle("checkbox-box", diamond(DIAMOND_ON, false)).partCls("check-mark", m -> m.style(s -> s.opacity(1))))
                .when(INDETERMINATE, c -> c.partStyle("checkbox-box", diamond(DIAMOND_ON, false)).partCls("check-mark", m -> m.style(s -> s.opacity(1))))
                .refine(sel -> sel.checked().hover(), v -> v.partStyle("checkbox-box", diamond(DIAMOND_ON_HOVER, false)))
                .refine(sel -> sel.checked().active(), v -> v.partStyle("checkbox-box", diamond(DIAMOND_ON_PRESSED, false)))
                .when(FOCUS_VISIBLE, f -> f.partStyle("checkbox-box", s -> s.shadow(diamondShadows(DIAMOND_OFF, true))))
                .refine(sel -> sel.checked().focusVisible(), v -> v.partStyle("checkbox-box", s -> s.shadow(diamondShadows(DIAMOND_ON, true))))
                .refine(sel -> sel.disabled(), d -> d.style(s -> s.opacity(1)).partStyle("checkbox-box", diamond(DIAMOND_DISABLED, false))));

        // Switch: a framed track (green when on, gray when off) and a General button knob with its depth.
        sheet.on("switch", r -> r
                .partStyle("switch-track", s -> s.size(25, 12).radius(0).pad(0).bg(Colors.hex("#8c8d90"))
                        .border(1, Colors.hex("#a3a4a6")).outline(1, OUTLINE, 0))
                .partStyle("switch-knob", s -> s.size(12, 11).mt(-1).radius(0).apply(face(GENERAL))
                        .transition(Tr.transform(0.12f, OUT), Tr.colors(0.08f)))
                .when(HOVER, h -> h.partStyle("switch-knob", s -> s.bg(GENERAL.hover())))
                .when(ACTIVE, a -> a.partStyle("switch-knob", s -> s.w(12)))
                .when(CHECKED, c -> c
                        .partStyle("switch-track", s -> s.bg(COLORED.bg()).borderColor(COLORED.stroke()))
                        .partStyle("switch-knob", s -> s.translateX(11)))
                .refine(sel -> sel.checked().active(), c -> c.partStyle("switch-knob", s -> s.translateX(11)))
                .when(FOCUS_VISIBLE, f -> f.partStyle("switch-track", s -> s.outline(1, SELECTION_FRAME, 1))));

        sheet.on("radio", r -> r
                .style(s -> s.gap(8))
                .partStyle("radio-dot", diamond(DIAMOND_OFF, false))
                .partStyle("radio-inner", s -> s.apply(DIAMOND_MARK).opacity(0).transition(Tr.opacity(0.08f)))
                .when(HOVER, h -> h.partStyle("radio-dot", diamond(DIAMOND_OFF_HOVER, false)))
                .when(ACTIVE, a -> a.partStyle("radio-dot", diamond(DIAMOND_OFF_PRESSED, false)))
                .when(CHECKED, c -> c.partStyle("radio-dot", diamond(DIAMOND_ON, false)).partStyle("radio-inner", s -> s.opacity(1)))
                .refine(sel -> sel.checked().hover(), v -> v.partStyle("radio-dot", diamond(DIAMOND_ON_HOVER, false)))
                .refine(sel -> sel.checked().active(), v -> v.partStyle("radio-dot", diamond(DIAMOND_ON_PRESSED, false)))
                .when(FOCUS_VISIBLE, f -> f.partStyle("radio-dot", s -> s.shadow(diamondShadows(DIAMOND_OFF, true))))
                .refine(sel -> sel.checked().focusVisible(), v -> v.partStyle("radio-dot", s -> s.shadow(diamondShadows(DIAMOND_ON, true)))));

        // Slider: a green face up to the handle, a darkened General face after it, a General button handle.
        sheet.on("slider", r -> r
                .style(s -> s.h(16))
                .partStyle("slider-track", s -> s.h(6).radius(0).bg(Colors.hex("#8c8d90")).borderW(0)
                        .shadow(t -> List.of(new Shadow(0, 0, 0, 1, OUTLINE.resolve(t), false))))
                .partStyle("slider-fill", s -> s.radius(0).apply(flatFace(COLORED)).noShadow()
                        .shadow(t -> List.of(Shadow.inset(-1, 0, 0, COLORED.stroke().resolve(t)), Shadow.inset(0, -1, 0, COLORED.stroke().resolve(t)),
                                Shadow.inset(0, 1, 0, COLORED.secondary().resolve(t)))))
                .partStyle("slider-thumb", s -> s.top(1).size(10, 11).radius(0).apply(face(GENERAL)).noShrink())
                .partCls("slider-value", v -> v.style(s -> s.radius(0).bg(TOOLTIP_BG).border(1, OUTLINE).fontSize(9).fg(Colors.WHITE)))
                // Hover and drag re-apply the face: the default theme rings the thumb, which would replace its bevel.
                .when(HOVER, h -> h.partStyle("slider-thumb", s -> s.untransformed().apply(face(GENERAL)).bg(GENERAL.hover())))
                .when(DRAGGING, d -> d.partStyle("slider-thumb", s -> s.untransformed().apply(face(GENERAL)).bg(GENERAL.hover())))
                .when(FOCUS_VISIBLE, f -> f.partStyle("slider-thumb", s -> s.shadow(faceShadows(GENERAL, true, false, true, true))
                        .outline(0, SELECTION_FRAME))));

        sheet.on("progress", r -> r
                .style(s -> s.h(6).radius(0).apply(WELL).noShadow())
                .partStyle("progress-fill", s -> s.radius(0).bg(COLORED.bg())
                        .shadow(t -> List.of(Shadow.inset(-1, 0, 0, COLORED.stroke().resolve(t)), Shadow.inset(0, 1, 0, COLORED.secondary().resolve(t))))));

        // Dropdowns are text boxes with a chevron; menus are panels.
        sheet.on("select", r -> {
            r.style(s -> s.h(FIELD_HEIGHT).pad(0, 6).m(0).radius(0).apply(FIELD).fg(TEXT))
                    .hover(s -> s.apply(FIELD).bg(DROPDOWN.hover()))
                    .active(FIELD)
                    .focusVisible(s -> s.apply(FIELD).outline(1, SELECTION_FRAME, 0))
                    .disabled(FIELD_DISABLED);
            // The kit's pixel chevron at its icon size, one unit a cell.
            r.partCls("select-arrow", a -> a.style(s -> s.fontSize(me.methodial.apothecary.widget.PixelIcon.KIT_FRAME)));
            r.open(s -> s.apply(FIELD).outline(1, SELECTION_FRAME, 0));
        });
        sheet.on("menu", r -> r.style(s -> s.radius(0).apply(PANEL).pad(2)));
        sheet.on("menu-item", r -> r
                .style(s -> s.radius(0))
                .state(HIGHLIGHTED, s -> s.bg(SURFACE_HOVER))
                .selected(s -> s.fg(GOLD)));

        // Tabs: General Dark faces in a bar; the selected tab is the darker "selected" surface.
        sheet.style("tab-bar", s -> s.row().gap(0).bg(GENERAL_DARK.bg()).border(1, OUTLINE));
        sheet.on("tab", r -> r
                .style(s -> s.pad(5, 9, 6, 9).fg(TEXT).bg(GENERAL_DARK.bg()))
                .hover(s -> s.bg(GENERAL_DARK.hover()))
                .selected(s -> s.bg(GENERAL_DARK_SELECTED.bg())));
        sheet.style("tab-indicator", s -> s.h(2).radius(0).bg(COLORED.stroke()));
        sheet.style("segmented", s -> s.radius(0).bg(OUTLINE).border(1, OUTLINE).pad(1));
        sheet.style("segment-pill", s -> s.radius(0).apply(flatFace(GENERAL_DARK)));
        sheet.style("accordion", s -> s.radius(0).apply(RAISED));
        sheet.on("accordion-header", r -> r.style(s -> s.radius(0)));
        sheet.style("stepper", s -> s.radius(0).apply(WELL));
    }

    private static void overlays(Sheet sheet) {
        // Tooltips are the kit's blue.
        // Tooltips: the sheet's blue speech bubble, framed, its right end stepped in and a stepped tail below.
        sheet.on("tooltip", r -> r.style(s -> s.radius(0).bg(TOOLTIP_BG).border(1, OUTLINE).noShadow().fontSize(9).fg(Colors.WHITE).pad(2, 3)));
        sheet.rule(cls("tooltip-cap"), s -> s.abs().right(-2).top(2).bottom(2).w(3).bg(TOOLTIP_BG).borders(1, 1, 1, 0, OUTLINE));
        // The tail: three steps 6, 4 and 2 wide and 2 high, right-aligned 8 in from the box's right edge; each step
        // covers the frame line above it so the blue runs through.
        sheet.rule(cls("tooltip-step"), s -> s.abs().right(7).h(4).bg(TOOLTIP_BG).borders(0, 1, 1, 1, OUTLINE));
        for (int i = 0; i < 3; i++) {
            int step = i;
            sheet.rule(cls("tooltip-step").withCls("s" + i), s -> s.w(8 - 2 * step).bottom(-3 - 3 * step));
        }
        sheet.rule(cls("tooltip-step").in(cls("below")), s -> s.hidden());
        sheet.rule(cls("modal-backdrop"), s -> s.bg(OVERLAY_POPUP));
        // Popups as the kit's: a raised header bar with the centered title, the dark body, a raised action bar.
        sheet.style("dialog", s -> s.radius(0).apply(PANEL));
        sheet.style("dialog-header", s -> s.apply(flatFace(POPUP_BAR)).pad(7, 7));
        sheet.rule(cls("dialog-title"), s -> s.fontSize(9).regular().textCenter());
        sheet.style("dialog-footer", s -> s.radius(0).apply(flatFace(POPUP_BAR)).pad(6, 4));
        sheet.rule(cls("dialog-text"), s -> s.fg(TEXT_DIM));
        sheet.on("toast", r -> r.style(s -> s.radius(0).apply(PANEL)));
        sheet.style("toast-timer", s -> s.radius(0));
    }
}
