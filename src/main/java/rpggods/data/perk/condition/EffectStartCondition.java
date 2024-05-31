/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;

public class EffectStartCondition extends PerkCondition {

    public static final Codec<EffectStartCondition> CODEC = ForgeRegistries.MOB_EFFECTS.getCodec()
            .xmap(EffectStartCondition::new, o -> o.effect)
            .fieldOf("effect").codec();

    private final MobEffect effect;

    public EffectStartCondition(MobEffect effect) {
        this.effect = effect;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        return context.getData().isPresent() && context.getData().get().equals(ForgeRegistries.MOB_EFFECTS.getKey(effect));
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        return ImmutableList.of(effect.getDisplayName());
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.EFFECT_START.get();
    }
}
