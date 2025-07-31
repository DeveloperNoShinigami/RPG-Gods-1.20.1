/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.deity.Deity;
import rpggods.data.favor.FavorLevel;
import rpggods.data.perk.Affinity;
import rpggods.data.perk.AffinityType;
import rpggods.data.tameable.ITameable;
import rpggods.util.DeferredHolderSet;
import rpggods.util.FavorChangedEvent;

import java.util.List;
import java.util.Optional;

public class AffinityAction extends PerkAction {

    public static final Codec<AffinityAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(DeferredHolderSet.codec(ForgeRegistries.Keys.ENTITY_TYPES).fieldOf("entity").forGetter(o -> o.entityType))
            .and(AffinityType.CODEC.fieldOf("affinity").forGetter(o -> o.affinity))
            .apply(instance, AffinityAction::new));

    private final DeferredHolderSet<EntityType<?>> entityType;
    private final AffinityType affinity;

    public AffinityAction(boolean isHidden, DeferredHolderSet<EntityType<?>> entityType, AffinityType affinity) {
        super(isHidden);
        this.entityType = entityType;
        this.affinity = affinity;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // only handles the TAME affinity type.
        // other affinity types are handled elsewhere.
        if(context.getEntity().isPresent() && affinity == AffinityType.TAME) {
            final Entity entity = context.getEntity().get();
            LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                if(tameable.orElse(null).setTamedBy(context.getPlayer())) {
                    // set custom name to prevent despawn
                    if(!entity.hasCustomName()) {
                        entity.setCustomName(entity.getDisplayName());
                    }
                    // spawn particles
                    if(context.getLevel() instanceof ServerLevel level) {
                        Vec3 pos = entity.getEyePosition(1.0F);
                        level.sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, 10, 0.5D, 0.5D, 0.5D, 0);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        /*Optional<EntityType<?>> entityType = EntityType.byString(entityType.toString());
        Component entityName = entityType.isPresent() ? entityType.get().getDescription() : Component.literal(getEntity().toString());
        return getType().getDisplayDescription(entityName);*/
        // TODO affinity description for all affinity types
        return ImmutableList.of();
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.AFFINITY.get();
    }
}
