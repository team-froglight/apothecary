package me.methodial.apothecary.mc.node;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.mc.render.McCanvas;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Renders an {@link ItemStack} (3D models, enchant glint, count and durability bar) scaled to its box.
 * {@link #vanillaTooltip()} shows the item's own tooltip lines. Type selector: {@code item};
 * {@link #slot(ItemStack)} wraps it in an {@code item-slot}.
 */
public class ItemView extends Node {
    private Supplier<ItemStack> stack;
    private boolean decorations = true;

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
        if (mc == null) return;
        float size = Math.min(r.width(), r.height());
        mc.drawItem(stack.get(), r.x() + (r.width() - size) / 2, r.y() + (r.height() - size) / 2, size, decorations);
    }
}
