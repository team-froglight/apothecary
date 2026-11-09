package me.methodial.apothecary.mc.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.methodial.apothecary.mc.ApothecaryMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Core shaders used by the canvas plus a registry of effect shaders usable from {@code ShaderView}.
 * <p>To add an effect from another mod, call {@link #registerEffect(ResourceLocation)} during mod
 * construction and ship {@code assets/<ns>/shaders/core/<name>.json} using vertex {@code apothecary:ui}
 * and {@code #moj_import <apothecary:effect.glsl>} in the fragment shader (see the bundled effects).
 */
public final class ApothecaryShaders {
    private static final Logger LOG = LoggerFactory.getLogger("apothecary");

    static ShaderInstance roundedRect, line, arc;
    private static final Set<ResourceLocation> EFFECT_IDS = new LinkedHashSet<>();
    private static final Map<String, ShaderInstance> EFFECTS = new LinkedHashMap<>();

    static {
        for (String e : new String[]{"aurora", "plasma", "mesh_gradient", "ripple", "voronoi", "starfield",
                "hsv_square", "hue_strip", "alpha_strip", "checker"}) {
            EFFECT_IDS.add(ResourceLocation.fromNamespaceAndPath(ApothecaryMod.MOD_ID, e));
        }
    }

    private ApothecaryShaders() { }

    /** Registers an effect shader id (call before shaders load, e.g. in your mod constructor). */
    public static void registerEffect(ResourceLocation id) { EFFECT_IDS.add(id); }

    public static Set<String> effectIds() { return java.util.Collections.unmodifiableSet(EFFECTS.keySet()); }

    static ShaderInstance effect(String id) { return EFFECTS.get(id); }

    public static boolean ready() { return roundedRect != null && line != null && arc != null; }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // Forget the last load's programs first: the game closes them on reload, and one that fails to load now
        // must leave ready() false (or its effect missing) rather than the closed program in use.
        roundedRect = line = arc = null;
        EFFECTS.clear();
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("rounded_rect"), DefaultVertexFormat.POSITION_TEX), s -> roundedRect = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("line"), DefaultVertexFormat.POSITION_TEX), s -> line = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("arc"), DefaultVertexFormat.POSITION_TEX), s -> arc = s);
        } catch (IOException e) {
            throw new RuntimeException("Apothecary core shaders failed to load", e);
        }
        for (ResourceLocation id : EFFECT_IDS) {
            try {
                String key = id.toString();
                event.registerShader(new ShaderInstance(event.getResourceProvider(), id, DefaultVertexFormat.POSITION_TEX), s -> EFFECTS.put(key, s));
            } catch (IOException | RuntimeException e) {
                LOG.error("Effect shader {} failed to load: {}", id, e.toString());
            }
        }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ApothecaryMod.MOD_ID, path); }
}
