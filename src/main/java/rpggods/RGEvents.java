/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods;

import com.google.common.collect.ImmutableSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.condition.PerkCondition;
import rpggods.data.tameable.ITameable;
import rpggods.data.tameable.Tameable;
import rpggods.entity.AffinityGoal;
import rpggods.network.SUpdateSittingPacket;
import rpggods.util.FavorChangedEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RGEvents {

    public static final int COMBAT_TIMER = 40;

    public static class ModHandler {

        @SubscribeEvent
        public static void onAddEntityAttributes(final EntityAttributeModificationEvent event) {
            for (final EntityType<? extends LivingEntity> type : event.getTypes()) {
                if (!event.has(type, Attributes.ATTACK_DAMAGE)) {
                    event.add(type, Attributes.ATTACK_DAMAGE, 0);
                }
            }
        }
    }

    public static class ForgeHandler {

        @SubscribeEvent
        public static void onLivingDeath(final LivingDeathEvent event) {
            if (!event.isCanceled() && event.getEntity() != null && !event.getEntity().level().isClientSide() && event.getEntity().isEffectiveAi()) {
                if (event.getEntity() instanceof ServerPlayer player) {
                    final Entity source = event.getSource().getEntity();
                    final ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(source.getType());
                    // onEntityKillPlayer
                    if (source instanceof LivingEntity && !player.isSpectator() && !player.isCreative()) {
                        RPGGods.getFavor(player).ifPresent(f -> {
                            PerkDispatcher.invoke(player, f)
                                    .withData(entityType)
                                    .withEntity(source)
                                    .runForCondition(RGRegistry.PerkConditionReg.ENTITY_KILLED_PLAYER.get());
                        });
                    }
                } else if (event.getSource().getEntity() instanceof ServerPlayer player) {
                    final ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
                    // onPlayerKillEntity
                    RPGGods.getFavor(player).ifPresent(f -> {
                        PerkDispatcher.invoke(player, f)
                                .withData(entityType)
                                .withEntity(event.getEntity())
                                .runForCondition(RGRegistry.PerkConditionReg.ENTITY_KILLED_BY_PLAYER.get());

                        PerkDispatcher.onSacrifice(player, f, event.getEntity());
                    });
                }
                // onTameDeath
                LazyOptional<ITameable> tameable = event.getEntity().getCapability(RPGGods.TAMEABLE);
                tameable.ifPresent(t -> {
                    Optional<LivingEntity> owner = t.getOwner(event.getEntity().level());
                    // send death message to owner
                    if (owner.isPresent() && owner.get() instanceof Player) {
                        Component message = event.getSource().getLocalizedDeathMessage(event.getEntity());
                        ((Player) owner.get()).displayClientMessage(message, false);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(final LivingHurtEvent event) {
            if (!event.isCanceled() && !event.getEntity().level().isClientSide() && event.getEntity().isEffectiveAi() && event.getEntity().isAlive()) {
                if (event.getSource().getDirectEntity() != null && event.getEntity() instanceof ServerPlayer player) {
                    Entity source = event.getSource().getDirectEntity();
                    ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(source.getType());
                    if (!player.isSpectator() && !player.isCreative()) {
                        // onEntityHurtPlayer
                        RPGGods.getFavor(player).ifPresent(f -> {
                            PerkDispatcher.invoke(player, f)
                                    .withData(entityType)
                                    .withEntity(source)
                                    .withEvent(event)
                                    .runForCondition(RGRegistry.PerkConditionReg.ENTITY_HURT_PLAYER.get());
                        });
                    }
                } else if (event.getSource().getDirectEntity() instanceof ServerPlayer player) {
                    LivingEntity target = event.getEntity();
                    ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                    // onPlayerHurtEntity
                    RPGGods.getFavor(player).ifPresent(f -> {
                        PerkDispatcher.invoke(player, f)
                                .withData(entityType)
                                .withEntity(target)
                                .withEvent(event)
                                .runForCondition(RGRegistry.PerkConditionReg.ENTITY_HURT_BY_PLAYER.get());
                        // onEnterCombat
                        if (player.getCombatTracker().getCombatDuration() < COMBAT_TIMER) {
                            PerkDispatcher.invoke(player, f)
                                    .withData(entityType)
                                    .withEntity(target)
                                    .runForCondition(RGRegistry.PerkConditionReg.COMBAT_START.get());
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
            if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player) {
                final ItemStack itemStack = event.getItemStack();
                final ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(event.getTarget().getType());
                // onPlayerInteractEntity
                RPGGods.getFavor(player).ifPresent(f -> {
                    if(PerkDispatcher.invoke(player, f)
                            .withData(entityType)
                            .withEntity(event.getTarget())
                            .withItemStack(itemStack)
                            .runForCondition(RGRegistry.PerkConditionReg.ENTITY_INTERACT_BY_PLAYER.get())) {
                        event.setCancellationResult(InteractionResult.SUCCESS);
                    }
                    if (event.getTarget() instanceof Merchant
                            && PerkDispatcher.invoke(player, f)
                                    .withData(entityType)
                                    .withEntity(event.getTarget())
                                    .withEvent(event)
                                    .runForAction(RGRegistry.PerkActionReg.MERCHANT_PRICE.get())) {
                        event.setCancellationResult(event.isCanceled() ? InteractionResult.FAIL : InteractionResult.SUCCESS);
                    }
                });
                // toggle sitting for tamed mobs
                if (null == event.getCancellationResult() || !event.getCancellationResult().consumesAction()) {
                    event.getTarget().getCapability(RPGGods.TAMEABLE).ifPresent(t -> {
                        if (t.isOwner(event.getEntity())) {
                            t.setSittingWithUpdate(event.getTarget(), !t.isSitting());
                            // send packet to notify client of new sitting state
                            RPGGods.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> event.getEntity()),
                                    new SUpdateSittingPacket(event.getTarget().getId(), t.isSitting()));
                            event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onInteractBlock(final PlayerInteractEvent.RightClickBlock event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                BlockState state = event.getEntity().level().getBlockState(event.getHitVec().getBlockPos());
                ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (blockId != null) {
                    // onPlayerInteractBlock
                    RPGGods.getFavor(player).ifPresent(f -> {
                        PerkDispatcher.invoke(player, f)
                                .withData(blockId)
                                .withBlockState(state)
                                .withItemStack(event.getItemStack())
                                .runForCondition(RGRegistry.PerkConditionReg.USE_BLOCK.get());
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onChangeFavor(FavorChangedEvent.Post event) {
            if (event.isLevelChange() && event.getPlayer() instanceof ServerPlayer player) {
                // onFavorChanged
                RPGGods.getFavor(event.getPlayer()).ifPresent(f -> {
                    PerkDispatcher.invoke(player, f)
                            .runForAction(RGRegistry.PerkActionReg.UNLOCK.get());
                });
            }
        }

        private static final AttributeModifier MOB_ATTACK = new AttributeModifier(
                UUID.fromString("2953b29d-7974-45e0-9a52-24b6ed738197"),
                "mob_attack", 1.0D, AttributeModifier.Operation.ADDITION);

        @SubscribeEvent
        public static void onEntityJoinWorld(final EntityJoinLevelEvent event) {
            if (!event.getEntity().level().isClientSide && (event.getEntity() instanceof Arrow || event.getEntity() instanceof SpectralArrow)) {
                final AbstractArrow arrow = (AbstractArrow) event.getEntity();
                final Entity thrower = arrow.getOwner();
                if (thrower instanceof ServerPlayer player) {
                    // onArrowDamage, onArrowEffect, onArrowCount
                    RPGGods.getFavor(thrower).ifPresent(f -> {
                        PerkDispatcher.Invoker invoker = PerkDispatcher.invoke(player, f)
                                .withEntity(arrow);
                        invoker.runForAction(RGRegistry.PerkActionReg.ARROW_DAMAGE.get());
                        invoker.runForAction(RGRegistry.PerkActionReg.ARROW_EFFECT.get());
                        invoker.runForAction(RGRegistry.PerkActionReg.ARROW_COUNT.get());
                    });
                }
            }
            if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Mob) {
                Mob mob = (Mob) event.getEntity();
                boolean fleeEnabled = RPGGods.CONFIG.isFleeEnabled();
                boolean hostileEnabled = RPGGods.CONFIG.isHostileEnabled();
                boolean passiveEnabled = RPGGods.CONFIG.isPassiveEnabled();
                boolean tameableEnabled = RPGGods.CONFIG.isTameableEnabled();
                boolean checkAttackGoal = false;
                // add tameable goals
                if (tameableEnabled && event.getEntity().getCapability(RPGGods.TAMEABLE).isPresent()) {
                    mob.goalSelector.addGoal(0, new AffinityGoal.AffinitySittingGoal(mob));
                    mob.goalSelector.addGoal(0, new AffinityGoal.AffinitySittingResetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.AffinityFollowOwnerGoal(mob, 1.0D, 10.0F, 5.0F, false));
                    mob.goalSelector.addGoal(1, new AffinityGoal.AffinityOwnerHurtByTargetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.AffinityOwnerHurtTargetGoal(mob));
                    checkAttackGoal = true;
                }
                // add flee goal
                if (fleeEnabled && event.getEntity() instanceof PathfinderMob) {
                    mob.goalSelector.addGoal(1, new AffinityGoal.AffinityFleeGoal((PathfinderMob) mob));
                }
                // add hostile goal
                if (hostileEnabled) {
                    mob.goalSelector.addGoal(4, new AffinityGoal.AffinityNearestAttackableGoal(mob, 0.1F));
                    checkAttackGoal = true;
                }
                // add target reset goal
                if (hostileEnabled || passiveEnabled) {
                    mob.goalSelector.addGoal(2, new AffinityGoal.AffinityNearestAttackableResetGoal(mob));
                }
                // ensure target has attack goal
                if (checkAttackGoal && event.getEntity() instanceof PathfinderMob
                        && !(event.getEntity() instanceof RangedAttackMob)
                        && !(event.getEntity() instanceof NeutralMob)) {
                    // check for existing attack goal
                    boolean hasAttackGoal = false;
                    for (Goal g : mob.goalSelector.getRunningGoals().toList()) {
                        if (g instanceof MeleeAttackGoal) {
                            hasAttackGoal = true;
                            break;
                        }
                    }
                    // add attack goal if none was found
                    if (!hasAttackGoal) {
                        mob.goalSelector.addGoal(4, new AffinityGoal.AffinityMeleeAttackGoal((PathfinderMob) event.getEntity(), 1.2D, false));
                        // ensure mob has attack damage
                        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                        if (attack != null && attack.getBaseValue() < 0.5D && !attack.hasModifier(MOB_ATTACK)) {
                            attack.addPermanentModifier(MOB_ATTACK);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onAddPotion(final MobEffectEvent.Added event) {
            if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player
                    && player.isAlive() && !player.isSpectator() && !player.isCreative()
                    && event.getEffectInstance() != null) {
                final ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect());
                // onEffectStart
                RPGGods.getFavor(player).ifPresent(f -> {
                    PerkDispatcher.invoke(player, f)
                            .withData(effectId)
                            .runForCondition(RGRegistry.PerkConditionReg.EFFECT_START.get());
                });
            }
        }

        @SubscribeEvent
        public static void onLivingTarget(final LivingChangeTargetEvent event) {
            if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Mob mob
                    && event.getNewTarget() instanceof Player player) {
                // Determine if entity is passive or hostile toward target
                ImmutablePair<Boolean, Boolean> passiveHostile = AffinityGoal.getPassiveAndHostile(event.getEntity(), player);
                if (passiveHostile.getLeft()) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onBabySpawn(final BabyEntitySpawnEvent event) {
            if (!event.isCanceled() && !event.getParentA().level().isClientSide()
                    && event.getCausedByPlayer() instanceof ServerPlayer player
                    && !player.isCreative() && !player.isSpectator()
                    && event.getParentA() instanceof Animal && event.getParentB() instanceof Animal) {
                RPGGods.getFavor(player).ifPresent(f -> {
                    PerkDispatcher.invoke(player, f)
                            .withEntity(event.getParentA())
                            .withEvent(event)
                            .runForAction(RGRegistry.PerkActionReg.OFFSPRING.get());
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerPickupXp(final PlayerXpEvent.PickupXp event) {
            if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
                RPGGods.getFavor(event.getEntity()).ifPresent(f -> {
                    PerkDispatcher.invoke(player, f)
                            .withEntity(event.getOrb())
                            .runForAction(RGRegistry.PerkActionReg.XP.get());
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if (!event.isCanceled() && !event.player.level().isClientSide() && event.player.isEffectiveAi()
                    && event.phase == TickEvent.Phase.END && event.player.isAlive() && canTickFavor(event.player)) {
                RPGGods.getFavor(event.player).ifPresent(f -> {
                    // trigger perks
                    if (Math.random() < RPGGods.CONFIG.getRandomPerkChance()) {
                        // onRandomTick
                        PerkDispatcher.invoke((ServerPlayer) event.player, f)
                                .runForCondition(RGRegistry.PerkConditionReg.RANDOM_TICK.get());
                    }
                    // player tick
                    if(RPGGods.CONFIG.usePlayerFavor()) {
                        // reduce cooldown
                        f.tickCooldown(event.player.level().getGameTime());
                        // deplete favor
                        if (Math.random() < RPGGods.CONFIG.getFavorDecayRate()) {
                            f.depleteFavor(event.player);
                        }
                    }
                });
            }
        }

        /**
         * Used to tick global and team favor
         * @param event the server tick event
         */
        @SubscribeEvent
        public static void onServerTick(final TickEvent.ServerTickEvent event) {
            // locate the current server
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            int tickCount = server.getTickCount();
            // attempt to tick non-player favor
            if(event.phase == TickEvent.Phase.END && !RPGGods.CONFIG.usePlayerFavor()
                    && canTickFavor(tickCount) && !server.getPlayerList().getPlayers().isEmpty()) {
                // load RGSavedData
                RGSavedData data = RGSavedData.get(server);
                // create set of favor to tick
                Set<IFavor> favorSet;
                if(RPGGods.CONFIG.useGlobalFavor()) {
                    // add global favor
                    favorSet = ImmutableSet.of(data.getFavor());
                } else /*if(RPGGods.CONFIG.useTeamFavor())*/ {
                    // add team favor
                    favorSet = ImmutableSet.copyOf(data.getTeamFavor());
                }
                // load game time
                long gameTime = server.getLevel(Level.OVERWORLD).getGameTime();
                // favor tick
                for(IFavor favor : favorSet) {
                    // reduce cooldown
                    favor.tickCooldown(gameTime);
                    // decay favor
                    if(Math.random() < RPGGods.CONFIG.getFavorDecayRate()) {
                        favor.depleteFavor(null);
                    }
                }
            }
        }

        /**
         * @param entity the entity
         * @return true if the favor is enabled and the correct number of ticks have elapsed
         * @see #canTickFavor(int)
         */
        public static boolean canTickFavor(final LivingEntity entity) {
            return canTickFavor(entity.tickCount + entity.getId());
        }

        /**
         * @param tickCount the current tick count
         * @return true if the favor is enabled and the correct number of ticks have elapsed
         */
        public static boolean canTickFavor(final int tickCount) {
            return RPGGods.CONFIG.isFavorEnabled() && tickCount % RPGGods.CONFIG.getFavorUpdateRate() == 0;
        }
    }

    public static class ClientHandler {

        @SubscribeEvent
        public static void onRenderLiving(final net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
            if (event.getEntity().isAlive() && event.getEntity() instanceof Mob && !(event.getEntity() instanceof TamableAnimal)) {
                LazyOptional<ITameable> tameable = event.getEntity().getCapability(RPGGods.TAMEABLE);
                if (tameable.isPresent() && tameable.orElse(Tameable.EMPTY).isSitting()) {
                    // shift down when sitting
                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
                    double dy = RPGGods.CONFIG.isSittingMob(id) ? -0.5D : -0.125D;
                    event.getPoseStack().translate(0, dy, 0);
                }
            }
        }
    }
}
