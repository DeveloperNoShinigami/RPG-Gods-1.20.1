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
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.PerkDispatcher;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Perk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class AutosmeltAndUnsmeltLootModifier extends LootModifier {

    // TODO verify the loot condition can check for ore tag and we don't need to define that here
    public static final Supplier<Codec<AutosmeltAndUnsmeltLootModifier>> CODEC_SUPPLIER = Suppliers.memoize(() -> RecordCodecBuilder.create(inst ->
            codecStart(inst)
                    .and(Codec.unboundedMap(TagKey.codec(ForgeRegistries.Keys.ITEMS), ForgeRegistries.ITEMS.getCodec()).fieldOf("ore_to_stone").forGetter(o -> o.oreToStoneMap))
                    .apply(inst, AutosmeltAndUnsmeltLootModifier::new)));;

    private final Map<TagKey<Item>, Item> oreToStoneMap;

    protected AutosmeltAndUnsmeltLootModifier(final LootItemCondition[] conditions, final Map<TagKey<Item>, Item> oreToStoneMap) {
        super(conditions);
        this.oreToStoneMap = oreToStoneMap;
    }

    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ItemStack itemStack = context.getParamOrNull(LootContextParams.TOOL);
        BlockState block = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        // validate all required data is present
        if(entity == null || itemStack == null || block == null) {
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
        // collect all perks that have autosmelt or unsmelt
        List<Perk> autosmeltPerks = new ArrayList<>();
        List<Perk> unsmeltPerks = new ArrayList<>();
        for(DeityContainer container : DeityContainer.getRegistry(context.getLevel().isClientSide()).values()) {
            autosmeltPerks.addAll(container.getPerksByAction(RGRegistry.PerkActionReg.AUTOSMELT.get()).values());
            unsmeltPerks.addAll(container.getPerksByAction(RGRegistry.PerkActionReg.UNSMELT.get()).values());
        }
        // verify at least one mining effect can activate
        if(autosmeltPerks.isEmpty() || unsmeltPerks.isEmpty()) {
            return generatedLoot;
        }
        // shuffle autosmelt perks
        Collections.shuffle(autosmeltPerks);
        // attempt to run all autosmelt perks until one is successful
        for(Perk perk : autosmeltPerks) {
            if(PerkDispatcher.runPerk(perk, player, favor, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())) {
                generatedLoot.replaceAll(i -> smelt(i, context));
                return generatedLoot;
            }
        }
        // shuffle unsmelt perks
        Collections.shuffle(unsmeltPerks);
        // attempt to run all unsmelt perks until one is successful
        for(Perk perk : unsmeltPerks) {
            if(PerkDispatcher.runPerk(perk, player, favor, Optional.empty(), Optional.of(entity), Optional.empty(), Optional.of(block), Optional.empty())) {
                generatedLoot.replaceAll(i -> unsmelt(i, oreToStoneMap));
                return generatedLoot;
            }
        }
        return generatedLoot;
    }

    /**
     * @param stack   the item stack to smelt
     * @param context the loot context
     * @return the ItemStack that would be the result when smelting the given item,
     * or the original ItemStack if no recipe was found
     */
    private static ItemStack smelt(ItemStack stack, LootContext context) {
        final RegistryAccess registryAccess = context.getLevel().registryAccess();
        return context.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), context.getLevel())
                .map(recipe -> recipe.getResultItem(registryAccess))
                .filter(itemStack -> !itemStack.isEmpty())
                .map(itemStack -> ItemHandlerHelper.copyStackWithSize(itemStack, stack.getCount() * itemStack.getCount()))
                .orElse(stack);
    }

    /**
     * @param stack the item stack
     * @param oreToStoneMap a map of ore tag keys to stone items
     * @return an itemstack with the associated stone item, or the original item stack if it was not an ore
     */
    private static ItemStack unsmelt(ItemStack stack, Map<TagKey<Item>, Item> oreToStoneMap) {
        // find first matching tag key
        for(Map.Entry<TagKey<Item>, Item> entry : oreToStoneMap.entrySet()) {
            if(stack.is(entry.getKey())) {
                return new ItemStack(entry.getValue(), stack.getCount());
            }
        }
        // no match found
        return stack;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC_SUPPLIER.get();
    }
}
