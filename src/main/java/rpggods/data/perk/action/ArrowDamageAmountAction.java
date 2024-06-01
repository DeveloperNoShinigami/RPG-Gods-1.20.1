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
import net.minecraft.world.entity.projectile.Arrow;
import rpggods.RGRegistry;
import rpggods.util.ComponentUtils;

public class ArrowDamageAmountAction extends PerkAction {

    public static final Codec<ArrowDamageAmountAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(Codec.FLOAT.fieldOf("multiplier").forGetter(o -> o.multiplier))
            .apply(instance, ArrowDamageAmountAction::new));

    private final float multiplier;

    public ArrowDamageAmountAction(boolean isHidden, float multiplier) {
        super(isHidden);
        this.multiplier = multiplier;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        if(context.getEntity().isPresent() && context.getEntity().get() instanceof Arrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
            return true;
        }
        return false;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final Component percentage = ComponentUtils.createPercentageComponent(multiplier - 1.0F);
        return Component.translatable(PREFIX + "arrow_damage" + SUFFIX, percentage);
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.ARROW_DAMAGE.get();
    }
}
