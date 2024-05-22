package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import rpggods.RGRegistry;
import rpggods.data.favor.IFavor;
import rpggods.util.DeferredHolderSet;

import java.util.Optional;

public class BiomeCondition extends PerkCondition {

    public static final Codec<BiomeCondition> CODEC = DeferredHolderSet.codec(Registries.BIOME)
            .xmap(BiomeCondition::new, o -> o.biome)
            .fieldOf("biome").codec();

    private final DeferredHolderSet<Biome> biome;

    public BiomeCondition(DeferredHolderSet<Biome> biome) {
        this.biome = biome;
    }

    @Override
    public boolean match(ResourceLocation deity, Player player, IFavor favor, Optional<ResourceLocation> data, Optional<CompoundTag> entityTag) {
        // load biome registry
        final Registry<Biome> registry = player.level().registryAccess().registryOrThrow(Registries.BIOME);
        // determine current biome
        final Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        // check if biome is in holder set
        return this.biome.get(registry).contains(biome);
    }

    @Override
    public Component getName(RegistryAccess registryAccess) {
        // load biome registry
        final Registry<Biome> registry = registryAccess.registryOrThrow(Registries.BIOME);
        return biome.get(registry)
                .unwrap()
                .map(tagKey -> Component.literal("#" + tagKey.location()),
                        list -> {
                            if (list.isEmpty()) return Component.empty();
                            ResourceLocation id = list.get(0)
                                    .unwrap()
                                    .map(left -> left.location(), right -> registry.getKey(right));
                            return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
                        });
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.BIOME.get();
    }
}
