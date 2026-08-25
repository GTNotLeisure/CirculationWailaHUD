package com.circulation.wailahud.coremod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.circulation.wailahud.config.ModConfig;
import com.circulation.wailahud.mixins.Mixins;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class CirculationWailaHudCoreMod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static {
        try {
            ModConfig.register();
        } catch (ConfigException exception) {
            throw new IllegalStateException("Unable to register Circulation Waila HUD configuration", exception);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.circulation_waila_hud.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
