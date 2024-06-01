/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.util;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.PerkDispatcher;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Perk;
import rpggods.data.perk.action.CropYieldAction;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.condition.PerkConditionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class CropYieldLootModifier extends LootModifier {

    public static final Supplier<Codec<CropYieldLootModifier>> CODEC_SUPPLIER = Suppliers.memoize(() -> RecordCodecBuilder.create(inst ->
            codecStart(inst)
                    .and(TagKey.codec(ForgeRegistries.Keys.BLOCKS).fieldOf("crops").forGetter(CropYieldLootModifier::getCrops))
                    .apply(inst, CropYieldLootModifier::new)));

    private final TagKey<Block> crops;

    protected CropYieldLootModifier(final LootItemCondition[] conditionsIn, final TagKey<Block> crops) {
        super(conditionsIn);
        this.crops = crops;
    }

    public TagKey<Block> getCrops() {
        return crops;
    }

    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        BlockState block = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        // validate all required data is present and block is a crop
        if(entity == null || block == null || !block.is(crops)) {
            return generatedLoot;
        }
        // validate player
        if(!(entity instanceof ServerPlayer player) || player.isSpectator() || player.isCreative()) {
            return generatedLoot;
        }
        // load and validate favor
        final IFavor favor = RPGGods.getFavor(player).orElse(null);
        if(null == favor || !favor.isEnabled()) {
            return generatedLoot;
        }
        // collect all crop yield perks that pass their perk conditions
        List<Perk> perks = new ArrayList<>();
        for(DeityContainer container : DeityContainer.getRegistry(entity.level().isClientSide()).values()) {
            final PerkConditionContext conditionContext = new PerkConditionContext(container.getId(), player, favor, Optional.empty(), Optional.of(entity), Optional.empty(), Optional.of(block));
            for(Perk p : container.getPerksByAction(RGRegistry.PerkActionReg.CROP_YIELD.get()).values()) {
                if(p.getRange().isInRange(favor) && p.getCondition().test(conditionContext)) {
                    perks.add(p);
                }
            }
        }
        // verify not empty
        if(perks.isEmpty()) {
            return generatedLoot;
        }
        // shuffle perks
        Collections.shuffle(perks);
        // attempt to run all crop yield perks until one is successful
        for(Perk perk : perks) {
            if(PerkDispatcher.runPerk(perk, player, favor, Optional.empty(), Optional.of(entity), Optional.empty(), Optional.of(block), Optional.empty())) {
                // determine bonus yield
                int bonusYield = 0;
                for(PerkAction action : perk.getActions()) {
                    if(action instanceof CropYieldAction cropYieldAction) {
                        bonusYield += cropYieldAction.getBonusYield(context.getRandom());
                    }
                }
                // grow each item stack in the result by the bonus yield amount.
                // note: any amount over the stack max size is discarded
                for(ItemStack i : generatedLoot) {
                    i.setCount(Mth.clamp(i.getCount() + bonusYield, 0, i.getMaxStackSize()));
                }
                // remove empty item stacks
                generatedLoot.removeIf(ItemStack::isEmpty);
                return generatedLoot;
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC_SUPPLIER.get();
    }
}
