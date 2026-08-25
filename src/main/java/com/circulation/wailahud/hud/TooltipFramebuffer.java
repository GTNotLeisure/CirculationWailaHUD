package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.circulation.wailahud.ClientProxy;
import com.circulation.wailahud.theme.ChromaticStyle;
import com.circulation.wailahud.theme.ChromaticTexture;
import com.circulation.wailahud.theme.HudTheme;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;

import mcp.mobius.waila.overlay.DisplayUtil;

public class TooltipFramebuffer {

    private static final int RENDER_SCALE = 4;
    private final HudScrollAnimation scrollAnimation = new HudScrollAnimation();
    private final WailaRowRenderer rowRenderer = new WailaRowRenderer();
    private final WorldRenderState worldRenderState = new WorldRenderState();
    private CustomFramebuffer framebuffer;
    private int width;
    private int height;

    public boolean render(HudLayout layout, int pageIndex, TooltipSnapshot snapshot, HudTheme theme,
        HudSettings settings, long ticks, float partialTick) {
        worldRenderState.capture();
        boolean orthoPrepared = false;
        try {
            ensureFramebuffer(layout.width() * RENDER_SCALE, layout.height() * RENDER_SCALE);
            framebuffer.clearBindFramebuffer(true);
            framebuffer.setupOrtho();
            orthoPrepared = true;
            GL11.glScalef(RENDER_SCALE, RENDER_SCALE, 1.0F);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawDecorators(layout.width(), layout.height(), theme.chromaticStyle());
            drawRows(
                layout,
                layout.pages()
                    .get(pageIndex),
                theme,
                ticks,
                partialTick);
            drawTargetIcon(snapshot, theme, layout.height());
            drawNavigation(layout, pageIndex, theme);
            return true;
        } catch (RuntimeException exception) {
            return false;
        } finally {
            if (orthoPrepared) framebuffer.restoreOrtho();
            worldRenderState.restore();
        }
    }

    public int texture() {
        return framebuffer == null ? 0 : framebuffer.framebufferTexture;
    }

    public void clear() {
        if (framebuffer != null) framebuffer.checkDeleteFramebuffer();
        framebuffer = null;
        width = 0;
        height = 0;
    }

    private void ensureFramebuffer(int requestedWidth, int requestedHeight) {
        if (framebuffer != null && width == requestedWidth && height == requestedHeight) return;
        clear();
        width = requestedWidth;
        height = requestedHeight;
        framebuffer = new CustomFramebuffer(width, height, CustomFramebuffer.TEXTURE_LINEAR);
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
            HudRowViewport viewport = HudRowViewport.forRow(row, theme, viewportWidth);
            int rowY = theme.contentY() + row.displayY();
            int offset = Math.round(scrollAnimation.offset(row.contentWidth(), viewport.width(), ticks, partialTick));
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                viewport.x() * RENDER_SCALE,
                height - (rowY + 1 + row.height()) * RENDER_SCALE,
                viewport.width() * RENDER_SCALE,
                row.height() * RENDER_SCALE);
            try {
                rowRenderer.draw(row, secondPass, viewport.x() - offset, theme.contentY() + 1, page.accessor());
            } finally {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
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

    private void drawNavigation(HudLayout layout, int pageIndex, HudTheme theme) {
        if (layout.pages()
            .size() < 2) return;
        String key = Keyboard.getKeyName(ClientProxy.NEXT_PAGE.getKeyCode());
        String text = StatCollector.translateToLocalFormatted(
            "circulation_waila_hud.navigation.page",
            pageIndex + 1,
            layout.pages()
                .size(),
            key);
        int y = theme.contentY() + layout.pages()
            .get(pageIndex)
            .contentHeight() + theme.navigationGap();
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, theme.contentX(), y, 0xFFFFFF);
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
