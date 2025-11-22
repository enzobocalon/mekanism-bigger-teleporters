package com.mekanismbiggerteleporters.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.client.render.tileentity.RenderTeleporter;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mekanismbiggerteleporters.util.BiggerTeleporterUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Mixin(value = RenderTeleporter.class, remap = false)
@NothingNullByDefault
public class RenderTeleporterMixin {

    @Unique
    private static final Map<String, Model3D> extendedModelCache = new HashMap<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderExtendedPortal(TileEntityTeleporter tile, float partialTick, PoseStack matrix,
                                      MultiBufferSource renderer, int light, int overlayLight,
                                      net.minecraft.util.profiling.ProfilerFiller profiler,
                                      CallbackInfo ci) {
        try {
            int width = BiggerTeleporterUtil.getFrameWidth(tile);
            int height = BiggerTeleporterUtil.getFrameHeight(tile);

            // Mekanism Default
            if (width == 3 && height == 4) {
                return;
            }


            Model3D model = getExtendedOverlayModel(
                    tile.frameDirection(),
                    tile.frameRotated(),
                    width,
                    height
            );

            MekanismRenderer.renderObject(
                    model,
                    matrix,
                    renderer.getBuffer(Sheets.translucentCullBlockSheet()),
                    MekanismRenderer.getColorARGB(tile.getColor(), 0.75F),
                    LightTexture.FULL_BRIGHT,
                    overlayLight,
                    FaceDisplay.FRONT,
                    getCamera(),
                    tile.getBlockPos()
            );

            ci.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private Model3D getExtendedOverlayModel(@Nullable Direction direction, boolean rotated,
                                                              int width, int height) {
        if (direction == null) {
            direction = Direction.UP;
        }

        String cacheKey = direction + "_" + rotated + "_" + width + "_" + height;
        Model3D model = extendedModelCache.get(cacheKey);

        if (model == null) {
            model = new Model3D().setTexture(MekanismRenderer.teleporterPortal);
            Axis renderAxis = direction.getAxis().isHorizontal() ? Axis.Y : rotated ? Axis.X : Axis.Z;

            for (Direction side : Direction.values()) {
                model.setSideRender(direction, side.getAxis() == renderAxis);
            }

            // Frame dimensions
            float halfWidth = (width - 1) / 2.0f;
            float depth = height - 2;
            float frameOffset = 0.01f;

            // Depth (portal height)
            float depthMin, depthMax;
            if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
                depthMin = 1.0f + frameOffset;
                depthMax = 1.0f + depth + frameOffset;
            } else {
                depthMin = -depth - frameOffset;
                depthMax = -frameOffset;
            }

            float widthMin = -halfWidth + 1.0f + frameOffset;
            float widthMax = halfWidth + frameOffset;

            switch (direction.getAxis()) {
                case X -> {
                    setExtendedDimensions(rotated, model::zBounds, model::yBounds, widthMin, widthMax);
                    model.xBounds(depthMin, depthMax);
                }
                case Y -> {
                    setExtendedDimensions(rotated, model::zBounds, model::xBounds, widthMin, widthMax);
                    model.yBounds(depthMin, depthMax);
                }
                case Z -> {
                    setExtendedDimensions(rotated, model::xBounds, model::yBounds, widthMin, widthMax);
                    model.zBounds(depthMin, depthMax);
                }
            }

            extendedModelCache.put(cacheKey, model);
        }

        return model;
    }

    @Unique
    private void setExtendedDimensions(boolean rotated, Model3D.ModelBoundsSetter setter1, Model3D.ModelBoundsSetter setter2,
                                       float widthMin, float widthMax) {
        if (rotated) {
            setExtendedDimensions(false, setter2, setter1, widthMin, widthMax);
        } else {
            setter1.set(0.46F, 0.54F);
            setter2.set(widthMin, widthMax);
        }
    }


    @Unique
    private net.minecraft.client.Camera getCamera() {
        try {
            Field field = RenderTeleporter.class.getSuperclass().getDeclaredField("camera");
            field.setAccessible(true);
            return (net.minecraft.client.Camera) field.get(this);
        } catch (Exception e) {
            return net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        }
    }

    @Inject(method = "resetCachedModels", at = @At("RETURN"))
    private static void clearExtendedCache(CallbackInfo ci) {
        extendedModelCache.clear();
    }
}