package me.methodial.apothecary.mc.node;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.kraz.camera.SceneCamera;
import me.methodial.kraz.render.RenderSettings;
import me.methodial.kraz.render.SceneCanvas;
import me.methodial.kraz.render.shaded.ShadedSceneCanvas;
import me.methodial.kraz.scene.Scene;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

/**
 * A Kraz {@link Scene} in the UI: blocks, block entities, entities and items, antialiased, following the node's
 * transform. Plays the scene's timeline in real time; with {@link #interactive()} the user can orbit (drag),
 * pan (right or middle drag) and zoom (wheel). {@link #shaded()} draws it through Kraz's shader pipeline instead:
 * sun shadows, sky and clouds, bloom. Type selector: {@code scene}.
 */
public class SceneView extends Node {
    private final Scene scene;
    private SceneCanvas canvas;
    private ShadedSceneCanvas shadedCanvas;
    private RenderSettings settings = RenderSettings.DEFAULT;
    private boolean shaded;
    private Consumer<ShadedSceneCanvas> shading;
    private boolean autoPlay = true;
    private long lastFrame = -1;

    public SceneView(Scene scene) {
        super("scene");
        this.scene = scene;
    }

    public Scene scene() { return scene; }

    /** Antialiasing and post effects (not used when {@link #shaded()}). */
    public SceneView settings(RenderSettings s) {
        this.settings = s;
        if (canvas != null) canvas.setSettings(s);
        return this;
    }

    /** Draws the scene lit the way a shader pack lights the game. */
    public SceneView shaded() { return shaded(null); }

    /** As {@link #shaded()}; {@code tune} sets the look (sun, time, clouds, bloom) each time the pipeline is created. */
    public SceneView shaded(Consumer<ShadedSceneCanvas> tune) {
        this.shaded = true;
        this.shading = tune;
        return this;
    }

    /** Whether this view advances the scene's timeline itself (default on). */
    public SceneView autoPlay(boolean on) {
        this.autoPlay = on;
        return this;
    }

    /** Lets the user orbit, pan and zoom the scene's camera with the mouse. */
    public SceneView interactive() {
        // DRAG carries the total distance since the press; the camera wants the step since the last event.
        float[] last = {0, 0};
        on(EventType.MOUSE_DOWN, e -> {
            last[0] = 0;
            last[1] = 0;
        });
        on(EventType.DRAG, e -> {
            float dx = e.dragX() - last[0], dy = e.dragY() - last[1];
            last[0] = e.dragX();
            last[1] = e.dragY();
            SceneCamera camera = scene.camera();
            if (e.button() == 0) {
                camera.orbit(-dx * 0.6f, dy * 0.6f);
            } else {
                float perUnit = (camera.orthographic() ? camera.orthoHeight()
                        : 2f * camera.distance() * (float) Math.tan(Math.toRadians(camera.fov()) * 0.5)) / Math.max(1f, height());
                camera.pan(-dx * perUnit, dy * perUnit);
            }
        });
        on(EventType.SCROLL, e -> {
            scene.camera().zoom((float) Math.pow(0.9, e.deltaY()));
            e.stopPropagation();
        });
        return this;
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) { return new float[]{160, 110}; }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        McCanvas mc = c.backend(McCanvas.class);
        if (mc == null || r.width() < 1 || r.height() < 1) return;
        long now = Util.getMillis();
        if (autoPlay && lastFrame >= 0) scene.timeline().advance(Math.min(now - lastFrame, 250) / 50f);
        lastFrame = now;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Rect gui = mc.toGui(r);
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        mc.withPose(() -> {
            if (shaded) {
                if (shadedCanvas == null) {
                    shadedCanvas = new ShadedSceneCanvas(scene);
                    if (shading != null) shading.accept(shadedCanvas);
                }
                shadedCanvas.draw(mc.graphics(), r.x(), r.y(), r.width(), r.height(), partial);
            } else {
                if (canvas == null) canvas = new SceneCanvas(scene).setSettings(settings);
                canvas.draw(mc.graphics(), r.x(), r.y(), r.width(), r.height(),
                        Math.max(1, (int) Math.round(gui.width() * guiScale)), Math.max(1, (int) Math.round(gui.height() * guiScale)), partial);
            }
        });
    }

    @Override
    protected void onDetach() {
        if (canvas != null) canvas.close();
        if (shadedCanvas != null) shadedCanvas.close();
        canvas = null;
        shadedCanvas = null;
        lastFrame = -1;
    }
}
