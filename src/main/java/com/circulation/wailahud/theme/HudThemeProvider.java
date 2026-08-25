package com.circulation.wailahud.theme;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class HudThemeProvider {

    private static final HudThemeProvider INSTANCE = new HudThemeProvider();
    private static final HudTheme DEFAULT_THEME = new HudTheme(
        new ResourceLocation("circulation_waila_hud", "textures/gui/node_hud_base.png"),
        new ResourceLocation("circulation_waila_hud", "textures/gui/node_hud_crystal.png"),
        165,
        93,
        89,
        12,
        62,
        1024,
        20,
        22,
        10,
        10,
        10,
        4,
        new ChromaticStyle(0, 0, 0, 0, null, true));

    private final ChromaticThemeReader chromaticThemeReader = new ChromaticThemeReader();
    private HudTheme theme = DEFAULT_THEME;
    private int generation;

    public static HudThemeProvider instance() {
        return INSTANCE;
    }

    public HudTheme current() {
        return theme;
    }

    public int generation() {
        return generation;
    }

    public void reload(IResourceManager resourceManager) {
        theme = chromaticThemeReader.read(resourceManager, DEFAULT_THEME);
        generation++;
    }
}
