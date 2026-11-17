package me.methodial.apothecary.mc.node;

import com.mojang.blaze3d.vertex.PoseStack;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.kraz.render.RenderSettings;
import me.methodial.kraz.render.SceneCanvas;
import me.methodial.kraz.scene.Scene;
import me.methodial.kraz.scene.element.ItemElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Renders an {@link ItemStack} (3D models, enchant glint, count and durability bar) scaled to its box.
 * {@link #vanillaTooltip()} shows the item's own tooltip lines. Type selector: {@code item};
 * {@link #slot(ItemStack)} wraps it in an {@code item-slot}.
 *
 * <p>The model is drawn by Kraz: antialiased at whatever size the box ends up, lit like an inventory slot, and
 * following every style transform (rotation included). An item that is not enchanted is rendered once and then
 * reused as an image until the stack or its size changes.
 */
public class ItemView extends Node {
    private Supplier<ItemStack> stack;
    private boolean decorations = true;

    private Scene scene;
    private ItemElement element;
    private SceneCanvas canvas;
    private ItemStack shown = ItemStack.EMPTY;

    public ItemView(ItemStack stack) {
        super("item");
        this.stack = () -> stack;
    }

    public ItemView(Item item) { this(new ItemStack(item)); }

    /** A live stack (e.g. from the player's inventory). */
    public ItemView(Supplier<ItemStack> stack) {
        super("item");
        this.stack = stack;
    }

    public ItemStack stack() { return stack.get(); }

    public ItemView stack(ItemStack s) { this.stack = () -> s; return this; }

    public ItemView decorations(boolean d) { this.decorations = d; return this; }

    /** Uses the item's vanilla tooltip (name, enchantments, lore...) as this node's tooltip. */
    public ItemView vanillaTooltip() {
        tooltip(() -> {
            ItemStack s = stack.get();
            if (s.isEmpty()) return null;
            List<Component> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), s);
            Node box = new Node("tooltip-lines").style(x -> x.col().gap(1));
            for (int i = 0; i < lines.size(); i++) {
                McText t = new McText(lines.get(i));
                if (i == 0) t.style(x -> x.mb(1));
                box.add(t);
            }
            return box;
        });
        return this;
    }

    /** An item inside a styled slot. */
    public static Node slot(ItemStack s) {
        return new Node("item-slot").add(new ItemView(s).vanillaTooltip().style(st -> st.full()));
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) { return new float[]{16, 16}; }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        McCanvas mc = c.backend(McCanvas.class);
        ItemStack current = stack.get();
        if (mc == null || current == null || current.isEmpty()) return;
        if (scene == null) {
            // One item, seen straight on through an orthographic lens exactly one item wide: an inventory icon.
            scene = Scene.create();
            element = scene.main().add(new ItemElement(current.copy()).setDisplayContext(ItemDisplayContext.GUI));
            scene.camera().setOrthographic(true).setYaw(0).setPitch(0).setTarget(0, 0, 0).setOrthoHeight(1).setContentRadius(1);
            shown = current.copy();
        } else if (!ItemStack.isSameItemSameComponents(shown, current)) {
            // Counts and damage are decorations; only a different item or different components need a new render.
            shown = current.copy();
            element.setStack(shown);
        }
        if (canvas == null) {
            canvas = new SceneCanvas(scene).setSettings(RenderSettings.DEFAULT).setRedraw(SceneCanvas.Redraw.WHEN_CHANGED);
        }
        float size = Math.min(r.width(), r.height());
        float x = r.x() + (r.width() - size) / 2, y = r.y() + (r.height() - size) / 2;
        Rect gui = mc.toGui(new Rect(x, y, size, size));
        int pixels = Math.max(1, (int) Math.ceil(Math.max(gui.width(), gui.height()) * Minecraft.getInstance().getWindow().getGuiScale()));
        mc.withPose(() -> {
            GuiGraphics g = mc.graphics();
            canvas.draw(g, x, y, size, size, pixels, pixels, 0f);
            if (decorations) {
                PoseStack pose = g.pose();
                pose.pushPose();
                pose.translate(x, y, 0);
                pose.scale(size / 16f, size / 16f, 1f);
                g.renderItemDecorations(Minecraft.getInstance().font, current, 0, 0);
                pose.popPose();
            }
        });
    }

    @Override
    protected void onDetach() {
        if (canvas != null) canvas.close();
        canvas = null;
    }
}
