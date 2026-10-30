package me.methodial.apothecary.mc.render.text;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * One TrueType face rasterized by FreeType at exact pixel sizes (light hinting, grayscale AA).
 * Glyphs are cached per pixel size in a shared {@link GlyphAtlas}.
 */
final class TtfFace {
    /** A rasterized glyph at one pixel size. Metrics in physical pixels. */
    record Glyph(int index, float advance, int left, int top, int w, int h,
                 GlyphAtlas.Page page, float u0, float v0, float u1, float v1) {
        boolean visible() { return page != null && w > 0 && h > 0; }
    }

    /** Per pixel-size metrics and glyph cache. */
    final class Sized {
        final int px;
        final float ascent, descent;   // descent is negative
        final Map<Integer, Glyph> glyphs = new HashMap<>();

        Sized(int px) {
            this.px = px;
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                select(px);
                ascent = face.size().metrics().ascender() / 64f;
                descent = face.size().metrics().descender() / 64f;
            }
        }

        /** The glyph for a codepoint, or null when the face doesn't have it. */
        Glyph glyph(int cp) {
            Glyph g = glyphs.get(cp);
            if (g == null && !glyphs.containsKey(cp)) {
                g = rasterize(this, cp);
                glyphs.put(cp, g);
            }
            return g;
        }

        float kerning(int leftIndex, int rightIndex) {
            if (!hasKerning || leftIndex == 0 || rightIndex == 0) return 0;
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                select(px);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FT_Vector v = FT_Vector.malloc(stack);
                    if (FreeType.FT_Get_Kerning(face, leftIndex, rightIndex, FreeType.FT_KERNING_DEFAULT, v) != 0) return 0;
                    return v.x() / 64f;
                }
            }
        }
    }

    private static final int LOAD_FLAGS = FreeType.FT_LOAD_RENDER | ((FreeType.FT_RENDER_MODE_LIGHT & 15) << 16);
    /** Coverage → alpha curve: a slight boost so light text on dark surfaces doesn't look thin. */
    private static final int[] GAMMA = new int[256];

    static {
        for (int i = 0; i < 256; i++) GAMMA[i] = Math.round((float) Math.pow(i / 255f, 0.82) * 255);
    }

    final ResourceLocation id;
    private ByteBuffer data;
    private FT_Face face;
    private final boolean hasKerning;
    private final GlyphAtlas atlas;
    private int selectedPx = -1;
    private final Map<Integer, Sized> sizes = new HashMap<>();

    private TtfFace(ResourceLocation id, ByteBuffer data, FT_Face face, GlyphAtlas atlas) {
        this.id = id;
        this.data = data;
        this.face = face;
        this.atlas = atlas;
        this.hasKerning = FreeType.FT_HAS_KERNING(face);
    }

    static TtfFace load(ResourceLocation id, GlyphAtlas atlas) throws IOException {
        ByteBuffer buf;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(id)) {
            buf = TextureUtil.readResource(in);
            buf.flip();
        }
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pb = stack.mallocPointer(1);
                int err = FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), buf, 0L, pb);
                if (err != 0) {
                    MemoryUtil.memFree(buf);
                    throw new IOException("FreeType could not open " + id + " (error " + err + ")");
                }
                FT_Face f = FT_Face.create(pb.get());
                FreeType.FT_Select_Charmap(f, FreeType.FT_ENCODING_UNICODE);
                return new TtfFace(id, buf, f, atlas);
            }
        }
    }

    Sized size(int px) { return sizes.computeIfAbsent(px, Sized::new); }

    private void select(int px) {
        if (selectedPx != px) {
            FreeType.FT_Set_Pixel_Sizes(face, 0, px);
            selectedPx = px;
        }
    }

    private Glyph rasterize(Sized s, int cp) {
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            int index = FreeType.FT_Get_Char_Index(face, cp);
            if (index == 0) return null;
            select(s.px);
            if (FreeType.FT_Load_Glyph(face, index, LOAD_FLAGS) != 0) return null;
            FT_GlyphSlot slot = face.glyph();
            if (slot == null) return null;
            float advance = slot.advance().x() / 64f;
            FT_Bitmap bmp = slot.bitmap();
            int w = bmp.width(), h = bmp.rows(), pitch = bmp.pitch();
            if (w <= 0 || h <= 0) return new Glyph(index, advance, 0, 0, 0, 0, null, 0, 0, 0, 0);
            GlyphAtlas.Slot slotPos = atlas.allocate(w, h);
            if (slotPos == null) return new Glyph(index, advance, 0, 0, 0, 0, null, 0, 0, 0, 0);
            ByteBuffer src = bmp.buffer(Math.abs(pitch) * h);
            NativeImage img = slotPos.page().image;
            for (int y = 0; y < h; y++) {
                int row = pitch >= 0 ? y * pitch : (h - 1 - y) * -pitch;
                for (int x = 0; x < w; x++) {
                    int a = GAMMA[src.get(row + x) & 0xFF];
                    img.setPixelRGBA(slotPos.x() + x, slotPos.y() + y, (a << 24) | 0x00FFFFFF);
                }
            }
            slotPos.page().markDirty(slotPos.x(), slotPos.y(), w, h);
            float inv = 1f / GlyphAtlas.SIZE;
            return new Glyph(index, advance, slot.bitmap_left(), slot.bitmap_top(), w, h, slotPos.page(),
                    slotPos.x() * inv, slotPos.y() * inv, (slotPos.x() + w) * inv, (slotPos.y() + h) * inv);
        }
    }

    void close() {
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            if (face != null) FreeType.FT_Done_Face(face);
            face = null;
        }
        if (data != null) MemoryUtil.memFree(data);
        data = null;
        sizes.clear();
    }
}
