package com.circulation.wailahud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.circulation.wailahud.hud.HudController;
import com.circulation.wailahud.theme.HudThemeProvider;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public class ClientProxy extends CommonProxy implements IResourceManagerReloadListener {

    public static final KeyBinding NEXT_PAGE = new KeyBinding(
        "key.circulation_waila_hud.next_page",
        Keyboard.KEY_NEXT,
        "key.categories.circulation_waila_hud");

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ClientRegistry.registerKeyBinding(NEXT_PAGE);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);

        IResourceManager resourceManager = Minecraft.getMinecraft()
            .getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager reloadableResourceManager) {
            reloadableResourceManager.registerReloadListener(this);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        advancePageIfPressed();
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        advancePageIfPressed();
    }

    private void advancePageIfPressed() {
        while (NEXT_PAGE.isPressed()) {
            HudController.instance()
                .advancePage();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        HudController.instance()
            .renderWorld(event);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        HudThemeProvider.instance()
            .reload(
                Minecraft.getMinecraft()
                    .getResourcePackRepository());
        HudController.instance()
            .clear();
    }
}
