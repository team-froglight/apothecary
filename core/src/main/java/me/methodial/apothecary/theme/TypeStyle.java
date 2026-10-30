package me.methodial.apothecary.theme;

import me.methodial.apothecary.style.FontWeight;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.style.TextTransform;

/**
 * A typography preset. Null fields are left untouched when applied: {@code s.type(TypeScale.H2)}.
 * <pre>{@code TypeStyle LABEL = TypeStyle.size(7).weight(FontWeight.SEMIBOLD).tracking(0.5f).upper();}</pre>
 */
public record TypeStyle(Float size, FontWeight weight, Float lineHeight, Float tracking, TextTransform transform,
                        Ref<String> family, Ref<me.methodial.apothecary.style.Color> color) {

    public static TypeStyle size(float px) { return new TypeStyle(px, null, null, null, null, null, null); }

    public TypeStyle weight(FontWeight w) { return new TypeStyle(size, w, lineHeight, tracking, transform, family, color); }
    public TypeStyle bold() { return weight(FontWeight.BOLD); }
    public TypeStyle semibold() { return weight(FontWeight.SEMIBOLD); }
    public TypeStyle medium() { return weight(FontWeight.MEDIUM); }
    public TypeStyle lineHeight(float f) { return new TypeStyle(size, weight, f, tracking, transform, family, color); }
    public TypeStyle tracking(float px) { return new TypeStyle(size, weight, lineHeight, px, transform, family, color); }
    public TypeStyle upper() { return new TypeStyle(size, weight, lineHeight, tracking, TextTransform.UPPERCASE, family, color); }
    public TypeStyle family(Ref<String> f) { return new TypeStyle(size, weight, lineHeight, tracking, transform, f, color); }
    public TypeStyle color(Ref<me.methodial.apothecary.style.Color> c) { return new TypeStyle(size, weight, lineHeight, tracking, transform, family, c); }

    public void applyTo(Style s) {
        if (size != null) s.fontSize(size);
        if (weight != null) s.weight(weight);
        if (lineHeight != null) s.lineHeight(lineHeight);
        if (tracking != null) s.tracking(tracking);
        if (transform != null) s.set(Props.TEXT_TRANSFORM, transform);
        if (family != null) s.font(family);
        if (color != null) s.fg(color);
    }
}
