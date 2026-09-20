package com.circulation.wailahud.integration;

import net.minecraft.client.gui.FontRenderer;

import com.gtnewhorizons.angelica.config.FontConfig;
import com.gtnewhorizons.angelica.mixins.interfaces.FontRendererAccessor;

public class AngelicaFontSupport {

    public static float customFontScale(FontRenderer fontRenderer) {
        if (!(fontRenderer instanceof FontRendererAccessor accessor) || accessor.angelica$getBatcher()
            .forceDefaults() || !FontConfig.enableCustomFont) return 1.0F;
        return Math.max(1.0F, FontConfig.customFontScale);
    }
}
