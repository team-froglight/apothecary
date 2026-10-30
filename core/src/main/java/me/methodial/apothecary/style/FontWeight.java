package me.methodial.apothecary.style;

/** Font weight; renderers pick a matching face or synthesize bold. */
public enum FontWeight {
    NORMAL, MEDIUM, SEMIBOLD, BOLD;

    public boolean isBold() { return this == BOLD; }
}
