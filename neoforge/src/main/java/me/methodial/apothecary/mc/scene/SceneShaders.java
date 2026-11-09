package me.methodial.apothecary.mc.scene;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.methodial.apothecary.mc.ApothecaryMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** The passes of {@link ShadedScene} ({@code assets/apothecary/shaders/core/scene_*}). */
public final class SceneShaders {
    private static final Logger LOG = LoggerFactory.getLogger("apothecary");

    static ShaderInstance composite, bloomDown, bloomUp, grade, present;

    private SceneShaders() { }

    static boolean ready() { return composite != null && bloomDown != null && bloomUp != null && grade != null && present != null; }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // Forget the last load's programs first: the game closes them on reload, and a pass that fails to load
        // now must leave ready() false rather than the closed program in use.
        composite = bloomDown = bloomUp = grade = present = null;
        try {
            event.registerShader(load(event, "scene_composite"), s -> composite = s);
            event.registerShader(load(event, "scene_bloom_down"), s -> bloomDown = s);
            event.registerShader(load(event, "scene_bloom_up"), s -> bloomUp = s);
            event.registerShader(load(event, "scene_grade"), s -> grade = s);
            event.registerShader(load(event, "scene_present"), s -> present = s);
        } catch (IOException | RuntimeException e) {
            LOG.error("Scene shaders failed to load; shaded scenes won't draw: {}", e.toString());
        }
    }

    private static ShaderInstance load(RegisterShadersEvent event, String name) throws IOException {
        return new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(ApothecaryMod.MOD_ID, name), DefaultVertexFormat.POSITION_TEX);
    }
}
