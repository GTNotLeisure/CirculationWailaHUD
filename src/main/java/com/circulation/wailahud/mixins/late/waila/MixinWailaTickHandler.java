package com.circulation.wailahud.mixins.late.waila;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.circulation.wailahud.hud.HudController;

import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import mcp.mobius.waila.overlay.RayTracing;
import mcp.mobius.waila.overlay.Tooltip;
import mcp.mobius.waila.overlay.WailaTickHandler;

@Mixin(value = WailaTickHandler.class, remap = false)
public class MixinWailaTickHandler {

    @Shadow
    private Tooltip tooltip;

    @Unique
    private Tooltip circulationWailaHud$previousTooltip;

    @Inject(method = "tickClient", at = @At("HEAD"))
    private void circulationWailaHud$beforeUpdate(ClientTickEvent event, CallbackInfo callbackInfo) {
        if (event.phase == Phase.END) circulationWailaHud$previousTooltip = tooltip;
    }

    @Inject(method = "tickClient", at = @At("RETURN"))
    private void circulationWailaHud$afterUpdate(ClientTickEvent event, CallbackInfo callbackInfo) {
        if (event.phase != Phase.END) return;
        // Waila can retain its old tooltip when the current target has no display data.
        Tooltip updatedTooltip = tooltip != circulationWailaHud$previousTooltip ? tooltip : null;
        circulationWailaHud$previousTooltip = null;
        HudController.instance()
            .updateTooltip(
                updatedTooltip,
                RayTracing.instance()
                    .getTarget());
    }
}
