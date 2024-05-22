package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import rpggods.RGRegistry;
import rpggods.data.deity.Altar;
import rpggods.data.favor.IFavor;

import java.util.Optional;

public class UnlockedCondition extends PerkCondition {

    public static final Codec<UnlockedCondition> CODEC = ResourceLocation.CODEC
            .xmap(UnlockedCondition::new, o -> o.deity)
            .fieldOf("deity").codec();

    private final ResourceLocation deity;

    public UnlockedCondition(ResourceLocation deity) {
        this.deity = deity;
    }

    @Override
    public boolean match(ResourceLocation deity, Player player, IFavor favor, Optional<ResourceLocation> data, Optional<CompoundTag> entityTag) {
        return getDeity(player.level().registryAccess(), deity).isPresent() && favor.getFavor(deity).isEnabled();
    }

    @Override
    public Component getName(RegistryAccess registryAccess) {
        return Component.translatable(Altar.createTranslationKey(deity));
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.UNLOCKED.get();
    }
}
