package com.circulation.wailahud.hud;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import com.circulation.wailahud.CirculationWailaHud;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;

public class WdmlaBlockFramebufferRenderer {

    private static final float CAMERA_DISTANCE = 2.3F;
    private static final float CAMERA_YAW = 30.0F;
    private static final float INITIAL_CAMERA_PITCH = 30.0F;
    private static final float FIELD_OF_VIEW = 60.0F;
    private static final int CLIENT_ATTRIBUTE_BITS = 0xFFFFFFFF;

    private final RenderBlocks renderBlocks = new RenderBlocks();
    private final WorldRenderState worldRenderState = new WorldRenderState();
    private CustomFramebuffer framebuffer;
    private int framebufferWidth;
    private int framebufferHeight;
    private float cameraPitch = INITIAL_CAMERA_PITCH;
    private long lastRotationTick;
    private String lastEmptyRenderTarget;

    public int render(MovingObjectPosition target, int width, int height, long ticks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return 0;

        Block block = minecraft.theWorld.getBlock(target.blockX, target.blockY, target.blockZ);
        if (block == null || block == Blocks.air) return 0;

        worldRenderState.capture();
        GL11.glPushClientAttrib(CLIENT_ATTRIBUTE_BITS);
        boolean projectionPushed = false;
        boolean modelViewPushed = false;
        try {
            ensureFramebuffer(width, height);
            if (framebuffer == null) return 0;
            framebuffer.clearBindFramebuffer(true);
            setupCamera(target, ticks);
            projectionPushed = true;
            modelViewPushed = true;
            RenderResult result = drawWorld(minecraft, block, target.blockX, target.blockY, target.blockZ);
            logEmptyRender(target, block, result);
            return framebuffer.framebufferTexture;
        } finally {
            ForgeHooksClient.setRenderPass(-1);
            if (modelViewPushed) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
            if (projectionPushed) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            }
            GL11.glPopClientAttrib();
            worldRenderState.restore();
        }
    }

    public void clear() {
        if (framebuffer != null) framebuffer.checkDeleteFramebuffer();
        framebuffer = null;
        framebufferWidth = 0;
        framebufferHeight = 0;
    }

    private void ensureFramebuffer(int requestedWidth, int requestedHeight) {
        int width = Math.max(1, requestedWidth);
        int height = Math.max(1, requestedHeight);
        if (framebuffer != null && framebufferWidth == width && framebufferHeight == height) return;

        clear();
        framebuffer = new CustomFramebuffer(width, height, CustomFramebuffer.DEPTH_ENABLED);
        framebufferWidth = width;
        framebufferHeight = height;
    }

    private void setupCamera(MovingObjectPosition target, long ticks) {
        updateCameraRotation(ticks);
        float lookAtX = target.blockX + 0.5F;
        float lookAtY = target.blockY + 0.5F;
        float lookAtZ = target.blockZ + 0.5F;
        float pitchRadians = (float) Math.toRadians(cameraPitch);
        float yawRadians = (float) Math.toRadians(CAMERA_YAW);
        float rawEyeX = (float) Math.cos(pitchRadians);
        float rawEyeY = (float) Math.tan(yawRadians);
        float rawEyeZ = (float) Math.sin(pitchRadians);
        float eyeLength = (float) Math.sqrt(rawEyeX * rawEyeX + rawEyeY * rawEyeY + rawEyeZ * rawEyeZ);
        float eyeX = lookAtX + rawEyeX / eyeLength * CAMERA_DISTANCE;
        float eyeY = lookAtY + rawEyeY / eyeLength * CAMERA_DISTANCE;
        float eyeZ = lookAtZ + rawEyeZ / eyeLength * CAMERA_DISTANCE;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluPerspective(FIELD_OF_VIEW, framebufferWidth / (float) framebufferHeight, 0.1F, 10000.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluLookAt(eyeX, eyeY, eyeZ, lookAtX, lookAtY, lookAtZ, 0.0F, 1.0F, 0.0F);
    }

    private void updateCameraRotation(long ticks) {
        cameraPitch += ticks - lastRotationTick;
        lastRotationTick = ticks;
    }

    private RenderResult drawWorld(Minecraft minecraft, Block block, int blockX, int blockY, int blockZ) {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        minecraft.entityRenderer.disableLightmap(0);
        minecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        boolean blockRendered = renderBlockPasses(minecraft, block, blockX, blockY, blockZ);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL11.GL_LIGHTING);
        boolean tileEntityRendered = renderTileEntityPasses(minecraft, blockX, blockY, blockZ);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        return new RenderResult(blockRendered, tileEntityRendered);
    }

    private boolean renderBlockPasses(Minecraft minecraft, Block block, int blockX, int blockY, int blockZ) {
        int savedAmbientOcclusion = minecraft.gameSettings.ambientOcclusion;
        boolean savedRenderAllFaces = renderBlocks.renderAllFaces;
        Tessellator tessellator = Tessellator.instance;
        boolean rendered = false;
        minecraft.gameSettings.ambientOcclusion = 0;
        tessellator.startDrawingQuads();
        try {
            tessellator.setBrightness(15 << 20 | 15 << 4);
            for (int pass = 0; pass < 2; pass++) {
                if (!block.canRenderInPass(pass)) continue;
                ForgeHooksClient.setRenderPass(pass);
                renderBlocks.blockAccess = minecraft.theWorld;
                renderBlocks.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                renderBlocks.renderAllFaces = true;
                rendered |= renderBlocks.renderBlockByRenderType(block, blockX, blockY, blockZ);
            }
        } finally {
            minecraft.gameSettings.ambientOcclusion = savedAmbientOcclusion;
            renderBlocks.renderAllFaces = savedRenderAllFaces;
            tessellator.draw();
            tessellator.setTranslation(0.0D, 0.0D, 0.0D);
        }
        return rendered;
    }

    private boolean renderTileEntityPasses(Minecraft minecraft, int blockX, int blockY, int blockZ) {
        TileEntity tileEntity = minecraft.theWorld.getTileEntity(blockX, blockY, blockZ);
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        if (tileEntity == null || !dispatcher.hasSpecialRenderer(tileEntity)) return false;

        boolean rendered = false;
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            setDefaultPassRenderState(pass);
            if (tileEntity.shouldRenderInPass(pass)) {
                dispatcher.renderTileEntityAt(tileEntity, blockX, blockY, blockZ, 0.0F);
                rendered = true;
            }
        }
        return rendered;
    }

    private void logEmptyRender(MovingObjectPosition target, Block block, RenderResult result) {
        if (result.blockRendered() || result.tileEntityRendered()) {
            lastEmptyRenderTarget = null;
            return;
        }
        String targetKey = target.blockX + "," + target.blockY + "," + target.blockZ + ":" + block.getClass()
            .getName();
        if (targetKey.equals(lastEmptyRenderTarget)) return;
        lastEmptyRenderTarget = targetKey;
        CirculationWailaHud.LOG.warn("World HUD renderer produced no content for {} at {}", block.getUnlocalizedName(), targetKey);
    }

    private void setDefaultPassRenderState(int pass) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (pass == 0) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            return;
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
    }

    private record RenderResult(boolean blockRendered, boolean tileEntityRendered) {}
}
