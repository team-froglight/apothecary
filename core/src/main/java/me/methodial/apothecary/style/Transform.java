package me.methodial.apothecary.style;

import me.methodial.apothecary.geom.Affine;

/**
 * A decomposed 2D transform: translate, then rotate, then scale/skew, about the transform origin.
 * Decomposed so transitions between transforms interpolate naturally.
 * Translation may be in pixels or percent of the element's own size.
 */
public record Transform(Length translateX, Length translateY, float rotate, float scaleX, float scaleY, float skewX, float skewY) {
    public static final Transform IDENTITY = new Transform(Length.ZERO, Length.ZERO, 0, 1, 1, 0, 0);

    public static Transform translate(float x, float y) { return IDENTITY.withTranslate(Length.px(x), Length.px(y)); }
    public static Transform scale(float s) { return IDENTITY.withScale(s, s); }
    public static Transform rotate(float degrees) { return IDENTITY.withRotate(degrees); }

    public Transform withTranslate(Length x, Length y) { return new Transform(x, y, rotate, scaleX, scaleY, skewX, skewY); }
    public Transform withRotate(float deg) { return new Transform(translateX, translateY, deg, scaleX, scaleY, skewX, skewY); }
    public Transform withScale(float sx, float sy) { return new Transform(translateX, translateY, rotate, sx, sy, skewX, skewY); }
    public Transform withSkew(float kx, float ky) { return new Transform(translateX, translateY, rotate, scaleX, scaleY, kx, ky); }

    /** Combines two decomposed transforms component-wise (used when parsing "translate(..) scale(..)"). */
    public Transform then(Transform o) {
        return new Transform(addLength(translateX, o.translateX), addLength(translateY, o.translateY),
                rotate + o.rotate, scaleX * o.scaleX, scaleY * o.scaleY, skewX + o.skewX, skewY + o.skewY);
    }

    private static Length addLength(Length a, Length b) {
        if (a.value() == 0) return b;
        if (b.value() == 0) return a;
        return a.unit() == b.unit() ? new Length(a.value() + b.value(), a.unit()) : b;
    }

    public boolean isIdentity() { return this.equals(IDENTITY); }

    /** The matrix for a box of size w*h with the given origin (in pixels, relative to the box). */
    public Affine toAffine(float w, float h, float originX, float originY) {
        float tx = translateX.resolveOr(w, 0), ty = translateY.resolveOr(h, 0);
        Affine m = Affine.translation(originX + tx, originY + ty);
        if (rotate != 0) m = m.rotate((float) Math.toRadians(rotate));
        if (skewX != 0 || skewY != 0) {
            m = m.multiply(new Affine(1, (float) Math.tan(Math.toRadians(skewY)), (float) Math.tan(Math.toRadians(skewX)), 1, 0, 0));
        }
        if (scaleX != 1 || scaleY != 1) m = m.scale(scaleX, scaleY);
        return m.translate(-originX, -originY);
    }

    public Transform lerp(Transform o, float t) {
        return new Transform(translateX.lerp(o.translateX, t), translateY.lerp(o.translateY, t),
                rotate + (o.rotate - rotate) * t, scaleX + (o.scaleX - scaleX) * t, scaleY + (o.scaleY - scaleY) * t,
                skewX + (o.skewX - skewX) * t, skewY + (o.skewY - skewY) * t);
    }
}
