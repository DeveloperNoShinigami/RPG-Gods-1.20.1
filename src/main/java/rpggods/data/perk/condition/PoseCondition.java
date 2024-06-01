/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.perk.condition;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import rpggods.RGRegistry;
import rpggods.util.RGComponentUtils;
import rpggods.util.RGCodecUtils;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Immutable
public class PoseCondition extends PerkCondition {

    public static final Codec<PoseCondition> CODEC = RGCodecUtils.listOrElementCodec(RGCodecUtils.POSE_CODEC)
            .xmap(PoseCondition::new, o -> o.pose)
            .fieldOf("pose").codec();

    private final List<Pose> pose;

    public PoseCondition(List<Pose> pose) {
        this.pose = pose;
    }

    @Override
    public boolean test(PerkConditionContext context) {
        final Pose playerPose = context.getPlayer().getPose();
        // check if any pose in list matches
        for(Pose p : pose) {
            if(p == playerPose) {
                return true;
            }
        }
        // no checks passed
        return false;
    }

    @Override
    public Component createDescription(RegistryAccess registryAccess) {
        final List<Component> poseComponentList = new ArrayList<>(pose.size());
        pose.forEach(p -> poseComponentList.add(Component.translatable("pose." + p.toString().toLowerCase(Locale.ENGLISH))));
        final Component delimiter = Component.translatable("favor.perk.condition.or");
        final Component poseComponent = RGComponentUtils.join(poseComponentList, delimiter);
        return Component.translatable(PREFIX + "pose", poseComponent);
    }

    @Override
    public Codec<? extends PerkCondition> getCodec() {
        return RGRegistry.PerkConditionReg.POSE.get();
    }
}
