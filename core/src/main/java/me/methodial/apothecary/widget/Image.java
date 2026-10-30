package me.methodial.apothecary.widget;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.ImageRef;
import me.methodial.apothecary.style.ObjectFit;
import me.methodial.apothecary.style.Props;

/** An image or atlas sprite, scaled by {@code object-fit} and multiplied by {@code tint}. Type selector: {@code image}. */
public class Image extends Node {
    private ImageRef image;

    public Image(ImageRef image) {
        super("image");
        this.image = image;
    }

    public ImageRef image() { return image; }

    public Image image(ImageRef img) {
        this.image = img;
        markLayoutDirty();
        return this;
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        if (image == null) return new float[]{0, 0};
        return new float[]{image.width(), image.height()};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        if (image == null || r.isEmpty()) return;
        ObjectFit fit = get(Props.OBJECT_FIT);
        float iw = Math.max(1, image.width()), ih = Math.max(1, image.height());
        Rect dst = r;
        float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
        switch (fit) {
            case CONTAIN -> {
                float s = Math.min(r.width() / iw, r.height() / ih);
                dst = new Rect(r.x() + (r.width() - iw * s) / 2, r.y() + (r.height() - ih * s) / 2, iw * s, ih * s);
            }
            case NONE -> dst = new Rect(r.x() + (r.width() - iw) / 2, r.y() + (r.height() - ih) / 2, iw, ih);
            case COVER -> {
                float s = Math.max(r.width() / iw, r.height() / ih);
                float cw = r.width() / (iw * s), ch = r.height() / (ih * s);
                u0 = (1 - cw) / 2; u1 = u0 + cw;
                v0 = (1 - ch) / 2; v1 = v0 + ch;
            }
            default -> { }
        }
        c.drawImage(image, dst, u0, v0, u1, v1, get(Props.TINT));
    }
}
