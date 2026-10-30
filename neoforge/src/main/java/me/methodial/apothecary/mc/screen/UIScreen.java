package me.methodial.apothecary.mc.screen;

import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.theme.DefaultTheme;
import me.methodial.apothecary.theme.Theme;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.mc.render.McPlatform;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A Minecraft screen hosting an Apothecary {@link UIRoot}. Subclass and override {@link #build(UIRoot)},
 * or create one directly: {@code UIScreen.of(Component.literal("Hi"), root -> root.setContent(myNode))}.
 * The default theme is applied first; add more sheets in {@code build}.
 */
public class UIScreen extends Screen {
    /** How the world behind the screen is treated. */
    public enum Backdrop { NONE, DIM, BLUR }

    protected final UIRoot ui = new UIRoot(McPlatform.INSTANCE);
    private boolean built;
    private long lastNanos;
    private Backdrop backdrop = Backdrop.BLUR;
    private boolean pauses;
    private java.util.function.Consumer<UIRoot> builder;
    private Screen parent;
    /** Fixed UI scale (physical pixels per UI unit), or 0 for automatic. */
    private float fixedScale;
    private float designWidth = 560, designHeight = 315;
    /** Root units → GUI units for the current frame. */
    private float base = 1;

    public UIScreen(Component title) {
        super(title);
        ui.addSheet(DefaultTheme.sheet());
        ui.onWarning((n, msg) -> org.slf4j.LoggerFactory.getLogger("apothecary").warn("{}: {}", n, msg));
    }

    public static UIScreen of(Component title, java.util.function.Consumer<UIRoot> builder) {
        UIScreen s = new UIScreen(title);
        s.builder = builder;
        return s;
    }

    public UIRoot ui() { return ui; }

    public UIScreen backdrop(Backdrop b) { this.backdrop = b; return this; }

    public UIScreen pauses(boolean p) { this.pauses = p; return this; }

    /** Screen to return to when closed. */
    public UIScreen parent(Screen p) { this.parent = p; return this; }

    /** Adds a sheet of rules on top of the default theme. */
    public UIScreen sheet(Sheet s) { ui.addSheet(s); return this; }

    /** Applies token values (colors, radius, fonts...) to this screen. */
    public UIScreen theme(Theme t) { ui.theme(t); return this; }

    /**
     * UI scaling. By default the UI ignores Minecraft's GUI scale and picks the largest half-step scale
     * that still leaves at least {@code designWidth x designHeight} UI units (560x315), so layouts get the
     * same room on every window. {@link #uiScale(float)} pins a fixed physical-pixels-per-unit scale;
     * {@link #useGuiScale()} follows Minecraft's GUI scale instead.
     */
    public UIScreen designSize(float w, float h) { this.designWidth = w; this.designHeight = h; this.fixedScale = 0; return this; }

    public UIScreen uiScale(float pixelsPerUnit) { this.fixedScale = pixelsPerUnit; return this; }

    public UIScreen useGuiScale() { this.fixedScale = -1; return this; }

    /** Physical pixels per UI unit currently in use. */
    public float currentScale() {
        var w = net.minecraft.client.Minecraft.getInstance().getWindow();
        if (fixedScale < 0) return (float) w.getGuiScale();
        if (fixedScale > 0) return fixedScale;
        float s = Math.min(w.getWidth() / designWidth, w.getHeight() / designHeight);
        return Math.max(1, (float) Math.floor(s * 2) / 2f);
    }

    private void updateScale() {
        var w = net.minecraft.client.Minecraft.getInstance().getWindow();
        float scale = currentScale();
        base = scale / (float) w.getGuiScale();
        ui.resize(w.getWidth() / scale, w.getHeight() / scale);
    }

    /** Builds the UI once; called on first {@link #init()}. */
    protected void build(UIRoot root) {
        if (builder != null) builder.accept(root);
    }

    protected Node content() { return ui.content(); }

    @Override
    protected void init() {
        updateScale();
        if (!built) {
            built = true;
            build(ui);
            lastNanos = System.nanoTime();
        }
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        updateScale();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        switch (backdrop) {
            case BLUR -> renderBackground(g, mouseX, mouseY, partialTick);
            case DIM -> renderTransparentBackground(g);
            case NONE -> { }
        }
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1e9f;
        lastNanos = now;
        updateScale();
        me.methodial.apothecary.mc.render.McFonts.INSTANCE.setDeviceScale(base * (float) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale());
        McCanvas canvas = new McCanvas(g, base);
        ui.render(canvas, dt);
        canvas.finish();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (backdrop == Backdrop.BLUR) super.renderBackground(g, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------ input

    @Override
    public void mouseMoved(double x, double y) { ui.mouseMove((float) x / base, (float) y / base); }

    @Override
    public boolean mouseClicked(double x, double y, int button) { return ui.mouseDown((float) x / base, (float) y / base, button); }

    @Override
    public boolean mouseReleased(double x, double y, int button) { return ui.mouseUp((float) x / base, (float) y / base, button); }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        ui.mouseMove((float) x / base, (float) y / base);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) { return ui.scroll((float) x / base, (float) y / base, (float) sx, (float) sy); }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (ui.keyDown(key, scancode, mods)) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int key, int scancode, int mods) { return ui.keyUp(key, scancode, mods); }

    @Override
    public boolean charTyped(char c, int mods) { return ui.charTyped(c, mods); }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void removed() { ui.dispose(); }

    @Override
    public boolean isPauseScreen() { return pauses; }
}
