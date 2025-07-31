/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGEvents;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.deity.Altar;
import rpggods.data.deity.Deity;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.FavorLevel;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Affinity;
import rpggods.data.perk.AffinityType;
import rpggods.data.perk.Patron;
import rpggods.data.perk.Perk;
import rpggods.data.tameable.ITameable;
import rpggods.util.FavorChangedEvent;
import rpggods.util.RGCodecUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class PerkAction {

    public static final Codec<PerkAction> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> RGRegistry.PERK_ACTION_TYPES_SUPPLIER.get().getCodec())
            .dispatch(PerkAction::getCodec, Function.identity());
    public static final Codec<List<PerkAction>> LIST_CODEC = RGCodecUtils.listOrElementCodec(DIRECT_CODEC);

    private final List<Component> description = new ArrayList<>();
    private final List<Component> descriptionView = Collections.unmodifiableList(description);

    protected final boolean isHidden;

    public PerkAction(final boolean isHidden) {
        this.isHidden = isHidden;
    }

    /**
     * @param context the {@link PerkActionContext} with parameters to facilitate the action
     * @return true if the perk action applied successfully
     */
    public abstract boolean apply(final PerkActionContext context);

    /**
     * @param registryAccess the Registry Access instance
     * @return the name of this perk action as a {@link Component}
     */
    public abstract List<Component> createDescription(final RegistryAccess registryAccess);

    /**
     * @return the Codec used to encode/decode this {@link PerkAction}
     * @see rpggods.RGRegistry.PerkActionReg
     */
    public abstract Codec<? extends PerkAction> getCodec();

    /**
     * @param registryAccess the registry access
     * @return a list of text components that describe this modifier condition
     */
    public final List<Component> getDescription(final RegistryAccess registryAccess) {
        if(description.isEmpty()) {
            description.addAll(createDescription(registryAccess));
        }
        return descriptionView;
    }

    //// HELPER METHODS ////

    /**
     * Simplifies codec creation, especially if no other fields are added
     * @param instance the record codec builder with additional parameters, if any
     */
    protected static <T extends PerkAction> Products.P1<RecordCodecBuilder.Mu<T>, Boolean> codecStart(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkAction::isHidden));
    }

    /**
     * Reads a {@link MobEffectInstance} from a {@link CompoundTag}, but allows the mob effect to be
     * specified by a {@code "Potion"} tag with the registry name of the {@link MobEffect} instead of the byte ID,
     * and with the default value of {@code "ShowParticles"} set to {@code false} when not specified
     * @param tag the compound tag
     * @return the {@link MobEffectInstance} in the argument tag, if any
     * @see MobEffectInstance#load(CompoundTag)
     */
    public static Optional<MobEffectInstance> readEffectInstance(final CompoundTag tag) {
        final CompoundTag mobEffectTag = tag.copy();
        if(tag.contains("Potion", 8)) {
            // "show particles" will default to false if not specified
            if(!mobEffectTag.contains("ShowParticles")) {
                mobEffectTag.putBoolean("ShowParticles", false);
            }
            MobEffect potion = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(mobEffectTag.getString("Potion")));
            if(potion != null) {
                mobEffectTag.putByte("Id", (byte) MobEffect.getId(potion));
            }
        }
        return Optional.of(MobEffectInstance.load(mobEffectTag));
    }

    /**
     * Formats the given float as a percentage. Example usage:
     * {@code input: -0.9, output: -90%};
     * {@code input: 0.25: output: +25%};
     * {@code input: 1.2: output: +120%}
     * @param percent a percent in the range {@code [-1.0, inf)}
     * @return a text component containing a signed percentage
     */
    public static Component createPercentageComponent(final float percent) {
        StringBuilder builder = new StringBuilder("");
        // add prefix
        if(!(percent < 0.0F)) {
            builder.append("+");
        }
        // add number and percent
        builder.append(
                String.format("%.2f", percent * 100.0F)
                .replace("0*$", "")
                .replace("\\.$", ""));
        builder.append("%");
        // create component
        return Component.literal(builder.toString());
    }

    // TODO dispatch codec for PerkAction

    public static final Codec<PerkAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkAction.Type.CODEC.fieldOf("type").forGetter(PerkAction::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkAction::getString),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(PerkAction::getId),
            CompoundTag.CODEC.optionalFieldOf("tag").forGetter(PerkAction::getTag),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(PerkAction::getItem),
            Codec.LONG.optionalFieldOf("favor").forGetter(PerkAction::getFavor),
            Codec.FLOAT.optionalFieldOf("multiplier").forGetter(PerkAction::getMultiplier),
            Affinity.CODEC.optionalFieldOf("affinity").forGetter(PerkAction::getAffinity),
            Patron.CODEC.optionalFieldOf("patron").forGetter(PerkAction::getPatron),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkAction::isHidden)
    ).apply(instance, PerkAction::new));



    /**
     * Runs a single Perk without any of the preliminary checks or cooldown.
     * If you want these, call {@link RGEvents#runPerk(Perk, Player, IFavor)} or
     * {@link RGEvents#runPerk(Perk, Player, IFavor, Optional, Optional, Optional)} instead.
     * @param deity the Deity that is associated with the perk
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @param data a ResourceLocation ID to use when running the perk, if any
     * @param object the Event to reference when running the perk, if any
     * @return True if the action ran successfully
     */
    public boolean run(final ResourceLocation deity, final Player player, final IFavor favor,
                                        final Optional<Entity> entity, final Optional<ResourceLocation> data, final Optional<? extends Event> object) {
        switch (this.getType()) {
            case FUNCTION: return getId().isPresent() && RGEvents.runFunction(player.level, player, getId().get());
            case POTION:
                if(getTag().isPresent()) {
                    Optional<MobEffectInstance> effect = readEffectInstance(getTag().get());
                    if(effect.isPresent()) {
                        return player.addEffect(effect.get());
                    }
                }
                return false;
            case SUMMON:
                float distance = getMultiplier().orElse(9F);
                return getTag().isPresent() && summonEntityNearPlayer(player.level, player, getTag(), distance).isPresent();
            case ITEM:
                if(getItem().isPresent()) {
                    ItemEntity itemEntity = new ItemEntity(player.level, player.getX(), player.getY(), player.getZ(), getItem().get().copy());
                    itemEntity.setNoPickUpDelay();
                    return player.level.addFreshEntity(itemEntity);
                }
                return false;
            case FAVOR:
                if(getFavor().isPresent() && getFavor().get() != 0 && getId().isPresent()) {
                    favor.getFavor(getId().get()).addFavor(player, getId().get(), getFavor().get(), FavorChangedEvent.Source.PERK);
                    return true;
                }
                return false;
            case AFFINITY:
                if(getAffinity().isPresent() && entity.isPresent() && data.isPresent() && getAffinity().get().getType() == AffinityType.TAME) {
                    LazyOptional<ITameable> tameable = entity.get().getCapability(RPGGods.TAMEABLE);
                    if(tameable.isPresent()) {
                        if(tameable.orElse(null).setTamedBy(player)) {
                            // set custom name
                            if(!entity.get().hasCustomName()) {
                                entity.get().setCustomName(entity.get().getDisplayName());
                            }
                            // send particle packet
                            if(entity.get().level instanceof ServerLevel) {
                                Vec3 pos = entity.get().getEyePosition(1.0F);
                                ((ServerLevel)entity.get().level).sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, 10, 0.5D, 0.5D, 0.5D, 0);
                            }
                            return true;
                        }
                    }
                }
                return false;
            case ARROW_DAMAGE:
                if(entity.isPresent() && getMultiplier().isPresent() && entity.get() instanceof Arrow) {
                    Arrow arrow = (Arrow) entity.get();
                    arrow.setBaseDamage(arrow.getBaseDamage() * getMultiplier().get());
                    return true;
                }
                return false;
            case ARROW_EFFECT:
                if(entity.isPresent() && getTag().isPresent() && entity.get() instanceof Arrow) {
                    Arrow arrow = (Arrow) entity.get();
                    readEffectInstance(getTag().get()).ifPresent(e -> arrow.addEffect(e));
                    return true;
                }
                return false;
            case ARROW_COUNT:
                if(entity.isPresent() && getMultiplier().isPresent() && entity.get() instanceof AbstractArrow) {
                    AbstractArrow arrow = (AbstractArrow) entity.get();
                    int arrowCount = Math.round(getMultiplier().get());
                    double motionScale = 0.8;
                    for(int i = 0; i < arrowCount; i++) {
                        AbstractArrow arrow2 = (AbstractArrow) arrow.getType().create(arrow.level);
                        arrow2.copyPosition(arrow);
                        arrow2.setDeltaMovement(arrow.getDeltaMovement().multiply(
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale));
                        arrow2.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        arrow.level.addFreshEntity(arrow2);
                    }
                    return true;
                }
                return false;
            case OFFSPRING:
                if(getMultiplier().isPresent() && entity.isPresent() && entity.get() instanceof AgeableMob
                        && object.isPresent() && object.get() instanceof BabyEntitySpawnEvent) {
                    int childCount = Math.round(getMultiplier().get());
                    if(childCount < 1) {
                        // number of babies is zero, so cancel the event
                        object.get().setCanceled(true);
                        if(entity.get().level instanceof ServerLevel) {
                            Vec3 pos = entity.get().getEyePosition(1.0F);
                            ((ServerLevel)entity.get().level).sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 6, 0.5D, 0.5D, 0.5D, 0);
                        }
                    } else if(childCount > 1) {
                        // number of babies is more than one, so spawn additional mobs
                        AgeableMob parent = (AgeableMob) entity.get();
                        for(int i = 1; i < childCount; i++) {
                            AgeableMob bonusChild = (AgeableMob) parent.getType().create(parent.level);
                            if(bonusChild != null) {
                                bonusChild.copyPosition(parent);
                                bonusChild.setBaby(true);
                                parent.level.addFreshEntity(bonusChild);
                                if(parent.level instanceof ServerLevel) {
                                    Vec3 pos = bonusChild.getEyePosition(1.0F);
                                    ((ServerLevel)parent.level).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
                                }
                            }
                        }
                    }
                    return true;
                }
                return false;
            case CROP_GROWTH:
                if(getMultiplier().isPresent()) {
                    return growCropsNearPlayer(player, favor, Math.round(getMultiplier().get()));
                }
                return false;
            case CROP_HARVEST:
                // This is handled using loot table modifiers
                return getMultiplier().isPresent();
            case DAMAGE:
                if(getMultiplier().isPresent() && object.isPresent() && object.get() instanceof LivingHurtEvent) {
                    LivingHurtEvent event = (LivingHurtEvent) object.get();
                    float amount = event.getAmount();
                    event.setAmount(amount * (getMultiplier().get()));
                }
                return false;
            case DURABILITY:
                if(getMultiplier().isPresent() && getString().isPresent()) {
                    EquipmentSlot slot = EquipmentSlot.byName(getString().get());
                    ItemStack item = player.getItemBySlot(slot);
                    // add or remove durability
                    if(!item.isEmpty() && item.isDamageableItem()) {
                        float multiplier = Math.max(-1.0F, Math.min(1.0F, getMultiplier().get()));
                        int delta = Math.round(multiplier * item.getMaxDamage());
                        int damage = Math.max(0, item.getDamageValue() - delta);
                        item.setDamageValue(damage);
                        return true;
                    }
                }
                return false;
            case AUTOSMELT:
            case UNSMELT:
                // These are handled using loot table modifiers
                return true;
            case SPECIAL_PRICE:
                if(getMultiplier().isPresent() && entity.isPresent() && entity.get() instanceof Merchant) {
                    final int diff = Math.round(getMultiplier().get());
                    final Merchant merchant = (Merchant) entity.get();
                    // cancel event if the diff is ridiculously high
                    if(diff >= 100 && object.isPresent()) {
                        object.get().setCanceled(true);
                        // cause villager to shake head and play unhappy sound
                        if(entity.get() instanceof AbstractVillager) {
                            ((AbstractVillager)entity.get()).setUnhappyCounter(40);
                            entity.get().playSound(SoundEvents.VILLAGER_NO, 0.5F, 1.0F);
                        }
                        // spawn angry particles
                        if(entity.get().level instanceof ServerLevel) {
                            Vec3 pos = entity.get().getEyePosition(1.0F);
                            ((ServerLevel)entity.get().level).sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 4, 0.5D, 0.5D, 0.5D, 0);
                        }
                        return true;
                    }
                    // add or reduce special price for all offers
                    final boolean add = diff > 0;
                    int special;
                    for(MerchantOffer offer : merchant.getOffers()) {
                        special = offer.getSpecialPriceDiff();
                        if((add && special < diff) || (!add && special > diff)) {
                            offer.setSpecialPriceDiff(diff);
                        }
                    }
                    return !merchant.getOffers().isEmpty();
                }
                return false;
            case PATRON:
                if(getPatron().isPresent()) {
                    return favor.setPatron(player, getPatron().get());
                }
                return false;
            case ADD_DECAY:
                if(getId().isPresent() && getMultiplier().isPresent()) {
                    FavorLevel level = favor.getFavor(getId().get());
                    if(level.isEnabled() && RPGGods.DEITY_MAP.getOrDefault(getId().get(), Deity.EMPTY).isEnabled()) {
                        level.setDecayRate(level.getDecayRate() + getMultiplier().get());
                        return true;
                    }
                }
                return false;
            case UNLOCK:
                if(getId().isPresent()) {
                    FavorLevel level = favor.getFavor(getId().get());
                    if(!level.isEnabled() && RPGGods.DEITY_MAP.getOrDefault(getId().get(), Deity.EMPTY).isEnabled()) {
                        favor.getFavor(getId().get()).setEnabled(true);
                        // send player feedback
                        Component message = getDisplayDescription();
                        player.displayClientMessage(message.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), true);
                        // play sound
                        player.level.playSound(player, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return false; // hacky solution to prevent feedback, since we don't need cooldown anyway
                    }}
                return false;
            case XP:
                if(entity.isPresent() && getMultiplier().isPresent() && entity.get() instanceof ExperienceOrb) {
                    ((ExperienceOrb)entity.get()).value *= getMultiplier().get();
                    return true;
                }
                return false;
        }
        return false;
    }

    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public String toString() {
        return "PerkData{" +
                "type=" + type +
                ", string=" + string +
                ", id=" + id +
                ", tag=" + tag +
                ", item=" + item +
                ", favor=" + favor +
                ", multiplier=" + multiplier +
                '}';
    }

    public Component getDisplayName() {
        return this.getType().getDisplayName();
    }

    public Component getDisplayDescription() {
        return getType().getDisplayDescription(dataToDisplay());
    }

    private Component dataToDisplay() {
        switch (getType()) {
            case POTION:
            case ARROW_EFFECT:
                if(tag.isPresent()) {
                    // format potion ID as effect name (with amplifier)
                    Optional<MobEffectInstance> effect = readEffectInstance(tag.get());
                    if(effect.isPresent()) {
                        String potencyKey = "potion.potency." + effect.get().getAmplifier();
                        return Component.translatable(effect.get().getDescriptionId())
                                .append(" ")
                                .append(Component.translatable(potencyKey));
                    }
                }
                return Component.empty();
            case SUMMON:
                if(tag.isPresent()) {
                    // format entity ID as name
                    String entity = tag.get().getString("id");
                    Optional<EntityType<?>> type = EntityType.byString(entity);
                    return type.isPresent() ? type.get().getDescription() : Component.literal(entity);
                }
                return Component.empty();
            case ITEM:
                return getItem().orElse(ItemStack.EMPTY).getHoverName();
            case FAVOR:
                if(favor.isPresent()) {
                    // format favor as discrete amount
                    // EX: multiplier of -1.1 becomes -1, 0.6 becomes +1, 1.2 becomes +1, etc.
                    String prefix = (favor.get() > 0) ? "+" : "";
                    return Component.literal(prefix + Math.round(getFavor().get()));
                }
                return Component.empty();
            case AFFINITY:
                if(getAffinity().isPresent()) {
                    return getAffinity().get().getDisplayDescription();
                }
                return Component.empty();
            case ARROW_COUNT:
            case SPECIAL_PRICE:
            case CROP_GROWTH:
                if(getMultiplier().isPresent()) {
                    // format multiplier as discrete bonus
                    // EX: multiplier of 0.0 becomes +0, 0.6 becomes +1, 1.2 becomes +1, etc.
                    String prefix = (getMultiplier().get() > 0) ? "+" : "";
                    return Component.literal(prefix + Math.round(getMultiplier().get()));
                }
                return Component.empty();
            case ADD_DECAY:
                if(getMultiplier().isPresent() && getId().isPresent()) {
                    // format multiplier as signed bonus
                    String prefix = (getMultiplier().get() > 0) ? "+" : "";
                    return Component.translatable("favor.perk.type.add_decay.description.full", prefix + getMultiplier().get(), DeityContainer.getName(getId().get()));
                }
                return Component.empty();
            case DURABILITY:
                if(getMultiplier().isPresent() && getString().isPresent()) {
                    // format multiplier as percentage
                    // EX: multiplier of -0.9 becomes -90%, 0.0 becomes +0%, 0.5 becomes +50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 0.0F ? "+" : "";
                    Component durability = Component.literal(prefix + Math.round((getMultiplier().get()) * 100.0F) + "%");
                    Component slot = Component.translatable("equipment.type." + getString().get());
                    return Component.translatable("favor.perk.type.durability.description.full", durability, slot);
                }
                return Component.empty();
            case DAMAGE:
            case ARROW_DAMAGE:
            case CROP_HARVEST:
            case OFFSPRING:
            case XP:
                if(getMultiplier().isPresent()) {
                    // format multiplier as adjusted percentage
                    // EX: multiplier of 0.0 becomes -100%, 0.5 becomes -50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 1.0F ? "+" : "";
                    return Component.literal(prefix + Math.round((getMultiplier().get() - 1.0F) * 100.0F) + "%");
                }
                return Component.empty();
            case PATRON:
                if(getPatron().isPresent()) {
                    if (getPatron().get().getDeity().isPresent()) {
                        Component deityName = DeityContainer.getName(getPatron().get().getDeity().get());
                        return Component.translatable("favor.perk.type.patron.description.add", deityName);
                    }
                    return Component.translatable("favor.perk.type.patron.description.remove");
                }
                return Component.empty();
            case UNLOCK:
                if(getId().isPresent()) {
                    ResourceLocation deityId = getId().get();
                    Component deityName = DeityContainer.getName(deityId);
                    Altar altar = RPGGods.ALTAR_MAP.getOrDefault(deityId, Altar.EMPTY);
                    String suffix = altar.isFemale() ? "female" : "male";
                    return Component.translatable("favor.perk.type.unlock.description." + suffix, deityName);
                }
                return Component.empty();
            case FUNCTION:
                if(getString().isPresent()) {
                    return Component.translatable(getString().get());
                }
                return Component.translatable("favor.perk.type.function.description.default");
            case AUTOSMELT: case UNSMELT: default:
                return Component.empty();
        }
    }

    // TODO dispatch codec for Perk Action
    @Deprecated
    public static enum Type implements StringRepresentable {
        @Deprecated FUNCTION("function"),
        @Deprecated POTION("potion"),
        @Deprecated SUMMON("summon"),
        @Deprecated ITEM("item"),
        @Deprecated FAVOR("favor"),
        @Deprecated AFFINITY("affinity"),
        @Deprecated ARROW_DAMAGE("arrow_damage"),
        @Deprecated ARROW_EFFECT("arrow_effect"),
        @Deprecated ARROW_COUNT("arrow_count"),
        @Deprecated OFFSPRING("offspring"),
        @Deprecated CROP_GROWTH("crop_growth"),
        @Deprecated CROP_HARVEST("crop_harvest"),
        @Deprecated AUTOSMELT("autosmelt"),
        @Deprecated UNSMELT("unsmelt"),
        @Deprecated SPECIAL_PRICE("special_price"),
        @Deprecated DURABILITY("durability"),
        @Deprecated DAMAGE("damage"),
        @Deprecated PATRON("patron"),
        @Deprecated UNLOCK("unlock"),
        @Deprecated ADD_DECAY("add_decay"),
        @Deprecated XP("xp");

        private static final Codec<PerkAction.Type> CODEC = Codec.STRING.comapFlatMap(PerkAction.Type::fromString, PerkAction.Type::getSerializedName).stable();

        private final String name;

        private Type(final String id) {
            this.name = id;
        }

        public static DataResult<PerkAction.Type> fromString(String id) {
            for(final PerkAction.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error(() -> "Failed to parse perk data type '" + id + "'");
        }

        /**
         * @param data the data to pass to the translation key
         * @return Translation key for the description of this perk type, using the provided data
         */
        public Component getDisplayDescription(final Component data) {
            return Component.translatable("favor.perk.type." + getSerializedName() + ".description", data);
        }

        /**
         * @return Translation key for the name of this perk type
         */
        public MutableComponent getDisplayName() {
            return Component.translatable("favor.perk.type." + getSerializedName());
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
