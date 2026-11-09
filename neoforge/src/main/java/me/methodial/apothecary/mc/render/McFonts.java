package me.methodial.apothecary.mc.render;

import me.methodial.apothecary.mc.ApothecaryMod;
import me.methodial.apothecary.mc.render.text.TtfText;
import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.FontWeight;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps {@code font(...)} family names to fonts. Two kinds:
 * <ul>
 *   <li><b>TrueType</b> families ({@link #registerTtf}): rendered by Apothecary's own FreeType text
 *   engine at exact pixel sizes — crisp at every size and UI scale.</li>
 *   <li><b>Vanilla</b> families ({@link #registerVanilla}): any Minecraft font definition
 *   (assets/&lt;ns&gt;/font/*.json), drawn with vanilla text rendering and snapped to whole pixels.</li>
 * </ul>
 * Families are normalized so that {@code font-size: 9px} matches one vanilla line.
 */
public final class McFonts implements TextMeasurer {
    public static final McFonts INSTANCE = new McFonts();

    /**
     * A family. TTF: faces per weight (null = fall back to regular, synthesizing bold), {@code emScale}
     * = em size per font-size px, {@code pixelGrid} &gt; 0 for pixel fonts (em snapped to multiples).
     * Vanilla: {@code vanilla} font id.
     */
    public record Family(ResourceLocation vanilla, ResourceLocation regular, ResourceLocation semibold, ResourceLocation bold,
                         ResourceLocation italic, float emScale, int pixelGrid) {
        public boolean isTtf() { return vanilla == null; }

        ResourceLocation face(FontWeight w) {
            return switch (w) {
                case BOLD -> bold != null ? bold : semibold != null ? semibold : regular;
                case SEMIBOLD, MEDIUM -> semibold != null ? semibold : regular;
                default -> regular;
            };
        }

        boolean synthBold(FontWeight w) {
            return (w == FontWeight.BOLD || w == FontWeight.SEMIBOLD) && bold == null && semibold == null;
        }
    }

    private static final Map<String, Family> FAMILIES = new LinkedHashMap<>();
    private final Map<String, Float> widths = new HashMap<>();
    private float deviceScale = 2;

    static {
        registerVanilla("default", ResourceLocation.withDefaultNamespace("default"));
        registerVanilla("minecraft", ResourceLocation.withDefaultNamespace("default"));
        registerVanilla("uniform", ResourceLocation.withDefaultNamespace("uniform"));
        registerVanilla("unicode", ResourceLocation.withDefaultNamespace("uniform"));
        registerVanilla("enchanting", ResourceLocation.withDefaultNamespace("alt"));
        registerVanilla("illager", ResourceLocation.withDefaultNamespace("illageralt"));
        Family poppins = new Family(null, ttf("poppins_regular"), ttf("poppins_semibold"), ttf("poppins_bold"), null, 1.1667f, 0);
        register("poppins", poppins);
        register("sans", poppins);
        Family mono = new Family(null, ttf("space_mono_regular"), null, ttf("space_mono_bold"), null, 1.111f, 0);
        register("space-mono", mono);
        register("mono", mono);
        Family pixel = new Family(null, ttf("silkscreen_regular"), null, null, null, 0.889f, 8);
        register("silkscreen", pixel);
        register("pixel", pixel);
    }

    private static ResourceLocation ttf(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApothecaryMod.MOD_ID, "font/" + name + ".ttf");
    }

    public static void register(String name, Family family) {
        FAMILIES.put(name.toLowerCase(Locale.ROOT), family);
        INSTANCE.widths.clear();
    }

    /** A TrueType family from .ttf resources ({@code mymod:font/x.ttf}); semibold/bold may be null. */
    public static void registerTtf(String name, ResourceLocation regular, ResourceLocation semibold, ResourceLocation bold, float emScale) {
        register(name, new Family(null, regular, semibold, bold, null, emScale, 0));
    }

    /** A vanilla font definition id (assets/ns/font/id.json). */
    public static void registerVanilla(String name, ResourceLocation fontId) {
        register(name, new Family(fontId, null, null, null, null, 1, 0));
    }

    public static Map<String, Family> families() { return Collections.unmodifiableMap(FAMILIES); }

    public Family family(FontSpec spec) {
        String key = spec.family().toLowerCase(Locale.ROOT);
        Family f = FAMILIES.get(key);
        if (f == null && key.contains(":")) {
            // A font id used directly; one that isn't a valid id (it is looked up every frame) falls back quietly.
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) f = new Family(id, null, null, null, null, 1, 0);
        }
        return f != null ? f : FAMILIES.get("poppins");
    }

    /** Physical pixels per root unit for the UI being laid out (set by the screen each frame). */
    public void setDeviceScale(float s) {
        if (Math.abs(s - deviceScale) > 1e-4f) widths.clear();
        this.deviceScale = s;
    }

    public float deviceScale() { return deviceScale; }

    /** Forget cached metrics and glyphs (resource reload). */
    public void invalidate() {
        widths.clear();
        TtfText.INSTANCE.reset();
    }

    TtfText.Request ttfRequest(Family f, FontSpec spec) {
        ResourceLocation main = f.face(spec.weight());
        ResourceLocation[] chain = main.equals(f.regular()) ? new ResourceLocation[]{main} : new ResourceLocation[]{main, f.regular()};
        return new TtfText.Request(chain, spec.size(), f.emScale(), f.pixelGrid(), spec.letterSpacing());
    }

    /** Vanilla style (font id, synthetic bold, italic) for a spec. */
    Style vanillaStyle(Family f, FontSpec spec) {
        return Style.EMPTY.withFont(f.vanilla()).withBold(spec.weight() == FontWeight.BOLD).withItalic(spec.italic());
    }

    /**
     * Scale for vanilla bitmap fonts, snapped so each font texel covers a whole number of physical
     * pixels (bitmap fonts only look right at integer multiples).
     */
    public static float vanillaScale(float fontSize, float devPx) {
        float k = devPx * fontSize / 9f;
        float snapped = Math.max(1, Math.round(k));
        return fontSize / 9f * (snapped / Math.max(k, 1e-4f));
    }

    @Override
    public float width(String text, FontSpec font) {
        if (text.isEmpty()) return 0;
        String key = font + "\u0000" + text;
        Float w = widths.get(key);
        if (w != null) return w;
        Family f = family(font);
        float result;
        if (f.isTtf()) {
            result = TtfText.INSTANCE.width(ttfRequest(f, font), text, deviceScale);
        } else {
            float base = Minecraft.getInstance().font.getSplitter().stringWidth(Component.literal(text).withStyle(vanillaStyle(f, font)));
            // Each glyph's advance includes a 1-pixel gap after it; the last one is not part of the text's look, so
            // leave it out and centered or right-aligned text sits evenly between its edges.
            float scale = vanillaScale(font.size(), deviceScale);
            result = Math.max(0, base - 1) * scale + font.letterSpacing() * Math.max(0, text.codePointCount(0, text.length()) - 1);
        }
        if (widths.size() > 20000) widths.clear();
        widths.put(key, result);
        return result;
    }

    @Override
    public float lineHeight(FontSpec font) { return font.size(); }
}
