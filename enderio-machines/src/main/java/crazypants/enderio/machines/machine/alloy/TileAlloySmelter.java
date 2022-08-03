package crazypants.enderio.machines.machine.alloy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.NullHelper;

import crazypants.enderio.api.capacitor.ICapacitorData;
import crazypants.enderio.api.capacitor.ICapacitorKey;
import crazypants.enderio.base.Log;
import crazypants.enderio.base.capacitor.CapacitorHelper;
import crazypants.enderio.base.machine.baselegacy.AbstractPoweredTaskEntity;
import crazypants.enderio.base.machine.baselegacy.SlotDefinition;
import crazypants.enderio.base.machine.interfaces.IPoweredTask;
import crazypants.enderio.base.machine.modes.IoMode;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.recipe.IMachineRecipe;
import crazypants.enderio.base.recipe.MachineLevel;
import crazypants.enderio.base.recipe.MachineRecipeInput;
import crazypants.enderio.base.recipe.MachineRecipeRegistry;
import crazypants.enderio.base.recipe.ManyToOneMachineRecipe;
import crazypants.enderio.base.recipe.RecipeLevel;
import crazypants.enderio.base.recipe.alloysmelter.AlloyRecipeManager;
import crazypants.enderio.base.recipe.alloysmelter.VanillaSmeltingRecipe;
import crazypants.enderio.machines.config.config.AlloySmelterConfig;
import crazypants.enderio.util.Prep;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import static crazypants.enderio.machines.capacitor.CapacitorKey.ALLOY_SMELTER_POWER_BUFFER;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ALLOY_SMELTER_POWER_INTAKE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ALLOY_SMELTER_POWER_USE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ENHANCED_ALLOY_SMELTER_DOUBLE_OP_CHANCE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ENHANCED_ALLOY_SMELTER_POWER_BUFFER;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ENHANCED_ALLOY_SMELTER_POWER_EFFICIENCY;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ENHANCED_ALLOY_SMELTER_POWER_INTAKE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.ENHANCED_ALLOY_SMELTER_POWER_USE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SIMPLE_ALLOY_SMELTER_POWER_BUFFER;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SIMPLE_ALLOY_SMELTER_POWER_INTAKE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SIMPLE_ALLOY_SMELTER_POWER_LOSS;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SIMPLE_ALLOY_SMELTER_POWER_USE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SIMPLE_STIRLING_POWER_LOSS;

@Storable
public class TileAlloySmelter extends AbstractPoweredTaskEntity implements IPaintable.IPaintableTileEntity {

  public static class Simple extends TileAlloySmelter {

    public Simple() {
      super(AlloySmelterConfig.profileSimpleAlloy.get().get(), new SlotDefinition(3, 1, 0), SIMPLE_ALLOY_SMELTER_POWER_INTAKE,
              SIMPLE_ALLOY_SMELTER_POWER_BUFFER, SIMPLE_ALLOY_SMELTER_POWER_USE);
      setEnergyLoss(SIMPLE_ALLOY_SMELTER_POWER_LOSS);
    }

    @Override
    protected @Nonnull RecipeLevel getMachineLevel() {
      return MachineLevel.SIMPLE;
    }

  }

  public static class Furnace extends TileAlloySmelter {

    public Furnace() {
      super(AlloySmelterConfig.profileSimpleFurnace.get().get(), new SlotDefinition(3, 1, 0), SIMPLE_ALLOY_SMELTER_POWER_INTAKE,
              SIMPLE_ALLOY_SMELTER_POWER_BUFFER, SIMPLE_ALLOY_SMELTER_POWER_USE);
      setEnergyLoss(SIMPLE_STIRLING_POWER_LOSS);
    }

    @Override
    protected @Nonnull RecipeLevel getMachineLevel() {
      return MachineLevel.SIMPLE;
    }

  }

  public static class Enhanced extends TileAlloySmelter {
    public Enhanced() {
      super(AlloySmelterConfig.profileEnhancedAlloy.get().get(), new SlotDefinition(3, 1, 1), ENHANCED_ALLOY_SMELTER_POWER_INTAKE,
              ENHANCED_ALLOY_SMELTER_POWER_BUFFER, ENHANCED_ALLOY_SMELTER_POWER_USE);
      setEfficiencyMultiplier(ENHANCED_ALLOY_SMELTER_POWER_EFFICIENCY);
    }

    @Nonnull
    @Override
    public ICapacitorData getCapacitorData() {
      return CapacitorHelper.increaseCapacitorLevel(super.getCapacitorData(), 1f);
    }

    @Override
    protected boolean shouldDoubleTick(@Nonnull IPoweredTask task, int usedEnergy) {
      double chance = getCapacitorData().getUnscaledValue(ENHANCED_ALLOY_SMELTER_DOUBLE_OP_CHANCE) * (usedEnergy / task.getRequiredEnergy());
      if (random.nextDouble() < chance) {
        return true;
      }
      return super.shouldDoubleTick(task, usedEnergy);
    }

    @Override
    public boolean supportsMode(@Nullable EnumFacing faceHit, @Nullable IoMode modeIn) {
      return (faceHit != EnumFacing.UP || modeIn == IoMode.NONE) && super.supportsMode(faceHit, modeIn);
    }

    @Override
    protected @Nonnull RecipeLevel getMachineLevel() {
      return MachineLevel.ADVANCED;
    }

  }

  protected final @Nonnull OperatingProfile operatingProfile;

  @Store
  protected @Nonnull OperatingMode mode = OperatingMode.ALL;

  public TileAlloySmelter() {
    this(AlloySmelterConfig.profileNormal.get().get(), new SlotDefinition(3, 1), ALLOY_SMELTER_POWER_INTAKE, ALLOY_SMELTER_POWER_BUFFER,
            ALLOY_SMELTER_POWER_USE);
  }

  protected TileAlloySmelter(@Nonnull OperatingProfile operatingProfile, @Nonnull SlotDefinition slotDefinition, @Nonnull ICapacitorKey maxEnergyRecieved,
                             @Nonnull ICapacitorKey maxEnergyStored, @Nonnull ICapacitorKey maxEnergyUsed) {
    super(slotDefinition, maxEnergyRecieved, maxEnergyStored, maxEnergyUsed);
    this.operatingProfile = operatingProfile;
  }

  @Override
  protected @Nonnull RecipeLevel getMachineLevel() {
    return MachineLevel.NORMAL;
  }

  public @Nonnull OperatingProfile getOperatingProfile() {
    return operatingProfile;
  }

  public @Nonnull OperatingMode getMode() {
    return operatingProfile.canSwitchProfiles() ? mode : operatingProfile.getOperatingMode();
  }

  public void setMode(OperatingMode mode) {
    if (mode == null) {
      mode = OperatingMode.ALL;
    }
    if (this.mode != mode) {
      this.mode = mode;
      updateClients = true;
    }
  }

  @Override
  protected IMachineRecipe canStartNextTask(long nextSeed) {
    if (getMode() == OperatingMode.FURNACE) {
      VanillaSmeltingRecipe vr = AlloyRecipeManager.getInstance().getVanillaRecipe();
      if (vr.isRecipe(getMachineLevel(), getRecipeInputs())) {
        final IPoweredTask task = createTask(vr, nextSeed);
        if (task == null) {
          return null;
        }
        IMachineRecipe.ResultStack[] res = task.getCompletedResult();
        if (res.length == 0) {
          return null;
        }
        return canInsertResult(nextSeed, vr) ? vr : null;
      }
      return null;
    }

    IMachineRecipe nextRecipe = getNextRecipe();
    if (getMode() == OperatingMode.ALLOY && nextRecipe instanceof VanillaSmeltingRecipe) {
      nextRecipe = null;
    }
    if (nextRecipe == null) {
      return null; // no template
    }
    // make sure we have room for the next output
    return canInsertResult(nextSeed, nextRecipe) ? nextRecipe : null;
  }

  @Override
  public boolean isMachineItemValidForSlot(int slot, @Nonnull ItemStack itemstack) {
    if (!slotDefinition.isInputSlot(slot)) {
      return false;
    }

    // We will assume anything that is in a slot is valid, so just return whether the new input can be stacked with the current one
    ItemStack currentStackInSlot = NullHelper.first(inventory[slot], Prep.getEmpty());
    if (Prep.isValid(currentStackInSlot)) {
      return currentStackInSlot.isItemEqual(itemstack);
    }

    int numSlotsFilled = 0;
    for (int i = slotDefinition.getMinInputSlot(); i <= slotDefinition.getMaxInputSlot(); i++) {
      ItemStack currentStackType = getStackInSlot(i);
      if (Prep.isValid(currentStackType)) {
        numSlotsFilled++;
      }
    }
    NNList<IMachineRecipe> recipes = MachineRecipeRegistry.instance.getRecipesForInput(getMachineLevel(), getMachineName(),
            MachineRecipeInput.create(slot, itemstack));

    if (getMode() == OperatingMode.FURNACE) {
      return isValidInputForFurnaceRecipe(itemstack, numSlotsFilled, recipes);
    } else if (getMode() == OperatingMode.ALLOY) {
      return isValidInputForAlloyRecipe(slot, itemstack, numSlotsFilled, recipes);
    }
    return isValidInputForFurnaceRecipe(itemstack, numSlotsFilled, recipes) || isValidInputForAlloyRecipe(slot, itemstack, numSlotsFilled, recipes);
  }

  private boolean isValidInputForAlloyRecipe(int slot, @Nonnull ItemStack itemstack, int numSlotsFilled, NNList<IMachineRecipe> recipes) {
    if (numSlotsFilled == 0) {
      return containsAlloyRecipe(recipes);
    }
    for (IMachineRecipe recipe : recipes) {
      if (!(recipe instanceof VanillaSmeltingRecipe)) {

        if (recipe instanceof ManyToOneMachineRecipe) {
          ItemStack[] resultInv = new ItemStack[slotDefinition.getNumInputSlots()];
          for (int i = slotDefinition.getMinInputSlot(); i <= slotDefinition.getMaxInputSlot(); i++) {
            if (i >= 0 && i < inventory.length) {
              if (i == slot) {
                resultInv[i] = itemstack;
              } else {
                resultInv[i] = inventory[i];
              }
            }
          }
          if (((ManyToOneMachineRecipe) recipe).isValidRecipeComponents(resultInv)) {
            return true;
          }

        } else {
          Log.warn("TileAlloySmelter.isMachineItemValidForSlot: A non alloy recipe was returned for the alloy smelter");
          return true;
        }
      }
    }
    return false;
  }

  private boolean isValidInputForFurnaceRecipe(@Nonnull ItemStack itemstack, int numSlotsFilled, NNList<IMachineRecipe> recipes) {
    return containsFurnaceRecipe(recipes) && (numSlotsFilled == 0 || isItemAlreadyInASlot(itemstack));
  }

  private boolean isItemAlreadyInASlot(@Nonnull ItemStack itemstack) {
    for (int i = slotDefinition.getMinInputSlot(); i <= slotDefinition.getMaxInputSlot(); i++) {
      ItemStack currentStackType = getStackInSlot(i);
      if (Prep.isValid(currentStackType)) {
        return currentStackType.isItemEqual(itemstack);
      }
    }
    return false;
  }

  private boolean containsFurnaceRecipe(NNList<IMachineRecipe> recipes) {
    for (IMachineRecipe rec : recipes) {
      if (rec instanceof VanillaSmeltingRecipe) {
        return true;
      }
    }
    return false;
  }

  private boolean containsAlloyRecipe(NNList<IMachineRecipe> recipes) {
    for (IMachineRecipe rec : recipes) {
      if (!(rec instanceof VanillaSmeltingRecipe)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nonnull String getMachineName() {
    return MachineRecipeRegistry.ALLOYSMELTER;
  }

}
