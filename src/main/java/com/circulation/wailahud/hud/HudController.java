package com.circulation.wailahud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import com.circulation.wailahud.config.ModConfig;
import com.circulation.wailahud.mixins.late.waila.AccessorTooltip;
import com.circulation.wailahud.theme.HudTheme;
import com.circulation.wailahud.theme.HudThemeProvider;

import mcp.mobius.waila.api.IWailaCommonAccessor;
import mcp.mobius.waila.api.impl.ConfigHandler;
import mcp.mobius.waila.overlay.OverlayRenderer;
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
    private HudFontMetrics fontMetrics;
    private CacheKey cacheKey;
    private TargetKey targetKey;
    private MovingObjectPosition displayTarget;
    private int pageIndex;

    public static HudController instance() {
        return INSTANCE;
    }

    public boolean renderIfEnabled(Tooltip tooltip) {
        // The world HUD owns presentation; the late overlay must not republish tick data.
        return ModConfig.HUD.enabled;
    }

    public void updateTooltip(Tooltip tooltip, MovingObjectPosition target) {
        World world = Minecraft.getMinecraft().theWorld;
        if (!ModConfig.HUD.enabled || world == null || tooltip == null || !isEligibleTarget(tooltip, target)) {
            resetDisplay();
            return;
        }
        TargetKey currentTarget = TargetKey.create(world, target);
        IWailaCommonAccessor accessor = ((AccessorTooltip) tooltip).circulationWailaHud$getAccessor();
        if (accessor.getWorld() != world || accessor.getPosition() == null
            || !TargetKey.create(world, accessor.getPosition())
                .equals(currentTarget)) {
            resetDisplay();
            return;
        }
        boolean targetChanged = !currentTarget.equals(targetKey);
        int tooltipContentHash = TooltipSnapshot.contentHash(tooltip);
        HudSettings hudSettings = HudSettings.fromConfig(ModConfig.HUD);
        HudFontMetrics currentFontMetrics = HudFontMetrics.current();
        HudThemeProvider themeProvider = HudThemeProvider.instance();
        HudTheme hudTheme = themeProvider.current();
        CacheKey newKey = new CacheKey(
            tooltipContentHash,
            hudSettings,
            currentFontMetrics,
            themeProvider.generation(),
            currentTarget);
        if (!newKey.equals(cacheKey)) {
            TooltipSnapshot updatedSnapshot = TooltipSnapshot.from(tooltip);
            HudLayout updatedLayout = layoutEngine.layout(updatedSnapshot, hudTheme, hudSettings, currentFontMetrics);
            snapshot = updatedSnapshot;
            layout = updatedLayout;
            cacheKey = newKey;
        }
        settings = hudSettings;
        theme = hudTheme;
        fontMetrics = currentFontMetrics;
        targetKey = currentTarget;
        displayTarget = target;
        if (targetChanged) pageIndex = 0;
        pageIndex = Math.min(
            pageIndex,
            layout.pages()
                .size() - 1);
    }

    public void renderWorld(RenderWorldLastEvent event) {
        if (!ModConfig.HUD.enabled || !OverlayRenderer.isOverlayVisible()
            || layout == null
            || snapshot == null
            || settings == null
            || theme == null
            || fontMetrics == null
            || displayTarget == null
            || targetKey == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld != targetKey.world()) {
            resetDisplay();
            return;
        }
        MovingObjectPosition liveTarget = minecraft.objectMouseOver;
        if (liveTarget == null || liveTarget.typeOfHit == MovingObjectType.MISS) return;
        if (displayTarget.typeOfHit == MovingObjectType.ENTITY
            && (displayTarget.entityHit == null || displayTarget.entityHit.isDead
                || displayTarget.entityHit.worldObj != minecraft.theWorld))
            return;
        // Keep the complete tick snapshot until Waila publishes its replacement.
        MovingObjectPosition renderTarget = TargetKey.create(minecraft.theWorld, liveTarget)
            .equals(targetKey) ? liveTarget : displayTarget;
        long ticks = minecraft.theWorld.getTotalWorldTime();
        worldRenderer.render(layout, pageIndex, theme, settings, fontMetrics, renderTarget, event.partialTicks, ticks);
    }

    public void advancePage() {
        if (ModConfig.HUD.enabled && OverlayRenderer.isOverlayVisible()
            && displayTarget != null
            && layout != null
            && layout.pages()
                .size() > 1)
            pageIndex = (pageIndex + 1) % layout.pages()
                .size();
    }

    public void clear() {
        resetDisplay();
        tooltipFramebuffer.clear();
        worldRenderer.clear();
    }

    private void resetDisplay() {
        layout = null;
        snapshot = null;
        settings = null;
        theme = null;
        fontMetrics = null;
        cacheKey = null;
        targetKey = null;
        displayTarget = null;
        pageIndex = 0;
    }

    private boolean isEligibleTarget(Tooltip tooltip, MovingObjectPosition target) {
        if (!OverlayRenderer.isOverlayVisible()) return false;
        if (target == null || target.hitVec == null) return false;
        if (target.typeOfHit == MovingObjectType.BLOCK) {
            ItemStack stack = ((AccessorTooltip) tooltip).circulationWailaHud$getStack();
            return stack != null && stack.getItem() != null;
        }
        return target.typeOfHit == MovingObjectType.ENTITY && target.entityHit != null
            && !target.entityHit.isDead
            && ConfigHandler.instance()
                .getConfig("general.showents");
    }

    private record CacheKey(int contentHash, HudSettings settings, HudFontMetrics fontMetrics, int themeGeneration,
        TargetKey targetKey) {}

    private record TargetKey(World world, MovingObjectType type, int first, int second, int third) {

        private static TargetKey create(World world, MovingObjectPosition target) {
            if (target.typeOfHit == MovingObjectType.ENTITY && target.entityHit != null) {
                return new TargetKey(world, target.typeOfHit, target.entityHit.getEntityId(), 0, 0);
            }
            return new TargetKey(world, target.typeOfHit, target.blockX, target.blockY, target.blockZ);
        }
    }
}
