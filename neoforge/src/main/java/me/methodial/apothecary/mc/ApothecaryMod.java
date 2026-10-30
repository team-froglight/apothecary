package me.methodial.apothecary.mc;

import com.mojang.blaze3d.platform.InputConstants;
import me.methodial.apothecary.mc.render.ApothecaryShaders;
import me.methodial.apothecary.mc.showcase.ShowcaseScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Apothecary UI — client-only library mod. Provides the 1.21.1 rendering backend, resource-pack
 * fonts, and a showcase opened with {@code /apothecary} or the keybind (default: U).
 */
@Mod(value = ApothecaryMod.MOD_ID, dist = Dist.CLIENT)
public final class ApothecaryMod {
    public static final String MOD_ID = "apothecary";

    /**
     * Opens the showcase. Bound to U in development; unbound in production, where Apothecary usually ships
     * inside another mod (players can still bind it, or run /apothecary).
     */
    public static final KeyMapping OPEN_SHOWCASE = new KeyMapping("key.apothecary.showcase",
            InputConstants.Type.KEYSYM, net.neoforged.fml.loading.FMLEnvironment.production ? InputConstants.UNKNOWN.getValue() : GLFW.GLFW_KEY_U,
            "key.categories.apothecary");

    private static boolean openRequested;

    public ApothecaryMod(IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, ApothecaryShaders::onRegisterShaders);
        // fonts are cached per resource set: drop them when resource packs reload
        modBus.addListener(RegisterClientReloadListenersEvent.class, e -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> me.methodial.apothecary.mc.render.McFonts.INSTANCE.invalidate()));
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(OPEN_SHOWCASE));

        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, e -> e.getDispatcher().register(
                Commands.literal("apothecary").executes(ctx -> {
                    openRequested = true;   // the chat screen closes after the command; open next tick
                    return 1;
                })));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_SHOWCASE.consumeClick()) {
                if (mc.screen == null) openRequested = true;
            }
            if (openRequested && mc.screen == null) {
                openRequested = false;
                openShowcase();
            }
        });
        // Also reachable from the title screen (no world needed): press U there.
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, e -> {
            if (e.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen && OPEN_SHOWCASE.matches(e.getKeyCode(), e.getScanCode())) {
                openShowcase();
                e.setCanceled(true);
            }
        });
    }

    public static void openShowcase() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ShowcaseScreen(mc.screen));
    }
}
