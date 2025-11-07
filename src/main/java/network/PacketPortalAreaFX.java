package network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
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
        Level world = context.player().level();

        int xRange = maxPos.getX() - minPos.getX();
        int yRange = maxPos.getY() - minPos.getY();
        int zRange = maxPos.getZ() - minPos.getZ();

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
            int fixedZ = minPos.getZ();
            for (int x = minPos.getX(); x < maxPos.getX(); x++) {
                for (int y = minPos.getY(); y < maxPos.getY(); y++) {
                    spawnParticlesAt(world, x + 0.5, y + 0.5, fixedZ + 0.5, depthAxis);
                }
            }
        } else if (depthAxis == Direction.Axis.X) {
            // YZ
            int fixedX = minPos.getX();
            for (int y = minPos.getY(); y < maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z < maxPos.getZ(); z++) {
                    spawnParticlesAt(world, fixedX + 0.5, y + 0.5, z + 0.5, depthAxis);
                }
            }
        } else {
            // XZ
            int fixedY = minPos.getY();
            for (int x = minPos.getX(); x < maxPos.getX(); x++) {
                for (int z = minPos.getZ(); z < maxPos.getZ(); z++) {
                    spawnParticlesAt(world, x + 0.5, fixedY + 0.5, z + 0.5, depthAxis);
                }
            }
        }
    }

    private int spawnParticlesAt(Level world, double centerX, double centerY, double centerZ, Direction.Axis depthAxis) {
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
        return particlesPerBlock;
    }
}