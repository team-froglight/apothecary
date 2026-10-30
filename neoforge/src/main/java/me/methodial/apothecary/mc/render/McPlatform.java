package me.methodial.apothecary.mc.render;

import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.Cursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/** Minecraft host services: fonts, clipboard, GLFW cursors and UI sounds. */
public final class McPlatform implements Platform {
    public static final McPlatform INSTANCE = new McPlatform();

    private final Map<Cursor, Long> cursors = new EnumMap<>(Cursor.class);
    private long lastSlide;
    private boolean sounds = true;

    private McPlatform() { }

    /** Globally enable/disable UI feedback sounds. */
    public void setSoundsEnabled(boolean on) { this.sounds = on; }

    @Override public TextMeasurer text() { return McFonts.INSTANCE; }

    @Override public String clipboard() { return Minecraft.getInstance().keyboardHandler.getClipboard(); }

    @Override public void setClipboard(String text) { Minecraft.getInstance().keyboardHandler.setClipboard(text); }

    @Override
    public void setCursor(Cursor cursor) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (cursor == Cursor.DEFAULT) {
            GLFW.glfwSetCursor(window, 0);
            return;
        }
        long handle = cursors.computeIfAbsent(cursor, c -> GLFW.glfwCreateStandardCursor(switch (c) {
            case POINTER, GRAB -> GLFW.GLFW_HAND_CURSOR;
            case TEXT -> GLFW.GLFW_IBEAM_CURSOR;
            case CROSSHAIR -> GLFW.GLFW_CROSSHAIR_CURSOR;
            case EW_RESIZE -> GLFW.GLFW_HRESIZE_CURSOR;
            case NS_RESIZE -> GLFW.GLFW_VRESIZE_CURSOR;
            case MOVE -> GLFW.GLFW_CROSSHAIR_CURSOR;
            case NOT_ALLOWED -> GLFW.GLFW_ARROW_CURSOR;
            default -> GLFW.GLFW_ARROW_CURSOR;
        }));
        GLFW.glfwSetCursor(window, handle);
    }

    @Override
    public void playSound(String id) {
        if (!sounds) return;
        var sm = Minecraft.getInstance().getSoundManager();
        switch (id) {
            case "click" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.25f));
            case "toggle" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f, 0.2f));
            case "slide" -> {
                long now = System.currentTimeMillis();
                if (now - lastSlide > 45) {
                    lastSlide = now;
                    sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.8f, 0.12f));
                }
            }
            case "open" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.6f));
            case "close" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9f, 0.4f));
            case "toast" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.8f));
            case "error" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.5f));
            default -> { }
        }
    }
}
