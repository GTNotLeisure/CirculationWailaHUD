package com.circulation.wailahud.mixins.late.waila;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.circulation.wailahud.hud.HudController;

import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = OverlayRenderer.class, remap = false, priority = 2000)
public class MixinOverlayRenderer {

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private static void circulationWailaHud$render(Tooltip tooltip, CallbackInfo callbackInfo) {
        if (HudController.instance()
            .renderIfEnabled(tooltip)) {
            callbackInfo.cancel();
        }
    }
}
