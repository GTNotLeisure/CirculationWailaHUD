package com.circulation.wailahud.theme;

import net.minecraft.util.ResourceLocation;

public record HudTheme(ResourceLocation frameTexture, ResourceLocation crystalTexture, int frameWidth, int frameHeight,
    int contentX, int contentY, int baseContentWidth, int maximumContentWidth, int iconX, int iconY, int borderTop,
    int borderBottom, int bottomPadding, int navigationGap, ChromaticStyle chromaticStyle) {}
