package me.methodial.apothecary.theme;

/** Typography presets: {@code s.type(TypeScale.H2)}. 9px = one vanilla Minecraft line. */
public final class TypeScale {
    private TypeScale() { }

    public static final TypeStyle DISPLAY = TypeStyle.size(26).bold().lineHeight(1.1f);
    public static final TypeStyle H1 = TypeStyle.size(20).bold().lineHeight(1.15f);
    public static final TypeStyle H2 = TypeStyle.size(14).bold().lineHeight(1.2f);
    public static final TypeStyle H3 = TypeStyle.size(10).semibold();
    public static final TypeStyle TITLE = TypeStyle.size(8).semibold();
    public static final TypeStyle BODY = TypeStyle.size(8);
    public static final TypeStyle LEAD = TypeStyle.size(9).color(Tokens.TEXT_DIM);
    public static final TypeStyle SMALL = TypeStyle.size(7);
    public static final TypeStyle CAPTION = TypeStyle.size(6.5f).color(Tokens.TEXT_FAINT);
    /** Small uppercase label. */
    public static final TypeStyle CAPS = TypeStyle.size(6.5f).semibold().tracking(0.6f).upper().color(Tokens.TEXT_FAINT);
    public static final TypeStyle CODE = TypeStyle.size(6.5f).family(Tokens.MONO);
}
