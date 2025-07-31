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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.AABB;
import rpggods.RGRegistry;
import rpggods.util.MobEffectProvider;
import rpggods.util.TargetType;

import java.util.List;

public class ArrowEffectAction extends PerkAction {

    public static final Codec<ArrowEffectAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(MobEffectProvider.CODEC.fieldOf("effect").forGetter(o -> o.mobEffectProvider))
            .apply(instance, ArrowEffectAction::new));

    private final MobEffectProvider mobEffectProvider;

    public ArrowEffectAction(boolean isHidden, MobEffectProvider mobEffectProvider) {
        super(isHidden);
        this.mobEffectProvider = mobEffectProvider;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        if(context.getEntity().isPresent() && context.getEntity().get() instanceof Arrow arrow) {
            // create the mob effect instance
            final RandomSource random = context.getRandom();
            final MobEffectInstance mobEffectInstance = mobEffectProvider.build(random);
            // add the effect to the arrow internal list
            arrow.addEffect(mobEffectInstance);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        return ImmutableList.of(mobEffectProvider.createDescription());
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.ARROW_EFFECT.get();
    }
}
