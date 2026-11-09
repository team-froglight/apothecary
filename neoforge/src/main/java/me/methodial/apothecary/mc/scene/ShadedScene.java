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

    /** What the scene is: drawn in world coordinates on {@code pose}, into {@code buffers}. */
    public interface Content {
        /** {@code shadow} is true for the pass from the sun, where only shape matters. */
        void draw(PoseStack pose, MultiBufferSource.BufferSource buffers, boolean shadow);
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
    /** A frosted rectangle (GUI units), blurred behind a panel drawn over the scene; empty when w is 0. */
    private float frostX, frostY, frostW, frostH;

    /** Looks at {@code target} from {@code distance} away, turned {@code yaw} and tilted down {@code pitch} (degrees). */
    public ShadedScene camera(Vector3f target, float yaw, float pitch, float distance, float fov) {
        this.target.set(target);
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
        this.fov = fov;
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
        frostX = x;
        frostY = y;
        frostW = w;
        frostH = h;
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

    /** The cloud layer's height, in world y. */
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
    private final Matrix4f savedProj = new Matrix4f();
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
        matrices(w / h);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        buffers.endBatch();
        savedProj.set(RenderSystem.getProjectionMatrix());
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
        content.draw(pose, buffers, true);
        buffers.endBatch();

        // the camera's view, lit as the level is
        scene.setClearColor(0f, 0f, 0f, 0f);
        scene.clear(Minecraft.ON_OSX);
        scene.bindWrite(true);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
        modelView.identity().mul(view);
        RenderSystem.applyModelViewMatrix();
        Lighting.setupLevel();
        content.draw(pose, buffers, false);
        buffers.endBatch();
        Lighting.setupFor3DItems();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        composite();
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

    private void matrices(float aspect) {
        // camera: orbit the target
        view.identity().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
        view.invert(turn);
        turn.transformDirection(scratch.set(0f, 0f, 1f));
        eye.set(target).fma(distance, scratch);
        view.translate(-eye.x, -eye.y, -eye.z);
        proj.translation(shiftX, shiftY, 0f).perspective((float) Math.toRadians(fov), aspect, 0.05f, far);
        proj.mul(view, viewProj);
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
        s.safeGetUniform("ShadowMat").set(shadowMat);
        s.safeGetUniform("CameraPos").set(eye.x, eye.y, eye.z);
        s.safeGetUniform("SunDir").set(sun.x, sun.y, sun.z);
        s.safeGetUniform("SkyColor").set(skyColor.x, skyColor.y, skyColor.z);
        s.safeGetUniform("ScreenSize").set((float) hdr.width, (float) hdr.height);
        s.safeGetUniform("Time").set(time);
        s.safeGetUniform("SunSize").set(sunSize);
        s.safeGetUniform("CloudHeight").set(cloudHeight);
        s.safeGetUniform("Fog").set(fogStart, fogEnd);
        s.safeGetUniform("ShadowInfo").set((float) shadowSize, shadowRadius * 4f);
        s.safeGetUniform("ProjScale").set(proj.m11());
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
        if (frostW > 0f && frostH > 0f) {
            // in the scene's texture coordinates, v up
            float u0 = (frostX - gx) / gw, u1 = (frostX + frostW - gx) / gw;
            float v0 = 1f - (frostY + frostH - gy) / gh, v1 = 1f - (frostY - gy) / gh;
            s.safeGetUniform("Frost").set(u0, v0, u1, v1);
        } else {
            s.safeGetUniform("Frost").set(0f, 0f, 0f, 0f);
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
