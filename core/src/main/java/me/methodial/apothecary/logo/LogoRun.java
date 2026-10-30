package me.methodial.apothecary.logo;

import java.util.List;

/** A stretch of a logo title in one style. */
public record LogoRun(String text, LogoStyle style) {
    /** A title in one style. */
    public static List<LogoRun> of(String text, LogoStyle style) { return List.of(new LogoRun(text, style)); }
}
