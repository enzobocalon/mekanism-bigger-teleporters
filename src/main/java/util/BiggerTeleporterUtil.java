package util;

import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;

public class BiggerTeleporterUtil {

    public static final int MAX_PARTICLES_TOTAL = 8192;
    public static final int MAX_PARTICLES_PER_BLOCK = 30;

    /**
     * Gets the cached frame width from the TileEntityTeleporter through reflection.
     *
     * @param tile The teleporter tile entity
     * @return The frame width, or 3 (default) if unable to retrieve
     */
    public static int getFrameWidth(TileEntityTeleporter tile) {
        try {
            Field field = tile.getClass().getDeclaredField("cachedFrameWidth");
            field.setAccessible(true);
            int width = field.getInt(tile);
            return width > 0 ? width : 3;
        } catch (Exception e) {
            return 3; // Default Mekanism size
        }
    }

    /**
     * Gets the cached frame height from the TileEntityTeleporter through reflection.
     *
     * @param tile The teleporter tile entity
     * @return The frame height, or 4 (default) if unable to retrieve
     */
    public static int getFrameHeight(TileEntityTeleporter tile) {
        try {
            Field field = tile.getClass().getDeclaredField("cachedFrameHeight");
            field.setAccessible(true);
            int height = field.getInt(tile);
            return height > 0 ? height : 4;
        } catch (Exception e) {
            return 4; // Default Mekanism size
        }
    }

    public static double calculateTeleportersDistance(TileEntityTeleporter tile) {
        TeleporterFrequency frequency = tile.getFrequencyComponent().getFrequency(FrequencyType.TELEPORTER);
        if (frequency == null) {
            return -1;
        }

        GlobalPos tilePos = GlobalPos.of(tile.getLevel().dimension(), tile.getBlockPos());
        GlobalPos targetTeleporter = frequency.getClosestCoords(tilePos);

        if (!tilePos.dimension().equals(targetTeleporter.dimension())) {
            return -1;
        }

        BlockPos thisTeleporterPos = tilePos.pos();
        BlockPos targetTeleporterPos = targetTeleporter.pos();

        double dx = targetTeleporterPos.getX() - thisTeleporterPos.getX();
        double dy = targetTeleporterPos.getY() - thisTeleporterPos.getY();
        double dz = targetTeleporterPos.getZ() - thisTeleporterPos.getZ();

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static int calculatePortalArea(AABB box) {
        int sizeX = (int) Math.ceil(box.maxX - box.minX);
        int sizeY = (int) Math.ceil(box.maxY - box.minY);
        int sizeZ = (int) Math.ceil(box.maxZ - box.minZ);

        return Math.max(Math.max(sizeX * sizeY, sizeX * sizeZ), sizeY * sizeZ);
    }


    public static int getParticlesCount(TileEntityTeleporter tile, AABB box) {
        int portalArea = calculatePortalArea(box);
        double teleportersDistance = calculateTeleportersDistance(tile);

        int particlesPerBlock = MAX_PARTICLES_TOTAL / portalArea;

        if (teleportersDistance > 0 && teleportersDistance <= 32) {
            particlesPerBlock = (int) (particlesPerBlock * 0.75);
        }

        return Math.max(1, Math.min(particlesPerBlock, MAX_PARTICLES_PER_BLOCK));
    }

}
