package me.methodial.apothecary.event;

/** Keyboard events, dispatched to the focused node (or the root). Key codes are GLFW's. */
public class KeyEvent extends UIEvent {
    public static final int MOD_SHIFT = 1, MOD_CONTROL = 2, MOD_ALT = 4, MOD_SUPER = 8;

    public static final int KEY_SPACE = 32, KEY_A = 65, KEY_C = 67, KEY_V = 86, KEY_X = 88, KEY_Z = 90, KEY_Y = 89,
            KEY_ESCAPE = 256, KEY_ENTER = 257, KEY_TAB = 258, KEY_BACKSPACE = 259, KEY_INSERT = 260, KEY_DELETE = 261,
            KEY_RIGHT = 262, KEY_LEFT = 263, KEY_DOWN = 264, KEY_UP = 265, KEY_PAGE_UP = 266, KEY_PAGE_DOWN = 267,
            KEY_HOME = 268, KEY_END = 269, KEY_KP_ENTER = 335;

    private final int key, scancode, modifiers;

    public KeyEvent(int key, int scancode, int modifiers) {
        this.key = key;
        this.scancode = scancode;
        this.modifiers = modifiers;
    }

    public int key() { return key; }
    public int scancode() { return scancode; }
    public int modifiers() { return modifiers; }
    public boolean shift() { return (modifiers & MOD_SHIFT) != 0; }
    /** Control, or Command on macOS. */
    public boolean primary() { return (modifiers & (MOD_CONTROL | MOD_SUPER)) != 0; }
    public boolean alt() { return (modifiers & MOD_ALT) != 0; }
    public boolean isEnter() { return key == KEY_ENTER || key == KEY_KP_ENTER; }
}
