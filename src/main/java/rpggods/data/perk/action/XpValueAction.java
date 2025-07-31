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
import net.minecraft.world.entity.ExperienceOrb;
import rpggods.RGRegistry;

import java.util.List;

public class XpValueAction extends PerkAction {

    public static final Codec<XpValueAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(Codec.floatRange(0.0F, Integer.MAX_VALUE).fieldOf("multiplier").forGetter(o -> o.multiplier))
            .apply(instance, XpValueAction::new));

    private final float multiplier;

    public XpValueAction(boolean isHidden, float multiplier) {
        super(isHidden);
        this.multiplier = multiplier;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        if(context.getEntity().isPresent() && context.getEntity().get() instanceof ExperienceOrb xpOrb) {
            xpOrb.value = Math.round(xpOrb.value * multiplier);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        final Component percentage = createPercentageComponent(multiplier);
        return ImmutableList.of(percentage);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.XP.get();
    }
}
