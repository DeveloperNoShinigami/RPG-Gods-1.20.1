/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.RPGGods;

import java.util.Optional;

public class SummonAction extends PerkAction {

    public static final Codec<SummonAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ForgeRegistries.ENTITY_TYPES.getCodec().fieldOf("id").forGetter(o -> o.entityType))
            .and(Codec.STRING.optionalFieldOf("nbt", "{}").forGetter(o -> o.nbtString))
            .and(Codec.intRange(0, 64).optionalFieldOf("distance", 8).forGetter(o -> o.distance))
            .apply(instance, SummonAction::new));

    private final EntityType<?> entityType;
    private final String nbtString;
    private final CompoundTag nbt;
    private final int distance;

    public SummonAction(boolean isHidden, EntityType<?> entityType, String nbtString, int distance) {
        super(isHidden);
        this.entityType = entityType;
        this.nbtString = nbtString;
        this.nbt = parseTag(nbtString).orElse(new CompoundTag());
        this.distance = distance;
    }

    protected static Optional<CompoundTag> parseTag(final String tagString) {
        try {
            CompoundTag tag = TagParser.parseTag(tagString);
            return Optional.of(tag);
        } catch (CommandSyntaxException e) {
            RPGGods.LOGGER.error("Failed to parse NBT tag in perk SummonAction\n" + e.getMessage());
        }
        return Optional.empty();
    }


    /**
     * Attempts to summon an entity near the player
     * @param player the server player
     * @param entityType the entity type
     * @param entityTag the NBT tag of the entity
     * @param distance the maximum distance from the player to summon. Using 0 will skip the usual canSpawn checks.
     * @return the entity if it was summoned, or an empty optional
     **/
    public static Optional<Entity> summonEntityNearPlayer(final ServerPlayer player, final EntityType<?> entityType,
                                                          final CompoundTag entityTag, final int distance) {
        final ServerLevel level = player.serverLevel();
        // create entity instance
        final Entity entity = entityType.create(player.level());
        final boolean waterMob = entityType == EntityType.DROWNED || entity instanceof WaterAnimal || entity instanceof Guardian
                || (entity instanceof Mob mob && mob.getNavigation() instanceof WaterBoundPathNavigation);
        // find a place to spawn the entity
        RandomSource random = player.getRandom();
        BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
        // attempt up to 32 times to find a suitable spawn position
        for(int range = 1 + distance, attempts = Math.min(32, range * 3); attempts > 0; attempts--) {
            if(range > 1) {
                spawnPos.setWithOffset(player.blockPosition(),
                        random.nextInt(range) - random.nextInt(range),
                        random.nextInt(2) - random.nextInt(2),
                        random.nextInt(range) - random.nextInt(range));
            } else {
                spawnPos.set(player.blockPosition());
            }
            // check if this is a valid position
            boolean canSpawnHere = (range == 1)
                    || SpawnPlacements.checkSpawnRules(entityType, level, MobSpawnType.SPAWN_EGG, spawnPos, random)
                    || (waterMob && level.getBlockState(spawnPos).is(Blocks.WATER))
                    || (!waterMob && level.getBlockState(spawnPos.below()).canOcclude()
                    && level.getBlockState(spawnPos).isAir()
                    && level.getBlockState(spawnPos.above()).isAir());
            if(canSpawnHere) {
                // spawn the entity at this position and finish
                entity.load(entityTag);
                entity.setPos(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.01D, spawnPos.getZ() + 0.5D);
                level.tryAddFreshEntityWithPassengers(entity);
                if(entity instanceof Mob mob) {
                    ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.TRIGGERED, null, entityTag);
                }
                return Optional.of(entity);
            }
        }
        // failed to spawn entity, discard it
        entity.discard();
        return Optional.empty();
    }

    @Override
    public boolean apply(PerkActionContext context) {
        return summonEntityNearPlayer(context.getPlayer(), entityType, nbt.copy(), distance).isPresent();
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        return Component.translatable(PREFIX + "summon" + SUFFIX, entityType.getDescription());
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.SUMMON.get();
    }
}
