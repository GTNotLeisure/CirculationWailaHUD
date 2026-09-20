package com.circulation.wailahud.hud;

import net.minecraft.util.MovingObjectPosition;

public interface TargetPreviewRenderer {

    boolean supports(MovingObjectPosition target);

    int render(MovingObjectPosition target, int width, int height, long ticks, float partialTick);

    void clear();
}
