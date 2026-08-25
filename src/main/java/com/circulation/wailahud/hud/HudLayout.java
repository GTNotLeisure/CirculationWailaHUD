package com.circulation.wailahud.hud;

import java.util.List;

public record HudLayout(List<HudPage> pages, int width, int height) {}
