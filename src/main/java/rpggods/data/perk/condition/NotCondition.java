/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import rpggods.RGRegistry;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

@Immutable
public class NotCondition extends PerkCondition {

    public static final Codec<NotCondition> CODEC = DIRECT_CODEC
            .xmap(NotCondition::new, o -> o.child)
            .fieldOf("value").codec();

    private final PerkCondition child;

    public NotCondition(PerkCondition child) {
        this.child = child;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        return !child.test(context);
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.NOT.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        final List<Component> builder = new ArrayList<>();
        builder.add(Component.translatable("rpggods.perk_condition.not").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        for(Component c : child.createDescription(registryAccess)) {
            builder.add(Component.literal("  ").append(c));
        }
        return builder;
    }

    @Override
    public String toString() {
        return "not {" + child.toString() + "}";
    }
}
