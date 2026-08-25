package com.circulation.wailahud.config;

import net.minecraft.client.gui.GuiScreen;

import com.circulation.wailahud.CirculationWailaHud;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;

public class ModGuiConfig extends SimpleGuiConfig {

    public ModGuiConfig(GuiScreen parentScreen) throws ConfigException {
        super(parentScreen, CirculationWailaHud.MODID, CirculationWailaHud.MOD_NAME, true, ModConfig.class);
    }
}
