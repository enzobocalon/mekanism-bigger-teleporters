package com.mekanismbiggerteleporters.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PacketPortalAreaFX(BlockPos minPos, BlockPos maxPos, int particlesPerBlock, Direction portalDirection) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketPortalAreaFX> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mekanismbiggerteleporters", "portal_area_fx"));

    public static final StreamCodec<ByteBuf, PacketPortalAreaFX> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PacketPortalAreaFX::minPos,
                    BlockPos.STREAM_CODEC, PacketPortalAreaFX::maxPos,
                    ByteBufCodecs.VAR_INT, PacketPortalAreaFX::particlesPerBlock,
                    Direction.STREAM_CODEC, PacketPortalAreaFX::portalDirection,
                    PacketPortalAreaFX::new
            );

    @NotNull
    @Override
    public CustomPacketPayload.Type<PacketPortalAreaFX> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        ClientPacketDispatcher.handlePortalAreaFx(this);
    }
}
