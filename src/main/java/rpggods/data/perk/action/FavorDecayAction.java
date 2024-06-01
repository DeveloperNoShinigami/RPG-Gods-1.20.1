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
import net.minecraft.resources.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.data.deity.Deity;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.FavorLevel;
import rpggods.util.RGComponentUtils;

public class FavorDecayAction extends PerkAction {

    public static final Codec<FavorDecayAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("deity").forGetter(o -> o.deity))
            .and(Codec.FLOAT.fieldOf("amount").forGetter(o -> o.amount))
            .apply(instance, FavorDecayAction::new));

    private final ResourceLocation deity;
    private final float amount;

    public FavorDecayAction(boolean isHidden, ResourceLocation deity, float amount) {
        super(isHidden);
        this.deity = deity;
        this.amount = amount;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // validate deity
        if(Deity.getRegistry(context.getRegistryAccess()).getOptional(this.deity).isEmpty()) {
            return false;
        }
        // load favor and validate enabled
        final FavorLevel favorLevel = context.getFavor().getFavor(this.deity);
        if(!favorLevel.isEnabled()) {
            return false;
        }
        // set decay rate
        favorLevel.setDecayRate(favorLevel.getDecayRate() + amount);
        return true;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final Component percentage = RGComponentUtils.createPercentageComponent(amount);
        final Component deityName = DeityContainer.getOrCreate(registryAccess, this.deity).getName();
        return Component.translatable(PREFIX + "favor_decay" + SUFFIX, percentage, deityName);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.FAVOR_DECAY.get();
    }
}
