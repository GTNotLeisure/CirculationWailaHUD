package com.circulation.wailahud.mixins;

import javax.annotation.Nonnull;

import com.circulation.wailahud.integration.Mods;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    WAILA(new MixinBuilder("Waila").addRequiredMod(Mods.Waila)
        .addClientMixins("waila.OverlayRendererMixin", "waila.TooltipAccessor", "waila.TooltipRenderableAccessor")
        .setPhase(Phase.LATE));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
