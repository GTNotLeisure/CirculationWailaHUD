package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.circulation.wailahud.integration.AngelicaFontSupport;
import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;

public record HudFontMetrics(int topPadding, int bottomPadding) {

    private static final int MINIMUM_LINE_HEIGHT = 14;
    private static final int LINE_PADDING = 2;

    public static HudFontMetrics current() {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        float verticalScale = 1.0F;
        float shadowOffset = 1.0F;
        if (fontRenderer instanceof IFontParameters fontParameters) {
            verticalScale = fontParameters.getGlyphScaleY();
            shadowOffset = fontParameters.getShadowOffset();
        }
        verticalScale *= AngelicaFontSupport.customFontScale(fontRenderer);
        float glyphHeight = Math.max(1.0F, fontRenderer.FONT_HEIGHT - 1.0F);
        // Angelica expands glyphs around their original vertical center.
        float overflow = Math.max(0.0F, glyphHeight * (verticalScale - 1.0F) / 2.0F);
        int topPadding = (int) Math.ceil(overflow);
        int bottomPadding = (int) Math.ceil(overflow + Math.max(0.0F, shadowOffset));
        return new HudFontMetrics(topPadding, bottomPadding);
    }

    public int rowHeight(int contentHeight) {
        return Math.max(MINIMUM_LINE_HEIGHT, contentHeight + topPadding + bottomPadding + LINE_PADDING * 2);
    }

    public int contentOffset(int contentHeight, int rowHeight) {
        return topPadding + Math.max(0, rowHeight - contentHeight - topPadding - bottomPadding) / 2;
    }

}
