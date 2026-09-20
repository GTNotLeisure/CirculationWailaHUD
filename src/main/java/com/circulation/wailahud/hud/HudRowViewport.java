package com.circulation.wailahud.hud;

import com.circulation.wailahud.theme.HudTheme;

public record HudRowViewport(int x, int width) {

    public static HudRowViewport forRow(HudRow row, HudTheme theme, int contentWidth) {
        return new HudRowViewport(theme.contentX(), contentWidth);
    }
}
