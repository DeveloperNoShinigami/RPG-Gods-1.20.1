/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;
import rpggods.data.deity.Deity;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Perk;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
public final class PerkActionContext {

    /** The deity associated with this {@link Perk} **/
    private final ResourceLocation deity;
    /** The server player **/
    private final ServerPlayer player;
    /** The player favor **/
    private final IFavor favor;
    /** Some arbitrary data as an ID **/
    private final Optional<ResourceLocation> data;
    /** The entity, if there is an entity associated with the {@link Perk} **/
    private final Optional<Entity> entity;
    /** The event that triggered the {@link Perk}, if any **/
    private final Optional<? extends Event> event;
    /** The block state, if there is a block state associated with the {@link Perk} **/
    private final Optional<BlockState> blockState;

    /**
     * Constructs a new {@link PerkActionContext} with the specified parameters.
     * @param deity the deity associated with this {@link Perk}
     * @param player the player
     * @param favor the player favor
     * @param data some arbitrary data as an ID
     * @param entity the entity, if there is an entity associated with the {@link Perk}
     * @param event the event that triggered the {@link Perk}, if any
     * @param blockState the block state, if there is a block state associated with the {@link Perk}
     **/
    public PerkActionContext(ResourceLocation deity, ServerPlayer player, IFavor favor,
                             Optional<ResourceLocation> data, Optional<Entity> entity,
                             Optional<? extends Event> event, Optional<BlockState> blockState) {
        this.deity = deity;
        this.player = player;
        this.favor = favor;
        this.data = data;
        this.entity = entity;
        this.event = event;
        this.blockState = blockState;
    }

    /** The deity ID associated with this {@link Perk} **/
    public ResourceLocation getDeityId() {
        return deity;
    }

    /** The deity associated with this {@link Perk} **/
    public Deity getDeity() {
        return Deity.getRegistry(getRegistryAccess()).get(deity);
    }

    /** @return The server player **/
    public ServerPlayer getPlayer() {
        return player;
    }

    /** @return The server player {@link RandomSource} **/
    public RandomSource getRandom() {
        return player.getRandom();
    }

    /** @return The server player player **/
    public ServerLevel getLevel() {
        return player.serverLevel();
    }

    /** @return The player favor **/
    public IFavor getFavor() {
        return favor;
    }

    /** @return Some arbitrary data as an ID **/
    public Optional<ResourceLocation> getData() {
        return data;
    }

    /** @return The entity, if there is an entity associated with the {@link Perk} **/
    public Optional<Entity> getEntity() {
        return entity;
    }

    /** @return The event that triggered the {@link Perk}, if any **/
    public Optional<? extends Event> getEvent() {
        return event;
    }

    /** @return the block state, if there is a block state associated with the {@link Perk} **/
    public Optional<BlockState> getBlockState() {
        return blockState;
    }

    /** @return the registry access of the player level **/
    public RegistryAccess getRegistryAccess() {
        return player.level().registryAccess();
    }

    /** @return the block position of the player **/
    public BlockPos getPos() {
        return player.blockPosition();
    }
}
