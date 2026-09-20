package com.circulation.wailahud.hud;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.circulation.wailahud.mixins.late.waila.AccessorTooltip;
import com.circulation.wailahud.mixins.late.waila.AccessorTooltipRenderable;

import mcp.mobius.waila.api.IWailaCommonAccessor;
import mcp.mobius.waila.overlay.Tooltip;

public class WailaRowRenderer {

    public HudPage createPage(List<String> sourceLines, HudFontMetrics fontMetrics) {
        if (sourceLines.isEmpty()) return new HudPage(null, Collections.emptyList(), 0);

        Tooltip tooltip = new Tooltip(sourceLines, false);
        AccessorTooltip tooltipAccessor = (AccessorTooltip) tooltip;
        IWailaCommonAccessor wailaAccessor = tooltipAccessor.circulationWailaHud$getAccessor();
        Map<Integer, List<AccessorTooltipRenderable>> primary = new TreeMap<>();
        Map<Integer, List<AccessorTooltipRenderable>> secondary = new TreeMap<>();
        Map<Integer, Dimension> contentSizes = new TreeMap<>();
        collect(tooltipAccessor.circulationWailaHud$getElements(), primary, contentSizes, wailaAccessor);
        collect(tooltipAccessor.circulationWailaHud$getElements2nd(), secondary, contentSizes, wailaAccessor);

        Set<Integer> positions = new TreeSet<>();
        positions.addAll(primary.keySet());
        positions.addAll(secondary.keySet());
        List<Integer> rowPositions = new ArrayList<>(positions);
        List<HudRow> rows = new ArrayList<>();
        int displayY = 0;
        for (int index = 0; index < rowPositions.size(); index++) {
            int y = rowPositions.get(index);
            Dimension size = contentSizes.get(y);
            int contentHeight = size.height;
            if (index + 1 < rowPositions.size()) {
                contentHeight = Math.max(contentHeight, rowPositions.get(index + 1) - y - 2);
            }
            int rowHeight = fontMetrics.rowHeight(contentHeight);
            rows.add(
                new HudRow(
                    y,
                    displayY,
                    rowHeight,
                    size.width,
                    fontMetrics.contentOffset(contentHeight, rowHeight),
                    immutable(primary.get(y)),
                    immutable(secondary.get(y))));
            displayY += rowHeight;
        }

        return new HudPage(wailaAccessor, Collections.unmodifiableList(rows), displayY);
    }

    public void draw(HudRow row, boolean secondPass, int x, int y, IWailaCommonAccessor accessor) {
        List<AccessorTooltipRenderable> renderables = secondPass ? row.secondary() : row.primary();
        for (AccessorTooltipRenderable renderable : renderables) {
            renderable.circulationWailaHud$draw(accessor, x, y + row.displayY() + row.contentOffsetY() - row.y());
        }
    }

    private void collect(List<Object> source, Map<Integer, List<AccessorTooltipRenderable>> destination,
        Map<Integer, Dimension> contentSizes, IWailaCommonAccessor accessor) {
        for (Object element : source) {
            AccessorTooltipRenderable renderable = (AccessorTooltipRenderable) element;
            int y = renderable.circulationWailaHud$getPosition().y;
            Dimension size = renderable.circulationWailaHud$getSize(accessor);
            int width = renderable.circulationWailaHud$getPosition().x + size.width;
            List<AccessorTooltipRenderable> row = destination.computeIfAbsent(y, k -> new ArrayList<>());
            row.add(renderable);
            Dimension rowSize = contentSizes.computeIfAbsent(y, k -> new Dimension());
            rowSize.width = Math.max(rowSize.width, width);
            rowSize.height = Math.max(rowSize.height, size.height);
        }
    }

    private List<AccessorTooltipRenderable> immutable(List<AccessorTooltipRenderable> renderables) {
        if (renderables == null) return Collections.emptyList();
        return List.copyOf(renderables);
    }
}
