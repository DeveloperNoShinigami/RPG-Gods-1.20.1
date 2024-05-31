/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import rpggods.RGRegistry;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public class WeatherCondition extends PerkCondition {

    public static final Codec<WeatherCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("raining").forGetter(o -> Optional.ofNullable(o.isRaining)),
            Codec.BOOL.optionalFieldOf("thundering").forGetter(o -> Optional.ofNullable(o.isThundering))
    ).apply(instance, WeatherCondition::new));

    @Nullable
    private final Boolean isRaining;
    @Nullable
    private final Boolean isThundering;

    public WeatherCondition(Optional<Boolean> raining, Optional<Boolean> thundering) {
        this.isRaining = raining.orElse(null);
        this.isThundering = thundering.orElse(null);
    }

    @Override
    public boolean test(PerkConditionContext context) {
        ServerLevel serverlevel = (ServerLevel) context.getLevel();
        if (this.isRaining != null && this.isRaining != serverlevel.isRaining()) {
            return false;
        } else {
            return this.isThundering == null || this.isThundering == serverlevel.isThundering();
        }
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.WEATHER.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        // create components
        final Component cRaining = Component.translatable("rpggods.perk_condition.weather.raining");
        final Component cThundering = Component.translatable("rpggods.perk_condition.weather.thundering");
        Component cRainingText = null;
        Component cThunderingText = null;
        if(isRaining != null) {
            cRainingText = isRaining ? cRaining : Component.translatable("rpggods.perk_condition.weather.not", cRaining);
        }
        if(isThundering != null) {
            cThunderingText = isThundering ? cThundering : Component.translatable("rpggods.perk_condition.weather.not", cThundering);
        }
        // create description
        if(cRainingText != null && cThunderingText != null) {
            return ImmutableList.of(Component.translatable("rpggods.perk_condition.weather.multiple", cRainingText, cThunderingText));
        } else if(cRainingText != null) {
            return ImmutableList.of(Component.translatable("rpggods.perk_condition.weather.single", cRainingText));
        } else if(cThunderingText != null) {
            return ImmutableList.of(Component.translatable("rpggods.perk_condition.weather.single", cThunderingText));
        } else {
            return ImmutableList.of(Component.translatable("rpggods.perk_condition.weather.never"));
        }
    }

    @Override
    public String toString() {
        return "weather {raining=" + Optional.ofNullable(isRaining) + ", thundering=" + Optional.ofNullable(isThundering) + "}";
    }
}
