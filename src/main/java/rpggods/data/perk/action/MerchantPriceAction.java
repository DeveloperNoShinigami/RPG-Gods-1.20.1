/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.util.DeferredHolderSet;

public class MerchantPriceAction extends PerkAction {

    public static final Codec<MerchantPriceAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(DeferredHolderSet.codec(ForgeRegistries.Keys.ENTITY_TYPES).optionalFieldOf("entity", DeferredHolderSet.empty()).forGetter(o -> o.entityType))
            .and(Codec.INT.fieldOf("amount").forGetter(o -> o.amount))
            .apply(instance, MerchantPriceAction::new));

    private final DeferredHolderSet<EntityType<?>> entityType;
    private final int amount;

    public MerchantPriceAction(boolean isHidden, DeferredHolderSet<EntityType<?>> entityType, int amount) {
        super(isHidden);
        this.entityType = entityType;
        this.amount = amount;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // verify amount
        if(amount == 0) {
            return false;
        }
        // verify entity
        final Entity entity = context.getEntity().orElse(null);
        if(entity != null && entity instanceof Merchant merchant) {
            // verify entity in holder set
            final HolderSet<EntityType<?>> holderSet = entityType.get(BuiltInRegistries.ENTITY_TYPE);
            final Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(context.getEntity().get().getType());
            if(holderSet.size() > 0 && !holderSet.contains(holder)) {
                return false;
            }
            // amounts over 64 indicate the price is way too high and the event should be canceled entirely
            if(amount > 64 && context.getEvent().isPresent()) {
                // mark the event as canceled
                context.getEvent().get().setCanceled(true);
                // spawn angry particles
                Vec3 pos = entity.getEyePosition(1.0F);
                context.getLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 4, 0.5D, 0.5D, 0.5D, 0);
                // cause villagers to shake heads and play a sound
                if(entity instanceof AbstractVillager villager) {
                    villager.setUnhappyCounter(40);
                    villager.playSound(SoundEvents.VILLAGER_NO, 0.5F, 1.0F);
                }
                return true;
            }
            // modify merchant offers
            final boolean isAddition = amount > 0;
            for(MerchantOffer offer : merchant.getOffers()) {
                int specialPriceDiff = offer.getSpecialPriceDiff();
                // cap special price diff to avoid stacking modifiers
                if((isAddition && specialPriceDiff < amount) || (!isAddition && specialPriceDiff > amount)) {
                    offer.setSpecialPriceDiff(amount);
                }
            }
            return !merchant.getOffers().isEmpty();
        }
        return false;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        return Component.translatable(PREFIX + "merchant_price" + SUFFIX, amount);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.MERCHANT_PRICE.get();
    }
}
