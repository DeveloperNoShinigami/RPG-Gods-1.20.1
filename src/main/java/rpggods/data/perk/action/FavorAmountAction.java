/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.action;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import rpggods.RGEvents;
import rpggods.RGRegistry;
import rpggods.data.deity.Deity;
import rpggods.data.favor.FavorCommand;
import rpggods.data.favor.FavorLevel;
import rpggods.util.FavorChangedEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class FavorAmountAction extends PerkAction {

    public static final Codec<FavorAmountAction> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("deity").forGetter(o -> o.deity))
            .and(IntProvider.CODEC.fieldOf("amount").forGetter(o -> o.amount))
            .and(FavorAmountAction.Type.CODEC.optionalFieldOf("type", Type.POINTS).forGetter(o -> o.type))
            .apply(instance, FavorAmountAction::new));

    private final ResourceLocation deity;
    private final IntProvider amount;
    private final Type type;

    public FavorAmountAction(boolean isHidden, ResourceLocation deity, IntProvider amount, Type type) {
        super(isHidden);
        this.deity = deity;
        this.amount = amount;
        this.type = type;
    }

    @Override
    public boolean apply(PerkActionContext context) {
        // validate deity
        if(Deity.getRegistry(context.getRegistryAccess()).getOptional(this.deity).isEmpty()) {
            return false;
        }
        // load favor and validate enabled
        final FavorLevel favorLevel = context.getFavor().getFavor(this.deity);
        if(!favorLevel.isEnabled()) {
            return false;
        }
        // add favor points or levels
        final int amount = this.amount.sample(context.getRandom());
        switch (this.type) {
            default:
            case POINTS:
                favorLevel.addFavor(context.getPlayer(), this.deity, amount, FavorChangedEvent.Source.PERK);
                return true;
            case LEVELS:
                int targetLevel = favorLevel.getLevel() + amount;
                long deltaFavorPoints = FavorLevel.calculateFavor(targetLevel) - favorLevel.getFavor() + 1;
                favorLevel.addFavor(context.getPlayer(), this.deity, deltaFavorPoints, FavorChangedEvent.Source.PERK);
                return true;
        }
    }

    @Override
    public List<Component> createDescription(RegistryAccess registryAccess) {
        // TODO favor amount perk action description, taking into account min-max and also points vs. levels
        return ImmutableList.of(Component.literal("" + amount.getMaxValue()));
    }

    @Override
    public Codec<? extends PerkAction> getCodec() {
        return RGRegistry.PerkActionReg.FAVOR_AMOUNT.get();
    }

    private static enum Type implements StringRepresentable {
        POINTS("points"),
        LEVELS("levels");

        private static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
