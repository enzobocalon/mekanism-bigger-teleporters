package com.mekanismbiggerteleporter.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PacketPortalAreaFX(BlockPos minPos, BlockPos maxPos, int particlesPerBlock, Direction portalDirection) {

    public static PacketPortalAreaFX fromBytes(FriendlyByteBuf buf) {
        BlockPos minPos = buf.readBlockPos();
        BlockPos maxPos = buf.readBlockPos();
        int particlesPerBlock = buf.readVarInt();
        Direction portalDirection = buf.readEnum(Direction.class);
        return new PacketPortalAreaFX(minPos, maxPos, particlesPerBlock, portalDirection);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(minPos);
        buf.writeBlockPos(maxPos);
        buf.writeVarInt(particlesPerBlock);
        buf.writeEnum(portalDirection);
    }

    public static void handle(PacketPortalAreaFX msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketDispatcher.handlePortalAreaFx(msg);
        });
        ctx.get().setPacketHandled(true);
    }
}
