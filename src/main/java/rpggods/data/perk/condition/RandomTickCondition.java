/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import rpggods.RGRegistry;

import javax.annotation.concurrent.Immutable;

@Immutable
public class RandomTickCondition extends PerkCondition {

    public static final RandomTickCondition INSTANCE = new RandomTickCondition();

    public static final Codec<RandomTickCondition> CODEC = Codec.unit(INSTANCE);

    public RandomTickCondition() {}

    @Override
    public boolean test(PerkConditionContext context) {
        return true;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.RANDOM_TICK.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        return Component.translatable(PREFIX + "random_tick");
    }

    @Override
    public String toString() {
        return "true";
    }
}
