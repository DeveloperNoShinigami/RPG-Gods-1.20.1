/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.entity.AltarEntity;
import rpggods.util.RGCodecUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public abstract class PerkCondition {

    public static final Codec<PerkCondition> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> RGRegistry.PERK_CONDITION_TYPES_SUPPLIER.get().getCodec())
            .dispatch(PerkCondition::getCodec, Function.identity());
    public static final Codec<List<PerkCondition>> LIST_CODEC = RGCodecUtils.listOrElementCodec(DIRECT_CODEC);

    private final List<Component> description = new ArrayList<>();
    private final List<Component> descriptionView = Collections.unmodifiableList(description);

    public PerkCondition() {
        // no op
    }

    /**
     * @param context the {@link PerkConditionContext} to test
     * @return true if the perk condition matches the given parameters
     */
    public abstract boolean test(final PerkConditionContext context);

    /**
     * @param registryAccess the Registry Access instance
     * @return the name of this perk condition as a {@link Component}
     */
    public abstract List<Component> createDescription(final RegistryAccess registryAccess);

    /**
     * @return the Codec used to encode/decode this {@link PerkCondition}
     * @see rpggods.RGRegistry.PerkConditionReg
     */
    public abstract Codec<? extends PerkCondition> getCodec();

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

    protected static Optional<CompoundTag> parseTag(final String tagString) {
        try {
            CompoundTag tag = TagParser.parseTag(tagString);
            return Optional.of(tag);
        } catch (CommandSyntaxException e) {
            RPGGods.LOGGER.error("Failed to parse NBT in PerkCondition\n" + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * @param resourceKey a resource key
     * @param <T> the resource key type
     * @return the text representation of the resource key location
     */
    protected static <T> Component createResourceKeyDescription(ResourceKey<T> resourceKey) {
        return Component.literal(resourceKey.location().toString()).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Checks if the player is in a specific structure, according to the Chunk data
     * @param world the world
     * @param pos the player location
     * @return True if this condition has a structure and the position is inside the structure
     */
    public boolean isInStructure(final ServerLevel world, final BlockPos pos) {
        /*final Registry<Structure> registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);

        if(type == PerkCondition.Type.STRUCTURE && data.isPresent()) {
            
            Structure structure = null;
            TagKey<Structure> structureTagKey = null;
            
            if(id.isPresent()) {
                structure = registry.get(id.get());
            } else if(data.get().startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(data.get().substring(1));
                if(tagId != null) {
                    structureTagKey = TagKey.create(registry.key(), tagId);
                }
            }
            // iterate over all structures at this position
            for(Structure f : world.structureManager().getAllStructuresAt(pos).keySet()) {
                // check structure value
                if(structure != null && f == structure) {
                    return true;
                }
                // check structure tag
                if(structureTagKey != null && Holder.direct(f).is(structureTagKey)) {
                    return true;
                }
            }
        }*/
        return false;
    }

    /**
     * @param registryAccess the registry access
     * @param key the structure key
     * @param structures a set of structures
     * @return true if any structure matches the given key
     */
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


    /**
     * @param level the level
     * @param origin the position of the event
     * @param distance the maximum distance to the altar
     * @return true if the origin is within the given distance to an altar to the given deity
     */
    public boolean isNearAltar(final Level level, final Vec3 origin, final double distance) {
        /*if(type == PerkCondition.Type.NEAR_ALTAR && id.isPresent()) {
            AABB aabb = new AABB(BlockPos.containing(origin)).inflate(distance, distance / 2.0D, distance);
            List<AltarEntity> altars = level.getEntities(EntityTypeTest.forClass(AltarEntity.class), aabb, a -> a.getDeity().isPresent() && id.get().equals(a.getDeity().get()));
            return !altars.isEmpty();
        }*/
        return false;
    }

/*    @Override
    public String toString() {
        return "PerkCondition: " + " type[" + type + "]" + " data[" + data + "]";
    }*/

    public Component getDisplayName() {
        return Component.empty(); // this.getType().getDisplayName(createDescription(getData().orElse("")));
    }

    /*public boolean match(final ResourceLocation deity, final Player player, final IFavor favor,
                         final Optional<ResourceLocation> data, final Optional<CompoundTag> entityTag) {
        boolean idMatch;
        boolean tagMatch;
        switch (this.getType()) {
            case PATRON: return favor.getPatron().isPresent() && getData().isPresent()
                    && favor.getPatron().get().toString().equals(getData().get());
            case BIOME: return isInBiome(player.level, player.blockPosition());
            case DAY: return player.level.isDay();
            case NIGHT: return player.level.isNight();
            case RANDOM_TICK: return true;
            case ENTER_COMBAT: return player.getCombatTracker().getCombatDuration() < RGEvents.COMBAT_TIMER;
            case PLAYER_CROUCHING: return player.isCrouching();
            case UNLOCKED: return getId().isPresent() && favor.getFavor(getId().get()).isEnabled();
            case LEVEL_UP: case LEVEL_DOWN: return getId().isPresent() && deity.equals(getId().get());
            case MAINHAND_ITEM:
                // match item registry name
                ItemStack heldItem = player.getMainHandItem();
                idMatch = getId().isPresent() && getId().get().equals(ForgeRegistries.ITEMS.getKey(heldItem.getItem()));
                // match item tag
                if(getData().isPresent() && getData().get().startsWith("#")) {
                    // match item tag
                    ResourceLocation tagId = ResourceLocation.tryParse(getData().get().substring(1));
                    TagKey<Item> tag = ItemTags.create(tagId);
                    idMatch = heldItem.is(tag);
                }
                // match nbt tag
                tagMatch = true;
                if(idMatch && tag.isPresent()) {
                    tagMatch = NbtUtils.compareNbt(tag.get(), heldItem.getTag(), true);
                }
                return idMatch && tagMatch;
            case PLAYER_INTERACT_BLOCK:
                if(data.isPresent()) {
                    // locate block from registry
                    Block block = ForgeRegistries.BLOCKS.getValue(data.get());
                    if(null == block) {
                        return false;
                    }
                    // match block registry name
                    idMatch = getId().isPresent() && getId().get().equals(data.get());
                    // match block tag
                    if(getData().isPresent() && getData().get().startsWith("#")) {
                        // match block tag
                        ResourceLocation tagId = ResourceLocation.tryParse(getData().get().substring(1));
                        TagKey<Block> tagKey = BlockTags.create(tagId);
                        idMatch = block.defaultBlockState().is(tagKey);
                    }
                    return idMatch;
                }
                return false;
            case PLAYER_RIDE_ENTITY:
                return player.isPassenger() && player.getVehicle() != null && getId().isPresent()
                        && getId().get().equals(ForgeRegistries.ENTITY_TYPES.getKey(player.getVehicle().getType()))
                        && (!tag.isPresent() || NbtUtils.compareNbt(tag.get(), entityTag.get(), true));
            case DIMENSION: return getId().isPresent() && getId().get().equals(player.level.dimension().location());
            case STRUCTURE: return hasStructure(player.level().registryAccess(), this.structure, player.level().structureManager().getAllStructuresAt(blockpos).keySet());
            // match data to perk condition data
            case RITUAL:
            case EFFECT_START:
                return getId().isPresent() && data.isPresent() && getId().get().equals(data.get());
            case NEAR_ALTAR: return getId().isPresent() && isNearAltar(player.level, player.position(), 8.0D);
            // match data and NBT tag
            case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER:
            case PLAYER_HURT_ENTITY:
            case PLAYER_KILLED_ENTITY:
            case PLAYER_INTERACT_ENTITY:
                idMatch = getId().isPresent() && data.isPresent() && getId().get().equals(data.get());
                tagMatch = true;
                if(idMatch && tag.isPresent()) {
                    tagMatch = entityTag.isPresent() && NbtUtils.compareNbt(tag.get(), entityTag.get(), true);
                }
                return idMatch && tagMatch;
        }
        return false;
    }*/

    /*private Component dataToDisplay(final String d) {
        ResourceLocation rl = ResourceLocation.tryParse(d);
        switch (getType()) {
            case PATRON: case UNLOCKED: case NEAR_ALTAR: case LEVEL_UP: case LEVEL_DOWN:
                if(rl != null) {
                    return Component.translatable(Altar.createTranslationKey(rl));
                }
                return Component.literal(d);
            case MAINHAND_ITEM: case RITUAL:
                // display name of item tag
                if(d.startsWith("#")) {
                    return Component.translatable("favor.perk.condition.mainhand_item.tag", d);
                }
                // display name of item
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if(item != null) {
                    ItemStack itemStack = new ItemStack(item);
                    tag.ifPresent(nbt -> itemStack.setTag(nbt));
                    return itemStack.getItem().getName(itemStack);
                }
                return Component.literal(d);
            case BIOME:
                if(rl != null) {
                    return Component.translatable("biome." + rl.getNamespace() + "." + rl.getPath());
                }
                return Component.literal(d);
            case PLAYER_INTERACT_BLOCK:
                if(!d.startsWith("#")) {
                    Block block = ForgeRegistries.BLOCKS.getValue(rl);
                    if(block != null) {
                        return block.getName();
                    }
                }
                return Component.literal(d);
            case EFFECT_START:
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
                if(effect != null) {
                    return effect.getDisplayName();
                }
                return Component.literal(d);
            case DIMENSION:
                if(rl != null) {
                    return Component.translatable("dimension." + rl.getNamespace() + "." + rl.getPath());
                }
                return Component.literal(d);
            case STRUCTURE:
                if(rl != null) {
                    return Component.translatable("structure." + rl.getNamespace() + "." + rl.getPath());
                }
                return Component.literal(d);
            case PLAYER_HURT_ENTITY: case PLAYER_KILLED_ENTITY: case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER: case PLAYER_INTERACT_ENTITY: case PLAYER_RIDE_ENTITY:
                // read data as Entity ID
                Optional<EntityType<?>> entityType = EntityType.byString(d);
                return entityType.isPresent()
                        ? Component.translatable(entityType.get().getDescriptionId())
                        : Component.literal("<ERR>");
            case DAY: case NIGHT: case RANDOM_TICK: case ENTER_COMBAT: case PLAYER_CROUCHING: default:
                return Component.empty();
        }
    }*/

    // TODO dispatch registry for PerkCondition
    @Deprecated
    public static enum Type implements StringRepresentable {
        @Deprecated PATRON("patron"),
        @Deprecated BIOME("biome"),
        @Deprecated DAY("day"),
        @Deprecated NIGHT("night"),
        @Deprecated RANDOM_TICK("random_tick"),
        @Deprecated MAINHAND_ITEM("mainhand_item"),
        @Deprecated STRUCTURE("structure"),
        @Deprecated DIMENSION("dimension"),
        @Deprecated EFFECT_START("effect_start"),
        @Deprecated ENTITY_HURT_PLAYER("entity_hurt_player"),
        @Deprecated ENTITY_KILLED_PLAYER("entity_killed_player"),
        @Deprecated PLAYER_HURT_ENTITY("player_hurt_entity"),
        @Deprecated PLAYER_KILLED_ENTITY("player_killed_entity"),
        @Deprecated PLAYER_INTERACT_ENTITY("player_interact_entity"),
        @Deprecated PLAYER_INTERACT_BLOCK("player_interact_block"),
        @Deprecated PLAYER_RIDE_ENTITY("player_ride_entity"),
        @Deprecated PLAYER_CROUCHING("player_crouching"),
        @Deprecated RITUAL("ritual"),
        @Deprecated UNLOCKED("unlocked"),
        @Deprecated ENTER_COMBAT("enter_combat"),
        @Deprecated NEAR_ALTAR("near_altar"),
        @Deprecated LEVEL_UP("level_up"),
        @Deprecated LEVEL_DOWN("level_down");

        private static final Codec<PerkCondition.Type> CODEC = Codec.STRING.comapFlatMap(PerkCondition.Type::fromString, PerkCondition.Type::getSerializedName).stable();
        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<Type> fromString(String id) {
            for(final PerkCondition.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error(() -> "Failed to parse perk condition '" + id + "'");
        }

        public Component getDisplayName(Component data) {
            return Component.translatable("favor.perk.condition." + getSerializedName(), data);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
