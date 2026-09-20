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

    public static int contentHash(Tooltip tooltip) {
        AccessorTooltip accessor = (AccessorTooltip) tooltip;
        int linesHash = 1;
        for (ArrayList<String> columns : accessor.circulationWailaHud$getLines()) {
            linesHash = 31 * linesHash + joinedLineHash(columns);
        }
        int hash = linesHash;
        hash = 31 * hash + stackHash(accessor.circulationWailaHud$getStack());
        hash = 31 * hash + Boolean.hashCode(accessor.circulationWailaHud$hasIcon());
        return hash;
    }

    public int contentHash() {
        int hash = lines.hashCode();
        hash = 31 * hash + stackHash(stack);
        hash = 31 * hash + Boolean.hashCode(hasIcon);
        return hash;
    }

    private static int stackHash(ItemStack stack) {
        if (stack == null) return 0;
        // Waila can create equivalent stacks on every tick.
        int hash = System.identityHashCode(stack.getItem());
        hash = 31 * hash + stack.getItemDamage();
        hash = 31 * hash + stack.stackSize;
        return 31 * hash + (stack.hasTagCompound() ? stack.getTagCompound()
            .hashCode() : 0);
    }

    private static int joinedLineHash(List<String> columns) {
        int hash = 0;
        boolean needsSeparator = false;
        for (String column : columns) {
            if (needsSeparator) hash = appendHash(hash, SpecialChars.TAB);
            hash = appendHash(hash, String.valueOf(column));
            needsSeparator = true;
        }
        return hash;
    }

    private static int appendHash(int hash, String value) {
        for (int index = 0; index < value.length(); index++) {
            hash = 31 * hash + value.charAt(index);
        }
        return hash;
    }
}
