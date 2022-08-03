package crazypants.enderio.machines.integration.jei;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.NullHelper;

import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.Log;
import crazypants.enderio.base.integration.jei.RecipeWrapperIRecipe;
import crazypants.enderio.base.integration.jei.energy.EnergyIngredient;
import crazypants.enderio.base.integration.jei.energy.EnergyIngredientRenderer;
import crazypants.enderio.base.recipe.IManyToOneRecipe;
import crazypants.enderio.base.recipe.IRecipe;
import crazypants.enderio.base.recipe.RecipeOutput;
import crazypants.enderio.base.recipe.alloysmelter.AlloyRecipeManager;
import crazypants.enderio.machines.EnderIOMachines;
import crazypants.enderio.machines.config.config.AlloySmelterConfig;
import crazypants.enderio.machines.config.config.PersonalConfig;
import crazypants.enderio.machines.machine.alloy.ContainerAlloySmelter;
import crazypants.enderio.machines.machine.alloy.GuiAlloySmelter;
import crazypants.enderio.util.Prep;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeCategory;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import static crazypants.enderio.machines.init.MachineObject.block_alloy_smelter;
import static crazypants.enderio.machines.init.MachineObject.block_enhanced_alloy_smelter;
import static crazypants.enderio.machines.init.MachineObject.block_simple_alloy_smelter;
import static crazypants.enderio.machines.init.MachineObject.block_simple_furnace;
import static crazypants.enderio.machines.machine.alloy.ContainerAlloySmelter.FIRST_INVENTORY_SLOT;
import static crazypants.enderio.machines.machine.alloy.ContainerAlloySmelter.FIRST_RECIPE_SLOT;
import static crazypants.enderio.machines.machine.alloy.ContainerAlloySmelter.NUM_INVENTORY_SLOT;
import static crazypants.enderio.machines.machine.alloy.ContainerAlloySmelter.NUM_RECIPE_SLOT;

public class AlloyRecipeCategory extends BlankRecipeCategory<AlloyRecipeCategory.AlloyRecipeWrapper> {

  public static final @Nonnull String UID = "AlloySmelter";

  // ------------ Recipes

  public static class AlloyRecipeWrapper extends RecipeWrapperIRecipe {

    public AlloyRecipeWrapper(IRecipe recipe) {
      super(recipe);
    }

    @Override
    public void getIngredients(@Nonnull IIngredients ingredients) {
      List<List<ItemStack>> inputStacks = recipe.getInputStackAlternatives();

      if (!(recipe instanceof IManyToOneRecipe) || !((IManyToOneRecipe) recipe).isDedupeInput()) {
        ingredients.setInputLists(ItemStack.class, inputStacks);
      } else {
        List<ItemStack> list0 = inputStacks.size() >= 1 ? inputStacks.get(0) : new NNList<>(Prep.getEmpty());
        List<ItemStack> list1 = inputStacks.size() >= 2 ? inputStacks.get(1) : new NNList<>(Prep.getEmpty());
        List<ItemStack> list2 = inputStacks.size() >= 3 ? inputStacks.get(2) : new NNList<>(Prep.getEmpty());

        List<ItemStack> out0 = new NNList<>(), out1 = new NNList<>(), out2 = new NNList<>();

        for (ItemStack stack0 : list0) {
          if (stack0 != null) {
            for (ItemStack stack1 : list1) {
              if (stack1 != null && !eq(stack0, stack1)) {
                for (ItemStack stack2 : list2) {
                  if (stack2 != null && !eq(stack0, stack2) && !eq(stack1, stack2)) {
                    out0.add(stack0);
                    out1.add(stack1);
                    out2.add(stack2);
                  }
                }
              }
            }
          }
        }

        if (!out0.isEmpty()) {
          final NNList<List<ItemStack>> inputs = new NNList<>();
          inputs.add(out0);
          if (!out1.isEmpty()) {
            inputs.add(out1);
            if (!out2.isEmpty()) {
              inputs.add(out2);
            }
          }
          ingredients.setInputLists(ItemStack.class, inputs);
        }
      }

      List<ItemStack> outputs = new ArrayList<ItemStack>();
      for (RecipeOutput out : recipe.getOutputs()) {
        if (Prep.isValid(out.getOutput())) {
          outputs.add(out.getOutput());
        }
      }
      ingredients.setOutputs(ItemStack.class, outputs);

      ingredients.setInput(EnergyIngredient.class, new EnergyIngredient(recipe.getEnergyRequired()));
    }

    private static boolean eq(@Nonnull ItemStack a, @Nonnull ItemStack b) {
      return a.getItem() == b.getItem()
              && (a.getItemDamage() == b.getItemDamage() || a.getItemDamage() == OreDictionary.WILDCARD_VALUE || b.getItemDamage() == OreDictionary.WILDCARD_VALUE);
    }
  }

  public static void register() {
    // Check JEI recipes are enabled
    if (!PersonalConfig.enableAlloySmelterAlloyingJEIRecipes.get() && !PersonalConfig.enableAlloySmelterFurnaceJEIRecipes.get()) {
      return;
    }

    MachinesPlugin.iModRegistry.addRecipeCategories(new AlloyRecipeCategory(MachinesPlugin.iGuiHelper));

    setModes(MachinesPlugin.iModRegistry, AlloySmelterConfig.profileSimpleAlloy.get(), GuiAlloySmelter.Simple.class, ContainerAlloySmelter.Simple.class,
            block_simple_alloy_smelter.getBlockNN(), 1);
    setModes(MachinesPlugin.iModRegistry, AlloySmelterConfig.profileSimpleFurnace.get(), GuiAlloySmelter.Furnace.class, ContainerAlloySmelter.Furnace.class,
            block_simple_furnace.getBlockNN(), 1);
    setModes(MachinesPlugin.iModRegistry, AlloySmelterConfig.profileNormal.get(), GuiAlloySmelter.Normal.class, ContainerAlloySmelter.Normal.class,
            block_alloy_smelter.getBlockNN(), 0);
    setModes(MachinesPlugin.iModRegistry, AlloySmelterConfig.profileEnhancedAlloy.get(), GuiAlloySmelter.Enhanced.class, ContainerAlloySmelter.Enhanced.class,
            block_enhanced_alloy_smelter.getBlockNN(), 0);

    long start = System.nanoTime();

    RecipeWrapperIRecipe.setLevelData(AlloyRecipeWrapper.class, MachinesPlugin.iGuiHelper, 137 - xOff, yOff + 1, "textures/blocks/block_alloy_smelter_simple_front.png",
            "textures/blocks/block_alloy_smelter_front.png");

    List<AlloyRecipeWrapper> result = new ArrayList<>();
    if (PersonalConfig.enableAlloySmelterAlloyingJEIRecipes.get()) {
      for (IManyToOneRecipe rec : AlloyRecipeManager.getInstance().getRecipes()) {
        if (!rec.isSynthetic()) {
          result.add(new AlloyRecipeWrapper(rec));
        }
      }
    }
    if (PersonalConfig.enableAlloySmelterFurnaceJEIRecipes.get()) {
      for (IRecipe rec : AlloyRecipeManager.getInstance().getVanillaRecipe().getAllRecipes()) {
        result.add(new AlloyRecipeWrapper(rec));
      }
    }

    long end = System.nanoTime();
    MachinesPlugin.iModRegistry.addRecipes(result, UID);

    Log.info(String.format("AlloyRecipeCategory: Added %d alloy smelter recipes to JEI in %.3f seconds.", result.size(), (end - start) / 1000000000d));
  }

  private static void setModes(IModRegistry registry, @Nonnull AlloySmelterConfig.Profile profile, @Nonnull Class<? extends GuiContainer> gui,
                               @Nonnull Class<? extends Container> container, @Nonnull Block block, int offset) {
    String[] UIDS;
    switch (profile.get().getOperatingMode()) {
      case ALL:
        UIDS = new String[] { AlloyRecipeCategory.UID, VanillaRecipeCategoryUid.SMELTING };
        break;
      case ALLOY:
        UIDS = new String[] { AlloyRecipeCategory.UID };
        break;
      case FURNACE:
        UIDS = new String[] { VanillaRecipeCategoryUid.SMELTING };
        break;
      default:
        return;
    }

    registry.addRecipeClickArea(gui, 155, 42, 16, 16, UIDS);
    registry.addRecipeCatalyst(new ItemStack(block), UIDS);
    for (int i = 0; i < UIDS.length; i++) {
      registry.getRecipeTransferRegistry().addRecipeTransferHandler(container, NullHelper.first(UIDS[i]), FIRST_RECIPE_SLOT, NUM_RECIPE_SLOT,
              FIRST_INVENTORY_SLOT - offset, NUM_INVENTORY_SLOT);
    }
  }

  // ------------ Category

  // Offsets from full size gui, makes it much easier to get the location correct
  private static final int xOff = 45;
  private static final int yOff = 3;
  private final @Nonnull IDrawable background;
  private final @Nonnull IDrawableAnimated flame;

  public AlloyRecipeCategory(IGuiHelper guiHelper) {
    ResourceLocation backgroundLocation = EnderIO.proxy.getGuiTexture("alloy_smelter_auto");
    background = MachinesPlugin.iGuiHelper.drawableBuilder(backgroundLocation, xOff, yOff, 82, 78).addPadding(0, 0, 0, 30).build();

    IDrawableStatic flameDrawable = guiHelper.createDrawable(backgroundLocation, 176, 0, 14, 14);
    flame = guiHelper.createAnimatedDrawable(flameDrawable, 200, IDrawableAnimated.StartDirection.BOTTOM, false);
  }

  @Override
  public @Nonnull String getUid() {
    return UID;
  }

  @Override
  public @Nonnull String getTitle() {
    return block_alloy_smelter.getBlockNN().getLocalizedName();
  }

  @Override
  public @Nonnull IDrawable getBackground() {
    return background;
  }

  @Override
  public void drawExtras(@Nonnull Minecraft minecraft) {
    flame.draw(minecraft, 56 - xOff - 1, 36 - yOff - 1);
    flame.draw(minecraft, 103 - xOff, 36 - yOff - 1);
  }

  @Override
  public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull AlloyRecipeCategory.AlloyRecipeWrapper recipeWrapper, @Nonnull IIngredients ingredients) {
    IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
    IGuiIngredientGroup<EnergyIngredient> group = recipeLayout.getIngredientsGroup(EnergyIngredient.class);

    guiItemStacks.init(0, true, 53 - xOff, 16 - yOff);
    guiItemStacks.init(1, true, 78 - xOff, 6 - yOff);
    guiItemStacks.init(2, true, 102 - xOff, 16 - yOff);
    guiItemStacks.init(3, false, 78 - xOff, 57 - yOff);
    group.init(4, true, EnergyIngredientRenderer.INSTANCE, 108 - xOff, 55 - yOff, 50, 10, 0, 0);

    List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
    for (int index = 0; index < inputs.size(); index++) {
      List<ItemStack> input = inputs.get(index);
      if (input != null) {
        guiItemStacks.set(index, input);
      }
    }
    List<List<ItemStack>> outputs = ingredients.getOutputs(ItemStack.class);
    guiItemStacks.set(3, outputs.get(0));

    group.set(ingredients);
  }

  @Override
  public @Nonnull String getModName() {
    return EnderIOMachines.MODID;
  }

}
