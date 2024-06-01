/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.LightPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.structure.Structure;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.util.ComponentUtils;
import rpggods.util.RGCodecUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Immutable
public class LocationCondition extends PerkCondition {

    public static final Codec<LocationCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DoublesPosition.CODEC.optionalFieldOf("position", DoublesPosition.ANY).forGetter(o -> o.position),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(o -> o.offset),
            Codec.BOOL.optionalFieldOf("day").forGetter(o -> Optional.ofNullable(o.day)),
            Codec.BOOL.optionalFieldOf("night").forGetter(o -> Optional.ofNullable(o.night)),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(o -> Optional.ofNullable(o.biome)),
            ResourceKey.codec(Registries.STRUCTURE).optionalFieldOf("structure").forGetter(o -> Optional.ofNullable(o.structure)),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(o -> Optional.ofNullable(o.dimension)),
            Codec.BOOL.optionalFieldOf("smokey").forGetter(o -> Optional.ofNullable(o.smokey)),
            RGCodecUtils.LIGHT_PREDICATE_CODEC.optionalFieldOf("light", LightPredicate.ANY).forGetter(o -> o.light),
            BlockPredicate.CODEC.optionalFieldOf("block", BlockPredicate.alwaysTrue()).forGetter(o -> o.block)
    ).apply(instance, LocationCondition::new));

    private final DoublesPosition position;
    private final Vec3i offset;
    @Nullable
    private final Boolean day;
    @Nullable
    private final Boolean night;
    @Nullable
    private final ResourceKey<Biome> biome;
    @Nullable
    private final ResourceKey<Structure> structure;
    @Nullable
    private final ResourceKey<Level> dimension;
    @Nullable
    private final Boolean smokey;
    private final LightPredicate light;
    private final BlockPredicate block;

    public LocationCondition(DoublesPosition position, Vec3i offset, Optional<Boolean> day, Optional<Boolean> night,
                             Optional<ResourceKey<Biome>> biome, Optional<ResourceKey<Structure>> structure,
                             Optional<ResourceKey<Level>> dimension, Optional<Boolean> smokey,
                             LightPredicate light, BlockPredicate block) {
        this.position = position;
        this.offset = offset;
        this.day = day.orElse(null);
        this.night = night.orElse(null);
        this.biome = biome.orElse(null);
        this.structure = structure.orElse(null);
        this.dimension = dimension.orElse(null);
        this.smokey = smokey.orElse(null);
        this.light = light;
        this.block = block;
    }


    @Override
    public boolean test(PerkConditionContext context) {
        final BlockPos blockpos = context.getPos().offset(offset);
        final ServerLevel level = (ServerLevel) context.getLevel();
        // validate position
        if (!position.x.matches(blockpos.getX())) {
            return false;
        }
        if (!position.y.matches(blockpos.getY())) {
            return false;
        }
        if (!position.z.matches(blockpos.getZ())) {
            return false;
        }
        // validate day
        if(this.day != null && this.day != context.getLevel().isDay()) {
            return false;
        }
        // validate night
        if(this.night != null && this.night != context.getLevel().isNight()) {
            return false;
        }
        // validate dimension
        if (this.dimension != null && !this.dimension.equals(level.dimension())) {
            return false;
        }
        // verify area is loaded
        boolean isLoaded = level.isLoaded(blockpos);
        if(!isLoaded) {
            return false;
        }
        // validate biome
        if(this.biome != null && !level.getBiome(blockpos).is(this.biome)) {
            return false;
        }
        // validate structure
        if(this.structure != null && !hasStructure(context.getRegistryAccess(), this.structure, level.structureManager().getAllStructuresAt(blockpos).keySet())) {
            return false;
        }
        // validate light
        if(!light.matches(level, blockpos)) {
            return false;
        }
        // validate block
        if(!block.test(level, blockpos)) {
            return false;
        }
        // validate smokey
        if(smokey != null && smokey != CampfireBlock.isSmokeyPos(level, blockpos)) {
            return false;
        }
        // all checks passed
        return true;
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.LOCATION.get();
    }

    @Override
    public Component createDescription(final RegistryAccess registryAccess) {
        // create description
        final List<Component> builder = new ArrayList<>();
        // offset
        if(!Vec3i.ZERO.equals(this.offset)) {
            builder.add(Component.translatable(PREFIX + "location.with_offset", this.offset.getX(), this.offset.getY(), this.offset.getZ()).withStyle(ChatFormatting.ITALIC));
        }
        // position
        if(this.position != DoublesPosition.ANY) {
            builder.addAll(this.position.getDescription());
        }
        // biome
        if(this.biome != null) {
            Component text = ComponentUtils.createResourceKeyDescription(this.biome);
            builder.add(Component.translatable(PREFIX + "location.biome", text));
        }
        // structure
        if(this.structure != null) {
            Component text = ComponentUtils.createResourceKeyDescription(this.structure);
            builder.add(Component.translatable(PREFIX + "location.structure", text));
        }
        // dimension
        if(this.dimension != null) {
            Component text = ComponentUtils.createResourceKeyDescription(this.dimension);
            builder.add(Component.translatable(PREFIX + "location.dimension", text));
        }
        // smokey
        if(this.smokey != null) {
            builder.add(Component.translatable(PREFIX + "location." + (this.smokey ? "smokey" : "not_smokey")));
        }
        // light
        if(this.light != LightPredicate.ANY) {
            final Component lightBoundsComponent = ComponentUtils.createBoundsComponent(this.light.composite.getMin(), this.light.composite.getMax());
            builder.add(Component.translatable(PREFIX + "location.light", lightBoundsComponent));
        }
        // join components
        final Component delimiter = Component.translatable("favor.perk.condition.and");
        return ComponentUtils.join(builder, delimiter);
    }

    private static boolean hasStructure(final RegistryAccess registryAccess, final ResourceKey<Structure> key, final Set<Structure> structures) {
        final Registry<Structure> registry = registryAccess.registryOrThrow(Registries.STRUCTURE);
        for(Structure structure : structures) {
            // load structure resource key
            Optional<ResourceKey<Structure>> resourceKey = registry.getResourceKey(structure);
            if(resourceKey.isEmpty()) continue;
            // check equality
            if(resourceKey.get().equals(key)) {
                return true;
            }
        }
        // all checks failed
        return false;
    }

    //// UTILITY CLASSES ////

    /**
     * Contains information about X, Y, and Z {@link MinMaxBounds.Doubles}
     **/
    public static final class DoublesPosition {

        public static final Codec<MinMaxBounds.Doubles> DOUBLES_DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("min").forGetter(o -> Optional.ofNullable(o.getMin())),
                Codec.DOUBLE.optionalFieldOf("max").forGetter(o -> Optional.ofNullable(o.getMax()))
        ).apply(instance, (p1, p2) -> {
            if(p1.isPresent() && p2.isEmpty()) return MinMaxBounds.Doubles.atLeast(p1.get());
            if(p1.isEmpty() && p2.isPresent()) return MinMaxBounds.Doubles.atMost(p2.get());
            if(p1.isEmpty() && p2.isEmpty()) return MinMaxBounds.Doubles.ANY;
            return MinMaxBounds.Doubles.between(p1.get(), p2.get());
        }));

        public static final Codec<MinMaxBounds.Doubles> DOUBLES_CODEC = Codec.either(Codec.DOUBLE, DOUBLES_DIRECT_CODEC)
                .xmap(either -> either.map(MinMaxBounds.Doubles::exactly, Function.identity()),
                        o -> (o.getMin() != null && o.getMax() != null && o.getMin().equals(o.getMax())) ? Either.left(o.getMin()) : Either.right(o));

        public static final Codec<DoublesPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DOUBLES_CODEC.optionalFieldOf("x").forGetter(o -> Optional.of(o.x)),
                DOUBLES_CODEC.optionalFieldOf("y").forGetter(o -> Optional.of(o.y)),
                DOUBLES_CODEC.optionalFieldOf("z").forGetter(o -> Optional.of(o.z))
        ).apply(instance, DoublesPosition::new));

        public static final DoublesPosition ANY = new DoublesPosition(Optional.empty(), Optional.empty(), Optional.empty());

        private final MinMaxBounds.Doubles x;
        private final MinMaxBounds.Doubles y;
        private final MinMaxBounds.Doubles z;

        private final List<Component> description;

        public DoublesPosition(Optional<MinMaxBounds.Doubles> x, Optional<MinMaxBounds.Doubles> y, Optional<MinMaxBounds.Doubles> z) {
            this.x = x.orElse(MinMaxBounds.Doubles.ANY);
            this.y = y.orElse(MinMaxBounds.Doubles.ANY);
            this.z = z.orElse(MinMaxBounds.Doubles.ANY);
            // create description
            final ImmutableList.Builder<Component> builder = ImmutableList.builder();
            if(!this.x.isAny()) {
                builder.add(createDescription("x", this.x));
            }
            if(!this.y.isAny()) {
                builder.add(createDescription("y", this.y));
            }
            if(!this.z.isAny()) {
                builder.add(createDescription("z", this.z));
            }
            this.description = builder.build();
        }

        private static Component createDescription(final String axis, final MinMaxBounds.Doubles bounds) {
            final Component boundsComponent = ComponentUtils.createBoundsComponent(bounds.getMin(), bounds.getMax());
            final Component axisComponent = Component.translatable(PREFIX + "location.position." + axis);
            return Component.translatable(PREFIX + "location.position", axisComponent, boundsComponent);
        }

        public MinMaxBounds.Doubles getX() {
            return x;
        }

        public MinMaxBounds.Doubles getY() {
            return y;
        }

        public MinMaxBounds.Doubles getZ() {
            return z;
        }

        public List<Component> getDescription() {
            return description;
        }
    }
}
