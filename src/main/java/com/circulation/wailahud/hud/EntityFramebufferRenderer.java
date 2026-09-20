package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.circulation.wailahud.CirculationWailaHud;
import com.circulation.wailahud.mixins.early.forge.AccessorForgeHooksClient;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;

public class EntityFramebufferRenderer implements TargetPreviewRenderer {

    private static final int MAX_FRAMEBUFFER_DIMENSION = 4096;
    private static final float CAMERA_PITCH = 15.0F;
    private static final double MODEL_MARGIN = 1.15D;

    private final WorldRenderState worldRenderState = new WorldRenderState();
    private CustomFramebuffer framebuffer;
    private EntityLivingBase failedEntity;

    @Override
    public boolean supports(MovingObjectPosition target) {
        return target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
            && target.entityHit instanceof EntityLivingBase;
    }

    @Override
    public int render(MovingObjectPosition target, int width, int height, long ticks, float partialTick) {
        if (!supports(target)) return 0;
        EntityLivingBase entity = (EntityLivingBase) target.entityHit;
        if (entity.isDead || entity.worldObj != Minecraft.getMinecraft().theWorld || entity == failedEntity) return 0;

        RenderManager manager = RenderManager.instance;
        Render renderer = manager.getEntityRenderObject(entity);
        if (renderer == null) return 0;
        EntityLivingBase savedLivingPlayer = manager.livingPlayer;
        float savedViewYaw = manager.playerViewY;
        float savedViewPitch = manager.playerViewX;
        float savedBrightnessX = OpenGlHelper.lastBrightnessX;
        float savedBrightnessY = OpenGlHelper.lastBrightnessY;
        int savedRenderPass = MinecraftForgeClient.getRenderPass();
        int savedWorldRenderPass = ForgeHooksClient.getWorldRenderPass();
        int projectionDepth = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        int modelViewDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        int textureDepth = 0;
        int lightmapDepth = 0;
        worldRenderState.capture();
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT | GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try {
            textureDepth = pushTextureMatrix(OpenGlHelper.defaultTexUnit);
            GL11.glLoadIdentity();
            lightmapDepth = pushTextureMatrix(OpenGlHelper.lightmapTexUnit);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            ensureFramebuffer(width, height);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            framebuffer.clearBindFramebuffer(true);
            float rotation = (ticks % 360L) + partialTick;
            setupCamera(entity, rotation);
            // Suppress the vanilla nameplate without changing the live entity.
            manager.livingPlayer = entity;
            manager.playerViewY = 180.0F - rotation;
            manager.playerViewX = CAMERA_PITCH;
            renderEntityPasses(renderer, entity, partialTick);
            failedEntity = null;
            return framebuffer.framebufferTexture;
        } catch (RuntimeException exception) {
            clear();
            failedEntity = entity;
            CirculationWailaHud.LOG.warn(
                "Unable to render HUD entity preview for {}",
                entity.getClass()
                    .getName(),
                exception);
            return 0;
        } finally {
            manager.livingPlayer = savedLivingPlayer;
            manager.playerViewY = savedViewYaw;
            manager.playerViewX = savedViewPitch;
            ForgeHooksClient.setRenderPass(savedRenderPass);
            AccessorForgeHooksClient.circulationWailaHud$setWorldRenderPass(savedWorldRenderPass);
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

    @Override
    public void clear() {
        if (framebuffer != null) framebuffer.checkDeleteFramebuffer();
        framebuffer = null;
        failedEntity = null;
    }

    private void ensureFramebuffer(int requestedWidth, int requestedHeight) {
        int width = Math.max(1, Math.min(MAX_FRAMEBUFFER_DIMENSION, requestedWidth));
        int height = Math.max(1, Math.min(MAX_FRAMEBUFFER_DIMENSION, requestedHeight));
        if (framebuffer != null && framebuffer.framebufferWidth == width && framebuffer.framebufferHeight == height)
            return;
        clear();
        framebuffer = new CustomFramebuffer(width, height, CustomFramebuffer.DEPTH_TEXTURE);
        framebuffer.bindFramebuffer();
        framebuffer.checkFramebufferComplete();
    }

    private void setupCamera(EntityLivingBase entity, float rotation) {
        double width = Math.max(0.1D, entity.boundingBox.maxX - entity.boundingBox.minX);
        double height = Math.max(0.1D, entity.boundingBox.maxY - entity.boundingBox.minY);
        double depth = Math.max(0.1D, entity.boundingBox.maxZ - entity.boundingBox.minZ);
        double radius = Math.sqrt(width * width + height * height + depth * depth) * 0.5D * MODEL_MARGIN;
        double aspect = framebuffer.framebufferWidth / (double) framebuffer.framebufferHeight;
        double halfHeight = radius / Math.min(1.0D, aspect);
        double centerY = (entity.boundingBox.minY + entity.boundingBox.maxY) * 0.5D - entity.posY;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-halfHeight * aspect, halfHeight * aspect, -halfHeight, halfHeight, -radius * 4.0D, radius * 4.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glRotatef(CAMERA_PITCH, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(0.0D, -centerY, 0.0D);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_NORMALIZE);
        RenderHelper.enableStandardItemLighting();
    }

    private void renderEntityPasses(Render renderer, EntityLivingBase entity, float partialTick) {
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTick;
        for (int pass = 0; pass < 2; pass++) {
            if (!entity.shouldRenderInPass(pass)) continue;
            ForgeHooksClient.setRenderPass(pass);
            AccessorForgeHooksClient.circulationWailaHud$setWorldRenderPass(pass);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(pass == 0);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            // Render only the model, excluding world shadows, fire, and debug hitboxes.
            renderer.doRender(entity, 0.0D, 0.0D, 0.0D, yaw, partialTick);
        }
    }

    private int pushTextureMatrix(int textureUnit) {
        OpenGlHelper.setActiveTexture(textureUnit);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        int depth = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        GL11.glPushMatrix();
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
}
