/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import rpggods.RGRegistry;
import rpggods.RPGGods;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class SolarCycleCondition extends PerkCondition {

    public static final Codec<SolarCycleCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("day").forGetter(o -> Optional.ofNullable(o.day)),
            Codec.BOOL.optionalFieldOf("night").forGetter(o -> Optional.ofNullable(o.night))
    ).apply(instance, SolarCycleCondition::new));

    @Nullable
    private final Boolean day;
    @Nullable
    private final Boolean night;

    public SolarCycleCondition(Optional<Boolean> day, Optional<Boolean> night) {
        this.day = day.orElse(null);
        this.night = night.orElse(null);
        if(null == this.day && null == this.night) {
            RPGGods.LOGGER.error("Both 'day' and 'night' are undefined in SolarCyclePerkCondition");
        }
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // test for day time
        if(this.day != null && this.day == context.getLevel().isDay()) {
            return true;
        }
        // test for night time
        if(this.night != null && this.night == context.getLevel().isNight()) {
            return true;
        }
        // no checks passed
        return false;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.SOLAR_CYCLE.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        // TODO solar cycle condition description
        return ImmutableList.of();
    }

    @Override
    public String toString() {
        return "time {day=" + Optional.ofNullable(day) + ", night=" + Optional.ofNullable(night) + "}";
    }
}
