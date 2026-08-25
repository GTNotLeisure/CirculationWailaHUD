package com.circulation.wailahud.hud;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.circulation.wailahud.mixins.late.waila.AccessorTooltip;

import mcp.mobius.waila.api.SpecialChars;
import mcp.mobius.waila.overlay.Tooltip;

public record TooltipSnapshot(List<String> lines, ItemStack stack, boolean hasIcon, Point anchor) {

    public static TooltipSnapshot from(Tooltip tooltip) {
        AccessorTooltip accessor = (AccessorTooltip) tooltip;
        List<String> lines = new ArrayList<>();

        for (ArrayList<String> columns : accessor.circulationWailaHud$getLines()) {
            lines.add(String.join(SpecialChars.TAB, columns));
        }

        Point anchor = accessor.circulationWailaHud$getAnchor();
        return new TooltipSnapshot(
            Collections.unmodifiableList(lines),
            accessor.circulationWailaHud$getStack(),
            accessor.circulationWailaHud$hasIcon(),
            new Point(anchor));
    }

    public int contentHash() {
        int hash = lines.hashCode();
        hash = 31 * hash + System.identityHashCode(stack);
        hash = 31 * hash + Boolean.hashCode(hasIcon);
        return hash;
    }
}
