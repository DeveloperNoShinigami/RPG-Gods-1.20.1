/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import rpggods.RGEvents;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.deity.Altar;
import rpggods.data.deity.Deity;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.FavorLevel;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.AffinityType;
import rpggods.data.perk.Perk;
import rpggods.data.tameable.ITameable;
import rpggods.util.FavorChangedEvent;
import rpggods.util.RGCodecUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class PerkAction {

    public static final Codec<PerkAction> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> RGRegistry.PERK_ACTION_TYPES_SUPPLIER.get().getCodec())
            .dispatch(PerkAction::getCodec, Function.identity());
    public static final Codec<List<PerkAction>> LIST_CODEC = RGCodecUtils.listOrElementCodec(DIRECT_CODEC);

    protected static final String PREFIX = "favor.perk.type.";
    protected static final String SUFFIX = ".description";

    private final boolean isHidden;
    private Component title;
    private Component description;

    //// CONSTRUCTOR ////

    public PerkAction(final boolean isHidden) {
        this.isHidden = isHidden;
    }

    //// ABSTRACT METHODS ////

    /**
     * @param context the {@link PerkActionContext} with parameters to facilitate the action
     * @return true if the perk action applied successfully and the cooldown should be updated
     */
    public abstract boolean apply(final PerkActionContext context);

    /**
     * @param registryAccess the Registry Access instance
     * @return the description of this perk action as a {@link Component}
     */
    public abstract Component createDescription(final RegistryAccess registryAccess);

    /**
     * @return the Codec used to encode/decode this {@link PerkAction}
     * @see rpggods.RGRegistry.PerkActionReg
     */
    public abstract Codec<? extends PerkAction> getCodec();

    //// METHODS ////

    /**
     * @param registryAccess the Registry Access instance
     * @return the title of this perk action as a {@link Component}
     */
    public Component createTitle(final RegistryAccess registryAccess) {
        final ResourceLocation typeId = RGRegistry.PERK_ACTION_TYPES_SUPPLIER.get().getKey(getCodec());
        return Component.translatable(PREFIX + typeId.getPath());
    }

    /**
     * @param registryAccess the registry access
     * @return a list of text components that describe this modifier condition
     */
    public Component getTitle(final RegistryAccess registryAccess) {
        if(null == title) {
            title = createTitle(registryAccess);
        }
        return title;
    }

    /**
     * @param registryAccess the registry access
     * @return the description of this perk action as a component
     */
    public Component getDescription(final RegistryAccess registryAccess) {
        if(null == description) {
            description = createDescription(registryAccess);
        }
        return description;
    }

    //// HELPER METHODS ////

    /**
     * Simplifies codec creation, especially if no other fields are added
     * @param instance the record codec builder with additional parameters, if any
     */
    protected static <T extends PerkAction> Products.P1<RecordCodecBuilder.Mu<T>, Boolean> codecStart(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkAction::isHidden));
    }

    /**
     * Formats the given float as a percentage. Example usage:
     * {@code input: -0.9, output: -90%};
     * {@code input: 0.25: output: +25%};
     * {@code input: 1.2: output: +120%}
     * @param percent a percent in the range {@code [-1.0, inf)}
     * @return a text component containing a signed percentage
     */
    public static Component createPercentageComponent(final float percent) {
        StringBuilder builder = new StringBuilder("");
        // add prefix
        if(!(percent < 0.0F)) {
            builder.append("+");
        }
        // add number and percent
        builder.append(
                String.format("%.2f", percent * 100.0F)
                .replace("0*$", "")
                .replace("\\.$", ""));
        builder.append("%");
        // create component
        return Component.literal(builder.toString());
    }

    /**
     * @param min the minimum value, can be null to indicate no minimum
     * @param max the maximum value, can be null to indicate no maximum
     * @return a component describing the min and max bounds
     */
    public static Component createBoundsComponent(final @Nullable Integer min, final @Nullable Integer max) {
        // TODO add min max bounds to lang file
        if(min != null && max != null) {
            // exact value
            if(min.equals(max)) {
                return Component.literal(min + "");
            }
            // between two defined values
            return Component.translatable("perk.bounds.between", min + "", max + "");
        }
        // below some value
        if(max != null) {
            return Component.translatable("perk.bounds.at_most", max + "");
        }
        // above some value
        if(min != null) {
            return Component.translatable("perk.bounds.at_least", min + "");
        }
        // any value
        return Component.translatable("perk.bounds.any");
    }

    //// GETTERS ////

    public boolean isHidden() {
        return isHidden;
    }

    //// METHODS ////

    @Override
    public String toString() {
        return "PerkData{" +
                "type=" + getClass().getSimpleName() +
                ", hidden=" + isHidden +
                "}";
    }

    @Deprecated
    public static enum Type implements StringRepresentable {
        @Deprecated FUNCTION("function"),
        @Deprecated POTION("potion"),
        @Deprecated SUMMON("summon"),
        @Deprecated ITEM("item"),
        @Deprecated FAVOR("favor"),
        @Deprecated AFFINITY("affinity"),
        @Deprecated ARROW_DAMAGE("arrow_damage"),
        @Deprecated ARROW_EFFECT("arrow_effect"),
        @Deprecated ARROW_COUNT("arrow_count"),
        @Deprecated OFFSPRING("offspring"),
        @Deprecated CROP_GROWTH("crop_growth"),
        @Deprecated CROP_HARVEST("crop_harvest"),
        @Deprecated AUTOSMELT("autosmelt"),
        @Deprecated UNSMELT("unsmelt"),
        @Deprecated SPECIAL_PRICE("special_price"),
        @Deprecated DURABILITY("durability"),
        @Deprecated DAMAGE("damage"),
        @Deprecated PATRON("patron"),
        @Deprecated UNLOCK("unlock"),
        @Deprecated ADD_DECAY("add_decay"),
        @Deprecated XP("xp");

        private final String name;

        private Type(final String id) {
            this.name = id;
        }

        public static DataResult<PerkAction.Type> fromString(String id) {
            for(final PerkAction.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error(() -> "Failed to parse perk data type '" + id + "'");
        }

        /**
         * @param data the data to pass to the translation key
         * @return Translation key for the description of this perk type, using the provided data
         */
        public Component getDisplayDescription(final Component data) {
            return Component.translatable("favor.perk.type." + getSerializedName() + ".description", data);
        }

        /**
         * @return Translation key for the name of this perk type
         */
        public MutableComponent getDisplayName() {
            return Component.translatable("favor.perk.type." + getSerializedName());
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
