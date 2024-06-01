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
import rpggods.util.RGComponentUtils;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

@Immutable
public class AndCondition extends PerkCondition {

    public static final Codec<AndCondition> CODEC = LIST_CODEC
            .xmap(AndCondition::new, o -> o.children)
            .fieldOf("value").codec();

    private final List<PerkCondition> children;

    public AndCondition(List<PerkCondition> children) {
        this.children = ImmutableList.copyOf(children);
    }

    @Override
    public boolean test(PerkConditionContext context) {
        for(PerkCondition child : children) {
            if(!child.test(context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.AND.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        // collect list of child descriptions
        final List<Component> list = new ArrayList<>(children.size());
        for(PerkCondition child : children) {
            list.add(child.createDescription(registryAccess));
        }
        // join with "and" delimiter
        final Component delimiter = Component.translatable(PREFIX + "and");
        return RGComponentUtils.join(list, delimiter);
    }

    @Override
    public String toString() {
        return "and {" + children.toString() + "}";
    }
}
