package com.circulation.wailahud.theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.circulation.wailahud.CirculationWailaHud;
import com.circulation.wailahud.integration.Mods;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChromaticThemeReader {

    private static final ResourceLocation WAILA_THEME = new ResourceLocation("chromatictooltips", "tooltip.waila.json");

    public HudTheme read(IResourceManager resourceManager, HudTheme fallback) {
        if (!Mods.ChromaticTooltips.isModLoaded()) {
            return fallback;
        }

        try {
            IResource resource = resourceManager.getResource(WAILA_THEME);
            try (InputStream stream = resource.getInputStream();
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader)
                    .getAsJsonObject();
                JsonObject style = root.has("style") ? root.getAsJsonObject("style") : root;
                return applyStyle(style, fallback);
            }
        } catch (IOException | RuntimeException exception) {
            CirculationWailaHud.LOG.warn("Unable to load ChromaticTooltips Waila theme", exception);
            return fallback;
        }
    }

    private HudTheme applyStyle(JsonObject style, HudTheme fallback) {
        int maximumContentWidth = readMaximumContentWidth(style, fallback);
        int padding = style.has("padding") ? style.get("padding")
            .getAsInt() : 0;
        ChromaticStyle chromaticStyle = readDecorators(style, padding);
        return new HudTheme(
            fallback.frameTexture(),
            fallback.crystalTexture(),
            fallback.frameWidth(),
            fallback.frameHeight(),
            fallback.contentX() + padding,
            fallback.contentY() + padding,
            fallback.baseContentWidth(),
            maximumContentWidth,
            fallback.iconX(),
            fallback.iconY(),
            fallback.borderTop(),
            fallback.borderBottom(),
            fallback.bottomPadding() + padding,
            fallback.navigationGap(),
            chromaticStyle);
    }

    private int readMaximumContentWidth(JsonObject style, HudTheme fallback) {
        if (!style.has("maxWidth")) return fallback.maximumContentWidth();
        int configuredWidth = style.get("maxWidth")
            .getAsInt();
        return configuredWidth == -1 ? -1 : Math.max(32, configuredWidth);
    }

    private ChromaticStyle readDecorators(JsonObject style, int padding) {
        int background = 0;
        int borderTop = 0;
        int borderBottom = 0;
        int borderMargin = padding;

        if (!style.has("decorators")) {
            return new ChromaticStyle(background, borderTop, borderBottom, borderMargin, null, false);
        }

        JsonArray decorators = style.getAsJsonArray("decorators");
        ChromaticTexture texture = null;
        for (JsonElement element : decorators) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject decorator = element.getAsJsonObject();
            String type = decorator.has("type") ? decorator.get("type")
                .getAsString() : "";
            if ("background".equals(type) && decorator.has("color")) {
                background = parseColor(decorator.get("color"));
            } else if ("border".equals(type) && decorator.has("color")) {
                JsonElement color = decorator.get("color");
                if (color.isJsonArray()) {
                    JsonArray colors = color.getAsJsonArray();
                    borderTop = colors.size() > 0 ? parseColor(colors.get(0)) : 0;
                    borderBottom = colors.size() > 1 ? parseColor(colors.get(1)) : borderTop;
                } else {
                    borderTop = parseColor(color);
                    borderBottom = borderTop;
                }
                borderMargin = decorator.has("margin") ? decorator.get("margin")
                    .getAsInt() : borderMargin;
            } else if ("texture".equals(type) && decorator.has("path")) {
                texture = readTexture(decorator);
            }
        }

        return new ChromaticStyle(background, borderTop, borderBottom, borderMargin, texture, readTargetIcon(style));
    }

    private boolean readTargetIcon(JsonObject style) {
        if (!style.has("icon")) return false;
        JsonElement icon = style.get("icon");
        if (icon.isJsonPrimitive() && icon.getAsJsonPrimitive()
            .isBoolean()) return icon.getAsBoolean();
        return icon.isJsonObject() && icon.getAsJsonObject()
            .has("enabled")
            && icon.getAsJsonObject()
                .get("enabled")
                .getAsBoolean();
    }

    private ChromaticTexture readTexture(JsonObject decorator) {
        String path = decorator.get("path")
            .getAsString();
        ResourceLocation location = new ResourceLocation(path.contains(":") ? path : "chromatictooltips:" + path);
        JsonObject region = decorator.has("region") && decorator.get("region")
            .isJsonObject() ? decorator.getAsJsonObject("region") : new JsonObject();
        int x = region.has("x") ? region.get("x")
            .getAsInt() : 0;
        int y = region.has("y") ? region.get("y")
            .getAsInt() : 0;
        int width = region.has("width") ? region.get("width")
            .getAsInt() : 0;
        int height = region.has("height") ? region.get("height")
            .getAsInt() : 0;
        return new ChromaticTexture(location, x, y, width, height);
    }

    private int parseColor(JsonElement element) {
        String value = element.getAsString();
        return value.startsWith("0x") ? (int) Long.parseLong(value.substring(2), 16) : Integer.parseInt(value);
    }
}
