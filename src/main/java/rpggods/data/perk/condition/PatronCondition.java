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
import net.minecraft.resources.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.data.deity.Altar;
import rpggods.data.deity.Deity;

import java.util.List;
import java.util.Optional;

public class PatronCondition extends PerkCondition {

    public static final Codec<PatronCondition> CODEC = ResourceLocation.CODEC
            .xmap(PatronCondition::new, o -> o.deity)
            .fieldOf("deity").codec();

    private final ResourceLocation deity;

    public PatronCondition(ResourceLocation deity) {
        this.deity = deity;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // verify deity exists
        Optional<Deity> oDeity = Deity.getRegistry(context.getRegistryAccess()).getOptional(deity);
        if(oDeity.isEmpty()) {
            return false;
        }
        // verify patron exists
        if(context.getFavor().getPatron().isEmpty()) {
            return false;
        }
        // verify patron matches
        if(!deity.equals(context.getFavor().getPatron().get())) {
            return false;
        }
        // all checks passed
        return true;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        return ImmutableList.of(Component.translatable(Altar.createTranslationKey(deity)));
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.PATRON.get();
    }
}
