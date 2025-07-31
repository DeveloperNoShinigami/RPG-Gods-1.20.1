/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import java.util.List;

public class ArrowCountAction extends PerkAction {

    public static final Codec<ArrowCountAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(IntProvider.NON_NEGATIVE_CODEC.fieldOf("amount").forGetter(o -> o.amount))
            .apply(instance, ArrowCountAction::new));

    private final IntProvider amount;

    public ArrowCountAction(boolean isHidden, IntProvider amount) {
        super(isHidden);
        this.amount = amount;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        if(context.getEntity().isPresent() && context.getEntity().get() instanceof AbstractArrow arrow) {
            int arrowCount = amount.sample(context.getRandom());
            double motionScale = 0.8;
            for(int i = 0; i < arrowCount; i++) {
                AbstractArrow copy = (AbstractArrow) arrow.getType().create(context.getLevel());
                copy.copyPosition(arrow);
                copy.setOwner(arrow.getOwner());
                copy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                copy.setDeltaMovement(arrow.getDeltaMovement().multiply(
                        (Math.random() * 2.0D - 1.0D) * motionScale,
                        (Math.random() * 2.0D - 1.0D) * motionScale,
                        (Math.random() * 2.0D - 1.0D) * motionScale));
                context.getLevel().addFreshEntity(copy);
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        if(amount.getMinValue() == amount.getMaxValue()) {
            return ImmutableList.of(Component.translatable("rpggods.perk_action.arrow_count.constant", amount.getMaxValue()));
        }
        return ImmutableList.of(Component.translatable("rpggods.perk_action.arrow_count.range", amount.getMinValue(), amount.getMaxValue()));
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.ARROW_COUNT.get();
    }
}
