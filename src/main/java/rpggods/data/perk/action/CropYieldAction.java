/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.IntProvider;
import rpggods.RGRegistry;

import java.util.List;

public class CropYieldAction extends PerkAction {

    public static final Codec<CropYieldAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(IntProvider.CODEC.fieldOf("amount").forGetter(o -> o.amount))
            .apply(instance, CropYieldAction::new));

    private final IntProvider amount;

    public CropYieldAction(boolean isHidden, IntProvider amount) {
        super(isHidden);
        this.amount = amount;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // the crop yield perk action is handled by the global loot modifier
        return true;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        // TODO crop yield perk action description using min and max
        return ImmutableList.of(Component.literal("" + amount.getMaxValue()));
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.CROP_YIELD.get();
    }
}
