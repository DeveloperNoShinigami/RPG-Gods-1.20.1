/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import java.util.List;
import java.util.function.Function;

public abstract class PerkCondition {

    public static final Codec<PerkCondition> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> RGRegistry.PERK_CONDITION_TYPES_SUPPLIER.get().getCodec())
            .dispatch(PerkCondition::getCodec, Function.identity());
    public static final Codec<List<PerkCondition>> LIST_CODEC = RGCodecUtils.listOrElementCodec(DIRECT_CODEC);

    protected static final String PREFIX = "favor.perk.condition.";

    private Component description;

    public PerkCondition() {
        // no op
    }

    /**
     * @param context the {@link PerkConditionContext} to test
     * @return true if the perk condition matches the given parameters
     */
    public abstract boolean test(final PerkConditionContext context);

    /**
     * @param registryAccess the Registry Access instance
     * @return the name of this perk condition as a {@link Component}
     */
    public abstract Component createDescription(final RegistryAccess registryAccess);

    /**
     * @return the Codec used to encode/decode this {@link PerkCondition}
     * @see rpggods.RGRegistry.PerkConditionReg
     */
    public abstract Codec<? extends PerkCondition> getCodec();

    /**
     * @param registryAccess the registry access
     * @return a list of text components that describe this modifier condition
     */
    public final Component getDescription(final RegistryAccess registryAccess) {
        if(null == description) {
            description = createDescription(registryAccess);
        }
        return description;
    }

    @Deprecated
    public static enum Type implements StringRepresentable {
        @Deprecated PATRON("patron"),
        @Deprecated BIOME("biome"),
        @Deprecated DAY("day"),
        @Deprecated NIGHT("night"),
        @Deprecated RANDOM_TICK("random_tick"),
        @Deprecated MAINHAND_ITEM("mainhand_item"),
        @Deprecated STRUCTURE("structure"),
        @Deprecated DIMENSION("dimension"),
        @Deprecated EFFECT_START("effect_start"),
        @Deprecated ENTITY_HURT_PLAYER("entity_hurt_player"),
        @Deprecated ENTITY_KILLED_PLAYER("entity_killed_player"),
        @Deprecated PLAYER_HURT_ENTITY("player_hurt_entity"),
        @Deprecated PLAYER_KILLED_ENTITY("player_killed_entity"),
        @Deprecated PLAYER_INTERACT_ENTITY("player_interact_entity"),
        @Deprecated PLAYER_INTERACT_BLOCK("player_interact_block"),
        @Deprecated PLAYER_RIDE_ENTITY("player_ride_entity"),
        @Deprecated PLAYER_CROUCHING("player_crouching"),
        @Deprecated RITUAL("ritual"),
        @Deprecated UNLOCKED("unlocked"),
        @Deprecated ENTER_COMBAT("enter_combat"),
        @Deprecated NEAR_ALTAR("near_altar"),
        @Deprecated LEVEL_UP("level_up"),
        @Deprecated LEVEL_DOWN("level_down");

        private final String name;

        private Type(final String id) {
            name = id;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
