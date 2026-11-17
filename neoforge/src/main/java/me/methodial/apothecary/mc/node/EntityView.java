package me.methodial.apothecary.mc.node;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.kraz.render.RenderSettings;
import me.methodial.kraz.render.SceneCanvas;
import me.methodial.kraz.scene.Scene;
import me.methodial.kraz.scene.element.LiveEntityElement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/**
 * A living entity rendered like the inventory paper-doll, looking at the mouse. Defaults to the local
 * player. Type selector: {@code entity}.
 *
 * <p>Drawn by Kraz into an antialiased image, so unlike vanilla's paper-doll it follows every style transform
 * (rotation included) and is clipped like any other node.
 */
public class EntityView extends Node {
    private final Supplier<LivingEntity> entity;
    private float zoom = 1;
    private boolean followMouse = true;

    private Scene scene;
    private LiveEntityElement element;
    private SceneCanvas canvas;

    public EntityView() { this(() -> Minecraft.getInstance().player); }

    public EntityView(Supplier<LivingEntity> entity) {
        super("entity");
        this.entity = entity;
    }

    public EntityView zoom(float z) { this.zoom = z; return this; }

    /** Whether the entity turns to look at the cursor (default on). */
    public EntityView followMouse(boolean on) { this.followMouse = on; return this; }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) { return new float[]{40, 70}; }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        McCanvas mc = c.backend(McCanvas.class);
        LivingEntity e = entity.get();
        if (mc == null || e == null || r.width() < 1 || r.height() < 1) return;
        if (scene == null) {
            scene = Scene.create();
            element = scene.main().add(new LiveEntityElement(entity));
            scene.camera().setOrthographic(true).setYaw(0).setPitch(0);
        }
        if (canvas == null) canvas = new SceneCanvas(scene).setSettings(RenderSettings.DEFAULT);
        // Frame the entity's box with a little headroom; zoom narrows the view.
        float tall = Math.max(0.5f, e.getBbHeight());
        scene.camera().setTarget(0, tall * 0.5f, 0).setOrthoHeight(tall * 1.2f / Math.max(0.05f, zoom)).setContentRadius(tall);

        Rect gui = mc.toGui(r);
        if (followMouse && root() != null) {
            // As the inventory does it: the further the cursor is from the entity, the further it turns, up to a limit.
            float mx = root().mouseX() * mc.base(), my = root().mouseY() * mc.base();
            float eyeY = gui.y() + gui.height() * (1f - e.getEyeHeight() / (tall * 1.2f)) ;
            float turn = (float) Math.atan((gui.x() + gui.width() / 2f - mx) / 40f);
            float tilt = (float) Math.atan((eyeY - my) / 40f);
            element.setYaw(-turn * 20f).setHeadYaw(-turn * 20f).setHeadPitch(-tilt * 20f);
        } else {
            element.setYaw(0).setHeadYaw(0).setHeadPitch(0);
        }
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        mc.withPose(() -> canvas.draw(mc.graphics(), r.x(), r.y(), r.width(), r.height(),
                Math.max(1, (int) Math.round(gui.width() * guiScale)), Math.max(1, (int) Math.round(gui.height() * guiScale)), partial));
    }

    @Override
    protected void onDetach() {
        if (canvas != null) canvas.close();
        canvas = null;
    }
}
