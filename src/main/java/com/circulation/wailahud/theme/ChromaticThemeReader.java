package com.circulation.wailahud.theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.ResourcePackRepository.Entry;
import net.minecraft.util.ResourceLocation;

import com.circulation.wailahud.CirculationWailaHud;
import com.circulation.wailahud.integration.Mods;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChromaticThemeReader {

    private static final ResourceLocation WAILA_THEME = new ResourceLocation("chromatictooltips", "tooltip.waila.json");

    public HudTheme read(ResourcePackRepository repository, HudTheme fallback) {
        if (!Mods.ChromaticTooltips.isModLoaded()) {
            return fallback;
        }

        try {
            IResourcePack resourcePack = findThemePack(repository);
            if (resourcePack == null) {
                return fallback;
            }

            try (InputStream stream = resourcePack.getInputStream(WAILA_THEME);
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader)
                    .getAsJsonObject();
                JsonObject style = root.has("style") ? root.getAsJsonObject("style") : root;
                return applyStyle(style, fallback);
            }
        } catch (IOException | RuntimeException exception) {
            CirculationWailaHud.LOG.warn("Unable to load resource-pack theme {}", WAILA_THEME, exception);
            return fallback;
        }
    }

    private IResourcePack findThemePack(ResourcePackRepository repository) {
        // Only explicit resource packs may override the built-in HUD theme.
        IResourcePack serverPack = repository.func_148530_e();
        if (serverPack != null && serverPack.resourceExists(WAILA_THEME)) {
            return serverPack;
        }

        List<Entry> entries = repository.getRepositoryEntries();
        for (int index = entries.size() - 1; index >= 0; index--) {
            IResourcePack resourcePack = entries.get(index)
                .getResourcePack();
            if (resourcePack.resourceExists(WAILA_THEME)) {
                return resourcePack;
            }
        }
        return null;
    }

    private HudTheme applyStyle(JsonObject style, HudTheme fallback) {
        int maximumContentWidth = readMaximumContentWidth(style, fallback);
        int padding = style.has("padding") ? style.get("padding")
            .getAsInt() : 0;
        ChromaticStyle chromaticStyle = readDecorators(style, padding, fallback.chromaticStyle());
        return new HudTheme(
            fallback.frameTexture(),
            fallback.crystalTexture(),
            fallback.frameWidth(),
            fallback.frameHeight(),
            fallback.contentX(),
            fallback.contentY(),
            fallback.baseContentWidth(),
            maximumContentWidth,
            fallback.iconX(),
            fallback.iconY(),
            fallback.borderTop(),
            fallback.borderBottom(),
            fallback.bottomPadding(),
            fallback.navigationGap(),
            chromaticStyle);
    }

    private int readMaximumContentWidth(JsonObject style, HudTheme fallback) {
        if (!style.has("maxWidth")) return fallback.maximumContentWidth();
        int configuredWidth = style.get("maxWidth")
            .getAsInt();
        return configuredWidth == -1 ? -1 : Math.max(32, configuredWidth);
    }

    private ChromaticStyle readDecorators(JsonObject style, int padding, ChromaticStyle fallback) {
        int background = 0;
        int borderTop = 0;
        int borderBottom = 0;
        int borderMargin = padding;
        boolean showTargetIcon = readTargetIcon(style, fallback.showTargetIcon());

        if (!style.has("decorators")) {
            return new ChromaticStyle(background, borderTop, borderBottom, borderMargin, null, showTargetIcon);
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

        return new ChromaticStyle(background, borderTop, borderBottom, borderMargin, texture, showTargetIcon);
    }

    private boolean readTargetIcon(JsonObject style, boolean fallback) {
        if (!style.has("icon")) return fallback;
        JsonElement icon = style.get("icon");
        if (icon.isJsonPrimitive() && icon.getAsJsonPrimitive()
            .isBoolean()) return icon.getAsBoolean();
        return icon.isJsonObject() && icon.getAsJsonObject()
            .has("enabled") ? icon.getAsJsonObject()
                .get("enabled")
                .getAsBoolean() : fallback;
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
