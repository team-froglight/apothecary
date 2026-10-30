package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.mc.node.EntityView;
import me.methodial.apothecary.mc.node.ItemView;
import me.methodial.apothecary.mc.node.McText;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.ImageRef;
import me.methodial.apothecary.widget.Image;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.ObjectFit;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.theme.Motion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class MinecraftPage {
    private MinecraftPage() { }

    static Node build(ShowcaseScreen app) {
        // ---- inventory grid
        Node inv = new Node("inventory");
        ItemStack[] stacks = {
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.BOW),
                new ItemStack(Items.POTION), new ItemStack(Items.BREWING_STAND), new ItemStack(Items.BLAZE_POWDER, 12),
                new ItemStack(Items.NETHER_WART, 37), new ItemStack(Items.GLOWSTONE_DUST, 64), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
                new ItemStack(Items.GRASS_BLOCK, 64), new ItemStack(Items.OAK_LOG, 23), new ItemStack(Items.CHEST),
                new ItemStack(Items.SPYGLASS), new ItemStack(Items.ELYTRA), new ItemStack(Items.TRIDENT),
                new ItemStack(Items.AMETHYST_SHARD, 8), new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.TOTEM_OF_UNDYING),
        };
        ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
        worn.setDamageValue(worn.getMaxDamage() * 2 / 3);
        stacks[1].set(DataComponents.CUSTOM_NAME, Component.literal("Apothecary's Pick").withStyle(ChatFormatting.LIGHT_PURPLE));
        stacks[1].set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        for (ItemStack st : stacks) inv.add(ItemView.slot(st));
        inv.add(ItemView.slot(worn));

        // ---- scaled & transformed items
        Node scaled = new Node().style(st -> st.flex().itemsEnd().gap(8)).add(
                new ItemView(Items.DIAMOND).style(st -> st.size(12)),
                new ItemView(Items.DIAMOND).style(st -> st.size(24)),
                new ItemView(Items.DIAMOND).style(st -> st.size(40).animation(Motion.FLOAT.play(2.4f).easing(Easings.EASE_IN_OUT).infinite().alternate())),
                new ItemView(Items.NETHER_STAR).style(st -> st.size(40).animation(Motion.SPIN.play(4).easing(Easings.LINEAR).infinite())),
                new ItemView(Items.HEART_OF_THE_SEA).style(st -> st.size(32).animation(Motion.PULSE.play(1.4f).easing(Easings.EASE_IN_OUT).infinite())));

        // ---- player doll
        Node doll = new Node("doll");
        if (Minecraft.getInstance().player != null) doll.add(new EntityView().style(Style::full));
        else doll.style(st -> st.flex().center().pad(6))
                .add(new Text("Join a world to see your player here").cls("faint", "small").style(Style::textCenter));

        // ---- sprites
        Node sprites = new Node().style(st -> st.col().gap(4)).add(
                new Node().style(st -> st.row().gap(4)).add(
                        sprite("widget/button", 200, 20, 90, 20),
                        sprite("widget/button_highlighted", 200, 20, 60, 20)),
                new Node().style(st -> st.row().gap(2)).add(
                        sprite("hud/heart/full", 9, 9, 9, 9), sprite("hud/heart/full", 9, 9, 9, 9), sprite("hud/heart/half", 9, 9, 9, 9),
                        sprite("hud/food_full", 9, 9, 9, 9).style(st -> st.ml(6)), sprite("hud/armor_full", 9, 9, 9, 9),
                        sprite("hud/heart/full", 9, 9, 18, 18).style(st -> st.ml(6)
                                .animation(Motion.PULSE.play(1).easing(Easings.EASE_IN_OUT).infinite()))),
                new Image(ImageRef.texture("minecraft:textures/gui/title/minecraft.png", 256, 44))
                        .style(st -> st.size(160, 28).objectFit(ObjectFit.CONTAIN)));

        // ---- chat-style text
        Node chat = new Node().style(st -> st.col().gap(2).pad(4).radius(4).bg(Color.hex("#00000066"))).add(
                new McText(Component.literal("<Steve> ").append(Component.literal("anyone got blaze rods?").withStyle(ChatFormatting.WHITE))),
                new McText(Component.literal("Alex has made the advancement ").append(Component.literal("[Local Brewery]").withStyle(ChatFormatting.GREEN))),
                new McText(Component.translatable("death.attack.drown", Component.literal("Herobrine")).withStyle(ChatFormatting.GRAY)));

        return page().add(
                section("Items", "ItemView renders stacks with models, glint, counts and durability; hover for vanilla tooltips.",
                        new Node().cls("cards-wide").add(
                                card("Inventory", "item-slot is styled by the theme sheet; the tooltip is the item's own", inv),
                                card("Transforms", "Items follow style transforms and keyframe animations", scaled))),
                new Node().cls("cards-wide").add(
                        card("Entities", "EntityView: the inventory paper-doll, looking at your cursor", doll),
                        card("Sprites & textures", "GUI atlas sprites (nine-sliced per .mcmeta) and whole textures", sprites),
                        card("Chat components", "Translation, formatting and fonts from vanilla", chat)));
    }

    private static Image sprite(String path, int iw, int ih, float w, float h) {
        return (Image) new Image(ImageRef.sprite("minecraft:" + path, iw, ih)).style(st -> st.size(w, h));
    }
}
