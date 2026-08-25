package com.circulation.wailahud.mixins.late.waila;

import java.awt.Dimension;
import java.awt.Point;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import mcp.mobius.waila.api.IWailaCommonAccessor;

@Mixin(targets = "mcp.mobius.waila.overlay.Tooltip$Renderable", remap = false)
public interface AccessorTooltipRenderable {

    @Accessor("pos")
    Point circulationWailaHud$getPosition();

    @Invoker("draw")
    void circulationWailaHud$draw(IWailaCommonAccessor accessor, int x, int y);

    @Invoker("getSize")
    Dimension circulationWailaHud$getSize(IWailaCommonAccessor accessor);
}
