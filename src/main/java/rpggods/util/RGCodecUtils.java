/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LightPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RGCodecUtils {

    /** Codec that accepts {@link Item}s or {@link ItemStack}s that have a single item and no tag **/
    public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(ForgeRegistries.ITEMS.getCodec(), ItemStack.CODEC)
            .xmap(either -> either.map(ItemStack::new, Function.identity()),
                    stack -> stack.getCount() == 1 && !stack.hasTag()
                            ? Either.left(stack.getItem())
                            : Either.right(stack));
    /** {@link Item} {@link HolderSet} codec **/
    public static final Codec<HolderSet<Item>> ITEM_HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ITEMS, ForgeRegistries.ITEMS.getCodec());

    /** {@link MinMaxBounds.Ints} codec **/
    public static final Codec<MinMaxBounds.Ints> INTS_DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min").forGetter(o -> Optional.ofNullable(o.getMin())),
            Codec.INT.optionalFieldOf("max").forGetter(o -> Optional.ofNullable(o.getMax()))
    ).apply(instance, (p1, p2) -> {
        if(p1.isPresent() && p2.isEmpty()) return MinMaxBounds.Ints.atLeast(p1.get());
        if(p1.isEmpty() && p2.isPresent()) return MinMaxBounds.Ints.atMost(p2.get());
        if(p1.isEmpty() && p2.isEmpty()) return MinMaxBounds.Ints.ANY;
        return MinMaxBounds.Ints.between(p1.get(), p2.get());
    }));
    /** {@link MinMaxBounds.Ints} or {@link Codec#INT} codec **/
    public static final Codec<MinMaxBounds.Ints> INTS_CODEC = Codec.either(Codec.INT, INTS_DIRECT_CODEC)
            .xmap(either -> either.map(MinMaxBounds.Ints::exactly, Function.identity()),
                    o -> (o.getMin() != null && o.getMax() != null && o.getMin().equals(o.getMax())) ? Either.left(o.getMin()) : Either.right(o));
    /** {@link MinMaxBounds.Ints} or {@link Codec#INT} codec that requires the value to be 0 or greater **/
    public static final Codec<MinMaxBounds.Ints> NON_NEGATIVE_INTS_CODEC = boundedIntCodec(0, Integer.MAX_VALUE);
    /** {@link MinMaxBounds.Ints} or {@link Codec#INT} codec that requires the value to be 1 or greater **/
    public static final Codec<MinMaxBounds.Ints> POSITIVE_INTS_CODEC = boundedIntCodec(1, Integer.MAX_VALUE);
    /** {@link LightPredicate} codec **/
    public static final Codec<LightPredicate> LIGHT_PREDICATE_CODEC = INTS_CODEC.xmap(composite -> new LightPredicate.Builder().setComposite(composite).build(), o -> o.composite);
    /** {@link EquipmentSlot} codec **/
    public static final Codec<EquipmentSlot> EQUIPMENT_SLOT_CODEC = Codec.STRING.comapFlatMap((string) -> {
        for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            if (equipmentslot.getName().equals(string)) {
                return DataResult.success(equipmentslot);
            }
        }
        return DataResult.error(() -> "Invalid slot \"" + string + "\"");
    }, EquipmentSlot::getName);
    /** {@link EnchantmentPredicate} codec **/
    public static final Codec<EnchantmentPredicate> ENCHANTMENT_PREDICATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.ENCHANTMENTS.getCodec().optionalFieldOf("enchantment").forGetter(o -> Optional.ofNullable(o.enchantment)),
            INTS_CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(o -> o.level)
    ).apply(instance, (enchantment, level) -> new EnchantmentPredicate(enchantment.orElse(null), level)));
    /** {@link EnchantmentPredicate} codec **/
    public static final Codec<NbtPredicate> NBT_PREDICATE_CODEC = Codec.STRING
            .comapFlatMap(string -> {
                try {
                    CompoundTag tag = TagParser.parseTag(string);
                    return DataResult.success(tag);
                } catch (CommandSyntaxException e) {
                    return DataResult.error(() -> "Failed to parse NBT predicate tag from string: \"" + string + "\"");
                }
            }, CompoundTag::getAsString)
            .xmap(NbtPredicate::new, o -> Optional.ofNullable(o.tag).orElse(new CompoundTag()));
    /** {@link ItemPredicate} codec **/
    public static final Codec<ItemPredicate> ITEM_PREDICATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(ForgeRegistries.Keys.ITEMS).optionalFieldOf("tag").forGetter(o -> Optional.ofNullable(o.tag)),
            RGCodecUtils.setOrElementCodec(ForgeRegistries.ITEMS.getCodec()).optionalFieldOf("items").forGetter(o -> Optional.ofNullable(o.items)),
            RGCodecUtils.INTS_CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(o -> o.count),
            RGCodecUtils.INTS_CODEC.optionalFieldOf("durability", MinMaxBounds.Ints.ANY).forGetter(o -> o.durability),
            RGCodecUtils.listOrElementCodec(RGCodecUtils.ENCHANTMENT_PREDICATE_CODEC).optionalFieldOf("enchantments", ImmutableList.copyOf(EnchantmentPredicate.NONE)).forGetter(o -> ImmutableList.copyOf(o.enchantments)),
            RGCodecUtils.listOrElementCodec(RGCodecUtils.ENCHANTMENT_PREDICATE_CODEC).optionalFieldOf("stored_enchantments", ImmutableList.copyOf(EnchantmentPredicate.NONE)).forGetter(o -> ImmutableList.copyOf(o.storedEnchantments)),
            ForgeRegistries.POTIONS.getCodec().optionalFieldOf("potion").forGetter(o -> Optional.ofNullable(o.potion)),
            RGCodecUtils.NBT_PREDICATE_CODEC.optionalFieldOf("nbt", NbtPredicate.ANY).forGetter(o -> o.nbt)
    ).apply(instance, (tag, items, count, durability, enchantments, storedEnchantments, potion, nbt) -> new ItemPredicate(tag.orElse(null), items.orElse(null), count, durability, enchantments.toArray(new EnchantmentPredicate[0]), storedEnchantments.toArray(new EnchantmentPredicate[0]), potion.orElse(null), nbt)));

    private static final BiMap<String, Pose> POSE_MAP = new ImmutableBiMap.Builder<String, Pose>()
            .putAll(Arrays
                    .stream(Pose.values())
                    .collect(Collectors.toMap(pose -> pose.name().toLowerCase(), pose -> pose)))
            .build();
    public static final Codec<Pose> POSE_CODEC = Codec.STRING.comapFlatMap((string) -> {
        final Pose pose = POSE_MAP.get(string);
        if(null == pose) {
            return DataResult.error(() -> "Invalid pose \"" + string + "\"");
        }
        return DataResult.success(pose);
    }, pose -> POSE_MAP.inverse().get(pose));

    /**
     * @param codec an element codec
     * @param <T> the element type
     * @return a codec that allows either a single element or a list of elements
     */
    public static <T> Codec<List<T>> listOrElementCodec(final Codec<T> codec) {
        return Codec.either(codec, codec.listOf())
                .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));
    }

    /**
     * @param codec an element codec
     * @param <T> the element type
     * @return a codec that allows either a single element or a list of elements
     */
    public static <T> Codec<Set<T>> setOrElementCodec(final Codec<T> codec) {
        return Codec.either(codec, codec.listOf().xmap(o -> (Set<T>) ImmutableSet.copyOf(o), ImmutableList::copyOf))
                .xmap(either -> either.map(ImmutableSet::of, Function.identity()),
                        set -> set.size() == 1 ? Either.left(set.iterator().next()) : Either.right(set));
    }

    /**
     * @param min the minimum value, inclusive
     * @param max the maximum value, inclusive
     * @return a codec that fails when the min or max of the int is outside the given range
     */
    public static Codec<MinMaxBounds.Ints> boundedIntCodec(final int min, final int max) {
        Function<MinMaxBounds.Ints, DataResult<MinMaxBounds.Ints>> function = (instance) -> {
            if (instance.getMin() != null && instance.getMin() < min) {
                return DataResult.error(() -> "Value too low. minimum " + min + "; provided [" + instance + "]");
            } else if(instance.getMax() != null && instance.getMax() > max) {
                return DataResult.error(() -> "Value too high. maximum " + max + "; provided [" + instance + "]");
            } else {
                return DataResult.success(instance);
            }
        };
        return INTS_CODEC.flatXmap(function, function);
    }
}
