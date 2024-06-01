/*
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package rpggods;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.data.deity.Cooldown;
import rpggods.data.deity.Deity;
import rpggods.data.deity.DeityContainer;
import rpggods.data.deity.Offering;
import rpggods.data.deity.Sacrifice;
import rpggods.data.favor.Favor;
import rpggods.data.favor.FavorLevel;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Perk;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.action.PerkActionContext;
import rpggods.data.perk.condition.PerkCondition;
import rpggods.data.perk.condition.PerkConditionContext;
import rpggods.entity.AltarEntity;
import rpggods.util.FavorChangedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PerkDispatcher {

    public static PerkDispatcher.Invoker invoke(final ServerPlayer player, final IFavor favor) {
        return new PerkDispatcher.Invoker(player, favor);
    }

    public static final class Invoker {

        /** The player **/
        private final ServerPlayer player;
        /** The player favor **/
        private final IFavor favor;

        /** Some arbitrary data as an ID **/
        private Optional<ResourceLocation> data;
        /** The entity, if there is an entity associated with the {@link Perk} **/
        private Optional<Entity> entity;
        /** The item stack, if there is an item stack associated with the {@link Perk} **/
        private Optional<ItemStack> itemStack;
        /** The block state, if there is a block state associated with the {@link Perk} **/
        private Optional<BlockState> blockState;
        /** The event that triggered the {@link Perk}, if any **/
        private Optional<? extends Event> event;

        private Invoker(final ServerPlayer player, final IFavor favor) {
            this.player = player;
            this.favor = favor;
            this.data = Optional.empty();
            this.entity = Optional.empty();
            this.itemStack = Optional.empty();
            this.blockState = Optional.empty();
            this.event = Optional.empty();
        }

        public Invoker withData(final ResourceLocation data) {
            this.data = Optional.of(data);
            return this;
        }

        public Invoker withEntity(final Entity entity) {
            this.entity = Optional.of(entity);
            return this;
        }

        public <T extends Event> Invoker withEvent(final T event) {
            this.event = Optional.of(event);
            return this;
        }

        public Invoker withItemStack(final ItemStack itemStack) {
            this.itemStack = Optional.of(itemStack);
            return this;
        }

        public Invoker withBlockState(final BlockState blockState) {
            this.blockState = Optional.of(blockState);
            return this;
        }

        public boolean runForCondition(final Codec<? extends PerkCondition> type) {
            // validate favor
            if(!favor.isEnabled()) {
                return false;
            }
            boolean success = false;
            // use a map to avoid duplicates
            Map<ResourceLocation, Perk> perks = new HashMap<>();
            // iterate all containers and add matching perks to the map
            for (DeityContainer container : DeityContainer.getRegistry(player.level().isClientSide()).values()) {
                // validate deity
                boolean deityEnabled = container.getDeity().isEnabled() && favor.getFavor(container.getId()).isEnabled();
                if (!deityEnabled) {
                    continue;
                }
                // add matching perks
                perks.putAll(container.getPerksByCondition(type));
            }
            // shuffle perks
            List<Perk> shuffledPerks = new ArrayList<>(perks.values());
            Collections.shuffle(shuffledPerks);
            // run each perk
            for (Perk perk : shuffledPerks) {
                success |= PerkDispatcher.runPerk(perk, player, favor, data, entity, itemStack, blockState, event);
            }
            return success;
        }

        public boolean runForAction(final Codec<? extends PerkAction> type) {
            // validate favor
            if(!favor.isEnabled()) {
                return false;
            }
            boolean success = false;
            // use a map to avoid duplicates
            Map<ResourceLocation, Perk> perks = new HashMap<>();
            // iterate all containers and add matching perks to the map
            for (DeityContainer container : DeityContainer.getRegistry(player.level().isClientSide()).values()) {
                // validate deity
                boolean deityEnabled = container.getDeity().isEnabled() && favor.getFavor(container.getId()).isEnabled();
                if (!deityEnabled) {
                    continue;
                }
                // add matching perks
                perks.putAll(container.getPerksByAction(type));
            }
            // shuffle perks
            final List<Perk> shuffledPerks = new ArrayList<>(perks.values());
            Collections.shuffle(shuffledPerks);
            // run each perk
            for (Perk perk : shuffledPerks) {
                success |= runPerk(perk, player, favor, data, entity, itemStack, blockState, event);
            }
            return success;
        }
    }



    /**
     * Called when the player attempts to give an offering
     *
     * @param entity the AltarEntity associated with this offering, if any
     * @param deity  the deity ID
     * @param player the player
     * @param favor  the player's favor
     * @param item   the item being offered
     * @param silent true if the player should not receive any feedback
     * @return the ItemStack to replace the one provided, if any
     */
    public static Optional<ItemStack> onOffering(final Optional<AltarEntity> entity, final ResourceLocation deity, final ServerPlayer player, final IFavor favor, final ItemStack item, boolean silent) {
        // validate item
        if(item.isEmpty()) {
            return Optional.empty();
        }
        // validate favor
        if (!favor.isEnabled()) {
            return Optional.empty();
        }
        // prepare data
        final DeityContainer container = DeityContainer.getOrCreate(player.level().registryAccess(), deity);
        // validate deity enabled
        if(!container.getDeity().isEnabled()) {
            return Optional.empty();
        }
        // validate deity unlocked
        final FavorLevel favorLevel = favor.getFavor(deity);
        final int favorLevelInt = favorLevel.getLevel();
        if(!favorLevel.isEnabled()) {
            return Optional.empty();
        }
        // iterate all offerings and process the first one that matches
        for(Map.Entry<ResourceLocation, Offering> entry : container.getOfferingsByItem(item.getItem()).entrySet()) {
            Offering offering = entry.getValue();
            // validate cooldown
            Cooldown cooldown = favor.getOfferingCooldown(entry.getKey());
            if(!cooldown.canUse()) {
                continue;
            }
            // validate item stack
            if(!offering.matches(item)) {
                continue;
            }
            // validate favor level
            if(offering.hasLevelRange() && (favorLevelInt < offering.getMinLevel() || favorLevelInt > offering.getMaxLevel())) {
                // Send message to player informing them of level requirements
                if (!silent) {
                    Component message;
                    if (offering.hasMinLevel() && offering.hasMaxLevel()) {
                        message = Component.translatable("favor.offering.deny.level.multiple", offering.getMinLevel(), offering.getMaxLevel());
                    } else {
                        message = Component.translatable("favor.offering.deny.level.single", offering.getMinLevel());
                    }
                    player.displayClientMessage(message, true);
                }
                return Optional.empty();
            }
            // the offering is valid.
            // process the offering.
            // add favor to the associated deity
            favorLevel.addFavor(player, deity, offering.getFavor(), FavorChangedEvent.Source.OFFERING);
            // run the offering function, if any
            offering.getFunction().ifPresent(f -> runFunction(player.level(), player, f));
            // add cooldown
            cooldown.addUse();
            // process trade, if any
            Optional<ItemStack> oResult = offering.getResult(item);
            if (oResult.isPresent() && !oResult.get().isEmpty()) {
                // add trade itemstack to player inventory, or drop it if that failed
                ItemStack tradeItemStack = oResult.get();
                if(!player.getInventory().add(tradeItemStack)) {
                    player.drop(tradeItemStack, false);
                }
            }
            // shrink offering item stack
            if (!player.getAbilities().instabuild) {
                item.shrink(offering.getOffering().getCount());
            }
            // spawn particles
            if (entity.isPresent()) {
                Vec3 pos = Vec3.atBottomCenterOf(entity.get().blockPosition().above());
                ParticleOptions particle = offering.getFavor() >= 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
                player.serverLevel().sendParticles(particle, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
            }
            // send player message
            if (!silent) {
                favor.getFavor(deity).sendStatusMessage(player, deity);
            }
            return Optional.of(item);

        }
        // no offerings were processed
        return Optional.empty();
    }

    /**
     * Called when the player kills a living entity
     *
     * @param player the player
     * @param favor  the player's favor
     * @param entity the entity that was killed
     * @return true if the player's favor was modified
     */
    public static boolean onSacrifice(final ServerPlayer player, final IFavor favor, final LivingEntity entity) {
        if (!favor.isEnabled()) {
            return false;
        }
        // prepare data
        final EntityType<?> entityType = entity.getType();
        final ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        final Optional<ResourceLocation> oEntityId = Optional.of(entityId);
        final Optional<Entity> oEntity = Optional.of(entity);
        int count = 0;

        // iterate all containers and process matching sacrifices
        for (DeityContainer container : DeityContainer.getRegistry(player.level().isClientSide()).values()) {
            // validate deity
            boolean deityEnabled = container.getDeity().isEnabled() && favor.getFavor(container.getId()).isEnabled();
            if (!deityEnabled) {
                continue;
            }
            // load favor level
            FavorLevel favorLevel = favor.getFavor(container.getId());
            // create condition context
            PerkConditionContext context = new PerkConditionContext(container.getId(), player, favor, oEntityId, oEntity, Optional.empty(), Optional.empty());
            // iterate matching sacrifices
            for (Map.Entry<ResourceLocation, Sacrifice> entry : container.getSacrificesByEntity(entityType).entrySet()) {
                // validate sacrifice cooldown
                Cooldown cooldown = favor.getSacrificeCooldown(entry.getKey());
                if (!cooldown.canUse()) {
                    continue;
                }
                // validate conditions
                if(!entry.getValue().getCondition().test(context)) {
                    continue;
                }
                // add favor to the associated deity
                favorLevel.addFavor(player, container.getId(), entry.getValue().getFavor(), FavorChangedEvent.Source.SACRIFICE);
                // run the sacrifice function, if any
                entry.getValue().getFunction().ifPresent(f -> runFunction(player.level(), player, f));
                // add cooldown
                cooldown.addUse();
                // increment count
                count++;
            }
        }
        return count > 0;
    }

    /**
     * Attempts to run a single perk and sets a cooldown if successful.
     * Checks favor range, cooldown, random chance, and conditions before running the perk.
     *
     * @param perk   the Perk to run
     * @param player the player to affect
     * @param favor  the player's favor
     * @param data   a ResourceLocation ID to use when running the perk, if any
     * @param entity an entity to use when running the perk, if any
     * @param itemStack an item stack to use when running the perk, if any
     * @param blockState a blockstate to use when running the perk, if any
     * @param event the Event to reference when running the perk, if any
     * @return True if the perk was run and cooldown was added.
     * @see #invoke(ServerPlayer, IFavor)
     */
    public static boolean runPerk(final Perk perk, final ServerPlayer player, final IFavor favor,
                                  final Optional<ResourceLocation> data, final Optional<Entity> entity,
                                  final Optional<ItemStack> itemStack, final Optional<BlockState> blockState,
                                  final Optional<? extends Event> event) {
        // validate favor range
        if(!perk.getRange().isInRange(favor)) {
            return false;
        }
        // validate cooldown
        if(favor.getPerkCooldown(perk.getCategory()) > 0) {
            return false;
        }
        // validate random chance
        if(player.getRandom().nextFloat() > perk.getAdjustedChance(favor.getFavor(perk.getDeity()))) {
            return false;
        }
        // validate condition
        final PerkConditionContext conditionContext = new PerkConditionContext(perk.getDeity(), player, favor, data, entity, itemStack, blockState);
        if (!perk.getCondition().test(conditionContext)) {
            return false;
        }
        // perk is valid
        // attempt to run each action
        boolean success = false;
        final PerkActionContext actionContext = new PerkActionContext(perk.getDeity(), player, favor, data, entity, event, blockState);
        for(PerkAction action : perk.getActions()) {
            success |= action.apply(actionContext);
        }
        // verify at least one action was successful
        if(success) {
            // send feedback
            final Deity deity = Deity.getRegistry(player.level().registryAccess()).get(perk.getDeity());
            sendPerkFeedback(deity, player, favor, perk.isPositive());
            // update cooldown
            long cooldown = (long) Math.floor(perk.getCooldown() * (1.0D + player.getRandom().nextDouble() * 0.25D));
            favor.setPerkCooldown(perk.getCategory(), cooldown);
        }
        return success;
    }

    /**
     * Loads and runs a single function at the entity position
     *
     * @param level      the world
     * @param entity     the entity (for example, a player)
     * @param functionId the function ID of a function to run
     * @return true if the function ran successfully
     */
    public static boolean runFunction(final Level level, final LivingEntity entity, ResourceLocation functionId) {
        final MinecraftServer server = level.getServer();
        if (server != null) {
            final ServerFunctionManager manager = server.getFunctions();
            final Optional<CommandFunction> function = manager.get(functionId);
            if (function.isPresent()) {
                final CommandSourceStack commandSource = manager.getGameLoopSender()
                        .withEntity(entity)
                        .withPosition(entity.position())
                        .withPermission(4)
                        .withSuppressedOutput();
                manager.execute(function.get(), commandSource);
                return true;
            }
        }
        return false;
    }


    /**
     * @param altar   the altar entity
     * @param deityId the Deity ID of the deity for this altar
     * @return true if a ritual was detected and patron was changed
     */
    public static boolean performRitual(final AltarEntity altar, final ResourceLocation deityId) {
        // validate server
        if(null == altar.getServer()) {
            return false;
        }
        // load all entities in the altar detection aabb
        Vec3i facing = altar.getDirection().getNormal();
        BlockPos pos = altar.blockPosition().offset(facing);
        AABB aabb = new AABB(pos).inflate(0.15D, 1.075D, 0.15D);
        List<ItemEntity> list = altar.level().getEntitiesOfClass(ItemEntity.class, aabb, e -> e.isOnFire());
        // validate there is at least one burning item
        if (list.isEmpty()) {
            return false;
        }
        ItemEntity item = list.get(0);
        // detect player who threw the item
        if (null == item.getOwner()) {
            return false;
        }
        // validate player
        final ServerPlayer player = altar.getServer().getPlayerList().getPlayer(item.getOwner().getUUID());
        if (null == player) {
            return false;
        }
        // load and validate favor
        final IFavor favor = RPGGods.getFavor(player).orElse(null);
        if(null == favor || !favor.isEnabled()) {
            return false;
        }
        // load deity container
        final RegistryAccess registryAccess = altar.level().registryAccess();
        final DeityContainer deity = DeityContainer.getOrCreate(registryAccess, deityId);
        // load ritual perks
        final Map<ResourceLocation, Perk> ritualPerks = deity.getPerksByCondition(RGRegistry.PerkConditionReg.RITUAL.get());
        // attempt to run all perks
        boolean success = false;
        for(Perk perk : ritualPerks.values()) {
            success |= runPerk(perk, player, favor, Optional.empty(), Optional.of(altar), Optional.of(item.getItem()), Optional.empty(), Optional.empty());
        }
        // send feedback when ritual resulted in a patron being assigned
        if(success && favor.getPatron().isPresent()) {
            // summon visual lightning bolt
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(altar.level());
            bolt.setVisualOnly(true);
            Vec3 position = Vec3.atBottomCenterOf(pos.below());
            bolt.setPos(position.x, position.y, position.z);
            altar.level().addFreshEntity(bolt);
            // send message
            final Deity patron = Deity.getRegistry(registryAccess).get(favor.getPatron().get());
            final Component patronName = patron.getName();
            Component message = Component.translatable("favor.perk.type.patron.description.add", patronName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            player.displayClientMessage(message, true);
        }
        return success;
    }

    public static void sendPerkFeedback(Deity deity, Player player, IFavor favor, boolean isPositive) {
        if (RPGGods.CONFIG.canGiveFeedback()) {
            final Component deityName = deity.getName();
            final Component message;
            if (isPositive) {
                message = Component.translatable("favor.perk.feedback.positive", deityName).withStyle(ChatFormatting.GREEN);
            } else {
                message = Component.translatable("favor.perk.feedback.negative", deityName).withStyle(ChatFormatting.RED);
            }
            player.displayClientMessage(message, !RPGGods.CONFIG.isFeedbackChat());
        }
    }
}
