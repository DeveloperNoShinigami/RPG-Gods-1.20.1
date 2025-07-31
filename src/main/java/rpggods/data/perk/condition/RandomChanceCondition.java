/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import rpggods.RGRegistry;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class RandomChanceCondition extends PerkCondition {

    public static final Codec<RandomChanceCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(o -> o.chance),
            FloatProvider.codec(0.0F, 1.0F).optionalFieldOf("provider", UniformFloat.of(0.0F, 1.0F)).forGetter(o -> o.floatProvider)
    ).apply(instance, RandomChanceCondition::new));

    private final float chance;
    private final FloatProvider floatProvider;

    public RandomChanceCondition(float chance, FloatProvider floatProvider) {
        this.chance = chance;
        this.floatProvider = floatProvider;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // validate chance
        if(!(chance > 0)) {
            return false;
        }
        // sample random number
        final float sample = floatProvider.sample(context.getRandom());
        // pass when the sampled number is less than the provided number
        return sample < chance;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.CHANCE.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        final String sChance = String.format("%.4f", chance * 100.0D).replaceAll("0*$", "").replaceAll("\\.$", "");
        return ImmutableList.of(Component.translatable("rpggods.perk_condition.random_chance", sChance));
    }

    @Override
    public String toString() {
        return "chance {" + chance + "}";
    }
}
