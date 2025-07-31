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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import rpggods.RGRegistry;

import java.util.List;

public class DamageAmountAction extends PerkAction {

    public static final Codec<DamageAmountAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(Codec.FLOAT.fieldOf("multiplier").forGetter(o -> o.multiplier))
            .apply(instance, DamageAmountAction::new));

    private final float multiplier;

    public DamageAmountAction(boolean isHidden, float multiplier) {
        super(isHidden);
        this.multiplier = multiplier;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        if(context.getEvent().isPresent() && context.getEvent().get() instanceof LivingHurtEvent event) {
            float amount = event.getAmount();
            event.setAmount(amount * multiplier);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        final Component percentage = createPercentageComponent(multiplier - 1.0F);
        return ImmutableList.of(percentage);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.DAMAGE.get();
    }
}
