package com.circulation.wailahud.hud;

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

    public HudPage createPage(List<String> sourceLines) {
        if (sourceLines.isEmpty()) return new HudPage(null, Collections.emptyList(), 0);

        Tooltip tooltip = new Tooltip(sourceLines, false);
        AccessorTooltip tooltipAccessor = (AccessorTooltip) tooltip;
        IWailaCommonAccessor wailaAccessor = tooltipAccessor.circulationWailaHud$getAccessor();
        Map<Integer, List<AccessorTooltipRenderable>> primary = new TreeMap<>();
        Map<Integer, List<AccessorTooltipRenderable>> secondary = new TreeMap<>();
        Map<Integer, Integer> contentWidths = new TreeMap<>();
        collect(tooltipAccessor.circulationWailaHud$getElements(), primary, contentWidths, wailaAccessor);
        collect(tooltipAccessor.circulationWailaHud$getElements2nd(), secondary, contentWidths, wailaAccessor);

        Set<Integer> positions = new TreeSet<>();
        positions.addAll(primary.keySet());
        positions.addAll(secondary.keySet());
        List<Integer> rowPositions = new ArrayList<>(positions);
        List<HudRow> rows = new ArrayList<>();
        int contentHeight = tooltipAccessor.circulationWailaHud$getRenderableHeight();
        int displayY = 0;
        for (int index = 0; index < rowPositions.size(); index++) {
            int y = rowPositions.get(index);
            int nextY = index + 1 < rowPositions.size() ? rowPositions.get(index + 1) : contentHeight;
            int rowHeight = Math.max(14, nextY - y);
            int rowWidth = contentWidths.getOrDefault(y, 0);
            rows.add(
                new HudRow(y, displayY, rowHeight, rowWidth, immutable(primary.get(y)), immutable(secondary.get(y))));
            displayY += rowHeight;
        }

        return new HudPage(wailaAccessor, Collections.unmodifiableList(rows), Math.max(contentHeight, displayY));
    }

    public void draw(HudRow row, boolean secondPass, int x, int y, IWailaCommonAccessor accessor) {
        List<AccessorTooltipRenderable> renderables = secondPass ? row.secondary() : row.primary();
        for (AccessorTooltipRenderable renderable : renderables) {
            renderable.circulationWailaHud$draw(accessor, x, y + row.displayY() - row.y());
        }
    }

    private void collect(List<Object> source, Map<Integer, List<AccessorTooltipRenderable>> destination,
        Map<Integer, Integer> contentWidths, IWailaCommonAccessor accessor) {
        for (Object element : source) {
            AccessorTooltipRenderable renderable = (AccessorTooltipRenderable) element;
            int y = renderable.circulationWailaHud$getPosition().y;
            int width = renderable.circulationWailaHud$getPosition().x
                + renderable.circulationWailaHud$getSize(accessor).width;
            List<AccessorTooltipRenderable> row = destination.computeIfAbsent(y, k -> new ArrayList<>());
            row.add(renderable);
            contentWidths
                .compute(y, (k, previousWidth) -> previousWidth == null ? width : Math.max(previousWidth, width));
        }
    }

    private List<AccessorTooltipRenderable> immutable(List<AccessorTooltipRenderable> renderables) {
        if (renderables == null) return Collections.emptyList();
        return List.copyOf(renderables);
    }
}
