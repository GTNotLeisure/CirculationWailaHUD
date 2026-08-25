package com.circulation.wailahud.config;

import com.circulation.wailahud.CirculationWailaHud;
import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeDouble;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

@Config(modid = CirculationWailaHud.MODID, filename = CirculationWailaHud.MODID)
@Config.LangKeyPattern(pattern = "circulation_waila_hud.config.%cat.%field", fullyQualified = true)
@Comment("Client HUD configuration")
public class ModConfig {

    public static final Hud HUD = new Hud();

    public static void register() throws ConfigException {
        ConfigurationManager.registerConfig(ModConfig.class);
    }

    @Comment("HUD rendering settings")
    public static class Hud {

        @Comment("Enable the Circulation Waila HUD renderer")
        @DefaultBoolean(true)
        public boolean enabled;

        @Comment("Content viewport width in local panel pixels, or -1 for unlimited automatic width")
        @DefaultInt(120)
        @RangeInt(min = -1, max = 1024)
        public int contentWidth;

        @Comment("Maximum original Waila lines displayed on one page")
        @DefaultInt(8)
        @RangeInt(min = 1, max = 100)
        public int maxLinesPerPage;

        @Comment("Horizontal offset in local panel pixels")
        @DefaultInt(0)
        @RangeInt(min = -10000, max = 10000)
        public int offsetX;

        @Comment("Vertical offset in local panel pixels")
        @DefaultInt(0)
        @RangeInt(min = -10000, max = 10000)
        public int offsetY;

        @Comment("HUD scale factor")
        @DefaultDouble(1.0D)
        @RangeDouble(min = 0.25D, max = 4.0D)
        public double scale;
    }
}
