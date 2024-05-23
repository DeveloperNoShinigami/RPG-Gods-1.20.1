/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class ItemCondition extends PerkCondition {

    // TODO refactor uses of the MAINHAND Perk Condition Type to support other equipment slots

    public static final Codec<ItemCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RGCodecUtils.ITEM_PREDICATE_CODEC.fieldOf("item").forGetter(o -> o.itemPredicate),
            RGCodecUtils.EQUIPMENT_SLOT_CODEC.optionalFieldOf("slot", EquipmentSlot.MAINHAND).forGetter(o -> o.equipmentSlot)
    ).apply(instance, ItemCondition::new));

    private final ItemPredicate itemPredicate;
    private final EquipmentSlot equipmentSlot;

    public ItemCondition(ItemPredicate itemPredicate, EquipmentSlot equipmentSlot) {
        this.itemPredicate = itemPredicate;
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        final ItemStack itemStack = context.getPlayer().getItemBySlot(this.equipmentSlot);
        return itemPredicate.matches(itemStack);
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.ITEM.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        // TODO item condition description
        return ImmutableList.of();
    }

    @Override
    public String toString() {
        return "item {" + "slot=" + equipmentSlot.getName() + ", item=" + itemPredicate.serializeToJson().getAsString() + "}";
    }

}
