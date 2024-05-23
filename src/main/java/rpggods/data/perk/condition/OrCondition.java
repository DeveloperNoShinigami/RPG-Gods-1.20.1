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
import java.util.ArrayList;
import java.util.List;

@Immutable
public class OrCondition extends PerkCondition {

    public static final Codec<OrCondition> CODEC = LIST_CODEC
            .xmap(OrCondition::new, o -> o.children)
            .fieldOf("value").codec();

    private final List<PerkCondition> children;

    public OrCondition(List<PerkCondition> children) {
        this.children = ImmutableList.copyOf(children);
    }


    @Override
    public boolean test(PerkConditionContext context) {
        for(PerkCondition child : children) {
            if(child.test(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.OR.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {

        final List<Component> builder = new ArrayList<>();
        for(PerkCondition child : children) {
            for (Component c : child.createDescription(registryAccess)) {
                builder.add(Component.literal("  ").append(c));
                builder.add(Component.translatable("rpggods.perk_condition.or"));
            }
        }
        // remove trailing element
        if(!builder.isEmpty()) {
            builder.remove(builder.size() - 1);
        }
        return builder;
    }

    @Override
    public String toString() {
        return "or {" + children.toString() + "}";
    }
}
