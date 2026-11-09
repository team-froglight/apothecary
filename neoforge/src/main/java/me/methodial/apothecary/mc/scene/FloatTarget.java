package me.methodial.apothecary.mc.scene;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * A colour-only framebuffer in half floats (RGBA16F), for light that runs brighter than white
 * until the tonemap: vanilla's {@code RenderTarget} only does 8 bits. Linear filtering, clamped.
 */
final class FloatTarget {
    int width, height;
    private int fbo = -1, texture = -1;

    /** (Re)allocates at {@code w} x {@code h} if the size changed. */
    void resize(int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        if (fbo >= 0 && w == width && h == height) return;
        RenderSystem.assertOnRenderThread();
        release();
        width = w;
        height = h;
        texture = GlStateManager._genTexture();
        GlStateManager._bindTexture(texture);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, w, h, 0, GL11.GL_RGBA, GL11.GL_FLOAT, null);
        fbo = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        GlStateManager._bindTexture(0);
    }

    /** Draws into this target, over all of it. */
    void bind() {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager._viewport(0, 0, width, height);
    }

    /** Draws into this target over all of it, depth-testing against {@code depthTexture} (0 detaches it). */
    void bindWithDepth(int depthTexture) {
        bind();
        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
    }

    int texture() { return texture; }

    void release() {
        if (fbo >= 0) GlStateManager._glDeleteFramebuffers(fbo);
        if (texture >= 0) GlStateManager._deleteTexture(texture);
        fbo = texture = -1;
    }
}
