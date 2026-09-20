package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.circulation.wailahud.CirculationWailaHud;
import com.circulation.wailahud.ClientProxy;
import com.circulation.wailahud.theme.ChromaticStyle;
import com.circulation.wailahud.theme.ChromaticTexture;
import com.circulation.wailahud.theme.HudTheme;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;
import com.gtnewhorizons.angelica.glsm.hooks.GLSMConfig;

import mcp.mobius.waila.overlay.DisplayUtil;

public class TooltipFramebuffer {

    private static final int RENDER_SCALE = 4;
    private static final int MAX_FRAMEBUFFER_DIMENSION = 4096;
    private final HudScrollAnimation scrollAnimation = new HudScrollAnimation();
    private final WailaRowRenderer rowRenderer = new WailaRowRenderer();
    private final WorldRenderState worldRenderState = new WorldRenderState();
    private CustomFramebuffer framebuffer;
    private int width;
    private int height;
    private float renderScaleX;
    private float renderScaleY;
    private boolean renderFailureLogged;

    public boolean render(HudLayout layout, int pageIndex, TooltipSnapshot snapshot, HudTheme theme,
        HudSettings settings, HudFontMetrics fontMetrics, long ticks, float partialTick) {
        return renderToTexture(layout, layout.width() * RENDER_SCALE, layout.height() * RENDER_SCALE, () -> {
            drawDecorators(layout.width(), layout.height(), theme.chromaticStyle());
            drawRows(
                layout,
                layout.pages()
                    .get(pageIndex),
                theme,
                ticks,
                partialTick);
            drawTargetIcon(snapshot, theme, layout.height());
            HudNavigation.draw(layout, pageIndex, theme, fontMetrics, ClientProxy.NEXT_PAGE.getKeyCode());
        }) != 0;
    }

    /** Renders GUI callbacks to a premultiplied-alpha texture, or returns zero on failure. */
    public int renderSecondary(HudLayout layout, HudPage page, HudTheme theme, int textureWidth, int textureHeight,
        long ticks, float partialTick) {
        return renderToTexture(layout, textureWidth, textureHeight, () -> {
            int viewportWidth = layout.width() - theme.frameWidth() + theme.baseContentWidth();
            drawRowPass(page, theme, viewportWidth, ticks, partialTick, true);
        });
    }

    private int renderToTexture(HudLayout layout, int textureWidth, int textureHeight, Runnable draw) {
        if (layout.width() <= 0 || layout.height() <= 0) return 0;
        boolean savedAlphaOverride = GLSMConfig.hudCacheOverride;
        float savedBrightnessX = OpenGlHelper.lastBrightnessX;
        float savedBrightnessY = OpenGlHelper.lastBrightnessY;
        int projectionDepth = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        int modelViewDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        int textureDepth = 0;
        int lightmapDepth = 0;
        worldRenderState.capture();
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT | GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try {
            textureDepth = pushTextureMatrix(OpenGlHelper.defaultTexUnit);
            lightmapDepth = pushTextureMatrix(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearDepth(1.0D);
            ensureFramebuffer(textureWidth, textureHeight);
            framebuffer.clearBindFramebuffer(true);
            setupProjection(layout);
            renderScaleX = width / (float) layout.width();
            renderScaleY = height / (float) layout.height();
            GL11.glDepthRange(0.0D, 1.0D);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glEnable(GL11.GL_BLEND);
            // Preserve accumulated alpha when item renderers change their blend functions.
            GLSMConfig.hudCacheOverride = true;
            OpenGlHelper
                .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            draw.run();
            renderFailureLogged = false;
            return framebuffer.framebufferTexture;
        } catch (RuntimeException exception) {
            if (!renderFailureLogged) {
                CirculationWailaHud.LOG.warn("Unable to render HUD GUI layer", exception);
                renderFailureLogged = true;
            }
            return 0;
        } finally {
            GLSMConfig.hudCacheOverride = savedAlphaOverride;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, savedBrightnessX, savedBrightnessY);
            restoreMatrixStack(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH, modelViewDepth);
            restoreMatrixStack(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH, projectionDepth);
            if (textureDepth > 0) {
                OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                restoreMatrixStack(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH, textureDepth);
            }
            if (lightmapDepth > 0) {
                OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                restoreMatrixStack(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH, lightmapDepth);
            }
            GL11.glPopClientAttrib();
            worldRenderState.restore();
        }
    }

    /** Returns the premultiplied-alpha color texture, or zero if no framebuffer is allocated. */
    public int texture() {
        return framebuffer == null ? 0 : framebuffer.framebufferTexture;
    }

    public void clear() {
        if (framebuffer != null) framebuffer.checkDeleteFramebuffer();
        framebuffer = null;
        width = 0;
        height = 0;
        renderFailureLogged = false;
    }

    private void ensureFramebuffer(int requestedWidth, int requestedHeight) {
        requestedWidth = Math.max(1, Math.min(MAX_FRAMEBUFFER_DIMENSION, requestedWidth));
        requestedHeight = Math.max(1, Math.min(MAX_FRAMEBUFFER_DIMENSION, requestedHeight));
        if (framebuffer != null && width == requestedWidth && height == requestedHeight) return;
        if (framebuffer != null) framebuffer.checkDeleteFramebuffer();
        framebuffer = null;
        width = requestedWidth;
        height = requestedHeight;
        framebuffer = new CustomFramebuffer(
            width,
            height,
            CustomFramebuffer.DEPTH_TEXTURE | CustomFramebuffer.TEXTURE_LINEAR);
        try {
            framebuffer.bindFramebuffer();
            framebuffer.checkFramebufferComplete();
        } catch (RuntimeException exception) {
            framebuffer.checkDeleteFramebuffer();
            framebuffer = null;
            throw exception;
        }
    }

    private void setupProjection(HudLayout layout) {
        // Scale through the viewport so GUI item normals retain their original proportions.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, layout.width(), layout.height(), 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
    }

    private void drawRows(HudLayout layout, HudPage page, HudTheme theme, long ticks, float partialTick) {
        int viewportWidth = layout.width() - theme.frameWidth() + theme.baseContentWidth();
        for (boolean secondPass : new boolean[] { false, true }) {
            drawRowPass(page, theme, viewportWidth, ticks, partialTick, secondPass);
        }
    }

    private void drawRowPass(HudPage page, HudTheme theme, int viewportWidth, long ticks, float partialTick,
        boolean secondPass) {
        for (HudRow row : page.rows()) {
            if (secondPass ? row.secondary()
                .isEmpty()
                : row.primary()
                    .isEmpty())
                continue;
            HudRowViewport viewport = HudRowViewport.forRow(row, theme, viewportWidth);
            int rowY = theme.contentY() + row.displayY();
            int offset = Math.round(scrollAnimation.offset(row.contentWidth(), viewport.width(), ticks, partialTick));
            int left = Math.max(0, (int) Math.floor((viewport.x() - 1) * renderScaleX));
            int right = Math.min(width, (int) Math.ceil((viewport.x() + viewport.width()) * renderScaleX));
            int top = Math.max(0, Math.round(rowY * renderScaleY));
            int bottom = Math.min(height, Math.round((rowY + row.height()) * renderScaleY));
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(left, height - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                rowRenderer.draw(row, secondPass, viewport.x() - offset, theme.contentY(), page.accessor());
            } finally {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    private int pushTextureMatrix(int textureUnit) {
        OpenGlHelper.setActiveTexture(textureUnit);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        int depth = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        return depth;
    }

    private void restoreMatrixStack(int matrixMode, int depthParameter, int savedDepth) {
        GL11.glMatrixMode(matrixMode);
        int currentDepth = GL11.glGetInteger(depthParameter);
        while (currentDepth > savedDepth) {
            GL11.glPopMatrix();
            currentDepth--;
        }
    }

    private void drawTargetIcon(TooltipSnapshot snapshot, HudTheme theme, int layoutHeight) {
        if (!theme.chromaticStyle()
            .showTargetIcon() || !snapshot.hasIcon()) return;
        ItemStack stack = snapshot.stack();
        if (stack == null || stack.getItem() == null) return;
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        DisplayUtil.renderStack(theme.iconX() + 17, layoutHeight / 2 - 8, stack);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }

    private void drawDecorators(int width, int height, ChromaticStyle style) {
        int margin = style.borderMargin();
        if (style.backgroundColor() != 0)
            Gui.drawRect(margin, margin, width - margin, height - margin, style.backgroundColor());
        if (style.borderTopColor() != 0 || style.borderBottomColor() != 0) {
            Gui.drawRect(margin, margin, width - margin, margin + 1, style.borderTopColor());
            Gui.drawRect(margin, height - margin - 1, width - margin, height - margin, style.borderBottomColor());
            Gui.drawRect(margin, margin, margin + 1, height - margin, style.borderTopColor());
            Gui.drawRect(width - margin - 1, margin, width - margin, height - margin, style.borderBottomColor());
        }
        drawTexture(width, height, style.texture());
    }

    private void drawTexture(int width, int height, ChromaticTexture texture) {
        if (texture == null) return;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture.location());
        int textureWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int textureHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int sourceWidth = texture.width() == 0 ? textureWidth - texture.x() : texture.width();
        int sourceHeight = texture.height() == 0 ? textureHeight - texture.y() : texture.height();
        drawQuad(0, 0, width, height, texture.x(), texture.y(), sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    private void drawQuad(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight,
        int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return;
        float minU = u / (float) textureWidth;
        float maxU = (u + sourceWidth) / (float) textureWidth;
        float minV = v / (float) textureHeight;
        float maxV = (v + sourceHeight) / (float) textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, minU, maxV);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, maxU, maxV);
        tessellator.addVertexWithUV(x + width, y, 0.0D, maxU, minV);
        tessellator.addVertexWithUV(x, y, 0.0D, minU, minV);
        tessellator.draw();
    }
}
