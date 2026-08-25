package com.circulation.wailahud.hud;

import java.util.List;

import com.circulation.wailahud.mixins.late.waila.TooltipRenderableAccessor;

public record HudRow(int y, int displayY, int height, int contentWidth, List<TooltipRenderableAccessor> primary,
    List<TooltipRenderableAccessor> secondary) {}
