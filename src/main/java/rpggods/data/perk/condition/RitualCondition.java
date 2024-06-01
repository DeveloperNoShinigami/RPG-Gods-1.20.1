/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.util.ComponentUtils;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class RitualCondition extends PerkCondition {

    public static final Codec<RitualCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RGCodecUtils.ITEM_PREDICATE_CODEC.fieldOf("item").forGetter(o -> o.itemPredicate)
    ).apply(instance, RitualCondition::new));

    private final ItemPredicate itemPredicate;

    public RitualCondition(ItemPredicate itemPredicate) {
        this.itemPredicate = itemPredicate;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // validate item stack exists
        final Optional<ItemStack> oStack = context.getItemStack();
        if(oStack.isEmpty()) {
            return false;
        }
        // validate item stack matches
        if(!itemPredicate.matches(oStack.get())) {
            return false;
        }
        // all checks passed
        return true;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.RITUAL.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        final Component itemPredicateComponent = ComponentUtils.createItemPredicateDescription(registryAccess, itemPredicate);
        return Component.translatable(PREFIX + "ritual", itemPredicateComponent);
    }

    @Override
    public String toString() {
        return "ritual {" + "item=" + itemPredicate.serializeToJson().getAsString() + "}";
    }

}
