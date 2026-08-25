package com.circulation.wailahud.hud;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class WorldRenderState {

    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private int framebuffer;
    private int matrixMode;

    public void capture() {
        viewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    }

    public void restore() {
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        GL11.glMatrixMode(matrixMode);
        GL11.glPopAttrib();
    }
}
