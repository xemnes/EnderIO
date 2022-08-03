package crazypants.enderio.machines.machine.slicensplice;

import java.util.List;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NNList;

import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.farming.FarmingTool;
import crazypants.enderio.base.machine.baselegacy.AbstractPoweredTaskEntity;
import crazypants.enderio.base.machine.baselegacy.SlotDefinition;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.recipe.IMachineRecipe;
import crazypants.enderio.base.recipe.IManyToOneRecipe;
import crazypants.enderio.base.recipe.IRecipeInput;
import crazypants.enderio.base.recipe.MachineRecipeInput;
import crazypants.enderio.base.recipe.MachineRecipeRegistry;
import crazypants.enderio.base.recipe.ManyToOneMachineRecipe;
import crazypants.enderio.machines.config.config.SliceAndSpliceConfig;
import crazypants.enderio.util.Prep;
import info.loenwind.autosave.annotations.Storable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;

import static crazypants.enderio.machines.capacitor.CapacitorKey.SLICE_POWER_BUFFER;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SLICE_POWER_INTAKE;
import static crazypants.enderio.machines.capacitor.CapacitorKey.SLICE_POWER_USE;

@Storable
public class TileSliceAndSplice extends AbstractPoweredTaskEntity implements IPaintable.IPaintableTileEntity {

  protected final static int axeIndex = 6;
  protected final static int shearsIndex = 7;
  private EntityLivingBase fakePlayer;

  private static final @Nonnull ResourceLocation SOUND = new ResourceLocation(EnderIO.DOMAIN, "machine.slicensplice");

  public TileSliceAndSplice() {
    super(new SlotDefinition(8, 1, 1), SLICE_POWER_INTAKE, SLICE_POWER_BUFFER, SLICE_POWER_USE);
  }

  @Override
  public @Nonnull String getMachineName() {
    return MachineRecipeRegistry.SLICENSPLICE;
  }

  @Override
  public ResourceLocation getSound() {
    return SOUND;
  }

  @Override
  public int getInventoryStackLimit() {
    return 1;
  }

  @Override
  public int getInventoryStackLimit(int slot) {
    return slot == getSlotDefinition().getMinOutputSlot() ? 64 : 1;
  }

  @Override
  protected IMachineRecipe canStartNextTask(long nextSeed) {
    if (!hasTools()) {
      return null;
    }
    return super.canStartNextTask(nextSeed);
  }

  private @Nonnull ItemStack getAxe() {
    return getStackInSlot(axeIndex);
  }

  private @Nonnull ItemStack getShears() {
    return getStackInSlot(shearsIndex);
  }

  @Override
  protected void checkProgress(boolean redstoneChecksPassed) {
    if (hasTools()) {
      super.checkProgress(redstoneChecksPassed);
    }
  }

  private boolean hasTools() {
    return Prep.isValid(getAxe()) && Prep.isValid(getShears());
  }

  @Override
  protected void taskComplete() {
    super.taskComplete();
    damageTool(getAxe(), axeIndex);
    damageTool(getShears(), shearsIndex);
  }

  @Override
  protected int usePower(int wantToUse) {
    if (wantToUse > 0) {
      if (random.nextFloat() < SliceAndSpliceConfig.toolDamageChance.get()) {
        damageTool(getAxe(), axeIndex);
      }
      if (random.nextFloat() < SliceAndSpliceConfig.toolDamageChance.get()) {
        damageTool(getShears(), shearsIndex);
      }
    }
    return super.usePower(wantToUse);
  }

  private void damageTool(@Nonnull ItemStack tool, int toolIndex) {
    if (Prep.isValid(tool) && tool.isItemStackDamageable()) {
      tool.damageItem(1, getFakePlayer());
      if (tool.getItemDamage() >= tool.getMaxDamage()) {
        tool.shrink(1);
      }
      markDirty();
    }
  }

  @SuppressWarnings("null")
  private @Nonnull EntityLivingBase getFakePlayer() {
    if (fakePlayer == null) {
      fakePlayer = FakePlayerFactory.getMinecraft(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(world.provider.getDimension()));
    }
    return fakePlayer;
  }

  @Override
  protected @Nonnull NNList<MachineRecipeInput> getRecipeInputs() {
    NNList<MachineRecipeInput> res = new NNList<>();
    for (int slot = slotDefinition.minInputSlot; slot <= slotDefinition.maxInputSlot - 2; slot++) {
      final ItemStack item = getStackInSlot(slot);
      if (Prep.isValid(item)) {
        res.add(new MachineRecipeInput(slot, item));
      }
    }
    return res;
  }

  @Override
  public boolean isMachineItemValidForSlot(int slot, @Nonnull ItemStack itemstack) {
    if (Prep.isInvalid(itemstack)) {
      return false;
    }
    if (!slotDefinition.isInputSlot(slot)) {
      return false;
    }
    if (slot == axeIndex) {
      return FarmingTool.AXE.itemMatches(itemstack);
    }
    if (slot == shearsIndex) {
      return FarmingTool.SHEARS.itemMatches(itemstack);
    }

    ItemStack currentStackInSlot = getStackInSlot(slot);
    if (Prep.isValid(currentStackInSlot)) {
      return currentStackInSlot.isItemEqual(itemstack);
    }

    int numSlotsFilled = 0;
    for (int i = slotDefinition.getMinInputSlot(); i <= slotDefinition.getMaxInputSlot(); i++) {
      if (i >= 0 && i < inventory.length && i != axeIndex && i != shearsIndex) {
        if (Prep.isValid(getStackInSlot(i))) {
          numSlotsFilled++;
        }
      }
    }
    List<IMachineRecipe> recipes = MachineRecipeRegistry.instance.getRecipesForInput(getMachineLevel(), getMachineName(),
            MachineRecipeInput.create(slot, itemstack));

    return isValidInputForAlloyRecipe(slot, itemstack, numSlotsFilled, recipes);
  }

  private boolean isValidInputForAlloyRecipe(int slot, ItemStack itemstack, int numSlotsFilled, List<IMachineRecipe> recipes) {
    ItemStack[] resultInv = new ItemStack[slotDefinition.getNumInputSlots()];
    for (int i = slotDefinition.getMinInputSlot(); i <= slotDefinition.getMaxInputSlot(); i++) {
      if (i >= 0 && i < inventory.length && i != axeIndex && i != shearsIndex) {
        if (i == slot) {
          resultInv[i] = itemstack;
        } else {
          resultInv[i] = inventory[i];
        }
      }
    }

    for (IMachineRecipe recipe : recipes) {
      if (recipe instanceof ManyToOneMachineRecipe) {
        for (IManyToOneRecipe oneRecipe : ((ManyToOneMachineRecipe) recipe).getRecipesThatHaveTheseAsValidRecipeComponents(resultInv)) {
          boolean valid = true;
          for (int i = 0; valid && i < resultInv.length; i++) {
            // skip Tool slots and empty slots
            final ItemStack resultInvI = resultInv[i];
            if (resultInvI == null || resultInvI.isEmpty())
              continue;
            // check if the current item set is valid for this recipe
            for (IRecipeInput ri : oneRecipe.getInputs()) {
              if (ri.getSlotNumber() != i)
                continue;
              if (!ri.isInput(resultInvI))
                valid = false;
              break;
            }
          }
          // Invalid, check the next recipe
          if (!valid)
            continue;
          return true;
        }
      }
    }
    return false;
  }

}
