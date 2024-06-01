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
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import rpggods.RGRegistry;
import rpggods.util.RGCodecUtils;

import java.util.List;
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
