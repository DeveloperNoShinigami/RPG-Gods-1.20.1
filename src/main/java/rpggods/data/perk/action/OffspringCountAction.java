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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.util.RGComponentUtils;
import rpggods.util.DeferredHolderSet;

public class OffspringCountAction extends PerkAction {

    public static final Codec<OffspringCountAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(DeferredHolderSet.codec(ForgeRegistries.Keys.ENTITY_TYPES).optionalFieldOf("entity", DeferredHolderSet.empty()).forGetter(o -> o.entityType))
            .and(IntProvider.CODEC.fieldOf("amount").forGetter(o -> o.amount))
            .apply(instance, OffspringCountAction::new));

    private final DeferredHolderSet<EntityType<?>> entityType;
    private final IntProvider amount;

    public OffspringCountAction(boolean isHidden, DeferredHolderSet<EntityType<?>> entityType, IntProvider amount) {
        super(isHidden);
        this.entityType = entityType;
        this.amount = amount;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        if(context.getEntity().isPresent() && context.getEntity().get() instanceof AgeableMob mob
                && context.getEvent().isPresent() && context.getEvent().get() instanceof BabyEntitySpawnEvent event
                && mob.level() instanceof ServerLevel level) {
            // validate entity type is in holder set or holder set is empty
            final HolderSet<EntityType<?>> holderSet = entityType.get(BuiltInRegistries.ENTITY_TYPE);
            Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType());
            if(holderSet.size() > 0 && !holderSet.contains(holder)) {
                return false;
            }
            // determine count
            final int offspringCount = this.amount.sample(context.getRandom());
            // do nothing when offspring count is 1
            if(offspringCount == 1) {
                return false;
            }
            // cancel the event when offspring count is less than 1
            if(offspringCount < 1) {
                event.setCanceled(true);
                // spawn particles
                Vec3 pos = mob.getEyePosition(1.0F);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 6, 0.5D, 0.5D, 0.5D, 0);
                return true;
            }
            // number of offspring is more than one, time to spawn additional mobs
            for(int i = 1; i < offspringCount; i++) {
                // create child entity
                AgeableMob bonusChild = (AgeableMob) mob.getType().create(level);
                if(bonusChild != null) {
                    // set up child entity
                    bonusChild.copyPosition(mob);
                    bonusChild.setBaby(true);
                    level.addFreshEntityWithPassengers(bonusChild);
                    ForgeEventFactory.onFinalizeSpawn(bonusChild, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.BREEDING, null, null);
                    // spawn particles
                    Vec3 pos = bonusChild.getEyePosition(1.0F);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final Component boundsComponent = RGComponentUtils.createBoundsComponent(amount.getMinValue(), amount.getMaxValue());
        return Component.translatable(PREFIX + "offspring_count" + SUFFIX, boundsComponent);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.OFFSPRING.get();
    }
}
