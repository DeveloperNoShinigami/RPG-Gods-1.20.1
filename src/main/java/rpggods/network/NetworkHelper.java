/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.network;

import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;

public class NetworkHelper {

    public static Optional<Level> getClientWorld(final NetworkEvent.Context context) {
        if(context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            return Optional.ofNullable(net.minecraft.client.Minecraft.getInstance().level);
        }
        return Optional.empty();
    }
}
