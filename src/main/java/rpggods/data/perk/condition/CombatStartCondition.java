/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGEvents;
import rpggods.RGRegistry;
import rpggods.util.RGComponentUtils;
import rpggods.util.DeferredHolderSet;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;

@Immutable
public class CombatStartCondition extends PerkCondition {

    public static final Codec<CombatStartCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(ForgeRegistries.Keys.ENTITY_TYPES).optionalFieldOf("entity", DeferredHolderSet.empty()).forGetter(o -> o.entityType)
    ).apply(instance, CombatStartCondition::new));

    private final DeferredHolderSet<EntityType<?>> entityType;

    public CombatStartCondition(DeferredHolderSet<EntityType<?>> entityType) {
        this.entityType = entityType;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        final HolderSet<EntityType<?>> holderSet = entityType.get(BuiltInRegistries.ENTITY_TYPE);
        // verify entity exists
        if(holderSet.size() > 0 && context.getEntity().isEmpty()) {
            return false;
        }
        // verify holder set is empty or entity type is in holder set
        final Optional<Holder<EntityType<?>>> oEntityType = ForgeRegistries.ENTITY_TYPES.getHolder(context.getEntity().get().getType());
        if(holderSet.size() > 0 && (oEntityType.isEmpty() || !holderSet.contains(oEntityType.get()))) {
            return false;
        }
        // verify combat started
        if(context.getPlayer().getCombatTracker().getCombatDuration() > RGEvents.COMBAT_TIMER) {
            return false;
        }
        // all checks passed
        return true;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final HolderSet<EntityType<?>> holderSet = entityType.get(BuiltInRegistries.ENTITY_TYPE);
        // create description when no entity is specified
        if(holderSet.size() == 0) {
            return Component.translatable(PREFIX + "combat_start");
        }
        // create description when one or more entity types are specified
        List<Component> holderSetComponent = RGComponentUtils.createHolderSetDescription(BuiltInRegistries.ENTITY_TYPE, holderSet, EntityType::getDescription);
        final Component delimiter = Component.translatable("favor.perk.condition.or");
        return Component.translatable(PREFIX + "combat_start.specific", RGComponentUtils.join(holderSetComponent, delimiter));
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.COMBAT_START.get();
    }
}
