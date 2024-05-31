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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

public class GiveItemAction extends PerkAction {

    public static final Codec<GiveItemAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(RGCodecUtils.ITEM_OR_STACK_CODEC.fieldOf("item").forGetter(o -> o.itemStack))
            .apply(instance, GiveItemAction::new));

    private final ItemStack itemStack;

    public GiveItemAction(boolean isHidden, ItemStack itemStack) {
        super(isHidden);
        this.itemStack = itemStack;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        final ItemStack itemStack = this.itemStack.copy();
        final Player player = context.getPlayer();
        // attempt to add directly to inventory
        if(player.getInventory().add(itemStack)) {
            return true;
        }
        // attempt to spawn as item entity
        ItemEntity entity = player.drop(itemStack, false);
        if(entity != null) {
            entity.setNoPickUpDelay();
            return true;
        }
        // all attempts failed
        return false;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        return Component.translatable(PREFIX + "item" + SUFFIX, itemStack.getCount(), itemStack.getHoverName());
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.ITEM.get();
    }
}
