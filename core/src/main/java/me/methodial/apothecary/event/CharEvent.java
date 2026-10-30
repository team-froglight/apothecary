package me.methodial.apothecary.event;

/** A typed character (after keyboard layout), dispatched to the focused node. */
public class CharEvent extends UIEvent {
    private final int codepoint;
    private final int modifiers;

    public CharEvent(int codepoint, int modifiers) {
        this.codepoint = codepoint;
        this.modifiers = modifiers;
    }

    public int codepoint() { return codepoint; }
    public int modifiers() { return modifiers; }
    public String text() { return new String(Character.toChars(codepoint)); }
}
