package com.mekanismbiggerteleporter.network;

import com.mekanismbiggerteleporter.MekanismBiggerTeleporter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_NAME = MekanismBiggerTeleporter.makeId("main");
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void init() {
        CHANNEL.registerMessage(
                nextId(), PacketPortalAreaFX.class,
                PacketPortalAreaFX::toBytes,
                PacketPortalAreaFX::fromBytes,
                PacketPortalAreaFX::handle
        );
    }

    public static <MSG> void sendToPlayersTrackingChunk(ServerLevel level, net.minecraft.core.BlockPos pos, MSG message) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4)
                ),
                message
        );
    }
}
