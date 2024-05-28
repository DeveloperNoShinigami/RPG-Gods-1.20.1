/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import rpggods.RGRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CropAgeAction extends PerkAction {

    public static final Codec<CropAgeAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(IntProvider.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(o -> o.count))
            .and(IntProvider.CODEC.fieldOf("amount").forGetter(o -> o.amount))
            .apply(instance, CropAgeAction::new));

    /** Array of {@link IntegerProperty}s commonly used by crops **/
    private static final IntegerProperty[] AGES = new IntegerProperty[] {
            BlockStateProperties.AGE_1, BlockStateProperties.AGE_2, BlockStateProperties.AGE_3,
            BlockStateProperties.AGE_4, BlockStateProperties.AGE_5, BlockStateProperties.AGE_7,
            BlockStateProperties.AGE_15, BlockStateProperties.AGE_25
    };

    /** Map of {@link IntegerProperty} to maximum age as an integer **/
    private static final Map<IntegerProperty, Integer> MAX_AGES = Arrays
            .stream(AGES)
            .collect(ImmutableMap.toImmutableMap(
                    Function.identity(),
                    property -> property.getPossibleValues()
                            .stream()
                            .max(Integer::compareTo)
                            .orElse(0)
            ));

    private final IntProvider count;
    private final IntProvider amount;

    public CropAgeAction(boolean isHidden, IntProvider count, IntProvider amount) {
        super(isHidden);
        this.count = count;
        this.amount = amount;
    }

    /**
     * @param level the level
     * @param blockState the block state to modify
     * @param blockPos the block position to modify
     * @param amount the amount of age to add or subtract
     * @return true if the block at this position was updated
     */
    private static boolean addCropAge(final Level level, final BlockState blockState, final BlockPos blockPos, final int amount) {
        // validate amount
        if(amount == 0) {
            return false;
        }
        // check each age property to see if exists in the block state
        for(final IntegerProperty ageProperty : AGES) {
            // verify block state has this age property
            if(!blockState.hasProperty(ageProperty)) {
                continue;
            }
            int currentAge = blockState.getValue(ageProperty);
            int maxAge = MAX_AGES.getOrDefault(ageProperty, 0);
            int targetAge = Mth.clamp(currentAge + amount, 0, maxAge);
            // attempt to set age property to the target value
            if(ageProperty.getPossibleValues().contains(targetAge)) {
                // update the block state
                level.setBlock(blockPos, blockState.setValue(ageProperty, targetAge), Block.UPDATE_ALL);
                // spawn particles when on server
                if(level instanceof ServerLevel serverLevel) {
                    ParticleOptions particle = (amount > 0) ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
                    serverLevel.sendParticles(particle, blockPos.getX() + 0.5D, blockPos.getY() + 0.25D, blockPos.getZ() + 0.5D, 10, 0.5D, 0.5D, 0.5D, 0);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // determine how many blocks to affect
        final RandomSource random = context.getRandom();
        final int repetitions = this.count.sample(random);
        if(repetitions <= 0) {
            return false;
        }
        final int maxAttempts = 12;
        final int variationY = 1;
        final int radius = 5;
        int attempts = maxAttempts;
        int total = 0;
        // repeatedly find a crop to affect
        final BlockPos.MutableBlockPos blockPos = context.getPos().mutable();
        while (attempts-- > 0 && total < repetitions) {
            // get random block in radius
            final int x1 = random.nextInt(radius * 2) - radius;
            final int y1 = random.nextInt(variationY * 2) - variationY;
            final int z1 = random.nextInt(radius * 2) - radius;
            blockPos.setWithOffset(context.getPos(), x1, y1, z1);
            final BlockState state = context.getLevel().getBlockState(blockPos);
            // if the block can be grown, grow it, then increment count and reset attempts
            if (state.getBlock() instanceof BonemealableBlock && addCropAge(context.getLevel(), state, blockPos, amount.sample(random))) {
                total++;
                attempts = maxAttempts;
            }
        }
        return total > 0;
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        final String sAmount = (this.amount.getMaxValue() > 0) ? "+" + this.amount.getMaxValue() : "" + this.amount.getMinValue();
        return ImmutableList.of(Component.literal(sAmount));
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.CROP_AGE.get();
    }
}
