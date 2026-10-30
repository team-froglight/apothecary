package me.methodial.apothecary.mc.node;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/**
 * A living entity rendered like the inventory paper-doll, looking at the mouse. Defaults to the local
 * player. Type selector: {@code entity}. Only translations/uniform scale of ancestors are honored
 * (vanilla's entity renderer clips with an axis-aligned scissor).
 */
public class EntityView extends Node {
    private final Supplier<LivingEntity> entity;
    private float zoom = 1;

    public EntityView() { this(() -> Minecraft.getInstance().player); }

    public EntityView(Supplier<LivingEntity> entity) {
        super("entity");
        this.entity = entity;
    }

    public EntityView zoom(float z) { this.zoom = z; return this; }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) { return new float[]{40, 70}; }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        McCanvas mc = c.backend(McCanvas.class);
        LivingEntity e = entity.get();
        if (mc == null || e == null) return;
        Rect gui = mc.toGui(r);
        int x0 = Math.round(gui.x()), y0 = Math.round(gui.y()), x1 = Math.round(gui.right()), y1 = Math.round(gui.bottom());
        float h = gui.height();
        int scale = Math.max(4, Math.round(h * 0.42f * zoom / Math.max(0.5f, e.getBbHeight() / 1.8f)));
        float mx = root() != null ? root().mouseX() * mc.base() : (x0 + x1) / 2f;
        float my = root() != null ? root().mouseY() * mc.base() : (y0 + y1) / 2f;
        mc.flush();
        InventoryScreen.renderEntityInInventoryFollowsMouse(mc.graphics(), x0, y0, x1, y1, scale, 0.0625f, mx, my, e);
        mc.afterVanilla3D();
    }
}
