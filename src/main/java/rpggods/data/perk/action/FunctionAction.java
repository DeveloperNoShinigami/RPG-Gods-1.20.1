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
import net.minecraft.resources.ResourceLocation;
import rpggods.RGEvents;
import rpggods.RGRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class FunctionAction extends PerkAction {

    public static final Codec<FunctionAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("function").forGetter(o -> o.function))
            .and(Codec.STRING.optionalFieldOf("translation_key").forGetter(o -> Optional.ofNullable(o.descriptionKey)))
            .apply(instance, FunctionAction::new));

    private final ResourceLocation function;
    private final @Nullable String descriptionKey;

    public FunctionAction(boolean isHidden, ResourceLocation function, Optional<String> descriptionKey) {
        super(isHidden);
        this.function = function;
        this.descriptionKey = descriptionKey.orElse(null);
    }

    @Override
    public boolean apply(PerkActionContext context) {
        return RGEvents.runFunction(context.getLevel(), context.getPlayer(), this.function);
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        if(this.descriptionKey != null) {
            return ImmutableList.of(Component.translatable(this.descriptionKey));
        }
        return ImmutableList.of(Component.translatable("favor.perk.type.function.description.default"));
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.FUNCTION.get();
    }
}
