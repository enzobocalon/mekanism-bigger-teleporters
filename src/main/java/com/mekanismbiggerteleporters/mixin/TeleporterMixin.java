package com.mekanismbiggerteleporters.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import network.PacketPortalAreaFX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import util.BiggerTeleporterUtil;

@Mixin(value = TileEntityTeleporter.class, remap = false)
public abstract class TeleporterMixin extends TileEntityMekanism {

    public TeleporterMixin(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super(null, pos, state);
    }

    @Shadow
    private boolean frameRotated;

    @Shadow
    protected abstract boolean isFrame(Long2ObjectMap<ChunkAccess> chunkMap,
                                       BlockPos.MutableBlockPos pos,
                                       Object2BooleanMap<BlockPos> cachedIsFrame,
                                       int xOffset, int yOffset, int zOffset);

    @Shadow public abstract AABB getTeleporterBoundingBox(@NotNull Direction frameDirection);

    @Unique
    private int cachedFrameWidth = -1;

    @Unique
    private int cachedFrameHeight = -1;

    @Unique
    private int lastCheckTick = 0;

    /**
     * Find the normal direction of the teleporter surface.
     */
    @Unique
    private static @Nullable Direction calculateTeleporterNormalDirection(AABB box, BlockPos target) {
        double X = box.maxX - box.minX;
        double Y = box.maxY - box.minY;
        double Z = box.maxZ - box.minZ;

        Direction normalDir = null;

        if (Z < X && Z < Y) {
            normalDir = (target.getZ() > box.minZ + Z / 2) ? Direction.SOUTH : Direction.NORTH;
        } else if (X < Y && X < Z) {
            normalDir = (target.getX() > box.minX + X / 2) ? Direction.EAST : Direction.WEST;
        } else if (Y < X && Y < Z) {
            normalDir = (target.getY() > box.minY + Y / 2) ? Direction.UP : Direction.DOWN;
        }

        return normalDir;
    }

    /**
     * Align player rotation when teleporting.
     */
    @Inject(method = "alignPlayer", at = @At("HEAD"), cancellable = true)
    private static void alignPlayerForBiggerPortals(ServerPlayer player, BlockPos target, TileEntityTeleporter teleporter, CallbackInfoReturnable<Float> cir) {
        Direction side = null;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Level level = teleporter.getLevel();
        Direction frameDir = teleporter.frameDirection();

        if (frameDir == null) {
            return;
        }
        if (level != null) {
            AABB box = teleporter.getTeleporterBoundingBox(frameDir);

            if (box != null) {
                Direction normal = calculateTeleporterNormalDirection(box, target);
                if (normal != null) {
                    mutable.setWithOffset(target, normal.getStepX(), normal.getStepY(), normal.getStepZ());
                    if (level.isEmptyBlock(mutable)) {
                        side = normal;
                    } else {
                        Direction opposite = normal.getOpposite();
                        mutable.setWithOffset(target, opposite.getStepX(), opposite.getStepY(), opposite.getStepZ());
                        if (level.isEmptyBlock(mutable)) {
                            side = opposite;
                        }
                    }
                }
            }
        }

        float yaw;
        if (side == null) {
            yaw = player.getYRot();
        } else {
            yaw = switch (side) {
                case NORTH -> 180.0F;
                case SOUTH -> 0.0F;
                case WEST -> 90.0F;
                case EAST -> 270.0F;
                default -> player.getYRot();
            };
        }

        cir.setReturnValue(yaw);
    }


    /**
         * Detects frame direction and size for bigger teleporters.
         */
    @Inject(method = "getFrameDirection", at = @At("HEAD"), cancellable = true)
    private void getExtendedFrameDirection(CallbackInfoReturnable<Direction> cir) {
        Long2ObjectMap<ChunkAccess> chunkMap = new Long2ObjectArrayMap<>(3);
        Object2BooleanMap<BlockPos> cachedIsFrame = new Object2BooleanOpenHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.values()) {
            // Tests sizes from 3x4 to 23x23 (always odd for width - teleporter must be in center)
            for (int width = 3; width <= 23; width += 2) {
                for (int height = 4; height <= 23; height ++) {
                    if (hasRectangularFrame(chunkMap, pos, cachedIsFrame, direction, false, width, height)) {
                        this.frameRotated = false;
                        this.cachedFrameWidth = width;
                        this.cachedFrameHeight = height;
                        cir.setReturnValue(direction);
                        return;
                    } else if (hasRectangularFrame(chunkMap, pos, cachedIsFrame, direction, true, width, height)) {
                        this.frameRotated = true;
                        this.cachedFrameWidth = width;
                        this.cachedFrameHeight = height;
                        cir.setReturnValue(direction);
                        return;
                    }
                }
            }
            chunkMap.clear();
            cachedIsFrame.clear();
        }

        cir.setReturnValue(null);
    }

    /**
     * Checks for a rectangular frame of given width and height in the specified direction.
     * width (perpendicular to direction), ex: 3, 5, 7... 23
     * height = altura (direction), ex: 4, 6, 8... 24
     */
    @Unique
    private boolean hasRectangularFrame(Long2ObjectMap<ChunkAccess> chunkMap,
                                        BlockPos.MutableBlockPos pos,
                                        Object2BooleanMap<BlockPos> cachedIsFrame,
                                        Direction direction,
                                        boolean rotated,
                                        int width,
                                        int height) {
        int alternatingX = 0, alternatingY = 0, alternatingZ = 0;

        if (rotated) {
            if (direction.getAxis() == Direction.Axis.Z) {
                alternatingX = 1;
            } else {
                alternatingZ = 1;
            }
        } else if (direction.getAxis() == Direction.Axis.Y) {
            alternatingX = 1;
        } else {
            alternatingY = 1;
        }

        int xComp = direction.getStepX();
        int yComp = direction.getStepY();
        int zComp = direction.getStepZ();

        int halfWidth = width / 2;
        int depth = height - 1;

        // Checks each depth layer
        for (int d = 0; d <= depth; d++) {
            int xBase = d * xComp;
            int yBase = d * yComp;
            int zBase = d * zComp;

            if (d == 0) {
                // Initial layer (next to the teleporter)
                for (int w = -halfWidth; w <= halfWidth; w++) {
                    if (w == 0) continue; // Skip the center (teleporter)

                    if (!isFrame(chunkMap, pos, cachedIsFrame,
                            xBase + (w * alternatingX),
                            yBase + (w * alternatingY),
                            zBase + (w * alternatingZ))) {
                        return false;
                    }
                }
            } else if (d == depth) {
                // Final layer (bottom of the frame)
                for (int w = -halfWidth; w <= halfWidth; w++) {
                    if (!isFrame(chunkMap, pos, cachedIsFrame,
                            xBase + (w * alternatingX),
                            yBase + (w * alternatingY),
                            zBase + (w * alternatingZ))) {
                        return false;
                    }
                }
            } else {
                // Final layer (bottom of the frame)
                if (!isFrame(chunkMap, pos, cachedIsFrame,
                        xBase - (halfWidth * alternatingX),
                        yBase - (halfWidth * alternatingY),
                        zBase - (halfWidth * alternatingZ))) {
                    return false;
                }
                if (!isFrame(chunkMap, pos, cachedIsFrame,
                        xBase + (halfWidth * alternatingX),
                        yBase + (halfWidth * alternatingY),
                        zBase + (halfWidth * alternatingZ))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculates extended bounding box for bigger teleporters.
     */
    @Inject(method = "getTeleporterBoundingBox", at = @At("HEAD"), cancellable = true)
    private void getExtendedBoundingBox(@NotNull Direction frameDirection,
                                        CallbackInfoReturnable<AABB> cir) {
        detectCurrentSizeCached(frameDirection);

        // Always apply custom bounding box, regardless of size
        // Calculates perpendicular axes
        int alternatingX = 0, alternatingY = 0, alternatingZ = 0;

        if (frameRotated) {
            if (frameDirection.getAxis() == Direction.Axis.Z) {
                alternatingX = 1;
            } else {
                alternatingZ = 1;
            }
        } else if (frameDirection.getAxis() == Direction.Axis.Y) {
            alternatingX = 1;
        } else {
            alternatingY = 1;
        }

        int halfWidth = cachedFrameWidth / 2;
        int depth = cachedFrameHeight - 1;

        // Starting point (1 block inside the frame)
        BlockPos start = worldPosition.relative(frameDirection, 1);

        // Calculates perpendicular limits (portal width)
        // The portal goes from -(halfWidth-1) to +(halfWidth-1)
        // because the external frames occupy -halfWidth and +halfWidth
        int widthOffset = halfWidth - 1;

        if (widthOffset < 1) widthOffset = 1;

        BlockPos corner1 = start.offset(
                -widthOffset * alternatingX,
                -widthOffset * alternatingY,
                -widthOffset * alternatingZ
        );

        // End point in depth (depth - 1 because the bottom frame is depth)
        BlockPos end = worldPosition.relative(frameDirection, depth - 1);

        // Ensure that the depth is at least 2 blocks
        if (depth < 2) {
            end = worldPosition.relative(frameDirection, 2);
        }

        BlockPos corner2 = end.offset(
                widthOffset * alternatingX,
                widthOffset * alternatingY,
                widthOffset * alternatingZ
        );

        AABB boundingBox = AABB.encapsulatingFullBlocks(corner1, corner2);
        cir.setReturnValue(boundingBox);
    }

    @Unique
    private void detectCurrentSizeCached(Direction direction) {
        int currentTick = this.level != null ? (int)this.level.getGameTime() : 0;

        if (cachedFrameWidth == -1 || currentTick - lastCheckTick > 20) {
            detectCurrentSize(direction);
            lastCheckTick = currentTick;
        }
    }

    @Unique
    private void detectCurrentSize(Direction direction) {
        Long2ObjectMap<ChunkAccess> chunkMap = new Long2ObjectArrayMap<>(3);
        Object2BooleanMap<BlockPos> cachedIsFrame = new Object2BooleanOpenHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int width = 23; width >= 3; width -= 2) {
            for (int height = 23; height >= 4; height--) {
                if (hasRectangularFrame(chunkMap, pos, cachedIsFrame, direction, frameRotated, width, height)) {
                    this.cachedFrameWidth = width;
                    this.cachedFrameHeight = height;
                    return;
                }
            }
        }

        // Mekanism Default
        this.cachedFrameWidth = 3;
        this.cachedFrameHeight = 4;
    }

    @Inject(method = "sendTeleportParticles", at = @At("HEAD"), cancellable = true)
    private void sendExtendedParticles(CallbackInfo ci) {
        try {
            TileEntityTeleporter tile = (TileEntityTeleporter) (Object) this;
            Direction frameDirection = tile.frameDirection();

            if (frameDirection == null || tile.getLevel() == null) {
                return;
            }

            detectCurrentSizeCached(frameDirection);

            int width = cachedFrameWidth;
            int height = cachedFrameHeight;

          //If its default mekanism portal size, proceed as normal
            if (width == 3 && height == 4) {
                return;
            }

            ci.cancel();

            AABB box = getTeleporterBoundingBox(frameDirection);
            int particlesCount = BiggerTeleporterUtil.getParticlesCount(tile, box);

            BlockPos minPos = new BlockPos((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ));
            BlockPos maxPos = new BlockPos((int) Math.ceil(box.maxX), (int) Math.ceil(box.maxY), (int) Math.ceil(box.maxZ));

            PacketDistributor.sendToPlayersTrackingChunk(
                    (net.minecraft.server.level.ServerLevel) tile.getLevel(),
                    new net.minecraft.world.level.ChunkPos(worldPosition),
                    new PacketPortalAreaFX(minPos, maxPos, particlesCount, tile.frameDirection())
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
