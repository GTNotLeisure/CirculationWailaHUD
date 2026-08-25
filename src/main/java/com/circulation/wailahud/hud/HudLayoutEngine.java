package com.circulation.wailahud.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.circulation.wailahud.theme.HudTheme;

public class HudLayoutEngine {

    private final WailaRowRenderer rowRenderer = new WailaRowRenderer();

    public HudLayout layout(TooltipSnapshot snapshot, HudTheme theme, HudSettings settings) {
        int pageCapacity = settings.maxLinesPerPage();

        List<HudPage> pages = new ArrayList<>();
        for (int start = 0; start < snapshot.lines()
            .size(); start += pageCapacity) {
            int end = Math.min(
                start + pageCapacity,
                snapshot.lines()
                    .size());
            pages.add(
                rowRenderer.createPage(
                    snapshot.lines()
                        .subList(start, end)));
        }

        if (pages.isEmpty()) {
            pages.add(rowRenderer.createPage(Collections.emptyList()));
        }

        int contentWidth = resolveContentWidth(pages, theme, settings);
        int contentHeight = 0;
        for (HudPage page : pages) {
            contentHeight = Math.max(contentHeight, page.contentHeight());
        }

        int navigationHeight = pages.size() > 1
            ? Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + theme.navigationGap()
            : 0;
        int frameHeight = Math
            .max(theme.frameHeight(), theme.contentY() + contentHeight + navigationHeight + theme.bottomPadding());
        int frameWidth = theme.frameWidth() + contentWidth - theme.baseContentWidth();
        return new HudLayout(pages, frameWidth, frameHeight);
    }

    private int resolveContentWidth(List<HudPage> pages, HudTheme theme, HudSettings settings) {
        if (settings.contentWidth() != -1) {
            return theme.maximumContentWidth() == -1 ? settings.contentWidth()
                : Math.min(settings.contentWidth(), theme.maximumContentWidth());
        }

        int widestRow = theme.baseContentWidth();
        for (HudPage page : pages) {
            for (HudRow row : page.rows()) {
                widestRow = Math.max(widestRow, row.contentWidth());
            }
        }
        return widestRow;
    }
}
