package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.circulation.wailahud.theme.HudTheme;

public class HudNavigation {

    public static String text(int pageIndex, int pageCount, int keyCode) {
        String key = keyCode == 0 ? null : GameSettings.getKeyDisplayString(keyCode);
        if (key == null || key.isEmpty()) {
            key = StatCollector.translateToLocal("circulation_waila_hud.navigation.unbound");
        }
        return StatCollector
            .translateToLocalFormatted("circulation_waila_hud.navigation.page", pageIndex + 1, pageCount, key);
    }

    public static void draw(HudLayout layout, int pageIndex, HudTheme theme, HudFontMetrics fontMetrics, int keyCode) {
        if (layout.pages()
            .size() < 2) return;
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String text = text(
            pageIndex,
            layout.pages()
                .size(),
            keyCode);
        int contentWidth = layout.width() - theme.frameWidth() + theme.baseContentWidth();
        int textWidth = fontRenderer.getStringWidth(text) + 1;
        float scale = Math.min(1.0F, Math.max(1, contentWidth - 2) / (float) Math.max(1, textWidth));
        int rowHeight = fontMetrics.rowHeight(fontRenderer.FONT_HEIGHT) + theme.navigationGap();
        float textHeight = (fontRenderer.FONT_HEIGHT + fontMetrics.topPadding() + fontMetrics.bottomPadding()) * scale;
        float y = theme.contentY() + layout.pages()
            .get(pageIndex)
            .contentHeight() + (rowHeight - textHeight) / 2.0F + fontMetrics.topPadding() * scale;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(theme.contentX(), y, 0.0F);
            GL11.glScalef(scale, scale, 1.0F);
            fontRenderer.drawStringWithShadow(text, 0, 0, 0xFFFFFF);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
