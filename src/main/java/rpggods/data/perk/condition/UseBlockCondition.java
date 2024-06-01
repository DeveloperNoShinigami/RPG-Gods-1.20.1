/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.util.RGComponentUtils;
import rpggods.util.DeferredHolderSet;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class UseBlockCondition extends PerkCondition {

    public static final Codec<UseBlockCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DeferredHolderSet.codec(ForgeRegistries.Keys.BLOCKS).optionalFieldOf("block", DeferredHolderSet.empty()).forGetter(o -> o.block)
    ).apply(instance, UseBlockCondition::new));

    private final DeferredHolderSet<Block> block;

    public UseBlockCondition(DeferredHolderSet<Block> block) {
        this.block = block;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        final HolderSet<Block> holderSet = block.get(BuiltInRegistries.BLOCK);
        // validate holder set has contents and block state exists
        if(holderSet.size() <= 0 && context.getBlockState().isEmpty()) {
            return false;
        }
        // validate block is in holder set
        return context.getBlockState().get().is(holderSet);
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        // create components for holder set
        final HolderSet<Block> holderSet = block.get(BuiltInRegistries.BLOCK);
        final List<Component> blockComponentList = RGComponentUtils.createHolderSetDescription(BuiltInRegistries.BLOCK, holderSet, Block::getName);
        // join holder components
        final Component delimiter = Component.translatable("favor.perk.condition.or");
        final Component blockComponent = RGComponentUtils.join(blockComponentList, delimiter);
        // create description
        return Component.translatable(PREFIX + "use_block", blockComponent);
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.USE_BLOCK.get();
    }
}
