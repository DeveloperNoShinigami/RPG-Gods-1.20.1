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
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import rpggods.RGRegistry;
import rpggods.util.MobEffectProvider;
import rpggods.util.TargetType;

import java.util.List;

public class PotionAction extends PerkAction {

    public static final Codec<PotionAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(MobEffectProvider.CODEC.fieldOf("effect").forGetter(o -> o.mobEffectProvider))
            .and(TargetType.CODEC.optionalFieldOf("target", TargetType.SELF).forGetter(o -> o.target))
            .and(FloatProvider.codec(0.0F, 64.0F).optionalFieldOf("radius", ConstantFloat.of(0)).forGetter(o -> o.radius))
            .apply(instance, PotionAction::new));

    private final MobEffectProvider mobEffectProvider;
    private final TargetType target;
    private final FloatProvider radius;

    public PotionAction(boolean isHidden, MobEffectProvider mobEffectProvider, TargetType target, FloatProvider radius) {
        super(isHidden);
        this.mobEffectProvider = mobEffectProvider;
        this.target = target;
        this.radius = radius;
    }

    private static boolean applyMobEffect(final Player player, final TargetType target, final FloatProvider radius, final MobEffectInstance mobEffectInstance) {
        switch (target) {
            case AREA:
                // determine distance to query entities
                float distance = radius.sample(player.getRandom());
                if(!(distance > 0)) {
                    return false;
                }
                // query nearby entities, excluding the player
                AABB aabb = new AABB(player.blockPosition()).inflate(distance);
                List<LivingEntity> entities = player.level().getNearbyEntities(LivingEntity.class, TargetingConditions.DEFAULT, player, aabb);
                // add effects to each entity
                for(LivingEntity e : entities) {
                    e.addEffect(new MobEffectInstance(mobEffectInstance));
                }
                return !entities.isEmpty();
            case ENEMY:
                LivingEntity enemy = player.getLastAttacker();
                return enemy != null && enemy.addEffect(mobEffectInstance);
            case SELF:
                return player.addEffect(mobEffectInstance);
        }
        return false;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // create the mob effect instance
        final RandomSource random = context.getRandom();
        final MobEffectInstance mobEffectInstance = mobEffectProvider.build(random);
        // add the effect to the player
        return applyMobEffect(context.getPlayer(), target, radius, mobEffectInstance);
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        return ImmutableList.of(mobEffectProvider.createDescription());
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.POTION.get();
    }
}
