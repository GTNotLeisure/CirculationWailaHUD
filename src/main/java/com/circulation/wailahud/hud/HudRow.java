package com.circulation.wailahud.hud;

import java.util.List;

import com.circulation.wailahud.mixins.late.waila.AccessorTooltipRenderable;

public record HudRow(int y, int displayY, int height, int contentWidth, List<AccessorTooltipRenderable> primary,
    List<AccessorTooltipRenderable> secondary) {}
