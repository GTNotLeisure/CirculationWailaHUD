package com.circulation.wailahud.hud;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import com.circulation.wailahud.CirculationWailaHud;
import com.circulation.wailahud.hud.BlockPreviewResolver.Preview;
import com.circulation.wailahud.mixins.early.forge.AccessorForgeHooksClient;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;

public class WdmlaBlockFramebufferRenderer implements TargetPreviewRenderer {

    private static final float CAMERA_DISTANCE = 2.3F;
    private static final float CAMERA_YAW = 30.0F;
    private static final float INITIAL_CAMERA_PITCH = 30.0F;
    private static final float FIELD_OF_VIEW = 60.0F;
    private static final double MODEL_MARGIN = 1.1D;
    private static final int CLIENT_ATTRIBUTE_BITS = 0xFFFFFFFF;
    private static final int MAX_FRAMEBUFFER_DIMENSION = 4096;

    private final RenderBlocks renderBlocks = new RenderBlocks();
    private final WorldRenderState worldRenderState = new WorldRenderState();
    private final BlockPreviewResolver previewResolver = new BlockPreviewResolver();
    private CustomFramebuffer framebuffer;
    private int framebufferWidth;
    private int framebufferHeight;
    private float cameraPitch = INITIAL_CAMERA_PITCH;
    private long lastRotationTick;
    private String lastEmptyRenderTarget;
    private World cachedWorld;
    private Block cachedBlock;
    private int cachedBlockX;
    private int cachedBlockY;
    private int cachedBlockZ;
    private int cachedWidth;
    private int cachedHeight;
    private long cachedTick = Long.MIN_VALUE;

    @Override
    public boolean supports(MovingObjectPosition target) {
        return target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    @Override
    public int render(MovingObjectPosition target, int width, int height, long ticks, float partialTick) {
        return render(target, width, height, ticks);
    }

    public int render(MovingObjectPosition target, int width, int height, long ticks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || !supports(target)) return 0;

        Block block = minecraft.theWorld.getBlock(target.blockX, target.blockY, target.blockZ);
        if (block == null || block == Blocks.air) return 0;
        int framebufferWidth = normalizeFramebufferDimension(width);
        int framebufferHeight = normalizeFramebufferDimension(height);
        if (canReuseFramebuffer(minecraft.theWorld, block, target, framebufferWidth, framebufferHeight, ticks)) {
            return framebuffer.framebufferTexture;
        }
        Preview preview = previewResolver.resolve(minecraft.theWorld, target.blockX, target.blockY, target.blockZ);

        int savedWorldRenderPass = ForgeHooksClient.getWorldRenderPass();
        worldRenderState.capture();
        GL11.glPushClientAttrib(CLIENT_ATTRIBUTE_BITS);
        boolean projectionPushed = false;
        boolean modelViewPushed = false;
        try {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            ensureFramebuffer(framebufferWidth, framebufferHeight);
            if (framebuffer == null) return 0;
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            framebuffer.clearBindFramebuffer(true);
            setupCamera(preview.bounds(), ticks);
            projectionPushed = true;
            modelViewPushed = true;
            RenderResult result = drawWorld(minecraft, block, target, preview.tileEntity());
            logEmptyRender(target, block, result);
            cacheFramebuffer(minecraft.theWorld, block, target, framebufferWidth, framebufferHeight, ticks);
            return framebuffer.framebufferTexture;
        } finally {
            ForgeHooksClient.setRenderPass(-1);
            AccessorForgeHooksClient.circulationWailaHud$setWorldRenderPass(savedWorldRenderPass);
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

    @Override
    public void clear() {
        if (framebuffer != null) framebuffer.checkDeleteFramebuffer();
        framebuffer = null;
        framebufferWidth = 0;
        framebufferHeight = 0;
        cachedWorld = null;
        cachedBlock = null;
        cachedTick = Long.MIN_VALUE;
    }

    private void ensureFramebuffer(int requestedWidth, int requestedHeight) {
        int width = normalizeFramebufferDimension(requestedWidth);
        int height = normalizeFramebufferDimension(requestedHeight);
        if (framebuffer != null && framebufferWidth == width && framebufferHeight == height) return;

        clear();
        // The depth-texture path avoids the invalid renderbuffer target in GTNHLib 0.11.48.
        framebuffer = new CustomFramebuffer(width, height, CustomFramebuffer.DEPTH_TEXTURE);
        framebuffer.bindFramebuffer();
        framebuffer.checkFramebufferComplete();
        framebufferWidth = width;
        framebufferHeight = height;
    }

    private int normalizeFramebufferDimension(int requestedDimension) {
        return Math.min(MAX_FRAMEBUFFER_DIMENSION, Math.max(1, requestedDimension));
    }

    private boolean canReuseFramebuffer(World world, Block block, MovingObjectPosition target, int width, int height,
        long ticks) {
        return framebuffer != null && framebuffer.framebufferTexture != 0
            && cachedWorld == world
            && cachedBlock == block
            && cachedBlockX == target.blockX
            && cachedBlockY == target.blockY
            && cachedBlockZ == target.blockZ
            && cachedWidth == width
            && cachedHeight == height
            && cachedTick == ticks;
    }

    private void cacheFramebuffer(World world, Block block, MovingObjectPosition target, int width, int height,
        long ticks) {
        cachedWorld = world;
        cachedBlock = block;
        cachedBlockX = target.blockX;
        cachedBlockY = target.blockY;
        cachedBlockZ = target.blockZ;
        cachedWidth = width;
        cachedHeight = height;
        cachedTick = ticks;
    }

    private void setupCamera(AxisAlignedBB bounds, long ticks) {
        updateCameraRotation(ticks);
        float lookAtX = (float) ((bounds.minX + bounds.maxX) * 0.5D);
        float lookAtY = (float) ((bounds.minY + bounds.maxY) * 0.5D);
        float lookAtZ = (float) ((bounds.minZ + bounds.maxZ) * 0.5D);
        float cameraDistance = cameraDistance(bounds);
        float pitchRadians = (float) Math.toRadians(cameraPitch);
        float yawRadians = (float) Math.toRadians(CAMERA_YAW);
        float rawEyeX = (float) Math.cos(pitchRadians);
        float rawEyeY = (float) Math.tan(yawRadians);
        float rawEyeZ = (float) Math.sin(pitchRadians);
        float eyeLength = (float) Math.sqrt(rawEyeX * rawEyeX + rawEyeY * rawEyeY + rawEyeZ * rawEyeZ);
        float eyeX = lookAtX + rawEyeX / eyeLength * cameraDistance;
        float eyeY = lookAtY + rawEyeY / eyeLength * cameraDistance;
        float eyeZ = lookAtZ + rawEyeZ / eyeLength * cameraDistance;

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

    private float cameraDistance(AxisAlignedBB bounds) {
        double width = bounds.maxX - bounds.minX;
        double height = bounds.maxY - bounds.minY;
        double depth = bounds.maxZ - bounds.minZ;
        double radius = Math.sqrt(width * width + height * height + depth * depth) * 0.5D;
        double aspect = framebufferWidth / (double) framebufferHeight;
        double limitingHalfFov = Math.atan(Math.tan(Math.toRadians(FIELD_OF_VIEW * 0.5D)) * Math.min(1.0D, aspect));
        return (float) Math.max(CAMERA_DISTANCE, radius * MODEL_MARGIN / Math.sin(limitingHalfFov));
    }

    private void updateCameraRotation(long ticks) {
        cameraPitch += ticks - lastRotationTick;
        lastRotationTick = ticks;
    }

    private RenderResult drawWorld(Minecraft minecraft, Block block, MovingObjectPosition target,
        TileEntity tileEntity) {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        minecraft.entityRenderer.disableLightmap(0);
        minecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        boolean blockRendered = renderBlockPasses(minecraft, block, target.blockX, target.blockY, target.blockZ);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL11.GL_LIGHTING);
        boolean tileEntityRendered = renderTileEntityPasses(tileEntity);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        return new RenderResult(blockRendered, tileEntityRendered);
    }

    private boolean renderBlockPasses(Minecraft minecraft, Block block, int blockX, int blockY, int blockZ) {
        int savedAmbientOcclusion = minecraft.gameSettings.ambientOcclusion;
        boolean rendered = false;
        minecraft.gameSettings.ambientOcclusion = 0;
        try {
            for (int pass = 0; pass < 2; pass++) {
                rendered |= renderBlockPass(minecraft, block, blockX, blockY, blockZ, pass);
            }
        } finally {
            minecraft.gameSettings.ambientOcclusion = savedAmbientOcclusion;
        }
        return rendered;
    }

    private boolean renderBlockPass(Minecraft minecraft, Block block, int blockX, int blockY, int blockZ, int pass) {
        if (!block.canRenderInPass(pass)) return false;

        ForgeHooksClient.setRenderPass(pass);
        AccessorForgeHooksClient.circulationWailaHud$setWorldRenderPass(pass);
        setDefaultPassRenderState(pass);
        Tessellator tessellator = Tessellator.instance;
        boolean savedRenderAllFaces = renderBlocks.renderAllFaces;
        tessellator.startDrawingQuads();
        try {
            tessellator.setBrightness(15 << 20 | 15 << 4);
            renderBlocks.blockAccess = minecraft.theWorld;
            renderBlocks.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
            renderBlocks.renderAllFaces = true;
            return renderBlocks.renderBlockByRenderType(block, blockX, blockY, blockZ);
        } finally {
            renderBlocks.renderAllFaces = savedRenderAllFaces;
            tessellator.draw();
            tessellator.setTranslation(0.0D, 0.0D, 0.0D);
        }
    }

    private boolean renderTileEntityPasses(TileEntity tileEntity) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        if (tileEntity == null || !dispatcher.hasSpecialRenderer(tileEntity)) return false;

        boolean rendered = false;
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            AccessorForgeHooksClient.circulationWailaHud$setWorldRenderPass(pass);
            setDefaultPassRenderState(pass);
            if (tileEntity.shouldRenderInPass(pass)) {
                dispatcher
                    .renderTileEntityAt(tileEntity, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 0.0F);
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
        String targetKey = target.blockX + ","
            + target.blockY
            + ","
            + target.blockZ
            + ":"
            + block.getClass()
                .getName();
        if (targetKey.equals(lastEmptyRenderTarget)) return;
        lastEmptyRenderTarget = targetKey;
        CirculationWailaHud.LOG
            .warn("World HUD renderer produced no content for {} at {}", block.getUnlocalizedName(), targetKey);
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
