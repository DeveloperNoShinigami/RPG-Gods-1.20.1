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
import rpggods.RGRegistry;

import javax.annotation.concurrent.Immutable;

@Immutable
public class TrueCondition extends PerkCondition {

    public static final TrueCondition INSTANCE = new TrueCondition();

    public static final Codec<TrueCondition> CODEC = Codec.unit(INSTANCE);

    public TrueCondition() {}

    @Override
    public boolean test(PerkConditionContext context) {
        return true;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.TRUE.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        return ImmutableList.of(Component.translatable("rpggods.perk_condition.true"));
    }

    @Override
    public String toString() {
        return "true";
    }
}
