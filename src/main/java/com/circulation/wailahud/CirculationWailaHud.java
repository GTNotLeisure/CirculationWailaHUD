package com.circulation.wailahud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = CirculationWailaHud.MODID,
    name = CirculationWailaHud.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:Waila",
    guiFactory = "com.circulation.wailahud.config.ModGuiFactory")
public class CirculationWailaHud {

    public static final String MODID = Tags.MODID;
    public static final String MOD_NAME = "Circulation Waila HUD";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.circulation.wailahud.ClientProxy",
        serverSide = "com.circulation.wailahud.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
