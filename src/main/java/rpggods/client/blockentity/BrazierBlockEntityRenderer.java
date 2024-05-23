/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.client.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import rpggods.block.entity.BrazierBlockEntity;

public class BrazierBlockEntityRenderer implements BlockEntityRenderer<BrazierBlockEntity> {

    protected final BlockEntityRendererProvider.Context context;

    public BrazierBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(BrazierBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        final ItemStack itemstack = blockEntity.getItem(0);
        if (!itemstack.isEmpty()) {
            final float ticks = blockEntity.getLevel().getGameTime() + blockEntity.getBlockPos().hashCode() + partialTicks;
            final float speed = 0.125F;
            final float scale = 1.0F; //0.68F;
            poseStack.pushPose();
            // transforms
            final float offsetY = 0.065F * Mth.cos(ticks * speed);
            poseStack.translate(0.5D, 0.95D + offsetY, 0.5D);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.YP.rotation(ticks * speed * Mth.PI * 0.125F));
            // render the item stack
            Minecraft.getInstance().getItemRenderer().renderStatic(itemstack, ItemDisplayContext.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, blockEntity.getLevel(), 0);
            // finish rendering
            poseStack.popPose();
        }
    }
}
