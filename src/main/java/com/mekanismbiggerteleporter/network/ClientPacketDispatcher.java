package com.mekanismbiggerteleporter.network;

import java.util.Objects;
import java.util.function.Consumer;

public final class ClientPacketDispatcher {

    private static Consumer<PacketPortalAreaFX> portalAreaFxHandler = msg -> {};

    private ClientPacketDispatcher() {}

    public static void registerPortalAreaFxHandler(Consumer<PacketPortalAreaFX> handler) {
        portalAreaFxHandler = Objects.requireNonNull(handler);
    }

    public static void handlePortalAreaFx(PacketPortalAreaFX msg) {
        portalAreaFxHandler.accept(msg);
    }
}
