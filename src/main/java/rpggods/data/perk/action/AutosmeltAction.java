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
import rpggods.RGRegistry;

import java.util.List;

public class AutosmeltAction extends PerkAction {

    public static final Codec<AutosmeltAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .apply(instance, AutosmeltAction::new));

    public AutosmeltAction(boolean isHidden) {
        super(isHidden);
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // the autosmelt perk action is handled by the global loot modifier
        return true;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        return ImmutableList.of();
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.AUTOSMELT.get();
    }
}
