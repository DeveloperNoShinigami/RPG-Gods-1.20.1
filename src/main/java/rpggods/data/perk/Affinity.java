/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

public final class Affinity {

    public static final Affinity EMPTY = new Affinity(AffinityType.PASSIVE, new ResourceLocation("null"));

    public static final Codec<Affinity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AffinityType.CODEC.fieldOf("type").forGetter(Affinity::getType),
            ResourceLocation.CODEC.fieldOf("entity").forGetter(Affinity::getEntity)
    ).apply(instance, Affinity::new));

    private final AffinityType type;
    private final ResourceLocation entity;

    public Affinity(AffinityType type, ResourceLocation entity) {
        this.type = type;
        this.entity = entity;
    }

    public AffinityType getType() {
        return type;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public Component getDisplayName() {
        return getType().getDisplayName();
    }

    public Component getDisplayDescription() {
        Optional<EntityType<?>> entityType = EntityType.byString(getEntity().toString());
        Component entityName = entityType.isPresent() ? entityType.get().getDescription() : Component.literal(getEntity().toString());
        return getType().getDisplayDescription(entityName);
    }

}
