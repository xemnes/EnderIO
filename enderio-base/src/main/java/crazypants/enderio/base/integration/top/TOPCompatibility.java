package crazypants.enderio.base.integration.top;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.api.client.render.IWidgetIcon;
import com.enderio.core.api.common.util.ITankAccess.ITankData;
import com.enderio.core.common.BlockEnder;
import com.enderio.core.common.fluid.SmartTank;
import com.enderio.core.common.util.NNList.Callback;
import com.enderio.core.common.util.NullHelper;
import com.google.common.base.Function;

import crazypants.enderio.api.ILocalizable;
import crazypants.enderio.base.BlockEio;
import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.Log;
import crazypants.enderio.base.config.config.TopConfig;
import crazypants.enderio.base.fluid.ItemTankHelper;
import crazypants.enderio.base.gui.IconEIO;
import crazypants.enderio.base.init.ModObject;
import crazypants.enderio.base.lang.LangPower;
import crazypants.enderio.base.machine.interfaces.ITEProxy;
import crazypants.enderio.base.material.material.Material;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.transceiver.Channel;
import crazypants.enderio.base.transceiver.ChannelType;
import crazypants.enderio.util.CapturedMob;
import crazypants.enderio.util.NbtValue;
import crazypants.enderio.util.Prep;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.rendering.RenderHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import static mcjty.theoneprobe.api.IProbeInfo.ENDLOC;
import static mcjty.theoneprobe.api.IProbeInfo.STARTLOC;
import static mcjty.theoneprobe.api.NumberFormat.COMPACT;

public class TOPCompatibility implements Function<ITheOneProbe, Void>, IProbeInfoProvider, IProbeConfigProvider {

  private static ITheOneProbe probe;

  @Nullable
  @Override
  public Void apply(@Nullable ITheOneProbe theOneProbe) {
    if (TopConfig.enabled.get()) {
      probe = theOneProbe;
      Log.info("Enabled support for The One Probe");
      probe.registerProvider(this);
      probe.registerProbeConfigProvider(this);
    } else {
      Log.info("Support for The One Probe is DISABLED by a configuration setting");
    }
    return null;
  }

  @Override
  public String getID() {
    return EnderIO.DOMAIN + ":default";
  }

  @Override
  public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData hitData) {
    if (probeInfo != null && blockState != null) {
      if (mode == ProbeMode.DEBUG) {
        probeInfo.text(blockState.toString());
      }
      if (world != null && hitData != null && (blockState.getBlock() instanceof BlockEio || blockState.getBlock() instanceof IPaintable)) {
        TileEntity tileEntity = blockState.getBlock() instanceof ITEProxy
            ? ((ITEProxy) blockState.getBlock()).getParent(world, NullHelper.notnull(hitData.getPos(), "hitData.getPos()"), blockState)
            : BlockEnder.getAnyTileEntitySafe(world, NullHelper.notnull(hitData.getPos(), "hitData.getPos()/2"));
        if (tileEntity != null) {
          EioBox eiobox = new EioBox(probeInfo);

          TOPData data = new TOPData(tileEntity, hitData);

          mkOwner(mode, eiobox, data);

          mkPaint(mode, eiobox, data);

          mkNotificationLine(mode, eiobox, data);

          mkProgressLine(mode, eiobox, data);

          mkEnergyLine(mode, eiobox, data);

          mkXPLine(mode, eiobox, data);

          mkRedstoneLine(mode, eiobox, data);

          mkSideConfigLine(mode, eiobox, data);

          mkRangeLine(mode, eiobox, data);

          mkTankLines(mode, eiobox, data);

          mkItemFillLevelLine(mode, eiobox, data);

          mkChannelLine(mode, eiobox, data);

          eiobox.finish();

          EioBox mobbox = new EioBox(probeInfo);

          mkMobsBox(mode, mobbox, world, data);

          mobbox.finish();
        }
      }
    }
  }

  private static class EioBox {
    private final IProbeInfo probeinfo;
    private IProbeInfo eiobox;
    private boolean addMoreIndicator = false;

    public EioBox(IProbeInfo probeinfo) {
      this.probeinfo = probeinfo;
    }

    public IProbeInfo getProbeinfo() {
      return probeinfo;
    }

    public IProbeInfo get() {
      if (eiobox == null) {
        eiobox = probeinfo.vertical(probeinfo.defaultLayoutStyle().borderColor(0x00ff0000));
      }
      return eiobox;
    }

    public ILayoutStyle center() {
      return probeinfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER);
    }

    @SuppressWarnings("unused")
    public ILayoutStyle right() {
      return probeinfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_BOTTOMRIGHT);
    }

    public void addMore() {
      addMoreIndicator = true;
    }

    public void finish() {
      if (eiobox != null) {
        if (addMoreIndicator) {
          addIcon(addIcon(get().horizontal(center()), IconEIO.TOP_NOICON, 0), IconEIO.TOP_MORE, 0);
        } else {
          addIcon(addIcon(get().horizontal(center()), IconEIO.TOP_NOICON, 0), IconEIO.TOP_NOMORE, 0);
        }
      } else if (addMoreIndicator) {
        addIcon(addIcon(probeinfo.vertical().horizontal(center()), IconEIO.TOP_NOICON_WIDE, 0), IconEIO.TOP_MORE, 0);
      }
    }
  }

  private @Nonnull String locRaw(@Nonnull String string) {
    return STARTLOC + string + ENDLOC;
  }

  private @Nonnull String loc(String string) {
    return string == null ? "(???)" : locRaw(EnderIO.lang.addPrefix(string));
  }

  private @Nonnull String loc(@Nonnull String langKey, @Nonnull String param) {
    return loc(langKey + ".pre") + param + loc(langKey + ".post");
  }

  private @Nonnull String loc(@Nonnull String langKey, @Nonnull String param1, @Nonnull String param2) {
    return loc(langKey + ".pre") + param1 + loc(langKey + ".middle") + param2 + loc(langKey + ".post");
  }

  private void mkMobsBox(ProbeMode mode, EioBox mobbox, World world, TOPData data) {
    if (data.hasMobs) {
      if (mode != ProbeMode.NORMAL || TopConfig.showMobsByDefault.get()) {
        mobbox.get().text(loc("top.action.header", loc(data.mobAction)));

        if (data.mobs.isEmpty()) {
          mobbox.get().text(loc("top.action.none"));
        } else if (data.mobs.size() <= 4) {
          for (CapturedMob capturedMob : data.mobs) {
            mobbox.get().horizontal(mobbox.center()).entity(capturedMob.getEntity(world, false))
                .text(locRaw("entity." + capturedMob.getTranslationName() + ".name"));
          }
        } else {
          IProbeInfo mobList = mobbox.get().horizontal(mobbox.center());
          int count = 0;
          for (CapturedMob capturedMob : data.mobs) {
            if (count++ >= 4) {
              mobList = mobbox.get().horizontal(mobbox.center());
              count = 0;
            }
            mobList.entity(capturedMob.getEntity(world, false));
          }
        }
      } else {
        mobbox.addMore();
      }
    }
  }

  private void mkRangeLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.hasRange) {
      if (mode != ProbeMode.NORMAL || TopConfig.showRangeByDefault.get()) {
        int sizeX = (int) data.bounds.sizeX();
        int sizeY = (int) data.bounds.sizeY();
        int sizeZ = (int) data.bounds.sizeZ();

        addIcon(eiobox.get().horizontal(eiobox.center()), IconEIO.SHOW_RANGE)
            .text(loc("top.range.header", EnderIO.lang.localize("top.range", sizeX, sizeY, sizeZ)));
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkNotificationLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.notifications != null && !data.notifications.isEmpty()) {
      IProbeInfo vertical = addIcon(eiobox.get().horizontal(eiobox.center()), IconEIO.REDSTONE_MODE_NEVER)
          .vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1));
      for (ILocalizable notification : data.notifications) {
        vertical.text(locRaw(notification.getUnlocalizedName()));
      }
    }
  }

  private void mkSideConfigLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.hasIOMode) {
      if (mode != ProbeMode.NORMAL || TopConfig.showSideConfigByDefault.get()) {
        addIcon(eiobox.get().horizontal(eiobox.center()), IconEIO.IO_CONFIG_UP).vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1))
            .text(TextFormatting.YELLOW + loc("top.machine.side", TextFormatting.WHITE + loc("top.machine.side." + data.sideName)))
            .text(TextFormatting.YELLOW + loc("top.machine.ioMode", loc(data.ioMode.getColorerUnlocalizedName())));
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkChannelLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.sendChannels != null && data.recvChannels != null) {
      if (mode != ProbeMode.NORMAL || TopConfig.showChannelsByDefault.get()) {
        ChannelType.VALUES.apply(new Callback<ChannelType>() {
          @Override
          public void apply(@Nonnull ChannelType type) {
            if (!data.sendChannels.get(type).isEmpty() || !data.recvChannels.get(type).isEmpty()) {
              final IProbeInfo lines = addIcon(eiobox.get().horizontal(eiobox.center()), type.getWidgetIcon())
                  .vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1));
              if (data.sendChannels.get(type).isEmpty()) {
                lines.text(loc("top.channel.send", loc("top.channel.none")));
              } else {
                for (Channel channel : data.sendChannels.get(type)) {
                  lines.text(loc("top.channel.send", channel.getName()));
                }
              }

              if (data.recvChannels.get(type).isEmpty()) {
                lines.text(loc("top.channel.recv", loc("top.channel.none")));
              } else {
                for (Channel channel : data.recvChannels.get(type)) {
                  lines.text(loc("top.channel.recv", channel.getName()));
                }
              }
            }
          }
        });
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkRedstoneLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.hasRedstone) {
      if (mode != ProbeMode.NORMAL || TopConfig.showRedstoneByDefault.get()) {
        addIcon(eiobox.get().horizontal(eiobox.center()), data.redstoneIcon).vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1))
            .text(loc(data.redstoneTooltip)).text(loc("top.redstone.header", loc("top.redstone." + data.redstoneControlStatus)));
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkPaint(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.isPainted) {
      IProbeInfo info = eiobox.get().horizontal(eiobox.center()).item(new ItemStack(Items.PAINTING))
          .vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1)).text(loc("top.paint.header"));
      // ItemStack.getDisplayName() should be run on the client, but I don't think we have a way to get the stack there
      if (Prep.isValid(data.paint2)) {
        info.horizontal(eiobox.center()).item(data.paint2).text(data.paint2.getDisplayName());
      }
      if (Prep.isValid(data.paint1)) {
        info.horizontal(eiobox.center()).item(data.paint1).text(data.paint1.getDisplayName());
      }
    }
  }

  private void mkOwner(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (mode == ProbeMode.DEBUG && data.owner != null) {
      ItemStack skull = new ItemStack(Items.SKULL, 1, 3);
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setTag("SkullOwner", NBTUtil.writeGameProfile(new NBTTagCompound(), data.owner.getAsGameProfile()));
      skull.setTagCompound(nbt);
      eiobox.get().horizontal(eiobox.center()).item(skull).vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1)).text(loc("top.owner.header"))
          .text(data.owner.getPlayerName());
    }
  }

  private void mkEnergyLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.hasRF) {
      if (mode != ProbeMode.NORMAL || TopConfig.showPowerByDefault.get()) {
        IProbeInfo rfLine = eiobox.get().horizontal(eiobox.center()).item(Material.POWDER_INFINITY.getStack());
        if (data.hasRFIO) {
          rfLine = rfLine.vertical();
        }
        if (data.isPowered) {
          rfLine.progress(data.rf, data.maxrf,
                  eiobox.getProbeinfo()
                  .defaultProgressStyle()
                  .suffix(EnderIO.lang.localize("top.suffix.rf"))
                  .filledColor(0xffb669d5)
                  .alternateFilledColor(0xff8a49a5)
                  .numberFormat(COMPACT));
        } else {
          rfLine.text(loc("top.machine.outofpower"));
        }
        if (data.hasRFIO) {
          rfLine = rfLine.horizontal();
          rfLine.vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1))//
              .text(loc("top.rf.header.avg")).text(loc("top.rf.header.maxin")).text(loc("top.rf.header.maxout"));

          // LangPower.format should be run on the client, but we have no way to do that
          String line1 = loc("top.rf.value",
              (data.avgRF == 0 ? TextFormatting.WHITE : data.avgRF > 0 ? TextFormatting.GREEN + "+" : TextFormatting.RED) + LangPower.format(data.avgRF));
          String line2 = loc("top.rf.value", LangPower.format(data.maxRFIn));
          String line3 = loc("top.rf.value", LangPower.format(data.maxRFOut));
          rfLine = rfLine.vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-1)).text(line1).text(line2).text(line3);
        }
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkXPLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.hasXP) {
      if (mode != ProbeMode.NORMAL || TopConfig.showXPByDefault.get()) {
        // We need to put the number of levels in as "current" value for it to be displayed as text. To make the progress bar scale to the partial level, we set
        // the "max" value in a way that is in the same ratio to the number of levels as the xp needed for the next level is to the current xp. If the bar
        // should be empty but we do have at least one level in, there will be a small error, as (levels/Integer.MAX_VALUE) > 0.
        int scalemax = data.xpBarScaled > 0 ? data.experienceLevel * 100 / data.xpBarScaled : 320127979;
        eiobox.get().horizontal(eiobox.center()).item(new ItemStack(Items.EXPERIENCE_BOTTLE)).progress(data.experienceLevel, scalemax,
            eiobox.getProbeinfo().defaultProgressStyle().suffix(EnderIO.lang.localize("top.suffix.levels")).filledColor(0xff00FF0F)
                .alternateFilledColor(0xff00AA0A));
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkItemFillLevelLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.hasItemFillLevel) {
      if (mode != ProbeMode.NORMAL || TopConfig.showItemCountDefault.get()) {
        eiobox.get().horizontal(eiobox.center()).item(new ItemStack(Blocks.CHEST)).progress(data.fillCur, data.fillMax,
            eiobox.getProbeinfo().defaultProgressStyle().suffix(EnderIO.lang.localize("top.suffix.items")).filledColor(0xfff8f83c)
                .alternateFilledColor(0xffcfac0b));
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkTankLines(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.tankData != null && !data.tankData.isEmpty()) {
      if (mode != ProbeMode.NORMAL || TopConfig.showTanksByDefault.get()) {
        for (ITankData tank : data.tankData) {
          SmartTank smartTank = new SmartTank(1000);
          String content1;
          IProgressStyle content2 = null;
          int content2current = 0;
          int content2max = 0;
          final FluidStack fluid = tank.getContent();
          if (fluid != null) {
            FluidStack fluid2 = fluid.copy();
            fluid2.amount = fluid.amount * 1000 / tank.getCapacity();
            smartTank.setFluid(fluid2);
            content1 = NullHelper.first(fluid.getLocalizedName(), "(???)");
            // TODO lang-format those numbers
//            content2 = loc("top.tank.content", "" + fluid.amount, "" + tank.getCapacity());
            content2 = eiobox.getProbeinfo()
                            .defaultProgressStyle()
                            .suffix("mB")
                            .filledColor(0xff4671f5)
                            .alternateFilledColor(0xff4671f5);
            content2current = fluid.amount;
            content2max = tank.getCapacity();
          } else {


            content1 = loc("top.tank.content.empty");
            // TODO lang-format those numbers
            content2 = eiobox.getProbeinfo()
                    .defaultProgressStyle()
                    .suffix("mB")
                    .filledColor(0xff4671f5)
                    .alternateFilledColor(0xff4671f5);
            content2current = 0;
            content2max = tank.getCapacity();
          }
          switch (tank.getTankType()) {
          case INPUT:
            content1 = loc("top.tank.header.input", content1);
            break;
          case OUTPUT:
            content1 = loc("top.tank.header.output", content1);
            break;
          case STORAGE:
            content1 = loc("top.tank.header.storage", content1);
            break;
          }
          ItemStack stack = new ItemStack(ModObject.blockFusedQuartz.getBlockNN()); // sic!
          ItemTankHelper.setTank(stack, smartTank);
          NbtValue.FAKE.setBoolean(stack, true);

          eiobox.get().horizontal(eiobox.center()).item(stack).vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(1)).progress(content2current, content2max, content2).textSmall(content1);
          eiobox.get().vertical(eiobox.getProbeinfo().defaultLayoutStyle().spacing(-6));
        }
      } else {
        eiobox.addMore();
      }
    }
  }

  private void mkProgressLine(ProbeMode mode, EioBox eiobox, TOPData data) {
    if (data.progressResult != TOPData.ProgressResult.NONE) {
      if (mode != ProbeMode.NORMAL || TopConfig.showProgressByDefault.get() || data.progressResult == TOPData.ProgressResult.PROGRESS_NO_POWER) {
        final IProbeInfo progressLine = eiobox.get().horizontal(eiobox.center()).item(new ItemStack(Items.CLOCK));
        switch (data.progressResult) {
        case PROGRESS:
          progressLine.progress((int) (data.progress * 100), 100, eiobox.getProbeinfo().defaultProgressStyle()
              .suffix(EnderIO.lang.localize("top.suffix.percent")).filledColor(0xffffb600).alternateFilledColor(0xffffb600));
          break;
        case PROGRESS_NO_POWER:
          progressLine.text(loc("top.progress.outofpower"));
          break;
        case PROGRESS_ACTIVE:
        case NO_PROGRESS_ACTIVE:
          progressLine.text(loc("top.machine.active"));
          break;
        case PROGRESS_IDLE:
        case NO_PROGRESS_IDLE:
          progressLine.text(loc("top.machine.idle"));
          break;
        case NONE:
          break;
        }
      } else {
        eiobox.addMore();
      }
    }
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return super.equals(obj);
  }

  @Override
  public void getProbeConfig(IProbeConfig config, EntityPlayer player, World world, Entity entity, IProbeHitEntityData data) {
  }

  @Override
  public void getProbeConfig(IProbeConfig config, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    if (config != null && blockState != null && blockState.getBlock() instanceof BlockEio) {
      config.setRFMode(0);
    }
  }

  private static IProbeInfo addIcon(IProbeInfo probeInfo, IWidgetIcon icon) {
    return addIcon(probeInfo, icon, 4);
  }

  private static IProbeInfo addIcon(IProbeInfo probeInfo, IWidgetIcon icon, int border) {
    ResourceLocation texture = icon.getMap().getTexture();
    int x = icon.getX();
    int y = icon.getY();
    int width = icon.getWidth();
    int height = icon.getHeight();

    return probeInfo.icon(texture, x, y, width, height, probeInfo.defaultIconStyle().width(width + border).height(height + border));
  }

}
