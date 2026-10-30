package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.mc.node.McText;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.TextAlign;
import me.methodial.apothecary.widget.SegmentedControl;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.theme.Mixins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class TypographyPage {
    private TypographyPage() { }

    private static final String PANGRAM = "The quick brown fox jumps over the lazy dog";

    static Node build(ShowcaseScreen app) {
        // ---- families
        String[][] families = {
                {"poppins", "Poppins (TTF)"}, {"default", "Minecraft"}, {"space-mono", "Space Mono"},
                {"silkscreen", "Silkscreen"}, {"uniform", "Unifont"}, {"enchanting", "Enchanting table"},
        };
        Node fam = new Node().style(s -> s.col().gap(6));
        for (String[] f : families) {
            fam.add(new Node().style(s -> s.col().gap(1)).add(
                    new Text(f[1] + "  ·  font(\"" + f[0] + "\")").cls("caps"),
                    new Text(PANGRAM).style(s -> s.font(f[0]).fontSize(10))));
        }

        // ---- scale
        Node scale = new Node().style(s -> s.col().gap(2));
        for (float size : new float[]{6, 7, 8, 9, 11, 14, 17, 20}) {
            String label = (size == Math.floor(size) ? String.valueOf((int) size) : String.valueOf(size)) + "px";
            Text sample = new Text("Brewing stand");
            sample.style(st -> st.fontSize(size).ellipsis().shrink(1).minW(0));
            scale.add(new Node().style(st -> st.row().alignItems(me.methodial.apothecary.style.Align.CENTER).gap(8)).add(
                    new Text(label).cls("faint", "mono").style(st -> st.width(24).fontSize(6.5f).nowrap().shrink(0)),
                    sample));
        }

        // ---- weights & styles
        Node weights = row(
                new Text("Regular"), new Text("Semibold").style(Style::semibold), new Text("Bold").style(Style::bold),
                new Text("Italic").style(Style::italic), new Text("Underline").style(Style::underline),
                new Text("Strike").style(Style::strike), new Text("Upper caps").style(s -> s.upper().tracking(1)),
                new Text("Shadowed").style(s -> s.textShadow(true).font("default")));

        // ---- wrapping playground
        Text para = new Text("Potions are brewed in a brewing stand from water bottles and an ingredient. Awkward potions "
                + "are the base for most effects; redstone extends the duration and glowstone amplifies the strength. "
                + "Gunpowder turns any potion into a splash potion that can be thrown.");
        Node box = new Node().style(s -> s.pad(6).apply(Mixins.WELL)).add(para);
        Slider width = new Slider(60, 320, 220).step(1).showValue(v -> Math.round(v) + "px");
        width.onChange(v -> box.style().set(Props.WIDTH, Length.px(v)));
        box.style().set(Props.WIDTH, Length.px(220));
        SegmentedControl<TextAlign> align = new SegmentedControl<TextAlign>()
                .option(TextAlign.LEFT, "left").option(TextAlign.CENTER, "center").option(TextAlign.RIGHT, "right");
        align.onChange(v -> para.style().set(Props.TEXT_ALIGN, v));
        SegmentedControl<Integer> lines = new SegmentedControl<Integer>().option(0, "all lines").option(3, "max 3").option(1, "1 line");
        lines.onChange(v -> para.style(s -> s.lines(v)));

        // ---- rich text
        Component rich = Component.literal("Vanilla ").append(Component.literal("rich text").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(" with ").append(Component.literal("colors").withStyle(ChatFormatting.AQUA))
                .append(", ").append(Component.literal("obfuscation").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.LIGHT_PURPLE))
                .append(", ").append(Component.translatable("block.minecraft.brewing_stand").withStyle(ChatFormatting.GREEN))
                .append(" (translated) and ").append(Component.keybind("key.inventory").withStyle(ChatFormatting.YELLOW)).append(" keybinds.");

        return page().add(
                section("Font families", "Bundled TTFs (OFL) plus every vanilla font; register your own with McFonts.register.", card(null, null, fam)),
                new Node().cls("cards-wide").add(
                        card("Type scale", "font-size scales any font; 9px = one vanilla line", scale),
                        card("Weights & decorations", "Dedicated faces when registered, synthetic bold otherwise", weights)),
                section("Wrapping, alignment and truncation", "Text wraps at word boundaries, long words break, max-lines adds an ellipsis.",
                        card(null, null, control("width", width.style(s -> s.w(140))), control("align", align), control("lines", lines), box)),
                section("Minecraft components", "McText renders any Component: formatting, translation, keybinds.",
                        card(null, null, new McText(rich).style(s -> s.fontSize(9)))));
    }
}
