package com.circulation.wailahud.hud;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.circulation.wailahud.ClientProxy;
import com.circulation.wailahud.theme.ChromaticStyle;
import com.circulation.wailahud.theme.HudTheme;

public class WorldHudRenderer {

    private static final int FRAME_TEXTURE_WIDTH = 165;
    private static final int FRAME_TEXTURE_HEIGHT = 93;
    private static final int LEFT_FRAME_WIDTH = 82;
    private static final int PANEL_SOURCE_WIDTH = 74;
    private static final int PANEL_EDGE_WIDTH = 5;
    private static final int RIGHT_FRAME_CAP_WIDTH = 9;
    private static final int PANEL_TOP_HEIGHT = 10;
    private static final int PANEL_ROW_HEIGHT = 14;
    private static final int PANEL_FIRST_ROW_SOURCE_Y = 10;
    private static final int PANEL_REPEATED_ROW_SOURCE_Y = 24;
    private static final int PANEL_LAST_ROW_SOURCE_Y = 66;
    private static final int PANEL_BOTTOM_SOURCE_Y = 80;
    private static final int PANEL_BOTTOM_HEIGHT = 13;
    private static final int LEFT_TOP_HEIGHT = 9;
    private static final int LEFT_MIDDLE_HEIGHT = 72;
    private static final int LEFT_BOTTOM_HEIGHT = 12;
    private static final int LEFT_BOTTOM_SOURCE_Y = 81;
    private static final int LEFT_FILL_COLOR = 0xBF1D212F;
    private static final int LEFT_FILL_TRANSITION_COLOR = 0x801C222E;
    private static final float ICON_ANCHOR_SIZE = 16.0F;
    private static final float MODEL_VIEWPORT_SIZE = 48.0F;
    private static final int MODEL_RESOLUTION_STEP = 16;
    private static final int MAX_MODEL_RENDER_RESOLUTION = 4096;

    private final HudScrollAnimation scrollAnimation = new HudScrollAnimation();
    private final WailaRowRenderer rowRenderer = new WailaRowRenderer();
    private final WdmlaBlockFramebufferRenderer blockRenderer = new WdmlaBlockFramebufferRenderer();
    private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
    private final float[] modelView = new float[16];
    private final float[] projection = new float[16];
    private final int[] viewport = new int[4];
    private final float[] projectedCorners = new float[8];
    private final int[] scissorBox = new int[4];

    public void render(HudLayout layout, HudPage page, HudTheme theme, HudSettings settings,
        MovingObjectPosition target, float partialTick, long ticks) {
        if (target.hitVec == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.renderViewEntity;
        if (camera == null) return;
        double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTick;
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTick;
        double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTick;
        double hitX = target.hitVec.xCoord - cameraX;
        double hitY = target.hitVec.yCoord - cameraY;
        double hitZ = target.hitVec.zCoord - cameraZ;
        double distance = Math.sqrt(hitX * hitX + hitY * hitY + hitZ * hitZ);
        double displayDistance = Math.max(2.0D, distance - 0.3D);
        double pull = distance > 1.0E-6D ? displayDistance / distance : 1.0D;
        float distanceScale = 0.006F + (float) Math.min(distance / 6.0D, 1.0D) * 0.004F;
        float scale = distanceScale * settings.scale();

        GL11.glPushAttrib(
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_ENABLE_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(hitX * pull, hitY * pull, hitZ * pull);
            GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-10.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(-scale, -scale, scale);
            GL11.glTranslatef(settings.offsetX(), settings.offsetY(), 0.0F);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float x = 5.0F;
            float y = -layout.height() / 2.0F;
            drawFrame(theme.frameTexture(), x, y, layout.width(), layout.height(), 0.85F);
            float crystalY = y + (layout.height() - theme.frameHeight()) / 2.0F;
            drawCrystal(theme, x + theme.iconX(), crystalY + theme.iconY(), ticks + partialTick);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(x, y, 0.0F);
                drawDecorators(layout.width(), layout.height(), theme.chromaticStyle());
                captureProjection();
                drawRows(layout, page, theme, ticks, partialTick);
                drawTargetIcon(theme, layout.height(), target, ticks);
                drawNavigation(layout, page, theme);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void drawCrystal(HudTheme theme, float x, float y, float ticks) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(theme.crystalTexture());
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x + 25.0F, y + 25.0F, 0.01F);
            GL11.glRotatef(ticks * 0.9F, 0.0F, 0.0F, 1.0F);
            drawTexturedQuad(-25.0F, -25.0F, 50.0F, 50.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void drawFrame(ResourceLocation texture, float x, float y, int width, int height, float alpha) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        drawLeftFrame(x, y, height, alpha);
        int panelWidth = Math.max(0, width - LEFT_FRAME_WIDTH);
        drawRightFrame(x + LEFT_FRAME_WIDTH, y, panelWidth, height, alpha);
    }

    private void drawLeftFrame(float x, float y, int height, float alpha) {
        if (height == FRAME_TEXTURE_HEIGHT) {
            drawTexturedQuad(
                x,
                y,
                LEFT_FRAME_WIDTH,
                FRAME_TEXTURE_HEIGHT,
                0.0F,
                0.0F,
                LEFT_FRAME_WIDTH / (float) FRAME_TEXTURE_WIDTH,
                1.0F,
                alpha);
            return;
        }
        drawTexturedQuad(
            x,
            y,
            LEFT_FRAME_WIDTH,
            LEFT_TOP_HEIGHT,
            0.0F,
            0.0F,
            LEFT_FRAME_WIDTH / (float) FRAME_TEXTURE_WIDTH,
            LEFT_TOP_HEIGHT / (float) FRAME_TEXTURE_HEIGHT,
            alpha);
        int extraHeight = Math.max(0, height - LEFT_TOP_HEIGHT - LEFT_MIDDLE_HEIGHT - LEFT_BOTTOM_HEIGHT);
        int topFillHeight = extraHeight / 2;
        int bottomFillHeight = extraHeight - topFillHeight;
        int middleY = Math.round(y) + LEFT_TOP_HEIGHT + topFillHeight;
        if (topFillHeight > 0) {
            drawLeftFillBand(x, Math.round(y) + LEFT_TOP_HEIGHT, topFillHeight, alpha);
        }
        drawTexturedQuad(
            x,
            middleY,
            LEFT_FRAME_WIDTH,
            LEFT_MIDDLE_HEIGHT,
            0.0F,
            LEFT_TOP_HEIGHT / (float) FRAME_TEXTURE_HEIGHT,
            LEFT_FRAME_WIDTH / (float) FRAME_TEXTURE_WIDTH,
            LEFT_BOTTOM_SOURCE_Y / (float) FRAME_TEXTURE_HEIGHT,
            alpha);
        if (bottomFillHeight > 0) {
            int bottomFillY = middleY + LEFT_MIDDLE_HEIGHT;
            drawLeftFillBand(x, bottomFillY, bottomFillHeight, alpha);
        }
        drawTexturedQuad(
            x,
            y + height - LEFT_BOTTOM_HEIGHT,
            LEFT_FRAME_WIDTH,
            LEFT_BOTTOM_HEIGHT,
            0.0F,
            LEFT_BOTTOM_SOURCE_Y / (float) FRAME_TEXTURE_HEIGHT,
            LEFT_FRAME_WIDTH / (float) FRAME_TEXTURE_WIDTH,
            1.0F,
            alpha);
    }

    private void drawLeftFillBand(float x, int y, int height, float alpha) {
        drawTexturedQuad(
            x,
            y,
            6.0F,
            height,
            0.0F,
            20.0F / FRAME_TEXTURE_HEIGHT,
            6.0F / FRAME_TEXTURE_WIDTH,
            21.0F / FRAME_TEXTURE_HEIGHT,
            alpha);
        Gui.drawRect(
            Math.round(x) + 6,
            y,
            Math.round(x) + 8,
            y + height,
            multiplyAlpha(LEFT_FILL_TRANSITION_COLOR, alpha));
        Gui.drawRect(
            Math.round(x) + 8,
            y,
            Math.round(x) + LEFT_FRAME_WIDTH,
            y + height,
            multiplyAlpha(LEFT_FILL_COLOR, alpha));
    }

    private void drawRightFrame(float x, float y, int panelWidth, int height, float alpha) {
        if (height == FRAME_TEXTURE_HEIGHT) {
            drawPanelBand(x, y, panelWidth, 0, FRAME_TEXTURE_HEIGHT, FRAME_TEXTURE_HEIGHT, alpha);
            return;
        }
        drawPanelBand(x, y, panelWidth, 0, PANEL_TOP_HEIGHT, PANEL_TOP_HEIGHT, alpha);
        int bodyY = PANEL_TOP_HEIGHT;
        drawPanelBand(x, y + bodyY, panelWidth, PANEL_FIRST_ROW_SOURCE_Y, PANEL_ROW_HEIGHT, PANEL_ROW_HEIGHT, alpha);
        bodyY += PANEL_ROW_HEIGHT;
        int repeatedHeight = height - PANEL_TOP_HEIGHT - PANEL_ROW_HEIGHT * 2 - PANEL_BOTTOM_HEIGHT;
        while (repeatedHeight > 0) {
            int segmentHeight = Math.min(PANEL_ROW_HEIGHT, repeatedHeight);
            drawPanelBand(x, y + bodyY, panelWidth, PANEL_REPEATED_ROW_SOURCE_Y, segmentHeight, segmentHeight, alpha);
            bodyY += segmentHeight;
            repeatedHeight -= segmentHeight;
        }
        drawPanelBand(x, y + bodyY, panelWidth, PANEL_LAST_ROW_SOURCE_Y, PANEL_ROW_HEIGHT, PANEL_ROW_HEIGHT, alpha);
        drawPanelBand(
            x,
            y + height - PANEL_BOTTOM_HEIGHT,
            panelWidth,
            PANEL_BOTTOM_SOURCE_Y,
            PANEL_BOTTOM_HEIGHT,
            PANEL_BOTTOM_HEIGHT,
            alpha);
    }

    private void drawPanelBand(float x, float y, int width, int sourceY, int sourceHeight, int destinationHeight,
        float alpha) {
        int panelCenterWidth = Math.max(0, width - PANEL_EDGE_WIDTH * 2 - RIGHT_FRAME_CAP_WIDTH);
        int panelCenterSourceWidth = PANEL_SOURCE_WIDTH - PANEL_EDGE_WIDTH * 2;
        int panelSourceX = LEFT_FRAME_WIDTH;
        int panelRightEdgeX = panelSourceX + PANEL_SOURCE_WIDTH - PANEL_EDGE_WIDTH;
        int capSourceX = panelSourceX + PANEL_SOURCE_WIDTH;
        float maxV = (sourceY + sourceHeight) / (float) FRAME_TEXTURE_HEIGHT;
        float minV = sourceY / (float) FRAME_TEXTURE_HEIGHT;
        drawTexturedQuad(
            x,
            y,
            PANEL_EDGE_WIDTH,
            destinationHeight,
            panelSourceX / (float) FRAME_TEXTURE_WIDTH,
            minV,
            (panelSourceX + PANEL_EDGE_WIDTH) / (float) FRAME_TEXTURE_WIDTH,
            maxV,
            alpha);
        drawTexturedQuad(
            x + PANEL_EDGE_WIDTH,
            y,
            panelCenterWidth,
            destinationHeight,
            (panelSourceX + PANEL_EDGE_WIDTH) / (float) FRAME_TEXTURE_WIDTH,
            minV,
            (panelSourceX + PANEL_EDGE_WIDTH + panelCenterSourceWidth) / (float) FRAME_TEXTURE_WIDTH,
            maxV,
            alpha);
        drawTexturedQuad(
            x + PANEL_EDGE_WIDTH + panelCenterWidth,
            y,
            PANEL_EDGE_WIDTH,
            destinationHeight,
            panelRightEdgeX / (float) FRAME_TEXTURE_WIDTH,
            minV,
            (panelRightEdgeX + PANEL_EDGE_WIDTH) / (float) FRAME_TEXTURE_WIDTH,
            maxV,
            alpha);
        drawTexturedQuad(
            x + PANEL_EDGE_WIDTH * 2 + panelCenterWidth,
            y,
            RIGHT_FRAME_CAP_WIDTH,
            destinationHeight,
            capSourceX / (float) FRAME_TEXTURE_WIDTH,
            minV,
            1.0F,
            maxV,
            alpha);
    }

    private void drawRows(HudLayout layout, HudPage page, HudTheme theme, long ticks, float partialTick) {
        int viewportWidth = layout.width() - theme.frameWidth() + theme.baseContentWidth();
        for (boolean secondPass : new boolean[] { false, true }) {
            for (HudRow row : page.rows()) {
                HudRowViewport viewport = HudRowViewport.forRow(row, theme, viewportWidth);
                int x = viewport.x();
                int y = theme.contentY() + 1 + row.displayY();
                int offset = Math
                    .round(scrollAnimation.offset(row.contentWidth(), viewport.width(), ticks, partialTick));
                enableScissor(x - 1.0F, y, viewport.width() + 1, row.height());
                try {
                    rowRenderer.draw(row, secondPass, x - offset, theme.contentY() + 1, page.accessor());
                } finally {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
            }
        }
    }

    private void drawTargetIcon(HudTheme theme, int height, MovingObjectPosition target, long ticks) {
        if (!theme.chromaticStyle()
            .showTargetIcon()) return;
        float size = MODEL_VIEWPORT_SIZE;
        float x = theme.iconX() + 17.0F - (size - ICON_ANCHOR_SIZE) / 2.0F;
        float y = height / 2.0F - 8.0F - (size - ICON_ANCHOR_SIZE) / 2.0F;
        projectIcon(x, y, size);
        int texture = blockRenderer.render(target, projectedIconWidth(), projectedIconHeight(), ticks);
        if (texture == 0) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        drawTexturedQuad(x, y, size, size, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
    }

    public void clear() {
        blockRenderer.clear();
    }

    private int projectedIconWidth() {
        return boundedProjectedExtent(0, viewport[2]);
    }

    private int projectedIconHeight() {
        return boundedProjectedExtent(1, viewport[3]);
    }

    private int boundedProjectedExtent(int coordinateOffset, int viewportExtent) {
        float first = projectedCorners[coordinateOffset];
        float second = projectedCorners[coordinateOffset + 2];
        float third = projectedCorners[coordinateOffset + 4];
        float fourth = projectedCorners[coordinateOffset + 6];
        if (!isFinite(first) || !isFinite(second) || !isFinite(third) || !isFinite(fourth)) {
            return Math.round(MODEL_VIEWPORT_SIZE);
        }
        float minimum = Math.min(Math.min(first, second), Math.min(third, fourth));
        float maximum = Math.max(Math.max(first, second), Math.max(third, fourth));
        float extent = maximum - minimum;
        if (!isFinite(extent)) return Math.round(MODEL_VIEWPORT_SIZE);
        int minimumResolution = Math.round(MODEL_VIEWPORT_SIZE);
        int maximumResolution = Math.max(minimumResolution, Math.min(MAX_MODEL_RENDER_RESOLUTION, viewportExtent));
        int requestedResolution = Math.min(maximumResolution, Math.max(minimumResolution, Math.round(extent)));
        return Math.min(maximumResolution, roundUpToResolutionStep(requestedResolution));
    }

    private boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private int roundUpToResolutionStep(int value) {
        int remainder = value % MODEL_RESOLUTION_STEP;
        return remainder == 0 ? value : value + MODEL_RESOLUTION_STEP - remainder;
    }

    private void projectIcon(float x, float y, float size) {
        project(x, y, 0);
        project(x + size, y, 2);
        project(x + size, y + size, 4);
        project(x, y + size, 6);
    }

    private void drawNavigation(HudLayout layout, HudPage page, HudTheme theme) {
        if (layout.pages()
            .size() < 2) return;
        String text = StatCollector.translateToLocalFormatted(
            "circulation_waila_hud.navigation.page",
            1,
            layout.pages()
                .size(),
            ClientProxy.NEXT_PAGE.getKeyDescription());
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
            text,
            theme.contentX(),
            theme.contentY() + page.contentHeight() + theme.navigationGap() + 1,
            0xFFFFFF);
    }

    private void drawDecorators(int width, int height, ChromaticStyle style) {
        int margin = style.borderMargin();
        if (style.borderTopColor() == 0 && style.borderBottomColor() == 0) return;
        Gui.drawRect(margin, margin, width - margin, margin + 1, style.borderTopColor());
        Gui.drawRect(margin, height - margin - 1, width - margin, height - margin, style.borderBottomColor());
        Gui.drawRect(margin, margin, margin + 1, height - margin, style.borderTopColor());
        Gui.drawRect(width - margin - 1, margin, width - margin, height - margin, style.borderBottomColor());
    }

    private void captureProjection() {
        modelViewBuffer.clear();
        projectionBuffer.clear();
        viewportBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
        modelViewBuffer.get(modelView);
        projectionBuffer.get(projection);
        viewportBuffer.get(viewport);
    }

    private void enableScissor(float x, float y, int width, int height) {
        project(x, y, 0);
        project(x + width, y, 2);
        project(x + width, y + height, 4);
        project(x, y + height, 6);
        float minX = Math.min(
            Math.min(projectedCorners[0], projectedCorners[2]),
            Math.min(projectedCorners[4], projectedCorners[6]));
        float minY = Math.min(
            Math.min(projectedCorners[1], projectedCorners[3]),
            Math.min(projectedCorners[5], projectedCorners[7]));
        float maxX = Math.max(
            Math.max(projectedCorners[0], projectedCorners[2]),
            Math.max(projectedCorners[4], projectedCorners[6]));
        float maxY = Math.max(
            Math.max(projectedCorners[1], projectedCorners[3]),
            Math.max(projectedCorners[5], projectedCorners[7]));
        scissorBox[0] = Math.round(minX);
        scissorBox[1] = Math.round(minY);
        scissorBox[2] = Math.max(1, Math.round(maxX - minX));
        scissorBox[3] = Math.max(1, Math.round(maxY - minY));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
    }

    private void project(float x, float y, int offset) {
        float eyeX = modelView[0] * x + modelView[4] * y + modelView[12];
        float eyeY = modelView[1] * x + modelView[5] * y + modelView[13];
        float eyeZ = modelView[2] * x + modelView[6] * y + modelView[14];
        float eyeW = modelView[3] * x + modelView[7] * y + modelView[15];
        float clipX = projection[0] * eyeX + projection[4] * eyeY + projection[8] * eyeZ + projection[12] * eyeW;
        float clipY = projection[1] * eyeX + projection[5] * eyeY + projection[9] * eyeZ + projection[13] * eyeW;
        float clipW = projection[3] * eyeX + projection[7] * eyeY + projection[11] * eyeZ + projection[15] * eyeW;
        projectedCorners[offset] = viewport[0] + viewport[2] * (clipX / clipW + 1.0F) / 2.0F;
        projectedCorners[offset + 1] = viewport[1] + viewport[3] * (clipY / clipW + 1.0F) / 2.0F;
    }

    private void drawTexturedQuad(float x, float y, float width, float height, float minU, float minV, float maxU,
        float maxV, float alpha) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha);
        tessellator.addVertexWithUV(x, y + height, 0.0D, minU, maxV);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, maxU, maxV);
        tessellator.addVertexWithUV(x + width, y, 0.0D, maxU, minV);
        tessellator.addVertexWithUV(x, y, 0.0D, minU, minV);
        tessellator.draw();
    }

    private int multiplyAlpha(int color, float alpha) {
        int sourceAlpha = color >>> 24;
        int multipliedAlpha = Math.round(sourceAlpha * alpha);
        return color & 0x00FFFFFF | multipliedAlpha << 24;
    }
}
