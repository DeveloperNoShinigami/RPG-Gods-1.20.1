/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import java.util.List;

public class ItemDurabilityAction extends PerkAction {

    public static final Codec<ItemDurabilityAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(RGCodecUtils.listOrElementCodec(RGCodecUtils.EQUIPMENT_SLOT_CODEC).fieldOf("slot").forGetter(o -> o.slots))
            .and(Codec.floatRange(-1.0F, 1.0F).fieldOf("percent").forGetter(o -> o.percent))
            .apply(instance, ItemDurabilityAction::new));

    private final List<EquipmentSlot> slots;
    private final float percent;

    public ItemDurabilityAction(boolean isHidden, List<EquipmentSlot> slots, float percent) {
        super(isHidden);
        this.slots = slots;
        this.percent = percent;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        final Player player = context.getPlayer();
        int affectedItems = 0;
        // iterate each defined equipment slot
        for(EquipmentSlot slot : slots) {
            ItemStack itemStack = player.getItemBySlot(slot);
            // verify item can be damaged
            if(itemStack.isEmpty() || !itemStack.isDamageableItem()) {
                continue;
            }
            // update number of affected items
            affectedItems++;
            // add or remove durability
            int deltaDamage = Math.round(percent * itemStack.getMaxDamage());
            // broadcast break event when total damage amount would exceed max damage
            if(itemStack.getDamageValue() + deltaDamage >= itemStack.getMaxDamage()) {
                itemStack.hurtAndBreak(itemStack.getMaxDamage(), player, e -> e.broadcastBreakEvent(slot));
                continue;
            }
            // otherwise, set damage value directly, negated so that positive values repair the item and negative values break it
            itemStack.setDamageValue(Math.max(0, itemStack.getDamageValue() - deltaDamage));
        }
        return affectedItems > 0;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        // TODO add slot(s) to item durability action description
        final Component percentage = createPercentageComponent(percent);
        return Component.translatable(PREFIX + "durability" + SUFFIX, percentage);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.DURABILITY.get();
    }
}
