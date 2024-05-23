/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.util.altar;

import net.minecraft.util.StringRepresentable;

public enum HumanoidPart implements StringRepresentable {
  HEAD("head"), 
  BODY("body"), 
  LEFT_ARM("left_arm"), 
  RIGHT_ARM("right_arm"), 
  LEFT_LEG("left_leg"), 
  RIGHT_LEG("right_leg"),
  ROOT("root");
  
  private String name;
  private HumanoidPart(final String n) {
    this.name = n;
  }
  
  @Override
  public String getSerializedName() {
    return name;
  }
}
