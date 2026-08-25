package com.circulation.wailahud.hud;

import com.circulation.wailahud.config.ModConfig;

public record HudSettings(int contentWidth, int maxLinesPerPage, int offsetX, int offsetY, float scale) {

    public static HudSettings fromConfig(ModConfig.Hud hud) {
        return new HudSettings(hud.contentWidth, hud.maxLinesPerPage, hud.offsetX, hud.offsetY, (float) hud.scale);
    }
}
