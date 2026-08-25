package com.circulation.wailahud.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.circulation.wailahud.mixins.late.waila.TooltipAccessor;
import com.circulation.wailahud.mixins.late.waila.TooltipRenderableAccessor;

import mcp.mobius.waila.api.IWailaCommonAccessor;
import mcp.mobius.waila.overlay.Tooltip;

public class WailaRowRenderer {

    public HudPage createPage(List<String> sourceLines) {
        if (sourceLines.isEmpty()) return new HudPage(null, Collections.<HudRow>emptyList(), 0);

        Tooltip tooltip = new Tooltip(sourceLines, false);
        TooltipAccessor tooltipAccessor = (TooltipAccessor) tooltip;
        IWailaCommonAccessor wailaAccessor = tooltipAccessor.circulationWailaHud$getAccessor();
        Map<Integer, List<TooltipRenderableAccessor>> primary = new TreeMap<>();
        Map<Integer, List<TooltipRenderableAccessor>> secondary = new TreeMap<>();
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
            int rowWidth = contentWidths.containsKey(y) ? contentWidths.get(y) : 0;
            rows.add(
                new HudRow(y, displayY, rowHeight, rowWidth, immutable(primary.get(y)), immutable(secondary.get(y))));
            displayY += rowHeight;
        }

        return new HudPage(wailaAccessor, Collections.unmodifiableList(rows), Math.max(contentHeight, displayY));
    }

    public void draw(HudRow row, boolean secondPass, int x, int y, IWailaCommonAccessor accessor) {
        List<TooltipRenderableAccessor> renderables = secondPass ? row.secondary() : row.primary();
        for (TooltipRenderableAccessor renderable : renderables) {
            renderable.circulationWailaHud$draw(accessor, x, y + row.displayY() - row.y());
        }
    }

    private void collect(List<Object> source, Map<Integer, List<TooltipRenderableAccessor>> destination,
        Map<Integer, Integer> contentWidths, IWailaCommonAccessor accessor) {
        for (Object element : source) {
            TooltipRenderableAccessor renderable = (TooltipRenderableAccessor) element;
            int y = renderable.circulationWailaHud$getPosition().y;
            int width = renderable.circulationWailaHud$getPosition().x
                + renderable.circulationWailaHud$getSize(accessor).width;
            List<TooltipRenderableAccessor> row = destination.get(y);
            if (row == null) {
                row = new ArrayList<>();
                destination.put(y, row);
            }
            row.add(renderable);
            Integer previousWidth = contentWidths.get(y);
            contentWidths.put(y, previousWidth == null ? width : Math.max(previousWidth, width));
        }
    }

    private List<TooltipRenderableAccessor> immutable(List<TooltipRenderableAccessor> renderables) {
        if (renderables == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(renderables));
    }
}
