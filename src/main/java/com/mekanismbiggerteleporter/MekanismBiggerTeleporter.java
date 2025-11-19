package com.mekanismbiggerteleporter;

import com.mekanismbiggerteleporter.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MekanismBiggerTeleporter.MOD_ID)
public class MekanismBiggerTeleporter
{
    public static final String MOD_ID = "mekanismbiggerteleporter";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MekanismBiggerTeleporter()
    {
        PacketHandler.init();
    }

    public static ResourceLocation makeId(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {}
}
