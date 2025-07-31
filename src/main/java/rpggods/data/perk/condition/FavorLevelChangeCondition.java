/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class FavorLevelChangeCondition extends PerkCondition {

    public static final Codec<FavorLevelChangeCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelDirection.CODEC.fieldOf("direction").forGetter(o -> o.levelDirection),
            ResourceLocation.CODEC.optionalFieldOf("deity").forGetter(o -> Optional.ofNullable(o.deity))
    ).apply(instance, FavorLevelChangeCondition::new));

   private final LevelDirection levelDirection;
   private final ResourceLocation deity;

    public FavorLevelChangeCondition(LevelDirection levelDirection, Optional<ResourceLocation> deity) {
        this.levelDirection = levelDirection;
        this.deity = deity.orElse(null);
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // note: the deity and level direction are checked before this method is called
        return true;
    }

    public LevelDirection getLevelDirection() {
        return levelDirection;
    }

    @Nullable
    public ResourceLocation getDeity() {
        return deity;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.FAVOR_LEVEL_CHANGE.get();
    }

    @Override
    public List<Component> createDescription(final RegistryAccess registryAccess) {
        // TODO favor level change condition description
        return ImmutableList.of();
    }

    @Override
    public String toString() {
        return "level_change {direction=" + levelDirection.getSerializedName() + ", deity=" + Optional.ofNullable(deity) + "}";
    }

    public static enum LevelDirection implements StringRepresentable {
        LEVEL_UP("level_up"),
        LEVEL_DOWN("level_down");

        protected static final Codec<LevelDirection> CODEC = StringRepresentable.fromEnum(LevelDirection::values);

        private final String name;

        LevelDirection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
