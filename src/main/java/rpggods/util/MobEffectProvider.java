/*
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package rpggods.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

public final class MobEffectProvider {

    public static final Codec<MobEffectProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.MOB_EFFECTS.getCodec().fieldOf("id").forGetter(o -> o.mobEffect),
            IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("duration", ConstantInt.of(0)).forGetter(o -> o.duration),
            IntProvider.codec(0, 255).optionalFieldOf("amplifier", ConstantInt.of(0)).forGetter(o -> o.amplifier),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(o -> o.ambient),
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(o -> o.visible),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(o -> o.showIcon)
    ).apply(instance, MobEffectProvider::new));

    private final MobEffect mobEffect;
    private final IntProvider duration;
    private final IntProvider amplifier;
    private final boolean ambient;
    private final boolean visible;
    private final boolean showIcon;

    public MobEffectProvider(MobEffect mobEffect, IntProvider duration, IntProvider amplifier, boolean ambient, boolean visible, boolean showIcon) {
        this.mobEffect = mobEffect;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.visible = visible;
        this.showIcon = showIcon;
    }

    /**
     * @param random a random instance
     * @return a new {@link MobEffectInstance} with randomized duration and amplifier values
     */
    public MobEffectInstance build(final RandomSource random) {
        return new MobEffectInstance(mobEffect, duration.sample(random), amplifier.sample(random), ambient, visible, showIcon);
    }

    /**
     * @return a text component with the name of the mob effect and the potency range
     */
    public Component createDescription() {
        final Component potency;
        if(amplifier.getMinValue() == amplifier.getMaxValue()) {
            potency = Component.translatable("potion.potency." + amplifier.getMinValue());
        } else {
            Component minPotency = Component.translatable("potion.potency." + amplifier.getMinValue());
            Component maxPotency = Component.translatable("potion.potency." + amplifier.getMaxValue());
            // TODO add translation key "%s - %s"
            potency = Component.translatable("rpggods.potion.potency.multiple", minPotency, maxPotency);
        }
        return Component.translatable(mobEffect.getDescriptionId())
                .append(" ")
                .append(potency);
    }
}
