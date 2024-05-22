package rpggods;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import rpggods.block.AltarLightBlock;
import rpggods.block.BrazierBlock;
import rpggods.block.entity.BrazierBlockEntity;
import rpggods.data.deity.Altar;
import rpggods.data.deity.Deity;
import rpggods.data.deity.Offering;
import rpggods.data.deity.Sacrifice;
import rpggods.data.favor.Favor;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Perk;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.condition.BiomeCondition;
import rpggods.data.perk.condition.PatronCondition;
import rpggods.data.perk.condition.PerkCondition;
import rpggods.data.perk.condition.UnlockedCondition;
import rpggods.data.tameable.ITameable;
import rpggods.entity.AltarEntity;
import rpggods.item.AltarItem;
import rpggods.item.ScrollItem;
import rpggods.menu.AltarContainerMenu;
import rpggods.menu.FavorContainerMenu;
import rpggods.util.AltarStructureProcessor;
import rpggods.util.AutosmeltOrCobbleModifier;
import rpggods.util.CropMultiplierModifier;
import rpggods.util.ShapedAltarRecipe;
import rpggods.util.ShapelessAltarRecipe;

import java.util.function.Supplier;

public final class RGRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RPGGods.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RPGGods.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), RPGGods.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RPGGods.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RPGGods.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RPGGods.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RPGGods.MODID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RPGGods.MODID);
    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(BuiltInRegistries.STRUCTURE_PROCESSOR.key(), RPGGods.MODID);
    // ALTARS //
    private static final DeferredRegister<Altar> ALTARS = DeferredRegister.create(Keys.ALTARS, RPGGods.MODID);
    // DEITIES //
    private static final DeferredRegister<Deity> DEITIES = DeferredRegister.create(Keys.DEITIES, RPGGods.MODID);
    // OFFERINGS //
    private static final DeferredRegister<Offering> OFFERINGS = DeferredRegister.create(Keys.OFFERINGS, RPGGods.MODID);
    // SACRIFICES //
    private static final DeferredRegister<Sacrifice> SACRIFICES = DeferredRegister.create(Keys.SACRIFICES, RPGGods.MODID);
    // PERKS //
    private static final DeferredRegister<Codec<? extends PerkCondition>> PERK_CONDITION_TYPES = DeferredRegister.create(Keys.PERK_CONDITION_TYPES, RPGGods.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends PerkCondition>>> PERK_CONDITION_TYPES_SUPPLIER = PERK_CONDITION_TYPES.makeRegistry(() -> new RegistryBuilder<>());
    private static final DeferredRegister<PerkCondition> PERK_CONDITIONS = DeferredRegister.create(Keys.PERK_CONDITIONS, RPGGods.MODID);
    private static final DeferredRegister<Codec<? extends PerkAction>> PERK_ACTION_TYPES = DeferredRegister.create(Keys.PERK_ACTION_TYPES, RPGGods.MODID);
    public static final Supplier<IForgeRegistry<Codec<? extends PerkAction>>> PERK_ACTION_TYPES_SUPPLIER = PERK_ACTION_TYPES.makeRegistry(() -> new RegistryBuilder<>());
    private static final DeferredRegister<PerkAction> PERK_ACTIONS = DeferredRegister.create(Keys.PERK_ACTIONS, RPGGods.MODID);
    private static final DeferredRegister<Perk> PERKS = DeferredRegister.create(Keys.PERKS, RPGGods.MODID);

    public static void register() {
        BlockReg.register();
        ItemReg.register();
        CreativeTabReg.register();
        EntityReg.register();
        BlockEntityReg.register();
        MenuReg.register();
        RecipeReg.register();
        LootModifierReg.register();
        CapabilityReg.register();
        StructureProcessorReg.register();
        // Custom registries
        AltarReg.register();
        DeityReg.register();
        OfferingReg.register();
        SacrificeReg.register();
        PerkConditionReg.register();
        PerkActionReg.register();
        PerkReg.register();
        // Register listener for data pack registry event
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry::onRegisterDatapackRegistries);
    }

    public static void onRegisterDatapackRegistries(final DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Keys.ALTARS, Altar.CODEC, Altar.CODEC);
        event.dataPackRegistry(Keys.DEITIES, Deity.CODEC, Deity.CODEC);
        event.dataPackRegistry(Keys.OFFERINGS, Offering.CODEC, Offering.CODEC);
        event.dataPackRegistry(Keys.SACRIFICES, Sacrifice.CODEC, Sacrifice.CODEC);
        event.dataPackRegistry(Keys.PERKS, Perk.CODEC, Perk.CODEC);
    }

    public static final class BlockReg {
        private static void register() {
            BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<AltarLightBlock> LIGHT = BLOCKS.register("light", () ->
                new AltarLightBlock(BlockBehaviour.Properties.of()
                        .strength(-1F).mapColor(MapColor.NONE).noCollission().randomTicks()
                        .lightLevel(b -> b.getValue(AltarLightBlock.LEVEL))));
        public static final RegistryObject<Block> BRAZIER = BLOCKS.register("brazier", () ->
                new BrazierBlock(BlockBehaviour.Properties.of()
                        .strength(3.0F).mapColor(MapColor.METAL).sound(SoundType.METAL)
                        .lightLevel(b -> b.getValue(BrazierBlock.LIT) ? 15 : 0)));

    }

    public static final class ItemReg {
        public static final RegistryObject<AltarItem> ALTAR = ITEMS.register("altar", () -> new AltarItem(new Item.Properties()));
        public static final RegistryObject<ScrollItem> SCROLL = ITEMS.register("scroll", () -> new ScrollItem(new Item.Properties()));
        public static final RegistryObject<BlockItem> BRAZIER = ITEMS.register("brazier", () -> new BlockItem(BlockReg.BRAZIER.get(), new Item.Properties()));

        private static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class CreativeTabReg {
        private static void register() {
            CREATIVE_MODE_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class EntityReg {

        private static void register() {
            ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry.EntityReg::registerEntityAttributes);
        }

        private static void registerEntityAttributes(final EntityAttributeCreationEvent event) {
            event.put(ALTAR.get(), AltarEntity.registerAttributes().build());
        }

        public static final RegistryObject<EntityType<? extends AltarEntity>> ALTAR = ENTITY_TYPES.register("altar", () ->
                EntityType.Builder
                        .of(AltarEntity::new, MobCategory.MISC)
                        .sized(0.8F, 2.48F).clientTrackingRange(10)
                        .build("altar"));
    }

    public static final class BlockEntityReg {

        private static void register() {
            BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<BlockEntityType<? extends BrazierBlockEntity>> BRAZIER = BLOCK_ENTITY_TYPES.register("brazier", () ->
                BlockEntityType.Builder.of(BrazierBlockEntity::new, BlockReg.BRAZIER.get()) .build(null)
        );
    }

    public static final class MenuReg {

        private static void register() {
            MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<MenuType<AltarContainerMenu>> ALTAR_CONTAINER = MENU_TYPES.register("altar_container", () ->
                IForgeMenuType.create((windowId, inv, data) -> {
                    final int entityId = data.readInt();
                    Entity entity = inv.player.level().getEntity(entityId);
                    AltarEntity altarEntity = (AltarEntity) entity;
                    return new AltarContainerMenu(windowId, inv, altarEntity.getInventory(), altarEntity);
                })
        );

        public static final RegistryObject<MenuType<FavorContainerMenu>> FAVOR_CONTAINER = MENU_TYPES.register("favor_container", () ->
                IForgeMenuType.create((windowId, inv, data) -> {
                    CompoundTag nbt = data.readNbt();
                    // load favor capability
                    LazyOptional<IFavor> ifavor = RPGGods.getFavor(inv.player);
                    IFavor favor = ifavor.orElse(Favor.EMPTY);
                    if(favor != Favor.EMPTY && nbt != null) {
                        favor.deserializeNBT(nbt);
                    }
                    // load deity
                    boolean hasDeity = data.readBoolean();
                    ResourceLocation deityId = null;
                    if(hasDeity) {
                        deityId = data.readResourceLocation();
                    }
                    return new FavorContainerMenu(windowId, inv, favor, deityId);
                })
        );
    }

    public static final class RecipeReg {

        private static void register() {
            RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<ShapelessAltarRecipe.Serializer> SHAPELESS_ALTAR_RECIPE_SERIALIZER =
                RECIPE_SERIALIZERS.register(ShapelessAltarRecipe.NAME, () -> new ShapelessAltarRecipe.Serializer());
        public static final RegistryObject<ShapedAltarRecipe.Serializer> SHAPED_ALTAR_RECIPE_SERIALIZER =
                RECIPE_SERIALIZERS.register(ShapedAltarRecipe.NAME, () -> new ShapedAltarRecipe.Serializer());
    }

    public static final class LootModifierReg {

        private static void register() {
            LOOT_MODIFIER_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<? extends AutosmeltOrCobbleModifier>> AUTOSMELT_LOOT_MODIFIER =
                LOOT_MODIFIER_SERIALIZERS.register("autosmelt_or_cobble", AutosmeltOrCobbleModifier.CODEC_SUPPLIER);
        public static final RegistryObject<Codec<? extends CropMultiplierModifier>> CROP_LOOT_MODIFIER =
                LOOT_MODIFIER_SERIALIZERS.register("crop_multiplier", CropMultiplierModifier.CODEC_SUPPLIER);
    }

    public static final class CapabilityReg {
        private static void register() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry.CapabilityReg::registerCapabilities);
        }

        private static void registerCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(IFavor.class);
            event.register(ITameable.class);
        }

        // TODO attach capability event
    }

    public static final class StructureProcessorReg {
        private static void register() {
            STRUCTURE_PROCESSORS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<StructureProcessorType<AltarStructureProcessor>> ALTAR = STRUCTURE_PROCESSORS.register("altar", () ->
                () -> AltarStructureProcessor.CODEC);
    }

    public static final class AltarReg {
        private static void register() {
            ALTARS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class DeityReg {
        private static void register() {
            DEITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class OfferingReg {
        private static void register() {
            OFFERINGS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class SacrificeReg {
        private static void register() {
            SACRIFICES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class PerkConditionReg {
        private static void register() {
            PERK_CONDITION_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            PERK_CONDITIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<BiomeCondition>> BIOME = PERK_CONDITION_TYPES.register("biome", () -> BiomeCondition.CODEC);
        public static final RegistryObject<Codec<PatronCondition>> PATRON = PERK_CONDITION_TYPES.register("patron", () -> PatronCondition.CODEC);
        public static final RegistryObject<Codec<UnlockedCondition>> UNLOCKED = PERK_CONDITION_TYPES.register("unlocked", () -> UnlockedCondition.CODEC);
    }

    public static final class PerkActionReg {
        private static void register() {
            PERK_ACTION_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            PERK_ACTIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class PerkReg {
        private static void register() {
            PERKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class Keys {
        private static final String NAMESPACE = "deity";
        public static final ResourceKey<Registry<Altar>> ALTARS = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "altar"));
        public static final ResourceKey<Registry<Deity>> DEITIES = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "deity"));
        public static final ResourceKey<Registry<Offering>> OFFERINGS = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "offering"));
        public static final ResourceKey<Registry<Sacrifice>> SACRIFICES = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "sacrifice"));
        public static final ResourceKey<Registry<Codec<? extends PerkAction>>> PERK_ACTION_TYPES = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "perk_action_serializer"));
        public static final ResourceKey<Registry<PerkAction>> PERK_ACTIONS = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "perk_action"));
        public static final ResourceKey<Registry<Codec<? extends PerkCondition>>> PERK_CONDITION_TYPES = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "perk_condition_serializer"));
        public static final ResourceKey<Registry<PerkCondition>> PERK_CONDITIONS = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "perk_condition"));
        public static final ResourceKey<Registry<Perk>> PERKS = ResourceKey.createRegistryKey(new ResourceLocation(NAMESPACE, "perk"));
    }
}
