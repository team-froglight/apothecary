package me.methodial.apothecary.mc.render.text;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.ArrayList;
import java.util.List;

/**
 * Shelf-packed RGBA atlas pages for rasterized glyphs (white with coverage in alpha), linearly filtered.
 * New glyphs are written to the CPU image and uploaded as one dirty rectangle before the next draw.
 */
final class GlyphAtlas {
    static final int SIZE = 1024;
    private static final int PAD = 1;

    static final class Page {
        final DynamicTexture texture;
        final NativeImage image;
        int shelfX = PAD, shelfY = PAD, shelfH;
        int dirtyX0 = Integer.MAX_VALUE, dirtyY0 = Integer.MAX_VALUE, dirtyX1, dirtyY1;

        Page() {
            image = new NativeImage(SIZE, SIZE, true);
            texture = new DynamicTexture(image);
            texture.setFilter(true, false);
        }

        int textureId() { return texture.getId(); }

        void markDirty(int x, int y, int w, int h) {
            dirtyX0 = Math.min(dirtyX0, x);
            dirtyY0 = Math.min(dirtyY0, y);
            dirtyX1 = Math.max(dirtyX1, x + w);
            dirtyY1 = Math.max(dirtyY1, y + h);
        }

        void upload() {
            if (dirtyX1 <= dirtyX0) return;
            texture.bind();
            image.upload(0, dirtyX0, dirtyY0, dirtyX0, dirtyY0, dirtyX1 - dirtyX0, dirtyY1 - dirtyY0, false, false);
            dirtyX0 = dirtyY0 = Integer.MAX_VALUE;
            dirtyX1 = dirtyY1 = 0;
        }
    }

    /** Where a glyph bitmap landed. */
    record Slot(Page page, int x, int y) { }

    private final List<Page> pages = new ArrayList<>();

    Slot allocate(int w, int h) {
        if (w + 2 * PAD > SIZE || h + 2 * PAD > SIZE) return null;
        Page p = pages.isEmpty() ? null : pages.get(pages.size() - 1);
        if (p != null) {
            if (p.shelfX + w + PAD > SIZE) {
                p.shelfX = PAD;
                p.shelfY += p.shelfH + PAD;
                p.shelfH = 0;
            }
            if (p.shelfY + h + PAD > SIZE) p = null;
        }
        if (p == null) {
            p = new Page();
            pages.add(p);
        }
        Slot s = new Slot(p, p.shelfX, p.shelfY);
        p.shelfX += w + PAD;
        p.shelfH = Math.max(p.shelfH, h);
        return s;
    }

    void uploadAll() {
        for (Page p : pages) p.upload();
    }

    void close() {
        for (Page p : pages) p.texture.close();
        pages.clear();
    }
}
