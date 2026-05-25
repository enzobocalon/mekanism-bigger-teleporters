package com.mekanismbiggerteleporters.client;

import com.mekanismbiggerteleporters.network.PacketPortalAreaFX;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void handlePortalAreaFx(PacketPortalAreaFX msg) {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        int xRange = msg.maxPos().getX() - msg.minPos().getX();
        int yRange = msg.maxPos().getY() - msg.minPos().getY();
        int zRange = msg.maxPos().getZ() - msg.minPos().getZ();

        Direction.Axis depthAxis;
        if (xRange <= yRange && xRange <= zRange) {
            depthAxis = Direction.Axis.X;
        } else if (yRange <= xRange && yRange <= zRange) {
            depthAxis = Direction.Axis.Y;
        } else {
            depthAxis = Direction.Axis.Z;
        }

        if (depthAxis == Direction.Axis.Z) {
            int fixedZ = msg.minPos().getZ();
            for (int x = msg.minPos().getX(); x < msg.maxPos().getX(); x++) {
                for (int y = msg.minPos().getY(); y < msg.maxPos().getY(); y++) {
                    spawnParticlesAt(msg, world, x + 0.5, y + 0.5, fixedZ + 0.5, depthAxis);
                }
            }
        } else if (depthAxis == Direction.Axis.X) {
            int fixedX = msg.minPos().getX();
            for (int y = msg.minPos().getY(); y < msg.maxPos().getY(); y++) {
                for (int z = msg.minPos().getZ(); z < msg.maxPos().getZ(); z++) {
                    spawnParticlesAt(msg, world, fixedX + 0.5, y + 0.5, z + 0.5, depthAxis);
                }
            }
        } else {
            int fixedY = msg.minPos().getY();
            for (int x = msg.minPos().getX(); x < msg.maxPos().getX(); x++) {
                for (int z = msg.minPos().getZ(); z < msg.maxPos().getZ(); z++) {
                    spawnParticlesAt(msg, world, x + 0.5, fixedY + 0.5, z + 0.5, depthAxis);
                }
            }
        }
    }

    private static void spawnParticlesAt(PacketPortalAreaFX msg, Level world, double centerX, double centerY,
                                         double centerZ, Direction.Axis depthAxis) {
        for (int i = 0; i < msg.particlesPerBlock(); i++) {
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
}
