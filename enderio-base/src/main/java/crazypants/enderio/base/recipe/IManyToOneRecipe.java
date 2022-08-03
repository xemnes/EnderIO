package crazypants.enderio.base.recipe;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NNList;

import net.minecraft.item.ItemStack;

public interface IManyToOneRecipe extends IRecipe {

  boolean isValidRecipeComponents(ItemStack... items);

  @Nonnull
  ItemStack getOutput();

  boolean isDedupeInput();

}
