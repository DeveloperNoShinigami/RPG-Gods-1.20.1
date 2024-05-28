/*
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package rpggods.data.perk;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum AffinityType implements StringRepresentable {
    PASSIVE("passive"),
    HOSTILE("hostile"),
    FLEE("flee"),
    TAME("tame");

    public static final Codec<AffinityType> CODEC = StringRepresentable.fromEnum(AffinityType::values);
    private final String name;

    private AffinityType(final String id) {
        name = id;
    }

    public Component getDisplayName() {
        return Component.translatable("favor.affinity." + getSerializedName());
    }

    public Component getDisplayDescription(Component entityName) {
        return Component.translatable("favor.affinity." + getSerializedName() + ".description", entityName);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
