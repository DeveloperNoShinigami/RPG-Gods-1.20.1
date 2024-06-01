/*
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package rpggods.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class RGComponentUtils {

    /**
     * @param resourceKey a resource key
     * @param <T> the resource key type
     * @return the text representation of the resource key location
     */
    public static <T> Component createResourceKeyDescription(ResourceKey<T> resourceKey) {
        return Component.literal(resourceKey.location().toString()).withStyle(ChatFormatting.GRAY);
    }

    /**
     * @param holderSet a holder set
     * @param toText a function to convert from the holder set element type to a component
     * @param <T> the holder set type
     * @return a list of components describing the holder set
     */
    public static <T> List<Component> createHolderSetDescription(final Registry<T> registry, final HolderSet<T> holderSet, final Function<T, Component> toText) {
        final List<Component> list = new ArrayList<>();
        Either<TagKey<T>, List<Holder<T>>> unwrapped = holderSet.unwrap();
        unwrapped.ifLeft(e -> list.add(Component.translatable("favor.perk.holder_set.tag", Component.literal("#" + e.location()).withStyle(ChatFormatting.GRAY))));
        unwrapped.ifRight(holderList -> {
            for(Holder<T> holder : holderList) {
                list.addAll(createHolderDescription(registry, holder, toText));
            }
        });
        return list;
    }

    /**
     * @param registry the registry
     * @param holder a holder
     * @param toText a function to convert from the holder element type to a component
     * @param <T> the holder type
     * @return a list of components describing the holder
     */
    public static <T> List<Component> createHolderDescription(final Registry<T> registry, final Holder<T> holder, final Function<T, Component> toText) {
        final List<Component> list = new ArrayList<>();
        Either<ResourceKey<T>, T> unwrappedHolder = holder.unwrap();
        unwrappedHolder.ifLeft(id -> list.add(toText.apply(registry.get(id))));
        unwrappedHolder.ifRight(o -> list.add(toText.apply(o)));
        return list;
    }

    public static Component createItemPredicateDescription(RegistryAccess registryAccess, ItemPredicate itemPredicate) {
        // create items component
        Component items = Component.empty();
        if(itemPredicate.potion != null) {
            // use potion item name
            items = PotionUtils.setPotion(new ItemStack(Items.POTION), itemPredicate.potion).getHoverName();
        } else if(itemPredicate.storedEnchantments.length > 0) {
            // join enchantment names from list
            final List<Component> enchantmentDescriptions = new ArrayList<>(itemPredicate.storedEnchantments.length);
            for(EnchantmentPredicate enchantment : itemPredicate.storedEnchantments) {
                if(null == enchantment.enchantment) continue;
                Component bounds = createBoundsComponent(enchantment.level.getMin(), enchantment.level.getMax());
                enchantmentDescriptions.add(Component
                        .translatable(enchantment.enchantment.getDescriptionId())
                        .withStyle(ChatFormatting.BLUE)
                        .append(CommonComponents.SPACE)
                        .append(bounds));
            }
            final Component delimiter = Component.translatable("favor.perk.condition.and");
            // use enchanted book item name with required enchantments
            items = Component.translatable("favor.perk.item_predicate.enchanted_book", Items.ENCHANTED_BOOK.getDescription(), join(enchantmentDescriptions, delimiter));
        } else if(itemPredicate.items != null) {
            // join item names from list
            final List<Component> itemDescriptions = new ArrayList<>(itemPredicate.items.size());
            itemPredicate.items.forEach(i -> itemDescriptions.add(i.getDescription()));
            final Component delimiter = Component.translatable("favor.perk.condition.or");
            items = join(itemDescriptions, delimiter);
        } else if(itemPredicate.tag != null) {
            // use item tag as name
            items = Component.translatable("favor.perk.holder_set.tag", Component.literal("#" + itemPredicate.tag.location()).withStyle(ChatFormatting.GRAY));
        } else {
            // any item (in practice this should never happen)
            items = Component.translatable("favor.perk.item_predicate.any_item");
        }
        // create with count component
        if(!itemPredicate.count.isAny()) {
            final Component count = createBoundsComponent(itemPredicate.count.getMin(), itemPredicate.count.getMax());
            return Component.translatable("favor.perk.item_predicate.with_count", count, items);
        }
        // no count component
        return items;
    }

    /**
     * @param list a list of components to join
     * @param delimiter the component to insert between each element in the list
     * @return a single component containing all elements in the list, separated by the delimiter and a space
     */
    public static Component join(final List<Component> list, final Component delimiter) {
        // hardcoded check for single element list
        if(list.size() == 1) {
            return list.get(0);
        }
        // create component to modify
        Component builder = Component.empty();
        // add a space, then the component, then the delimiter
        for(Component c : list) {
            builder.getSiblings().add(CommonComponents.SPACE);
            builder.getSiblings().add(c);
            builder.getSiblings().add(delimiter);
        }
        // remove trailing entry
        if(!builder.getSiblings().isEmpty()) {
            builder.getSiblings().remove(builder.getSiblings().size() - 1);
        }
        return builder;
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
    public static Component createBoundsComponent(final @Nullable Number min, final @Nullable Number max) {
        if(min != null && max != null) {
            // exact value
            if(min.equals(max)) {
                return Component.literal(min + "");
            }
            // between two defined values
            return Component.translatable("favor.perk.bounds.between", min + "", max + "");
        }
        // below some value
        if(max != null) {
            return Component.translatable("favor.perk.bounds.at_most", max + "");
        }
        // above some value
        if(min != null) {
            return Component.translatable("favor.perk.bounds.at_least", min + "");
        }
        // any value
        return Component.translatable("favor.perk.bounds.any");
    }
}
