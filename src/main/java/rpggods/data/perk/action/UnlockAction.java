/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import rpggods.RGRegistry;
import rpggods.data.deity.Deity;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.FavorLevel;
import rpggods.data.favor.IFavor;

import java.util.Optional;

public class UnlockAction extends PerkAction {

    public static final Codec<UnlockAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("deity").forGetter(o -> o.deity))
            .apply(instance, UnlockAction::new));

    private final ResourceLocation deity;

    public UnlockAction(boolean isHidden, ResourceLocation deity) {
        super(isHidden);
        this.deity = deity;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // validate deity
        final Optional<Deity> oDeity = Deity.getRegistry(context.getRegistryAccess()).getOptional(this.deity);
        if(oDeity.isEmpty() || !oDeity.get().isEnabled()) {
            return false;
        }
        // load favor for deity
        final Player player = context.getPlayer();
        final IFavor favor = context.getFavor();
        final FavorLevel level = favor.getFavor(this.deity);
        // verify not currently unlocked
        if(!level.isEnabled()) {
            level.setEnabled(true);
            // send player feedback
            Component message = getDescription(context.getRegistryAccess());
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), true);
            // play sound
            context.getLevel().playSound(player, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return false; // hacky solution to prevent feedback, since we don't need cooldown anyway
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final DeityContainer container = DeityContainer.getOrCreate(registryAccess, this.deity);
        final String suffix = container.getDeity().getGender().getSerializedName();
        return Component.translatable( PREFIX + "unlock" + SUFFIX + "." + suffix, container.getDeity());
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.UNLOCK.get();
    }
}
