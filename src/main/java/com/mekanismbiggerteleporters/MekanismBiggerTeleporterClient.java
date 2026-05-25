package com.mekanismbiggerteleporters;

import com.mekanismbiggerteleporters.client.ClientPacketHandler;
import com.mekanismbiggerteleporters.network.ClientPacketDispatcher;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MekanismBiggerTeleporter.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MekanismBiggerTeleporter.MOD_ID, value = Dist.CLIENT)
public class MekanismBiggerTeleporterClient {
    public MekanismBiggerTeleporterClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ClientPacketDispatcher.registerPortalAreaFxHandler(ClientPacketHandler::handlePortalAreaFx);
    }
}
