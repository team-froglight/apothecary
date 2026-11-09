package me.methodial.apothecary.mc.scene;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * A little 3D world drawn the way a shader pack draws the game, into a rectangle of the GUI.
 *
 * <p>The scene draws itself with vanilla's renderers ({@link Content}), twice: once from the sun
 * into a shadow map, once from the camera. Then everything else is done in screen space from the
 * camera's colour and depth: positions and normals rebuilt from depth, soft sun shadows, ambient
 * occlusion, the overworld sky with the sun and moon, the stars and volumetric clouds, haze, volumetric light
 * through the shadow map, bloom, a filmic tonemap with colour grading, and FXAA. The sky, clouds,
 * light colours, tonemap, grading and FXAA follow Complementary Unbound's.
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

    /** Complementary's noise, for the volumetric clouds. */
    private static final ResourceLocation NOISE = ResourceLocation.fromNamespaceAndPath("apothecary", "textures/scene/noise.png");
    private static final ResourceLocation VANILLA_SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceLocation VANILLA_MOON = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final int BLOOM_LEVELS = 6;

    // ------------------------------------------------------------------ settings

    private final Vector3f target = new Vector3f();
    private float yaw = -30f, pitch = 25f, distance = 10f, fov = 40f, far = 256f;
    private final Vector3f sun = new Vector3f(0.3f, 0.6f, -0.75f).normalize();
    private final Vector3f skyColor = new Vector3f(0.47f, 0.655f, 1.0f);
    private ResourceLocation sunTexture = VANILLA_SUN;
    private float sunSize = 0.3f;
    private ResourceLocation moonTexture = VANILLA_MOON;
    private int moonPhase;
    private float time;
    private float cloudHeight = 128f;
    private float fogStart = 64f, fogEnd = 192f;
    private final Vector3f shadowCenter = new Vector3f();
    private float shadowRadius = 8f;
    private int shadowSize = 1024;
    private float resolution = 1f;
    private float bloom = 0.1f;
    private float shiftX, shiftY;
    /** Frosted rectangles (GUI units, x y w h each), blurred behind panels drawn over the scene. */
    private static final int MAX_FROST = 4;
    private final float[] frosts = new float[MAX_FROST * 4];
    private int frostCount;
    /** Screen-camera mode: the caller's world-to-GUI transform, its eye, and the sky's look direction. */
    private boolean screen;
    private final Matrix4f worldToGui = new Matrix4f();
    private final Vector3f screenEye = new Vector3f();
    private float skyYaw, skyPitch;

    /** Looks at {@code target} from {@code distance} away, turned {@code yaw} and tilted down {@code pitch} (degrees). */
    public ShadedScene camera(Vector3f target, float yaw, float pitch, float distance, float fov) {
        this.screen = false;
        this.target.set(target);
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
        this.fov = fov;
        return this;
    }

    /**
     * A camera the caller already has, for a view drawn in screen space (a map with its own pan, zoom and picking):
     * {@code worldToGui} takes world points to GUI units on the screen, {@code eye} is where its light rays come from
     * (far along the view, for an orthographic one), and the sky is seen turned {@code skyYaw}, tilted down
     * {@code skyPitch} (degrees) through a perspective lens, so it keeps its gradient.
     */
    public ShadedScene screenCamera(Matrix4f worldToGui, Vector3f eye, float skyYaw, float skyPitch) {
        this.screen = true;
        this.worldToGui.set(worldToGui);
        this.screenEye.set(eye);
        this.skyYaw = skyYaw;
        this.skyPitch = skyPitch;
        return this;
    }

    /**
     * Slides the picture across its rectangle without turning the camera (an off-axis lens), in
     * clip units: {@code x = 0.3} puts what the camera looks at 15% right of center.
     */
    public ShadedScene shift(float x, float y) {
        shiftX = x;
        shiftY = y;
        return this;
    }

    /**
     * Frosts the rectangle {@code (x, y, w, h)} (GUI units) of the scene: blurred there, for a translucent panel
     * drawn over it. It costs nothing: the bloom pyramid is that blur already. {@code w = 0} clears it.
     */
    public ShadedScene frost(float x, float y, float w, float h) {
        clearFrost();
        return w > 0f && h > 0f ? addFrost(x, y, w, h) : this;
    }

    /** No frosted rectangles. */
    public ShadedScene clearFrost() {
        frostCount = 0;
        return this;
    }

    /** One more frosted rectangle (up to four; more are ignored), for a screen with several panels. */
    public ShadedScene addFrost(float x, float y, float w, float h) {
        if (frostCount == MAX_FROST || w <= 0f || h <= 0f) return this;
        frosts[frostCount * 4] = x;
        frosts[frostCount * 4 + 1] = y;
        frosts[frostCount * 4 + 2] = w;
        frosts[frostCount * 4 + 3] = h;
        frostCount++;
        return this;
    }

    /** The direction toward the sun, in world terms. */
    public ShadedScene sun(Vector3f towardSun) {
        sun.set(towardSun).normalize();
        return this;
    }

    /** Vanilla's sky colour for the place and time (the biome's, darkened toward night). */
    public ShadedScene skyColor(float r, float g, float b) {
        skyColor.set(r, g, b);
        return this;
    }

    /** The sun's picture, drawn added on as vanilla draws {@code sun.png}, {@code size} its half-width as a tangent. */
    public ShadedScene sunTexture(ResourceLocation texture, float size) {
        sunTexture = texture;
        sunSize = size;
        return this;
    }

    /** The moon's phases, a 4 by 2 sheet as vanilla's {@code moon_phases.png}, and which one shows (0 full, 4 new). */
    public ShadedScene moon(ResourceLocation phases, int phase) {
        moonTexture = phases;
        moonPhase = Math.floorMod(phase, 8);
        return this;
    }

    /** Seconds, for the clouds' drift. */
    public ShadedScene time(float seconds) {
        time = seconds;
        return this;
    }

    /** The cloud layer's height, in world y; {@code Float.NaN} for none (the caller draws its own). */
    public ShadedScene clouds(float height) {
        cloudHeight = height;
        return this;
    }

    /** Where haze starts and where it has swallowed everything, in blocks from the camera; and the far plane. */
    public ShadedScene fog(float start, float end, float far) {
        fogStart = start;
        fogEnd = end;
        this.far = far;
        return this;
    }

    /** The part of the world that casts and catches shadows: a sphere. */
    public ShadedScene shadows(Vector3f center, float radius, int mapSize) {
        shadowCenter.set(center);
        shadowRadius = radius;
        shadowSize = mapSize;
        return this;
    }

    /** Render scale against the GUI rectangle's real pixels: 1 is one to one, above 1 supersamples (up to 2). */
    public ShadedScene resolution(float scale) {
        resolution = Math.max(0.25f, Math.min(2f, scale));
        return this;
    }

    /** How much bloom the final mix takes, 0..1. */
    public ShadedScene bloom(float strength) {
        bloom = strength;
        return this;
    }

    // ------------------------------------------------------------------ state

    private TextureTarget scene, shadow;
    private final FloatTarget hdr = new FloatTarget(), ldr = new FloatTarget();
    private final FloatTarget[] blooms = new FloatTarget[BLOOM_LEVELS];
    private final Matrix4f view = new Matrix4f(), proj = new Matrix4f(), viewProj = new Matrix4f(), inverse = new Matrix4f();
    private final Matrix4f shadowView = new Matrix4f(), shadowProj = new Matrix4f(), shadowMat = new Matrix4f();
    private final Matrix4f savedProj = new Matrix4f(), guiView = new Matrix4f();
    private final Matrix4f skyProj = new Matrix4f(), skyInverse = new Matrix4f();
    /** Half a block in pixels: at the camera (orthographic) or divided by distance (perspective). */
    private float aoScale;
    private final Matrix4f turn = new Matrix4f();
    private final Vector3f eye = new Vector3f(), scratch = new Vector3f(), from = new Vector3f(), up = new Vector3f(), light = new Vector3f();
    private final Vector4f clip = new Vector4f();
    private final PoseStack pose = new PoseStack();
    private float gx, gy, gw, gh;

    {
        for (int i = 0; i < BLOOM_LEVELS; i++) blooms[i] = new FloatTarget();
    }

    // ------------------------------------------------------------------ rendering

    /** Draws the scene into the GUI rectangle {@code (x, y, w, h)}, in GUI units, on {@code g}'s pose. */
    public void render(GuiGraphics g, float x, float y, float w, float h, Content content) {
        if (!SceneShaders.ready() || w < 1 || h < 1) return;
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.assertOnRenderThread();
        gx = x;
        gy = y;
        gw = w;
        gh = h;
        double scale = mc.getWindow().getGuiScale() * resolution;
        int pw = Math.max(1, (int) Math.round(w * scale)), ph = Math.max(1, (int) Math.round(h * scale));
        allocate(pw, ph);
        savedProj.set(RenderSystem.getProjectionMatrix());
        guiView.set(RenderSystem.getModelViewMatrix());
        matrices(w / h, (float) scale);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        buffers.endBatch();
        VertexSorting savedSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        // the sun's view: only depth matters
        shadow.setClearColor(1f, 1f, 1f, 1f);
        shadow.clear(Minecraft.ON_OSX);
        shadow.bindWrite(true);
        RenderSystem.setProjectionMatrix(shadowProj, VertexSorting.ORTHOGRAPHIC_Z);
        modelView.identity().mul(shadowView);
        RenderSystem.applyModelViewMatrix();
        content.draw(pose, buffers, Pass.SHADOW);
        buffers.endBatch();

        // the camera's view, in the surfaces' own colours
        scene.setClearColor(0f, 0f, 0f, 0f);
        scene.clear(Minecraft.ON_OSX);
        scene.bindWrite(true);
        cameraMatrices(modelView);
        Lighting.setupLevel();
        content.draw(pose, buffers, Pass.SCENE);
        buffers.endBatch();
        Lighting.setupFor3DItems();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        composite();

        // light that needs no lighting, onto the lit picture, behind whatever stands in front of it
        hdr.bindWithDepth(scene.getDepthTextureId());
        cameraMatrices(modelView);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        content.draw(pose, buffers, Pass.GLOW);
        buffers.endBatch();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        hdr.bindWithDepth(0);

        bloomPyramid();
        grade();

        // back onto the screen, into the rectangle
        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.setProjectionMatrix(savedProj, savedSorting);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        present(g);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void allocate(int pw, int ph) {
        if (scene == null) scene = new TextureTarget(pw, ph, true, Minecraft.ON_OSX);
        else if (scene.width != pw || scene.height != ph) scene.resize(pw, ph, Minecraft.ON_OSX);
        if (shadow == null) shadow = new TextureTarget(shadowSize, shadowSize, true, Minecraft.ON_OSX);
        else if (shadow.width != shadowSize) shadow.resize(shadowSize, shadowSize, Minecraft.ON_OSX);
        hdr.resize(pw, ph);
        ldr.resize(pw, ph);
        int bw = pw, bh = ph;
        for (FloatTarget b : blooms) {
            bw = Math.max(1, bw / 2);
            bh = Math.max(1, bh / 2);
            b.resize(bw, bh);
        }
    }

    /** The camera's projection and model-view for the scene and glow passes. */
    private void cameraMatrices(Matrix4fStack modelView) {
        if (screen) {
            RenderSystem.setProjectionMatrix(proj, VertexSorting.ORTHOGRAPHIC_Z);
            modelView.set(guiView);
        } else {
            RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
            modelView.identity().mul(view);
        }
        RenderSystem.applyModelViewMatrix();
    }

    private void matrices(float aspect, float pixelsPerGui) {
        if (screen) {
            // the caller's own screen projection, narrowed to the rectangle so the rectangle fills the target
            Vector3f a = savedProj.transformProject(gx, gy, 0f, new Vector3f());
            Vector3f b = savedProj.transformProject(gx + gw, gy + gh, 0f, new Vector3f());
            proj.identity().scale(2f / (b.x - a.x), 2f / (a.y - b.y), 1f)
                    .translate(-(a.x + b.x) / 2f, -(a.y + b.y) / 2f, 0f).mul(savedProj);
            proj.mul(guiView, viewProj).mul(worldToGui);
            eye.set(screenEye);
            view.identity().rotateX((float) Math.toRadians(skyPitch)).rotateY((float) Math.toRadians(skyYaw));
            skyProj.setPerspective((float) Math.toRadians(70f), aspect, 0.05f, far).mul(view, skyInverse).invert();
            // an orthographic view: a block is the same size everywhere, this many pixels
            aoScale = worldToGui.getScale(scratch).x * pixelsPerGui * 0.5f;
        } else {
            // camera: orbit the target
            view.identity().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
            proj.translation(shiftX, shiftY, 0f).perspective((float) Math.toRadians(fov), aspect, 0.05f, far);
            proj.mul(view, skyInverse).invert();
            view.invert(turn);
            turn.transformDirection(scratch.set(0f, 0f, 1f));
            eye.set(target).fma(distance, scratch);
            view.translate(-eye.x, -eye.y, -eye.z);
            proj.mul(view, viewProj);
            aoScale = 0.5f * proj.m11() * 0.5f * gh * pixelsPerGui;
        }
        viewProj.invert(inverse);

        // the sun (the moon by night): an orthographic box around the shadow sphere, looking along the light
        light.set(sun);
        if (light.y < 0f) light.negate();
        from.set(shadowCenter).fma(shadowRadius * 2f, light);
        if (Math.abs(light.y) > 0.99f) up.set(0f, 0f, 1f); else up.set(0f, 1f, 0f);
        shadowView.setLookAt(from, shadowCenter, up);
        shadowProj.setOrtho(-shadowRadius, shadowRadius, -shadowRadius, shadowRadius, 0.05f, shadowRadius * 4f);
        shadowProj.mul(shadowView, shadowMat);
    }

    private void composite() {
        ShaderInstance s = SceneShaders.composite;
        hdr.bind();
        s.safeGetUniform("InvViewProj").set(inverse);
        s.safeGetUniform("SkyInvViewProj").set(skyInverse);
        s.safeGetUniform("AoScale").set(aoScale);
        s.safeGetUniform("Ortho").set(screen ? 1f : 0f);
        s.safeGetUniform("ShadowCenter").set(shadowCenter.x, shadowCenter.y, shadowCenter.z);
        s.safeGetUniform("ShadowMat").set(shadowMat);
        s.safeGetUniform("CameraPos").set(eye.x, eye.y, eye.z);
        s.safeGetUniform("SunDir").set(sun.x, sun.y, sun.z);
        s.safeGetUniform("SkyColor").set(skyColor.x, skyColor.y, skyColor.z);
        s.safeGetUniform("ScreenSize").set((float) hdr.width, (float) hdr.height);
        s.safeGetUniform("Time").set(time);
        s.safeGetUniform("SunSize").set(sunSize);
        s.safeGetUniform("CloudHeight").set(Float.isNaN(cloudHeight) ? -1e9f : cloudHeight);
        s.safeGetUniform("Fog").set(fogStart, fogEnd);
        s.safeGetUniform("ShadowInfo").set((float) shadowSize, shadowRadius * 4f);
        s.safeGetUniform("MoonPhase").set((float) moonPhase);
        RenderSystem.setShaderTexture(0, scene.getColorTextureId());
        RenderSystem.setShaderTexture(1, scene.getDepthTextureId());
        RenderSystem.setShaderTexture(2, shadow.getDepthTextureId());
        RenderSystem.setShaderTexture(3, NOISE);
        RenderSystem.setShaderTexture(4, sunTexture);
        RenderSystem.setShaderTexture(5, moonTexture);
        blit(s);
    }

    /** Down the mip chain with a 13-tap filter, then back up with a tent, adding as it goes. */
    private void bloomPyramid() {
        int src = hdr.texture();
        int sw = hdr.width, sh = hdr.height;
        ShaderInstance down = SceneShaders.bloomDown;
        for (FloatTarget b : blooms) {
            b.bind();
            down.safeGetUniform("TexelSize").set(1f / sw, 1f / sh);
            RenderSystem.setShaderTexture(0, src);
            blit(down);
            src = b.texture();
            sw = b.width;
            sh = b.height;
        }
        ShaderInstance up = SceneShaders.bloomUp;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        for (int i = BLOOM_LEVELS - 2; i >= 0; i--) {
            FloatTarget from = blooms[i + 1];
            blooms[i].bind();
            up.safeGetUniform("TexelSize").set(1f / from.width, 1f / from.height);
            RenderSystem.setShaderTexture(0, from.texture());
            blit(up);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Bloom in, the tonemap and grading: the picture as it will look, before antialiasing. */
    private void grade() {
        ShaderInstance s = SceneShaders.grade;
        ldr.bind();
        s.safeGetUniform("Bloom").set(bloom);
        for (int i = 0; i < MAX_FROST; i++) {
            if (i < frostCount) {
                // in the scene's texture coordinates, v up
                float fx = frosts[i * 4], fy = frosts[i * 4 + 1], fw = frosts[i * 4 + 2], fh = frosts[i * 4 + 3];
                s.safeGetUniform("Frost" + i).set((fx - gx) / gw, 1f - (fy + fh - gy) / gh, (fx + fw - gx) / gw, 1f - (fy - gy) / gh);
            } else {
                s.safeGetUniform("Frost" + i).set(0f, 0f, 0f, 0f);
            }
        }
        RenderSystem.setShaderTexture(0, hdr.texture());
        RenderSystem.setShaderTexture(1, blooms[0].texture());
        blit(s);
    }

    /** FXAA on the way onto the screen (and the downsample, when supersampled). */
    private void present(GuiGraphics g) {
        ShaderInstance s = SceneShaders.present;
        s.safeGetUniform("TexelSize").set(1f / ldr.width, 1f / ldr.height);
        RenderSystem.setShaderTexture(0, ldr.texture());
        RenderSystem.setShader(() -> s);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, gx, gy, 0f).setUv(0f, 1f);
        b.addVertex(m, gx, gy + gh, 0f).setUv(0f, 0f);
        b.addVertex(m, gx + gw, gy + gh, 0f).setUv(1f, 0f);
        b.addVertex(m, gx + gw, gy, 0f).setUv(1f, 1f);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** A full-target quad: the pass's vertex shader takes positions as they are. */
    private static void blit(ShaderInstance s) {
        RenderSystem.setShader(() -> s);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        b.addVertex(1f, -1f, 0f).setUv(1f, 0f);
        b.addVertex(1f, 1f, 0f).setUv(1f, 1f);
        b.addVertex(-1f, 1f, 0f).setUv(0f, 1f);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Where a world point lands on the GUI, as of the last render; false when it's behind the
     * camera. For labels drawn over the scene, like a name tag.
     */
    public boolean project(Vector3f world, Vector2f gui) {
        viewProj.transform(clip.set(world, 1f));
        if (clip.w <= 0f) return false;
        float nx = clip.x / clip.w, ny = clip.y / clip.w;
        gui.set(gx + (nx * 0.5f + 0.5f) * gw, gy + (0.5f - ny * 0.5f) * gh);
        return true;
    }

    /** The camera's position, as of the last render. */
    public Vector3f eye() { return eye; }

    @Override
    public void close() {
        if (scene != null) scene.destroyBuffers();
        if (shadow != null) shadow.destroyBuffers();
        scene = shadow = null;
        hdr.release();
        ldr.release();
        for (FloatTarget b : blooms) b.release();
    }
}
