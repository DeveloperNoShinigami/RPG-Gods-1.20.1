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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import rpggods.RGRegistry;
import rpggods.data.deity.DeityContainer;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Patron;


public class PatronAction extends PerkAction {

    public static final Codec<PatronAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(Patron.CODEC.fieldOf("patron").forGetter(o -> o.patron))
            .apply(instance, PatronAction::new));

    private final Patron patron;

    public PatronAction(boolean isHidden, Patron patron) {
        super(isHidden);
        this.patron = patron;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        final Player player = context.getPlayer();
        final IFavor favor = context.getFavor();
        if(favor.setPatron(player, patron)) {
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
        if (patron.getDeity().isPresent()) {
            Component deityName = DeityContainer.getOrCreate(registryAccess, patron.getDeity().get()).getName();
            return Component.translatable(PREFIX + "patron" + SUFFIX + ".add", deityName);
        }
        return Component.translatable(PREFIX + "patron" + SUFFIX + ".remove");
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.PATRON.get();
    }
}
