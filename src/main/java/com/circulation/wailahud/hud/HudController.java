package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import com.circulation.wailahud.config.ModConfig;
import com.circulation.wailahud.theme.HudTheme;
import com.circulation.wailahud.theme.HudThemeProvider;

import mcp.mobius.waila.api.impl.ConfigHandler;
import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.RayTracing;
import mcp.mobius.waila.overlay.Tooltip;

public class HudController {

    private static final HudController INSTANCE = new HudController();

    private final HudLayoutEngine layoutEngine = new HudLayoutEngine();
    private final TooltipFramebuffer tooltipFramebuffer = new TooltipFramebuffer();
    private final WorldHudRenderer worldRenderer = new WorldHudRenderer();
    private HudLayout layout;
    private TooltipSnapshot snapshot;
    private HudSettings settings;
    private HudTheme theme;
    private CacheKey cacheKey;
    private TargetKey targetKey;
    private int pageIndex;

    public static HudController instance() {
        return INSTANCE;
    }

    public boolean renderIfEnabled(Tooltip tooltip) {
        if (!ModConfig.HUD.enabled || !isEligibleTarget()) return false;
        MovingObjectPosition target = RayTracing.instance()
            .getTarget();
        TargetKey currentTarget = TargetKey.create(target);
        TooltipSnapshot tooltipSnapshot = TooltipSnapshot.from(tooltip);
        HudSettings hudSettings = HudSettings.fromConfig(ModConfig.HUD);
        HudThemeProvider themeProvider = HudThemeProvider.instance();
        HudTheme hudTheme = themeProvider.current();
        CacheKey newKey = new CacheKey(
            tooltipSnapshot.contentHash(),
            hudSettings,
            themeProvider.generation(),
            currentTarget);
        if (!newKey.equals(cacheKey)) {
            layout = layoutEngine.layout(tooltipSnapshot, hudTheme, hudSettings);
            cacheKey = newKey;
            pageIndex = 0;
        }
        if (layout == null) return false;
        snapshot = tooltipSnapshot;
        settings = hudSettings;
        theme = hudTheme;
        targetKey = currentTarget;
        pageIndex = Math.min(
            pageIndex,
            layout.pages()
                .size() - 1);
        return true;
    }

    public void renderWorld(RenderWorldLastEvent event) {
        MovingObjectPosition currentTarget = RayTracing.instance()
            .getTarget();
        if (!ModConfig.HUD.enabled || layout == null
            || snapshot == null
            || settings == null
            || theme == null
            || currentTarget == null
            || !TargetKey.create(currentTarget)
                .equals(targetKey))
            return;
        Minecraft minecraft = Minecraft.getMinecraft();
        MovingObjectPosition liveTarget = minecraft.objectMouseOver;
        if (liveTarget == null || !TargetKey.create(liveTarget)
            .equals(targetKey)) return;
        long ticks = minecraft.theWorld == null ? 0L : minecraft.theWorld.getTotalWorldTime();
        worldRenderer.render(
            layout,
            layout.pages()
                .get(pageIndex),
            theme,
            settings,
            liveTarget,
            event.partialTicks,
            ticks);
    }

    public void advancePage() {
        if (layout != null && layout.pages()
            .size() > 1)
            pageIndex = (pageIndex + 1) % layout.pages()
                .size();
    }

    public void clear() {
        layout = null;
        snapshot = null;
        settings = null;
        theme = null;
        cacheKey = null;
        targetKey = null;
        pageIndex = 0;
        tooltipFramebuffer.clear();
        worldRenderer.clear();
    }

    private boolean isEligibleTarget() {
        if (!OverlayRenderer.isOverlayVisible()) return false;
        MovingObjectPosition target = RayTracing.instance()
            .getTarget();
        if (target == null) return false;
        if (target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) return RayTracing.instance()
            .getTargetStack() != null;
        return target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && ConfigHandler.instance()
            .getConfig("general.showents");
    }

    private record CacheKey(int contentHash, HudSettings settings, int themeGeneration, TargetKey targetKey) {}

    private record TargetKey(MovingObjectPosition.MovingObjectType type, int first, int second, int third) {

        private static TargetKey create(MovingObjectPosition target) {
            if (target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && target.entityHit != null) {
                return new TargetKey(target.typeOfHit, target.entityHit.getEntityId(), 0, 0);
            }
            return new TargetKey(target.typeOfHit, target.blockX, target.blockY, target.blockZ);
        }
    }
}
