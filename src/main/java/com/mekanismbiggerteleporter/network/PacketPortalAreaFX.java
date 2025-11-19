package com.mekanismbiggerteleporter.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketPortalAreaFX {

    private final BlockPos minPos;
    private final BlockPos maxPos;
    private final int particlesPerBlock;
    private final Direction portalDirection;

    public PacketPortalAreaFX(BlockPos minPos, BlockPos maxPos, int particlesPerBlock, Direction portalDirection) {
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.particlesPerBlock = particlesPerBlock;
        this.portalDirection = portalDirection;
    }

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
            Level world = ctx.get().getSender() != null
                    ? ctx.get().getSender().level()
                    : net.minecraft.client.Minecraft.getInstance().level;

            if (world == null) return;

            int xRange = msg.maxPos.getX() - msg.minPos.getX();
            int yRange = msg.maxPos.getY() - msg.minPos.getY();
            int zRange = msg.maxPos.getZ() - msg.minPos.getZ();

            Direction.Axis depthAxis;
            if (xRange <= yRange && xRange <= zRange) {
                depthAxis = Direction.Axis.X;
            } else if (yRange <= xRange && yRange <= zRange) {
                depthAxis = Direction.Axis.Y;
            } else {
                depthAxis = Direction.Axis.Z;
            }

            if (depthAxis == Direction.Axis.Z) {
                // XY
                int fixedZ = msg.minPos.getZ();
                for (int x = msg.minPos.getX(); x < msg.maxPos.getX(); x++) {
                    for (int y = msg.minPos.getY(); y < msg.maxPos.getY(); y++) {
                        msg.spawnParticlesAt(world, x + 0.5, y + 0.5, fixedZ + 0.5, depthAxis);
                    }
                }
            } else if (depthAxis == Direction.Axis.X) {
                // YZ
                int fixedX = msg.minPos.getX();
                for (int y = msg.minPos.getY(); y < msg.maxPos.getY(); y++) {
                    for (int z = msg.minPos.getZ(); z < msg.maxPos.getZ(); z++) {
                        msg.spawnParticlesAt(world, fixedX + 0.5, y + 0.5, z + 0.5, depthAxis);
                    }
                }
            } else {
                // XZ
                int fixedY = msg.minPos.getY();
                for (int x = msg.minPos.getX(); x < msg.maxPos.getX(); x++) {
                    for (int z = msg.minPos.getZ(); z < msg.maxPos.getZ(); z++) {
                        msg.spawnParticlesAt(world, x + 0.5, fixedY + 0.5, z + 0.5, depthAxis);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void spawnParticlesAt(Level world, double centerX, double centerY, double centerZ, Direction.Axis depthAxis) {
        for (int i = 0; i < particlesPerBlock; i++) {
            if (world.random.nextDouble() < 0.5) {
                double offsetX = world.random.nextDouble() * 1.5 - 0.25;
                double offsetY = world.random.nextDouble() * 1.5 - 0.25;
                double offsetZ = world.random.nextDouble() * 1.5 - 0.25;

                if (depthAxis == Direction.Axis.X) {
                    offsetX *= 0.5;
                } else if (depthAxis == Direction.Axis.Y) {
                    offsetY *= 0.5;
                } else if (depthAxis == Direction.Axis.Z) {
                    offsetZ *= 0.5;
                }

                double velocityX = (world.random.nextDouble() - 0.5) * 0.05;
                double velocityY = (world.random.nextDouble() - 0.5) * 0.05;
                double velocityZ = (world.random.nextDouble() - 0.5) * 0.05;
                world.addParticle(
                        ParticleTypes.PORTAL,
                        centerX + offsetX,
                        centerY + offsetY,
                        centerZ + offsetZ,
                        velocityX,
                        velocityY,
                        velocityZ
                );
            }
        }
    }

    public BlockPos getMinPos() {
        return minPos;
    }

    public BlockPos getMaxPos() {
        return maxPos;
    }

    public int getParticlesPerBlock() {
        return particlesPerBlock;
    }

    public Direction getPortalDirection() {
        return portalDirection;
    }
}