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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import rpggods.RGRegistry;
import rpggods.data.deity.Altar;
import rpggods.entity.AltarEntity;
import rpggods.util.DeferredHolderSet;
import rpggods.util.RGCodecUtils;

import java.util.List;
import java.util.Optional;

public class NearAltarCondition extends PerkCondition {

    private static final int MAX_DISTANCE = 64;

    public static final Codec<NearAltarCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(RGRegistry.Keys.ALTARS).optionalFieldOf("altar", DeferredHolderSet.empty()).forGetter(o -> o.altar),
            RGCodecUtils.INTS_CODEC.optionalFieldOf("distance", MinMaxBounds.Ints.between(0, 4)).forGetter(o -> o.distance)
    ).apply(instance, NearAltarCondition::new));

    private final DeferredHolderSet<Altar> altar;
    private final MinMaxBounds.Ints distance;

    public NearAltarCondition(DeferredHolderSet<Altar> altar, MinMaxBounds.Ints distance) {
        this.altar = altar;
        this.distance = distance;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // load altar holder set
        final Registry<Altar> altarRegistry = Altar.getRegistry(context.getRegistryAccess());
        final HolderSet<Altar> altarHolderSet = altar.get(altarRegistry);
        // determine min and max distances
        final int minDistance = Math.max(0, Optional.ofNullable(distance.getMin()).orElse(Integer.MIN_VALUE));
        final int maxDistance = Math.min(MAX_DISTANCE, Optional.ofNullable(distance.getMax()).orElse(Integer.MAX_VALUE));
        final int minDistanceSq = minDistance * minDistance;
        // create AABB for the maximum distance
        final AABB aabb = new AABB(context.getPos()).inflate(maxDistance);
        // query all altars within max distance
        final List<AltarEntity> altarEntityList = context.getLevel().getEntitiesOfClass(AltarEntity.class, aabb, entity -> {
            // validate minimum distance
            final double distanceSq = entity.distanceToSqr(context.getPlayer());
            if(distanceSq < minDistanceSq) {
                return false;
            }
            // validate altar
            final Optional<Altar> oAltar = altarRegistry.getOptional(entity.getAltar());
            if(oAltar.isEmpty()) {
                return false;
            }
            // validate altar is in holder set
            final Holder<Altar> holder = altarRegistry.wrapAsHolder(oAltar.get());
            if(!altarHolderSet.contains(holder)) {
                return false;
            }
            // all checks passed
            return true;
        });
        // pass if list is non-empty
        return !altarEntityList.isEmpty();
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        // TODO near altar condition description
        return ImmutableList.of();
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.NEAR_ALTAR.get();
    }
}
