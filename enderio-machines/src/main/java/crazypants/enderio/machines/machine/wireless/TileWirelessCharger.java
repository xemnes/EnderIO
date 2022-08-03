package crazypants.enderio.machines.machine.wireless;

import javax.annotation.Nonnull;

import com.enderio.core.client.render.BoundingBox;
import com.enderio.core.common.vecmath.Vector4f;

import crazypants.enderio.base.Log;
import crazypants.enderio.base.TileEntityEio;
import crazypants.enderio.base.diagnostics.Prof;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.paint.YetaUtil;
import crazypants.enderio.base.power.PowerHandlerUtil;
import crazypants.enderio.base.power.forge.tile.ILegacyPoweredTile;
import crazypants.enderio.base.power.forge.tile.InternalRecieverTileWrapper;
import crazypants.enderio.base.power.wireless.IWirelessCharger;
import crazypants.enderio.base.power.wireless.WirelessChargerController;
import crazypants.enderio.base.render.ranged.IRanged;
import crazypants.enderio.base.render.ranged.RangeParticle;
import crazypants.enderio.machines.capacitor.CapacitorKey;
import crazypants.enderio.machines.config.config.ChargerConfig;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import info.loenwind.autosave.util.NBTAction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Storable
public class TileWirelessCharger extends TileEntityEio implements ILegacyPoweredTile.Receiver, IWirelessCharger, IPaintable.IPaintableTileEntity, IRanged {

  // Client sync monitoring
  protected int ticksSinceSync = -1;
  protected boolean updateClients = false;
  protected boolean lastActive;
  protected int ticksSinceActiveChanged = 0;

  @Store
  private int storedEnergyRF;

  // @Store
  // private boolean chargedItem = false;
  @Store
  private boolean charging = false;

  private boolean registered = false;

  private @Nonnull BoundingBox bb = BoundingBox.UNIT_CUBE;
  private IBlockState blockState = null;

  public TileWirelessCharger() {
    addICap(CapabilityEnergy.ENERGY, facing -> InternalRecieverTileWrapper.get(this, facing));
  }

  @Override
  public void invalidate() {
    super.invalidate();
    WirelessChargerController.deregisterCharger(this);
    registered = false;
  }

  @Override
  public void doUpdate() {
    if (world.isRemote) {
      YetaUtil.refresh(this);
      return;
    }

    if (!registered) {
      WirelessChargerController.registerCharger(this);
      registered = true;
    }

    if (charging) {
      if (++ticksSinceActiveChanged == 20) {
        updateEntityClient(false);
      }
    }


  }

  @Override
  public boolean chargeItems(NonNullList<ItemStack> stacks) {
    int available = Math.min(CapacitorKey.WIRELESS_POWER_OUTPUT.getDefault(), storedEnergyRF);
    boolean active = false;
    for (int i = 0, end = stacks.size(); i < end && available > 0; i++) {
      ItemStack stack = stacks.get(i);
      if (stack.getCount() == 1 && shouldChargeWirelessly(stack)) {
        IEnergyStorage chargable = PowerHandlerUtil.getCapability(stack, null);
        if (chargable != null) {
          int max = chargable.getMaxEnergyStored();
          int cur = chargable.getEnergyStored();
          int canUse = Math.min(available, max - cur);
          if (cur < max) {
            int used = chargable.receiveEnergy(canUse, false);
            if (used > 0) {
              storedEnergyRF = storedEnergyRF - used;
              if (!world.isRemote) {
                active = true;
              }
              available -= used;
            }
          }
        }
      }
    }
    return active;
  }

  @Override
  public void updateEntityClient(boolean active) {
    if (this.charging != active) {
      ticksSinceActiveChanged = 0;
      this.charging = active;
      updateBlock();
      YetaUtil.refresh(this);
    }
    // check if the block on the client needs to update its texture
    /*
    if (isActiveTexture() != lastActive) {
      ticksSinceActiveChanged++;
      if (lastActive ? ticksSinceActiveChanged > 20 : ticksSinceActiveChanged > 4) {
        ticksSinceActiveChanged = 0;
        lastActive = isActiveTexture();
        updateBlock();
      }
    } else {
      ticksSinceActiveChanged = 0;
    }
     */
  }

  private static boolean shouldChargeWirelessly(ItemStack stack) {
    return !(stack.getItem() instanceof IWirelessCharger.ExcludedItem) || ((IWirelessCharger.ExcludedItem) stack.getItem()).shouldChargeWirelessly(stack);
  }

  @Override
  public int getMaxEnergyRecieved(EnumFacing dir) {
    return CapacitorKey.WIRELESS_POWER_INTAKE.getDefault();
  }

  @Override
  public int getEnergyStored() {
    return storedEnergyRF;
  }

  @Override
  public int getMaxEnergyStored() {
    return CapacitorKey.WIRELESS_POWER_BUFFER.getDefault();
  }

  @Override
  public void setEnergyStored(int stored) {
    storedEnergyRF = stored;
  }

  @Override
  public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
    return PowerHandlerUtil.recieveInternal(this, maxReceive, from, simulate);
  }

  @Override
  public int takeEnergy(int max) {
    int prev = storedEnergyRF;
    storedEnergyRF = Math.max(0, storedEnergyRF - max);
    return prev - storedEnergyRF;
  }

  @Override
  public boolean canConnectEnergy(@Nonnull EnumFacing from) {
    return from == EnumFacing.DOWN || !((BlockNormalWirelessCharger) getBlockType()).isAntenna();
  }

  @Override
  public @Nonnull World getworld() {
    return getWorld();
  }

  @Override
  public boolean displayPower() {
    return true;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  public boolean isActiveTexture() {
    return charging;
  }

  @Nonnull
  @Override
  public BlockPos getLocation() {
    return pos;
  }

  @Override
  @Nonnull
  public BoundingBox getRange() throws SelfDestructionException {
    if (this.isInvalid()) {
      Log.error("TileEntity " + this + " at " + pos + " is invalid but was not invalidated. This should not be possible!");
      return bb;
    }
    IBlockState actualState = world.getBlockState(pos).getActualState(world, pos);
    if (actualState != blockState) {
      if (!(actualState.getBlock() instanceof BlockNormalWirelessCharger)) {
        Log.error("TileEntity " + this + " at " + pos + " is assigned to a wrong block (" + actualState
            + "). This should not be possible unless the world was severly corrupted!");
        world.removeTileEntity(pos);
        world.createExplosion(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1, false);
        throw new SelfDestructionException();
      }
      blockState = actualState;
      bb = ((BlockNormalWirelessCharger) actualState.getBlock()).getChargingStrength(actualState, pos);
    }
    return bb;
  }

  private boolean showingRange = false;
  private final static Vector4f color = new Vector4f(0x6a / 255f, 0x3d / 255f, 0x7d / 255f, .4f);

  protected void toggleRange() {
    showingRange = !showingRange;
    if (world.isRemote && showingRange) {
      spawnRangeParticle();
    }
  }

  @SideOnly(Side.CLIENT)
  private void spawnRangeParticle() {
    Minecraft.getMinecraft().effectRenderer.addEffect(new RangeParticle<TileWirelessCharger>(this, color));
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean isShowingRange() {
    return showingRange;
  }

  @Override
  @Nonnull
  public BoundingBox getBounds() {
    try {
      return getRange();
    } catch (SelfDestructionException e) {
      return bb;
    }
  }

  @Override
  public boolean forceSingle() {
    return ChargerConfig.singleCharger.get();
  }
}
