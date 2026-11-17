package me.methodial.apothecary.mc.scene;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * A little 3D world drawn the way a shader pack draws the game, into a rectangle of the GUI.
 *
 * <p>The pipeline itself (shadow map, screen-space lighting, sky and clouds, bloom, tonemap, FXAA) now lives in
 * Kraz as {@link me.methodial.kraz.render.shaded.ShadedScene}; this class keeps Apothecary's API over it, so
 * code written against it keeps working unchanged. New code that draws a Kraz scene can skip the {@link Content}
 * callback altogether and use {@code SceneView.shaded()} or Kraz's {@code ShadedSceneCanvas}.
 *
 * <p>World units are blocks, y up. One instance per view; {@link #close()} frees its targets.
 */
public final class ShadedScene implements AutoCloseable {

    /** The passes the scene is drawn in. */
    public enum Pass {
        /** From the sun (or moon), into the shadow map: only shape matters. World coordinates. */
        SHADOW,
        /** The camera's view: surfaces in their own colours, unlit (the scene lights them). */
        SCENE,
        /**
         * Only with {@link #materials}: water, lava and light-emitting surfaces again, as a mask, so they shade as what
         * they are. Draw them as in {@link #SCENE}, with blending off and the shader colour red for water, green for
         * lava, blue for light; the depth is the scene's (tested, not written).
         */
        MATERIAL,
        /**
         * Light that needs no lighting: lanterns, glows, beams, sparks. Added onto the lit picture, depth-tested
         * against the scene (not writing depth), so what's in front still hides it.
         */
        GLOW
    }

    /**
     * What the scene is. In {@link Pass#SHADOW} it draws in world coordinates (the sun's matrices are set). In the
     * others it draws as the camera has it: world coordinates for {@link #camera}, or, for {@link #screenCamera},
     * exactly as it would on the screen (GUI matrices, its own world-to-screen transform).
     */
    public interface Content {
        void draw(PoseStack pose, MultiBufferSource.BufferSource buffers, Pass pass);
    }

    private static final Pass[] PASSES = Pass.values();

    private final me.methodial.kraz.render.shaded.ShadedScene scene = new me.methodial.kraz.render.shaded.ShadedScene();

    /** The Kraz pipeline behind this scene. */
    public me.methodial.kraz.render.shaded.ShadedScene kraz() { return scene; }

    /** Looks at {@code target} from {@code distance} away, turned {@code yaw} and tilted down {@code pitch} (degrees). */
    public ShadedScene camera(Vector3f target, float yaw, float pitch, float distance, float fov) {
        scene.camera(target, yaw, pitch, distance, fov);
        return this;
    }

    /**
     * A camera the caller already has, for a view drawn in screen space (a map with its own pan, zoom and picking):
     * {@code worldToGui} takes world points to GUI units on the screen, {@code eye} is where its light rays come from
     * (far along the view, for an orthographic one), and the sky is seen turned {@code skyYaw}, tilted down
     * {@code skyPitch} (degrees) through a perspective lens, so it keeps its gradient.
     */
    public ShadedScene screenCamera(Matrix4f worldToGui, Vector3f eye, float skyYaw, float skyPitch) {
        scene.screenCamera(worldToGui, eye, skyYaw, skyPitch);
        return this;
    }

    /** Slides the picture across its rectangle without turning the camera, in clip units. */
    public ShadedScene shift(float x, float y) {
        scene.shift(x, y);
        return this;
    }

    /** Frosts the rectangle {@code (x, y, w, h)} (GUI units) of the scene, for a translucent panel over it. {@code w = 0} clears it. */
    public ShadedScene frost(float x, float y, float w, float h) {
        scene.frost(x, y, w, h);
        return this;
    }

    /** No frosted rectangles. */
    public ShadedScene clearFrost() {
        scene.clearFrost();
        return this;
    }

    /** One more frosted rectangle (up to eight; more are ignored). */
    public ShadedScene addFrost(float x, float y, float w, float h) {
        scene.addFrost(x, y, w, h);
        return this;
    }

    /** The direction toward the sun, in world terms. */
    public ShadedScene sun(Vector3f towardSun) {
        scene.sun(towardSun);
        return this;
    }

    /** Vanilla's sky colour for the place and time. */
    public ShadedScene skyColor(float r, float g, float b) {
        scene.skyColor(r, g, b);
        return this;
    }

    /** The sun's picture, {@code size} its half-width as a tangent. */
    public ShadedScene sunTexture(ResourceLocation texture, float size) {
        scene.sunTexture(texture, size);
        return this;
    }

    /** The moon's phases, a 4 by 2 sheet, and which one shows (0 full, 4 new). */
    public ShadedScene moon(ResourceLocation phases, int phase) {
        scene.moon(phases, phase);
        return this;
    }

    /** Seconds, for the clouds' drift. */
    public ShadedScene time(float seconds) {
        scene.time(seconds);
        return this;
    }

    /** The cloud layer's height, in world y; {@code Float.NaN} for none. */
    public ShadedScene clouds(float height) {
        scene.clouds(height);
        return this;
    }

    /** Whether the content draws water, lava and light in {@link Pass#MATERIAL}. */
    public ShadedScene materials(boolean on) {
        scene.materials(on);
        return this;
    }

    /** The clouds smaller by {@code scale}. */
    public ShadedScene cloudScale(float scale) {
        scene.cloudScale(scale);
        return this;
    }

    /** No point lights. */
    public ShadedScene clearLights() {
        scene.clearLights();
        return this;
    }

    /** A point of block light at {@code (x, y, z)} reaching {@code range} blocks, in colour {@code (r, g, b)}. Up to 128. */
    public ShadedScene addLight(float x, float y, float z, float range, float r, float g, float b) {
        scene.addLight(x, y, z, range, r, g, b);
        return this;
    }

    /** Where haze starts and where it has swallowed everything, in blocks from the camera; and the far plane. */
    public ShadedScene fog(float start, float end, float far) {
        scene.fog(start, end, far);
        return this;
    }

    /** The part of the world that casts and catches shadows: a sphere. */
    public ShadedScene shadows(Vector3f center, float radius, int mapSize) {
        scene.shadows(center, radius, mapSize);
        return this;
    }

    /** Render scale against the GUI rectangle's real pixels: 1 is one to one, above 1 supersamples (up to 2). */
    public ShadedScene resolution(float scale) {
        scene.resolution(scale);
        return this;
    }

    /** How much bloom the final mix takes, 0..1. */
    public ShadedScene bloom(float strength) {
        scene.bloom(strength);
        return this;
    }

    /** Draws the scene into the GUI rectangle {@code (x, y, w, h)}, in GUI units, on {@code g}'s pose. */
    public void render(GuiGraphics g, float x, float y, float w, float h, Content content) {
        scene.render(g, x, y, w, h, (pose, buffers, pass) -> content.draw(pose, buffers, PASSES[pass.ordinal()]));
    }

    /** Where a world point lands on the GUI, as of the last render; false when it's behind the camera. */
    public boolean project(Vector3f world, Vector2f gui) { return scene.project(world, gui); }

    /** The camera's position, as of the last render. */
    public Vector3f eye() { return scene.eye(); }

    @Override
    public void close() { scene.close(); }
}
