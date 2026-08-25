package com.circulation.wailahud.hud;

import java.util.List;

import mcp.mobius.waila.api.IWailaCommonAccessor;

public record HudPage(IWailaCommonAccessor accessor, List<HudRow> rows, int contentHeight) {}
