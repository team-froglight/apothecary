package me.methodial.apothecary.mc.render.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.methodial.apothecary.geom.Affine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Crisp TrueType text: glyphs are rasterized by FreeType at the exact physical pixel size they'll be
 * shown at (so one atlas texel = one screen pixel), with light hinting and pixel-snapped baselines.
 * Rotated or scaled text reuses the closest size with linear filtering. Vertical placement uses the
 * face's real ascent/descent, centered in the line box the way browsers do.
 */
public final class TtfText {
    public static final TtfText INSTANCE = new TtfText();

    /** Synthetic italic shear (x per y) used when a family has no italic face. */
    private static final float ITALIC_SHEAR = 0.2f;

    private GlyphAtlas atlas = new GlyphAtlas();
    private final Map<ResourceLocation, TtfFace> faces = new HashMap<>();
    private final Set<ResourceLocation> failed = new HashSet<>();

    private TtfText() { }

    /** A font request: face fallback chain plus sizing rules. */
    public record Request(ResourceLocation[] faces, float size, float emScale, int pixelGrid, float letterSpacing) { }

    /** Where a laid-out line's baseline and extent ended up (local units), for decorations. */
    public record Extent(float baseline, float width, float emPx, float devPx) { }

    private record Placed(TtfFace.Glyph glyph, float pen) { }

    /** Releases faces and atlas pages (resource reload). */
    public void reset() {
        faces.values().forEach(TtfFace::close);
        faces.clear();
        failed.clear();
        atlas.close();
        atlas = new GlyphAtlas();
    }

    private TtfFace face(ResourceLocation id) {
        TtfFace f = faces.get(id);
        if (f != null || failed.contains(id)) return f;
        try {
            f = TtfFace.load(id, atlas);
            faces.put(id, f);
        } catch (IOException | RuntimeException e) {
            failed.add(id);
            LoggerFactory.getLogger("apothecary").error("Could not load font {}: {}", id, e.toString());
        }
        return f;
    }

    /** Pixel size of the em box for a request shown at {@code devPx} physical pixels per unit. */
    public static int pixelSize(Request r, float devPx) {
        float em = r.size() * r.emScale() * devPx;
        int px = Math.round(em);
        if (r.pixelGrid() > 0 && px >= r.pixelGrid()) px = Math.round(em / r.pixelGrid()) * r.pixelGrid();
        return Math.max(3, Math.min(px, 400));
    }

    private List<Placed> layout(Request r, String text, int px, float devPx, TtfFace.Sized[] sized) {
        List<Placed> out = new ArrayList<>(text.length());
        float pen = 0, spacing = r.letterSpacing() * devPx;
        int prevIndex = 0;
        TtfFace.Sized prevFace = null;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            TtfFace.Glyph g = null;
            TtfFace.Sized from = null;
            for (TtfFace.Sized s : sized) {
                if (s == null) continue;
                g = s.glyph(cp);
                if (g != null) { from = s; break; }
            }
            if (g == null) {
                // unknown codepoint: advance like a space so layout stays stable
                pen += px * 0.3f + spacing;
                prevIndex = 0;
                continue;
            }
            if (from == prevFace) pen += from.kerning(prevIndex, g.index());
            out.add(new Placed(g, pen));
            pen += g.advance() + spacing;
            prevIndex = g.index();
            prevFace = from;
        }
        return out;
    }

    private TtfFace.Sized[] sized(Request r, int px) {
        TtfFace.Sized[] s = new TtfFace.Sized[r.faces().length];
        for (int i = 0; i < s.length; i++) {
            TtfFace f = r.faces()[i] == null ? null : face(r.faces()[i]);
            s[i] = f == null ? null : f.size(px);
        }
        return s;
    }

    private static TtfFace.Sized primary(TtfFace.Sized[] s) {
        for (TtfFace.Sized x : s) if (x != null) return x;
        return null;
    }

    /** Advance width in local units at {@code devPx} physical pixels per unit. */
    public float width(Request r, String text, float devPx) {
        if (text.isEmpty()) return 0;
        int px = pixelSize(r, devPx);
        TtfFace.Sized[] s = sized(r, px);
        if (primary(s) == null) return text.length() * r.size() * 0.5f;
        List<Placed> placed = layout(r, text, px, devPx, s);
        float end = 0;
        if (!placed.isEmpty()) {
            Placed last = placed.get(placed.size() - 1);
            end = last.pen() + last.glyph().advance();
        }
        return end / devPx;
    }

    /**
     * Draws one line with its line box (height {@code r.size()}) top-left at local (x, y).
     *
     * @param dev      local → GUI transform
     * @param guiScale physical pixels per GUI unit
     * @param argb     color including alpha
     */
    public Extent draw(Request r, String text, float x, float y, Affine dev, float guiScale, Matrix4f pose, int argb,
                       boolean shadow, boolean syntheticBold, boolean syntheticItalic) {
        float devPx = Math.max(0.01f, dev.approxScale() * guiScale);
        int px = pixelSize(r, devPx);
        TtfFace.Sized[] s = sized(r, px);
        TtfFace.Sized p = primary(s);
        if (p == null) return new Extent(y + r.size() * 0.8f, 0, px, devPx);
        List<Placed> placed = layout(r, text, px, devPx, s);
        atlas.uploadAll();

        float contentH = (p.ascent - p.descent) / devPx;
        float baseline = y + (r.size() - contentH) / 2 + p.ascent / devPx;
        float shear = syntheticItalic ? ITALIC_SHEAR : 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        if (shadow) {
            int a = (argb >>> 24);
            int sc = (a << 24) | ((argb >> 2) & 0x3F3F3F);
            float off = r.size() / 9f;
            emit(placed, x + off, baseline + off, dev, guiScale, pose, sc, devPx, shear, 0);
            if (syntheticBold) emit(placed, x + off, baseline + off, dev, guiScale, pose, sc, devPx, shear, 1);
        }
        emit(placed, x, baseline, dev, guiScale, pose, argb, devPx, shear, 0);
        if (syntheticBold) emit(placed, x, baseline, dev, guiScale, pose, argb, devPx, shear, 1);

        float w = placed.isEmpty() ? 0 : (placed.get(placed.size() - 1).pen() + placed.get(placed.size() - 1).glyph().advance()) / devPx;
        return new Extent(baseline, w, px, devPx);
    }

    private void emit(List<Placed> placed, float x, float baseline, Affine dev, float guiScale, Matrix4f pose,
                      int argb, float devPx, float shear, float extraPx) {
        float a = (argb >>> 24) / 255f, rr = ((argb >> 16) & 0xFF) / 255f, gg = ((argb >> 8) & 0xFF) / 255f, bb = (argb & 0xFF) / 255f;
        boolean axis = Math.abs(dev.b()) < 1e-5f && Math.abs(dev.c()) < 1e-5f && dev.a() > 0 && dev.d() > 0;
        // Group by atlas page (usually one).
        Map<GlyphAtlas.Page, List<Placed>> byPage = new HashMap<>();
        for (Placed pl : placed) if (pl.glyph().visible()) byPage.computeIfAbsent(pl.glyph().page(), k -> new ArrayList<>()).add(pl);
        for (Map.Entry<GlyphAtlas.Page, List<Placed>> e : byPage.entrySet()) {
            RenderSystem.setShaderTexture(0, e.getKey().textureId());
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            if (axis) {
                // Physical-pixel space: snap the baseline and each glyph's origin to whole pixels.
                float ox = dev.mapX(x, baseline) * guiScale;
                float oy = Math.round(dev.mapY(x, baseline) * guiScale);
                float inv = 1f / guiScale;
                for (Placed pl : e.getValue()) {
                    TtfFace.Glyph g = pl.glyph();
                    float gx = Math.round(ox + pl.pen()) + g.left() + extraPx;
                    float gy = oy - g.top();
                    float topShift = shear * g.top(), botShift = shear * (g.top() - g.h());
                    vertex(b, pose, (gx + topShift) * inv, gy * inv, g.u0(), g.v0(), rr, gg, bb, a);
                    vertex(b, pose, (gx + botShift) * inv, (gy + g.h()) * inv, g.u0(), g.v1(), rr, gg, bb, a);
                    vertex(b, pose, (gx + g.w() + botShift) * inv, (gy + g.h()) * inv, g.u1(), g.v1(), rr, gg, bb, a);
                    vertex(b, pose, (gx + g.w() + topShift) * inv, gy * inv, g.u1(), g.v0(), rr, gg, bb, a);
                }
            } else {
                // Transformed text: place glyphs in local units and let the transform (and linear filtering) do the rest.
                float k = 1f / devPx;
                for (Placed pl : e.getValue()) {
                    TtfFace.Glyph g = pl.glyph();
                    float lx = x + (pl.pen() + g.left() + extraPx) * k, ly = baseline - g.top() * k;
                    float lw = g.w() * k, lh = g.h() * k;
                    float topShift = shear * g.top() * k, botShift = shear * (g.top() - g.h()) * k;
                    float[][] c = {{lx + topShift, ly, g.u0(), g.v0()}, {lx + botShift, ly + lh, g.u0(), g.v1()},
                            {lx + lw + botShift, ly + lh, g.u1(), g.v1()}, {lx + lw + topShift, ly, g.u1(), g.v0()}};
                    for (float[] v : c) vertex(b, pose, dev.mapX(v[0], v[1]), dev.mapY(v[0], v[1]), v[2], v[3], rr, gg, bb, a);
                }
            }
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
    }

    private static void vertex(BufferBuilder b, Matrix4f pose, float x, float y, float u, float v, float r, float g, float bl, float a) {
        b.addVertex(pose, x, y, 0).setUv(u, v).setColor(r, g, bl, a);
    }
}
