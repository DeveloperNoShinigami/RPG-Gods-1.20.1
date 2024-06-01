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
public class FalseCondition extends PerkCondition {

    public static final FalseCondition INSTANCE = new FalseCondition();

    public static final Codec<FalseCondition> CODEC = Codec.unit(INSTANCE);

    public FalseCondition() {}

    @Override
    public boolean test(PerkConditionContext context) {
        return false;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.FALSE.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        return Component.translatable(PREFIX + "false");
    }

    @Override
    public String toString() {
        return "false";
    }
}
