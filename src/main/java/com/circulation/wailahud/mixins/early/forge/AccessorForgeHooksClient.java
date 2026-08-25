package com.circulation.wailahud.mixins.early.forge;

import net.minecraftforge.client.ForgeHooksClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ForgeHooksClient.class, remap = false)
public interface AccessorForgeHooksClient {

    @Accessor("worldRenderPass")
    static void circulationWailaHud$setWorldRenderPass(int value) {
        throw new AssertionError();
    }
}
