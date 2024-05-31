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
import rpggods.PerkDispatcher;
import rpggods.RGRegistry;

import javax.annotation.Nullable;
import java.util.Optional;

public class FunctionAction extends PerkAction {

    public static final Codec<FunctionAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("function").forGetter(o -> o.function))
            .and(Codec.STRING.optionalFieldOf("translation_key").forGetter(o -> Optional.ofNullable(o.translationKey)))
            .apply(instance, FunctionAction::new));

    private final ResourceLocation function;
    private final @Nullable String translationKey;

    public FunctionAction(boolean isHidden, ResourceLocation function, Optional<String> translationKey) {
        super(isHidden);
        this.function = function;
        this.translationKey = translationKey.orElse(null);
    }

    @Override
    public boolean apply(PerkActionContext context) {
        return PerkDispatcher.runFunction(context.getLevel(), context.getPlayer(), this.function);
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        if(this.translationKey != null) {
            return Component.translatable(this.translationKey);
        }
        return Component.translatable(PREFIX + "function" + SUFFIX + ".default");
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.FUNCTION.get();
    }
}
