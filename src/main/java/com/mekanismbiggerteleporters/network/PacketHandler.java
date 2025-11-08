package com.mekanismbiggerteleporters.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    private static final String PROTOCOL = "1";

    private PacketHandler() {}

    public static void init (IEventBus bus) {
        bus.addListener(PacketHandler::handlePacketRegistration);
    }

    public static void handlePacketRegistration(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                PacketPortalAreaFX.TYPE,
                PacketPortalAreaFX.STREAM_CODEC,
                PacketPortalAreaFX::handle
        );
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> handler(IPayloadHandler<T> handler) {
        return (payload, context) -> context.enqueueWork(() -> handler.handle(payload, context));
    };
}