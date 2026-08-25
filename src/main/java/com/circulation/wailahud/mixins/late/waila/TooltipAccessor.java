package com.circulation.wailahud.mixins.late.waila;

import java.awt.Point;
import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import mcp.mobius.waila.api.IWailaCommonAccessor;
import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = Tooltip.class, remap = false)
public interface TooltipAccessor {

    @Accessor("lines")
    ArrayList<ArrayList<String>> circulationWailaHud$getLines();

    @Accessor("stack")
    ItemStack circulationWailaHud$getStack();

    @Accessor("hasIcon")
    boolean circulationWailaHud$hasIcon();

    @Accessor("pos")
    Point circulationWailaHud$getAnchor();

    @Accessor("x")
    int circulationWailaHud$getX();

    @Accessor("x")
    void circulationWailaHud$setX(int x);

    @Accessor("y")
    int circulationWailaHud$getY();

    @Accessor("y")
    void circulationWailaHud$setY(int y);

    @Accessor("h")
    int circulationWailaHud$getHeight();

    @Accessor("w")
    int circulationWailaHud$getWidth();

    @Accessor("offsetX")
    int circulationWailaHud$getOffsetX();

    @Accessor("ty")
    int circulationWailaHud$getTextYOffset();

    @Accessor("elements")
    ArrayList<Object> circulationWailaHud$getElements();

    @Accessor("elements2nd")
    ArrayList<Object> circulationWailaHud$getElements2nd();

    @Accessor("accessor")
    IWailaCommonAccessor circulationWailaHud$getAccessor();

    @Invoker("getRenderableTotalHeight")
    int circulationWailaHud$getRenderableHeight();
}
