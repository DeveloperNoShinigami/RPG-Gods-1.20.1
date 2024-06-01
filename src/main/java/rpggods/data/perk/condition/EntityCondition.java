/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.util.ComponentUtils;
import rpggods.util.DeferredHolderSet;

import java.util.List;
import java.util.Optional;

public abstract class EntityCondition extends PerkCondition {

    protected final DeferredHolderSet<EntityType<?>> entityType;

    protected EntityCondition(DeferredHolderSet<EntityType<?>> entityType) {
        this.entityType = entityType;
    }

    /**
     * Simplifies codec creation, especially if no other fields are added
     * @param instance the record codec builder with additional parameters, if any
     */
    protected static <T extends EntityCondition> Products.P1<RecordCodecBuilder.Mu<T>, DeferredHolderSet<EntityType<?>>> codecStart(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(DeferredHolderSet.codec(ForgeRegistries.Keys.ENTITY_TYPES).fieldOf("entity").forGetter(o -> o.entityType));
    }

    @Override
    public boolean test(PerkConditionContext context) {
        // verify data
        if(context.getData().isEmpty()) {
            return false;
        }
        // verify entity type
        final Optional<Holder<EntityType<?>>> oType = ForgeRegistries.ENTITY_TYPES.getHolder(context.getData().get());
        if(oType.isEmpty()) {
            return false;
        }
        return this.entityType.get(BuiltInRegistries.ENTITY_TYPE).contains(oType.get());
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final HolderSet<EntityType<?>> holderSet = entityType.get(BuiltInRegistries.ENTITY_TYPE);
        final Component delimiter = Component.translatable(PREFIX + "or");
        final List<Component> descriptions = ComponentUtils.createHolderSetDescription(BuiltInRegistries.ENTITY_TYPE, holderSet, EntityType::getDescription);
        return ComponentUtils.join(descriptions, delimiter);
    }

    /** Entity hurt player **/
    public static final class HurtPlayer extends EntityCondition {

        public static final Codec<EntityCondition.HurtPlayer> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
                .apply(instance, EntityCondition.HurtPlayer::new));

        protected HurtPlayer(DeferredHolderSet<EntityType<?>> entityType) {
            super(entityType);
        }

        @Override
        public Codec<? extends PerkCondition> getCodec() {
            return RGRegistry.PerkConditionReg.ENTITY_HURT_PLAYER.get();
        }

        @Override
        public Component createDescription(RegistryAccess registryAccess) {
            return Component.translatable(PREFIX + "entity_hurt_player", super.createDescription(registryAccess));
        }
    }

    /** Player hurt entity **/
    public static final class HurtByPlayer extends EntityCondition {

        public static final Codec<EntityCondition.HurtByPlayer> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
                .apply(instance, EntityCondition.HurtByPlayer::new));

        protected HurtByPlayer(DeferredHolderSet<EntityType<?>> entityType) {
            super(entityType);
        }

        @Override
        public Codec<? extends PerkCondition> getCodec() {
            return RGRegistry.PerkConditionReg.ENTITY_HURT_BY_PLAYER.get();
        }

        @Override
        public Component createDescription(RegistryAccess registryAccess) {
            return Component.translatable(PREFIX + "entity_hurt_by_player", super.createDescription(registryAccess));
        }
    }

    /** Entity kill player **/
    public static final class KilledPlayer extends EntityCondition {

        public static final Codec<KilledPlayer> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
                .apply(instance, KilledPlayer::new));

        protected KilledPlayer(DeferredHolderSet<EntityType<?>> entityType) {
            super(entityType);
        }

        @Override
        public Codec<? extends PerkCondition> getCodec() {
            return RGRegistry.PerkConditionReg.ENTITY_KILLED_PLAYER.get();
        }

        @Override
        public Component createDescription(RegistryAccess registryAccess) {
            return Component.translatable(PREFIX + "entity_killed_player", super.createDescription(registryAccess));
        }
    }

    /** Player kill entity **/
    public static final class KilledByPlayer extends EntityCondition {

        public static final Codec<EntityCondition.KilledByPlayer> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
                .apply(instance, EntityCondition.KilledByPlayer::new));

        protected KilledByPlayer(DeferredHolderSet<EntityType<?>> entityType) {
            super(entityType);
        }

        @Override
        public Codec<? extends PerkCondition> getCodec() {
            return RGRegistry.PerkConditionReg.ENTITY_KILLED_BY_PLAYER.get();
        }

        @Override
        public Component createDescription(RegistryAccess registryAccess) {
            return Component.translatable(PREFIX + "entity_killed_by_player", super.createDescription(registryAccess));
        }
    }

    /** Player ride entity **/
    public static final class RiddenByPlayer extends EntityCondition {

        public static final Codec<EntityCondition.RiddenByPlayer> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
                .apply(instance, EntityCondition.RiddenByPlayer::new));

        protected RiddenByPlayer(DeferredHolderSet<EntityType<?>> entityType) {
            super(entityType);
        }

        @Override
        public Codec<? extends PerkCondition> getCodec() {
            return RGRegistry.PerkConditionReg.ENTITY_RIDDEN_BY_PLAYER.get();
        }

        @Override
        public Component createDescription(RegistryAccess registryAccess) {
            return Component.translatable(PREFIX + "entity_ridden_by_player", super.createDescription(registryAccess));
        }
    }

    /** Player interact entity **/
    public static final class InteractedByPlayer extends EntityCondition {

        public static final Codec<EntityCondition.InteractedByPlayer> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
                .apply(instance, EntityCondition.InteractedByPlayer::new));

        protected InteractedByPlayer(DeferredHolderSet<EntityType<?>> entityType) {
            super(entityType);
        }

        @Override
        public Codec<? extends PerkCondition> getCodec() {
            return RGRegistry.PerkConditionReg.ENTITY_INTERACT_BY_PLAYER.get();
        }

        @Override
        public Component createDescription(RegistryAccess registryAccess) {
            return Component.translatable(PREFIX + "entity_interact_by_player", super.createDescription(registryAccess));
        }
    }
}
