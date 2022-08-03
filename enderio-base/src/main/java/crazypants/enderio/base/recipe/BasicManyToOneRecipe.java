package crazypants.enderio.base.recipe;

import java.util.List;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NNList;

import crazypants.enderio.util.Prep;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class BasicManyToOneRecipe implements IManyToOneRecipe {

  private final int energyRequired;
  private final @Nonnull ItemStack output;

  private final @Nonnull RecipeBonusType bonusType;

  private final @Nonnull Recipe recipe;

  private boolean synthetic = false;
  private boolean dedupeInput = false;

  public BasicManyToOneRecipe(@Nonnull Recipe recipe) {
    this.recipe = recipe;
    this.output = recipe.getOutputs()[0].getOutput().copy();
    energyRequired = recipe.getEnergyRequired();
    bonusType = recipe.getBonusType();
  }

  @Override
  public boolean isValidRecipeComponents(ItemStack... items) {
    if (dedupeInput) {
      return false;
    }

    NNList<IRecipeInput> inputs = new NNList<>(recipe.getInputs());
    for (ItemStack is : items) {
      if (is != null && Prep.isValid(is)) {
        IRecipeInput remove = null;
        for (IRecipeInput ri : inputs) {
          if (ri.isInput(is)) {
            remove = ri;
            break;
          }
        }
        if (remove != null) {
          inputs.remove(remove);
        } else {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public @Nonnull ItemStack getOutput() {
    return output;
  }

  @Override
  public boolean isValidInput(int slot, @Nonnull ItemStack input) {
    if (Prep.isInvalid(input) || dedupeInput) {
      return false;
    }
    return getRecipeComponentFromInput(input) != null;
  }

  @Override
  public boolean isValidInput(@Nonnull FluidStack fluid) {
    return false;
  }

  @Override
  public boolean isValid() {
    return recipe.isValid();
  }

  @Override
  public int getEnergyRequired() {
    return energyRequired;
  }

  @Override
  public @Nonnull RecipeBonusType getBonusType() {
    return bonusType;
  }

  @Override
  public @Nonnull RecipeOutput[] getOutputs() {
    return recipe.getOutputs();
  }

  @Override
  public @Nonnull NNList<ItemStack> getInputStacks() {
    return recipe.getInputStacks();
  }

  @Override
  public boolean isInputForRecipe(NNList<MachineRecipeInput> inputs) {
    if (inputs == null || dedupeInput) {
      return false;
    }
    return recipe.isInputForRecipe(inputs);
  }

  @Override
  public @Nonnull IRecipeInput[] getInputs() {
    return recipe.getInputs();
  }

  @Override
  public NNList<FluidStack> getInputFluidStacks() {
    return NNList.emptyList();
  }

  private ItemStack getRecipeComponentFromInput(@Nonnull ItemStack input) {
    if (Prep.isInvalid(input)) {
      return null;
    }
    for (IRecipeInput ri : recipe.getInputs()) {
      if (ri.isInput(input)) {
        return ri.getInput();
      }
    }
    return null;
  }

  @Override
  public @Nonnull NNList<List<ItemStack>> getInputStackAlternatives() {
    return recipe.getInputStackAlternatives();
  }

  @Override
  public String toString() {
    return "BasicManyToOneRecipe [output=" + output + ", recipe=" + recipe + "]";
  }

  @Override
  public boolean isSynthetic() {
    return synthetic;
  }

  public @Nonnull BasicManyToOneRecipe setSynthetic() {
    this.synthetic = true;
    return this;
  }

  @Override
  public boolean isDedupeInput() {
    return dedupeInput;
  }

  public @Nonnull IManyToOneRecipe setDedupeInput() {
    dedupeInput = true;
    return this;
  }

  @Override
  @Nonnull
  public RecipeLevel getRecipeLevel() {
    return recipe.getRecipeLevel();
  }

}
