package mekanism.common.tile.base;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.Action;
import mekanism.api.DataHandlerUtils;
import mekanism.api.IMekWrench;
import mekanism.api.NBTConstants;
import mekanism.api.Upgrade;
import mekanism.api.block.IHasTileEntity;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.gas.IMekanismGasHandler;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.infuse.IMekanismInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IMekanismFluidHandler;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.inventory.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.IMekanismInventory;
import mekanism.api.math.FloatingLong;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.sustained.ISustainedInventory;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.ITileComponent;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeSound;
import mekanism.common.block.attribute.AttributeStateActive;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.attribute.Attributes.AttributeComparator;
import mekanism.common.block.attribute.Attributes.AttributeInventory;
import mekanism.common.block.attribute.Attributes.AttributeRedstone;
import mekanism.common.block.attribute.Attributes.AttributeSecurity;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import mekanism.common.capabilities.heat.ITileHeatHandler;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.heat.IHeatCapacitorHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.resolver.manager.EnergyHandlerManager;
import mekanism.common.capabilities.resolver.manager.FluidHandlerManager;
import mekanism.common.capabilities.resolver.manager.GasHandlerManager;
import mekanism.common.capabilities.resolver.manager.HeatHandlerManager;
import mekanism.common.capabilities.resolver.manager.ICapabilityHandlerManager;
import mekanism.common.capabilities.resolver.manager.InfusionHandlerManager;
import mekanism.common.capabilities.resolver.manager.ItemHandlerManager;
import mekanism.common.config.MekanismConfig;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.frequency.IFrequencyHandler;
import mekanism.common.inventory.container.ITrackableContainer;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.container.sync.SyncableFloatingLong;
import mekanism.common.inventory.container.sync.SyncableFluidStack;
import mekanism.common.inventory.container.sync.SyncableGasStack;
import mekanism.common.inventory.container.sync.SyncableInfusionStack;
import mekanism.common.inventory.slot.UpgradeInventorySlot;
import mekanism.common.item.ItemConfigurationCard;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileActive;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.tile.interfaces.ITileRedstone;
import mekanism.common.tile.interfaces.ITileSound;
import mekanism.common.tile.interfaces.ITileUpgradable;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.SecurityUtils;
import mekanism.common.util.text.TextComponentUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemHandlerHelper;

//TODO: We need to move the "supports" methods into the source interfaces so that we make sure they get checked before being used
public abstract class TileEntityMekanism extends CapabilityTileEntity implements IFrequencyHandler, ITickableTileEntity, ITileDirectional,
      ITileActive, ITileSound, ITileRedstone, ISecurityTile, IMekanismInventory, ISustainedInventory, ITileUpgradable, ITierUpgradable, IComparatorSupport,
      ITrackableContainer, IMekanismGasHandler, IMekanismInfusionHandler, IMekanismFluidHandler, IMekanismStrictEnergyHandler, ITileHeatHandler {

    /**
     * The players currently using this block.
     */
    public Set<PlayerEntity> playersUsing = new ObjectOpenHashSet<>();

    /**
     * A timer used to send packets to clients.
     */
    public int ticker;
    private final List<ICapabilityHandlerManager<?>> capabilityHandlerManagers = new ArrayList<>();
    private final List<ITileComponent> components = new ArrayList<>();

    protected IBlockProvider blockProvider;

    private boolean supportsComparator;
    private boolean supportsUpgrades;
    private boolean supportsRedstone;
    private boolean canBeUpgraded;
    private boolean isDirectional;
    private boolean isActivatable;
    private boolean hasInventory;
    private boolean hasSecurity;
    private boolean hasSound;
    private boolean hasGui;

    //Variables for handling ITileRedstone
    //TODO: Move these to private variables?
    public boolean redstone = false;
    private boolean redstoneLastTick = false;
    /**
     * This machine's current RedstoneControl type.
     */
    private RedstoneControl controlType = RedstoneControl.DISABLED;
    //End variables ITileRedstone

    //Variables for handling IComparatorSupport
    protected int currentRedstoneLevel;
    //End variables IComparatorSupport

    //Variables for handling ITileUpgradable
    //TODO: Convert this to being private
    protected TileComponentUpgrade upgradeComponent;
    //End variables ITileUpgradable

    //Variables for handling ITileContainer
    protected final ItemHandlerManager itemHandlerManager;
    //End variables ITileContainer

    //Variables for handling IMekanismGasHandler
    protected final GasHandlerManager gasHandlerManager;
    //End variables IMekanismGasHandler

    //Variables for handling IMekanismInfusionHandler
    protected final InfusionHandlerManager infusionHandlerManager;
    //End variables IMekanismInfusionHandler

    //Variables for handling IMekanismFluidHandler
    protected final FluidHandlerManager fluidHandlerManager;
    //End variables IMekanismFluidHandler

    //Variables for handling IMekanismStrictEnergyHandler
    protected final EnergyHandlerManager energyHandlerManager;
    private FloatingLong lastEnergyReceived = FloatingLong.ZERO;
    //End variables IMekanismStrictEnergyHandler

    //Variables for handling IMekanismHeatHandler
    protected final HeatHandlerManager heatHandlerManager;
    //End variables for IMekanismHeatHandler

    //Variables for handling ITileSecurity
    private TileComponentSecurity securityComponent;
    //End variables ITileSecurity

    //Variables for handling ITileActive
    private boolean currentActive;
    private int updateDelay;
    protected IntSupplier delaySupplier = MekanismConfig.general.blockDeactivationDelay;
    //End variables ITileActive

    //Variables for handling ITileSound
    @Nullable
    private final SoundEvent soundEvent;

    /**
     * Only used on the client
     */
    private ISound activeSound;
    private int playSoundCooldown = 0;
    //End variables ITileSound

    public TileEntityMekanism(IBlockProvider blockProvider) {
        super(((IHasTileEntity<? extends TileEntity>) blockProvider.getBlock()).getTileType());
        this.blockProvider = blockProvider;
        setSupportedTypes(this.blockProvider.getBlock());
        presetVariables();
        capabilityHandlerManagers.add(gasHandlerManager = new GasHandlerManager(getInitialGasTanks(), this));
        capabilityHandlerManagers.add(infusionHandlerManager = new InfusionHandlerManager(getInitialInfusionTanks(), this));
        capabilityHandlerManagers.add(fluidHandlerManager = new FluidHandlerManager(getInitialFluidTanks(), this));
        capabilityHandlerManagers.add(energyHandlerManager = new EnergyHandlerManager(getInitialEnergyContainers(), this));
        capabilityHandlerManagers.add(heatHandlerManager = new HeatHandlerManager(getInitialHeatCapacitors(), this));
        capabilityHandlerManagers.add(itemHandlerManager = new ItemHandlerManager(getInitialInventory(), hasInventory, this));
        for (ICapabilityHandlerManager<?> capabilityHandlerManager : capabilityHandlerManagers) {
            //Add all managers that we support in our tile, as capability resolvers
            if (capabilityHandlerManager.canHandle()) {
                addCapabilityResolver(capabilityHandlerManager);
            }
        }
        if (supportsUpgrades()) {
            upgradeComponent = new TileComponentUpgrade(this, UpgradeInventorySlot.of(this, getSupportedUpgrade()));
        }
        if (hasSecurity()) {
            securityComponent = new TileComponentSecurity(this);
        }
        if (hasSound()) {
            soundEvent = Attribute.get(blockProvider.getBlock(), AttributeSound.class).getSoundEvent();
        } else {
            soundEvent = null;
        }
    }

    private void setSupportedTypes(Block block) {
        //Used to get any data we may need
        supportsUpgrades = Attribute.has(block, AttributeUpgradeSupport.class);
        canBeUpgraded = Attribute.has(block, AttributeUpgradeable.class);
        isDirectional = Attribute.has(block, AttributeStateFacing.class);
        supportsRedstone = Attribute.has(block, AttributeRedstone.class);
        hasSound = Attribute.has(block, AttributeSound.class);
        hasGui = Attribute.has(block, AttributeGui.class);
        hasInventory = Attribute.has(block, AttributeInventory.class);
        hasSecurity = Attribute.has(block, AttributeSecurity.class);
        isActivatable = hasSound || Attribute.has(block, AttributeStateActive.class);
        supportsComparator = Attribute.has(block, AttributeComparator.class);
    }

    /**
     * Sets variables up, called immediately after {@link #setSupportedTypes(Block)} but before any things start being created.
     *
     * @implNote This method should be used for setting any variables that would normally be set directly, except that gets run to late to set things up properly in our
     * constructor.
     */
    protected void presetVariables() {
    }

    public Block getBlockType() {
        return blockProvider.getBlock();
    }

    /**
     * Should data related to the given type be persisted in this tile save
     */
    public boolean persists(SubstanceType type) {
        return type.canHandle(this);
    }

    /**
     * Should data related to the given type be saved to the item and synced to the client in the GUI
     */
    public boolean handles(SubstanceType type) {
        return persists(type);
    }

    @Override
    public final boolean supportsUpgrades() {
        return supportsUpgrades;
    }

    @Override
    public final boolean supportsComparator() {
        return supportsComparator;
    }

    @Override
    public final boolean canBeUpgraded() {
        return canBeUpgraded;
    }

    @Override
    public final boolean isDirectional() {
        return isDirectional;
    }

    @Override
    public final boolean supportsRedstone() {
        return supportsRedstone;
    }

    @Override
    public final boolean hasSound() {
        return hasSound;
    }

    public final boolean hasGui() {
        return hasGui;
    }

    @Override
    public final boolean hasSecurity() {
        return hasSecurity;
    }

    @Override
    public final boolean isActivatable() {
        return isActivatable;
    }

    @Override
    public final boolean hasInventory() {
        return itemHandlerManager.canHandle();
    }

    @Override
    public final boolean canHandleInfusion() {
        return infusionHandlerManager.canHandle();
    }

    @Override
    public final boolean canHandleFluid() {
        return fluidHandlerManager.canHandle();
    }

    @Override
    public final boolean canHandleGas() {
        return gasHandlerManager.canHandle();
    }

    @Override
    public final boolean canHandleEnergy() {
        return energyHandlerManager.canHandle();
    }

    @Override
    public final boolean canHandleHeat() {
        return heatHandlerManager.canHandle();
    }

    public void addComponent(ITileComponent component) {
        components.add(component);
        if (component instanceof TileComponentConfig) {
            addConfigComponent((TileComponentConfig) component);
        }
    }

    public List<ITileComponent> getComponents() {
        return components;
    }

    @Nonnull
    public ITextComponent getName() {
        //TODO: Is this useful or should the gui title be got a different way
        // We can probably do it via the containers name
        return TextComponentUtil.translate(getBlockType().getTranslationKey());
    }

    @Override
    public void markDirtyComparator() {
        //Only update the comparator state if we support comparators
        if (supportsComparator() && !getBlockState().isAir(world, pos)) {
            int newRedstoneLevel = getRedstoneLevel();
            if (newRedstoneLevel != currentRedstoneLevel) {
                currentRedstoneLevel = newRedstoneLevel;
                world.updateComparatorOutputLevel(pos, getBlockType());
            }
        }
    }

    public WrenchResult tryWrench(BlockState state, PlayerEntity player, Hand hand, BlockRayTraceResult rayTrace) {
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty()) {
            IMekWrench wrenchHandler = MekanismUtils.getWrench(stack);
            if (wrenchHandler != null) {
                if (wrenchHandler.canUseWrench(stack, player, rayTrace.getPos())) {
                    if (hasSecurity() && !SecurityUtils.canAccess(player, this)) {
                        SecurityUtils.displayNoAccess(player);
                        return WrenchResult.NO_SECURITY;
                    }
                    if (player.isSneaking()) {
                        MekanismUtils.dismantleBlock(state, getWorld(), pos, this);
                        return WrenchResult.DISMANTLED;
                    }
                    //Special ITileDirectional handling
                    if (isDirectional()) {
                        //TODO: Extract this out into a handleRotation method?
                        setFacing(getDirection().rotateY());
                    }
                    return WrenchResult.SUCCESS;
                }
            }
        }
        return WrenchResult.PASS;
    }

    public ActionResultType openGui(PlayerEntity player) {
        //Everything that calls this has isRemote being false but add the check just in case anyways
        if (hasGui() && !isRemote() && !player.isSneaking()) {
            if (hasSecurity() && !SecurityUtils.canAccess(player, this)) {
                SecurityUtils.displayNoAccess(player);
                return ActionResultType.FAIL;
            }
            //Pass on this activation if the player is rotating with a configurator
            ItemStack stack = player.getHeldItemMainhand();
            if (isDirectional() && !stack.isEmpty() && stack.getItem() instanceof ItemConfigurator) {
                ItemConfigurator configurator = (ItemConfigurator) stack.getItem();
                if (configurator.getState(stack) == ItemConfigurator.ConfiguratorMode.ROTATE) {
                    return ActionResultType.PASS;
                }
            }
            //Pass on this activation if the player is using a configuration card (and this tile supports the capability)
            if (CapabilityUtils.getCapability(this, Capabilities.CONFIG_CARD_CAPABILITY, null).isPresent()) {
                if (!stack.isEmpty() && stack.getItem() instanceof ItemConfigurationCard) {
                    return ActionResultType.PASS;
                }
            }

            NetworkHooks.openGui((ServerPlayerEntity) player, Attribute.get(blockProvider.getBlock(), AttributeGui.class).getProvider(this), pos);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void tick() {
        for (ITileComponent component : components) {
            component.tick();
        }
        if (isRemote()) {
            if (hasSound()) {
                updateSound();
            }
            if (isActivatable()) {
                if (ticker == 0) {
                    MekanismUtils.updateBlock(getWorld(), getPos());
                }
            }
            onUpdateClient();
        } else {
            if (isActivatable()) {
                if (updateDelay > 0) {
                    updateDelay--;
                    if (updateDelay == 0 && getClientActive() != currentActive) {
                        //If it doesn't match and we are done with the delay period, then update it
                        world.setBlockState(pos, Attribute.setActive(getBlockState(), currentActive));
                    }
                }
            }
            onUpdateServer();
            if (persists(SubstanceType.HEAT)) {
                // update heat after server tick as we now have simulate changes
                // we use persists, as only one reference should update
                updateHeatCapacitors(null);
            }
            lastEnergyReceived = FloatingLong.ZERO;
        }
        ticker++;
        if (supportsRedstone()) {
            redstoneLastTick = redstone;
        }
    }

    public void open(PlayerEntity player) {
        playersUsing.add(player);
    }

    public void close(PlayerEntity player) {
        playersUsing.remove(player);
    }

    @Override
    public void remove() {
        super.remove();
        for (ITileComponent component : components) {
            component.invalidate();
        }
        if (isRemote() && hasSound()) {
            updateSound();
        }
    }

    /**
     * Update call for machines. Use instead of updateEntity -- it's called every tick on the client side.
     */
    protected void onUpdateClient() {
    }

    /**
     * Update call for machines. Use instead of updateEntity -- it's called every tick on the server side.
     */
    protected void onUpdateServer() {
    }

    @Override
    public void read(CompoundNBT nbtTags) {
        super.read(nbtTags);
        redstone = nbtTags.getBoolean(NBTConstants.REDSTONE);
        for (ITileComponent component : components) {
            component.read(nbtTags);
        }
        if (supportsRedstone()) {
            NBTUtils.setEnumIfPresent(nbtTags, NBTConstants.CONTROL_TYPE, RedstoneControl::byIndexStatic, type -> controlType = type);
        }
        if (hasInventory() && persistInventory()) {
            DataHandlerUtils.readSlots(getInventorySlots(null), nbtTags.getList(NBTConstants.ITEMS, NBT.TAG_COMPOUND));
        }
        for (SubstanceType type : SubstanceType.values()) {
            if (type.canHandle(this) && persists(type)) {
                type.read(this, nbtTags);
            }
        }
        if (isActivatable()) {
            currentActive = nbtTags.getBoolean(NBTConstants.ACTIVE_STATE);
            updateDelay = nbtTags.getInt(NBTConstants.UPDATE_DELAY);
        }
    }

    @Nonnull
    @Override
    public CompoundNBT write(CompoundNBT nbtTags) {
        super.write(nbtTags);
        nbtTags.putBoolean(NBTConstants.REDSTONE, redstone);
        for (ITileComponent component : components) {
            component.write(nbtTags);
        }
        if (supportsRedstone()) {
            nbtTags.putInt(NBTConstants.CONTROL_TYPE, controlType.ordinal());
        }
        if (hasInventory() && persistInventory()) {
            nbtTags.put(NBTConstants.ITEMS, DataHandlerUtils.writeSlots(getInventorySlots(null)));
        }

        for (SubstanceType type : SubstanceType.values()) {
            if (type.canHandle(this) && persists(type)) {
                type.write(this, nbtTags);
            }
        }

        if (isActivatable()) {
            nbtTags.putBoolean(NBTConstants.ACTIVE_STATE, currentActive);
            nbtTags.putInt(NBTConstants.UPDATE_DELAY, updateDelay);
        }
        return nbtTags;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        for (ITileComponent component : components) {
            component.trackForMainContainer(container);
        }
        if (supportsRedstone()) {
            container.track(SyncableEnum.create(RedstoneControl::byIndexStatic, RedstoneControl.DISABLED, () -> controlType, value -> controlType = value));
        }
        if (canHandleGas() && handles(SubstanceType.GAS)) {
            List<IGasTank> gasTanks = getGasTanks(null);
            for (IGasTank gasTank : gasTanks) {
                container.track(SyncableGasStack.create(gasTank));
            }
        }
        if (canHandleInfusion() && handles(SubstanceType.INFUSION)) {
            List<IInfusionTank> infusionTanks = getInfusionTanks(null);
            for (IInfusionTank infusionTank : infusionTanks) {
                container.track(SyncableInfusionStack.create(infusionTank));
            }
        }
        if (canHandleFluid() && handles(SubstanceType.FLUID)) {
            List<IExtendedFluidTank> fluidTanks = getFluidTanks(null);
            for (IExtendedFluidTank fluidTank : fluidTanks) {
                container.track(SyncableFluidStack.create(fluidTank));
            }
        }
        if (canHandleHeat() && handles(SubstanceType.HEAT)) {
            List<IHeatCapacitor> heatCapacitors = getHeatCapacitors(null);
            for (IHeatCapacitor capacitor : heatCapacitors) {
                container.track(SyncableDouble.create(capacitor::getHeat, capacitor::setHeat));
                if (capacitor instanceof BasicHeatCapacitor) {
                    container.track(SyncableDouble.create(capacitor::getHeatCapacity, (capacity) -> ((BasicHeatCapacitor) capacitor).setHeatCapacity(capacity, false)));
                }
            }
        }
        if (canHandleEnergy() && handles(SubstanceType.ENERGY)) {
            container.track(SyncableFloatingLong.create(this::getInputRate, this::setInputRate));
            List<IEnergyContainer> energyContainers = getEnergyContainers(null);
            for (IEnergyContainer energyContainer : energyContainers) {
                container.track(SyncableFloatingLong.create(energyContainer::getEnergy, energyContainer::setEnergy));
                if (energyContainer instanceof MachineEnergyContainer<?>) {
                    MachineEnergyContainer<?> machineEnergy = (MachineEnergyContainer<?>) energyContainer;
                    if (supportsUpgrades() || machineEnergy.adjustableRates()) {
                        container.track(SyncableFloatingLong.create(machineEnergy::getMaxEnergy, machineEnergy::setMaxEnergy));
                        container.track(SyncableFloatingLong.create(machineEnergy::getEnergyPerTick, machineEnergy::setEnergyPerTick));
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    public CompoundNBT getReducedUpdateTag() {
        CompoundNBT updateTag = super.getReducedUpdateTag();
        for (ITileComponent component : components) {
            component.addToUpdateTag(updateTag);
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundNBT tag) {
        super.handleUpdateTag(tag);
        for (ITileComponent component : components) {
            component.readFromUpdateTag(tag);
        }
    }

    public void onNeighborChange(Block block) {
        if (!isRemote() && supportsRedstone()) {
            updatePower();
        }
    }

    /**
     * Called when block is placed in world
     */
    public void onAdded() {
        if (supportsRedstone()) {
            updatePower();
        }
    }

    @Override
    public Frequency getFrequency(FrequencyManager manager) {
        //TODO: I don't think this is needed, only thing that uses this method is querying the quantum entangloporter
        if (manager == Mekanism.securityFrequencies && hasSecurity) {
            return getSecurity().getFrequency();
        }
        return null;
    }

    //Methods pertaining to IUpgradeableTile
    public void parseUpgradeData(@Nonnull IUpgradeData data) {
        Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
    }
    //End methods IUpgradeableTile

    //Methods for implementing ITileDirectional
    @Nonnull
    @Override
    public Direction getDirection() {
        if (isDirectional()) {
            return Attribute.getFacing(getBlockState());
        }
        //TODO: Remove, give it some better default, or allow it to be null
        return Direction.NORTH;
    }

    @Override
    public void setFacing(@Nonnull Direction direction) {
        if (isDirectional()) {
            BlockState state = Attribute.setFacing(getBlockState(), direction);
            if (world != null && state != null) {
                world.setBlockState(pos, state);
            }
        }
    }
    //End methods ITileDirectional

    //Methods for implementing ITileRedstone
    @Override
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(@Nonnull RedstoneControl type) {
        if (supportsRedstone()) {
            controlType = Objects.requireNonNull(type);
            markDirty(false);
        }
    }

    @Override
    public boolean isPowered() {
        return supportsRedstone() && redstone;
    }

    @Override
    public boolean wasPowered() {
        return supportsRedstone() && redstoneLastTick;
    }

    private void updatePower() {
        boolean power = world.isBlockPowered(getPos());
        if (redstone != power) {
            redstone = power;
            onPowerChange();
        }
    }
    //End methods ITileRedstone

    //Methods for implementing IComparatorSupport
    @Override
    public int getRedstoneLevel() {
        if (supportsComparator()) {
            if (hasInventory()) {
                return ItemHandlerHelper.calcRedstoneFromInventory(this);
            }
            //TODO: Do we want some other defaults as well?
        }
        return 0;
    }

    @Override
    public int getCurrentRedstoneLevel() {
        return currentRedstoneLevel;
    }
    //End methods IComparatorSupport

    //Methods for implementing ITileUpgradable
    @Nonnull
    @Override
    public Set<Upgrade> getSupportedUpgrade() {
        if (supportsUpgrades()) {
            return Attribute.get(blockProvider.getBlock(), AttributeUpgradeSupport.class).getSupportedUpgrades();
        }
        return Collections.emptySet();
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        if (upgrade == Upgrade.SPEED) {
            for (IEnergyContainer energyContainer : getEnergyContainers(null)) {
                if (energyContainer instanceof MachineEnergyContainer<?>) {
                    ((MachineEnergyContainer<?>) energyContainer).updateEnergyPerTick();
                }
            }
        } else if (upgrade == Upgrade.ENERGY) {
            for (IEnergyContainer energyContainer : getEnergyContainers(null)) {
                if (energyContainer instanceof MachineEnergyContainer<?>) {
                    MachineEnergyContainer<?> machineEnergy = (MachineEnergyContainer<?>) energyContainer;
                    machineEnergy.updateMaxEnergy();
                    machineEnergy.updateEnergyPerTick();
                }
            }
        }
    }
    //End methods ITileUpgradable

    //Methods for implementing ITileContainer
    @Nullable
    protected IInventorySlotHolder getInitialInventory() {
        return null;
    }

    @Nonnull
    @Override
    public final List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return itemHandlerManager.getContainers(side);
    }

    @Override
    public void onContentsChanged() {
        markDirty(false);
    }

    @Override
    public void setInventory(ListNBT nbtTags, Object... data) {
        if (nbtTags != null && !nbtTags.isEmpty() && persistInventory()) {
            DataHandlerUtils.readSlots(getInventorySlots(null), nbtTags);
        }
    }

    @Override
    public ListNBT getInventory(Object... data) {
        return persistInventory() ? DataHandlerUtils.writeSlots(getInventorySlots(null)) : new ListNBT();
    }

    /**
     * Should the inventory be persisted in this tile save
     */
    public boolean persistInventory() {
        return hasInventory();
    }
    //End methods ITileContainer

    //Methods for implementing IMekanismGasHandler
    @Nullable
    protected IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks() {
        return null;
    }

    @Nonnull
    @Override
    public final List<IGasTank> getGasTanks(@Nullable Direction side) {
        return gasHandlerManager.getContainers(side);
    }
    //End methods IMekanismGasHandler

    //Methods for implementing IMekanismInfusionHandler
    @Nullable
    protected IChemicalTankHolder<InfuseType, InfusionStack, IInfusionTank> getInitialInfusionTanks() {
        return null;
    }

    @Nonnull
    @Override
    public final List<IInfusionTank> getInfusionTanks(@Nullable Direction side) {
        return infusionHandlerManager.getContainers(side);
    }
    //End methods IMekanismInfusionHandler

    //Methods for implementing IMekanismFluidHandler
    @Nullable
    protected IFluidTankHolder getInitialFluidTanks() {
        return null;
    }

    @Nonnull
    @Override
    public final List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return fluidHandlerManager.getContainers(side);
    }
    //End methods IMekanismFluidHandler

    //Methods for implementing IMekanismStrictEnergyHandler
    @Nullable
    protected IEnergyContainerHolder getInitialEnergyContainers() {
        return null;
    }

    @Nonnull
    @Override
    public final List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyHandlerManager.getContainers(side);
    }

    @Nonnull
    @Override
    public FloatingLong insertEnergy(int container, @Nonnull FloatingLong amount, @Nullable Direction side, @Nonnull Action action) {
        IEnergyContainer energyContainer = getEnergyContainer(container, side);
        if (energyContainer == null) {
            return amount;
        }
        FloatingLong remainder = energyContainer.insert(amount, action, side == null ? AutomationType.INTERNAL : AutomationType.EXTERNAL);
        if (action.execute()) {
            lastEnergyReceived = lastEnergyReceived.plusEqual(amount.subtract(remainder));
        }
        return remainder;
    }

    public FloatingLong getInputRate() {
        return lastEnergyReceived;
    }

    protected void setInputRate(FloatingLong inputRate) {
        this.lastEnergyReceived = inputRate;
    }
    //End methods IMekanismStrictEnergyHandler

    //Methods for implementing IInWorldHeatHandler
    @Nullable
    protected IHeatCapacitorHolder getInitialHeatCapacitors() {
        return null;
    }

    @Nullable
    @Override
    public IHeatHandler getAdjacent(Direction side) {
        if (canHandleHeat() && getHeatCapacitorCount(side) > 0) {
            TileEntity adj = MekanismUtils.getTileEntity(getWorld(), getPos().offset(side));
            return MekanismUtils.toOptional(CapabilityUtils.getCapability(adj, Capabilities.HEAT_HANDLER_CAPABILITY, side.getOpposite())).orElse(null);
        }
        return null;
    }

    @Nonnull
    @Override
    public final List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return heatHandlerManager.getContainers(side);
    }
    //End methods for IInWorldHeatHandler

    //Methods for implementing ITileSecurity
    @Override
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }
    //End methods ITileSecurity

    //Methods for implementing ITileActive
    @Override
    public boolean getActive() {
        return isRemote() ? getClientActive() : currentActive;
    }

    private boolean getClientActive() {
        return Attribute.isActive(getBlockState());
    }

    @Override
    public void setActive(boolean active) {
        if (isActivatable()) {
            BlockState state = getBlockState();
            Block block = state.getBlock();
            if (active != currentActive && Attribute.has(block, AttributeStateActive.class)) {
                currentActive = active;
                if (getClientActive() != active) {
                    if (active) {
                        //Always turn on instantly
                        state = Attribute.setActive(state, true);
                        world.setBlockState(pos, state);
                    } else {
                        // if the update delay is already zero, we can go ahead and set the state
                        if (updateDelay == 0) {
                            world.setBlockState(pos, Attribute.setActive(getBlockState(), currentActive));
                        }
                        // we always reset the update delay when turning off
                        updateDelay = delaySupplier.getAsInt();
                    }
                }
            }
        }
    }
    //End methods ITileActive

    //Methods for implementing ITileSound

    /**
     * Used to check if this tile should attempt to play its sound
     */
    protected boolean canPlaySound() {
        return getActive();
    }

    /**
     * Only call this from the client
     */
    private void updateSound() {
        // If machine sounds are disabled, noop
        if (!hasSound() || !MekanismConfig.client.enableMachineSounds.get() || soundEvent == null) {
            return;
        }
        if (canPlaySound() && !isRemoved()) {
            // If sounds are being muted, we can attempt to start them on every tick, only to have them
            // denied by the event bus, so use a cooldown period that ensures we're only trying once every
            // second or so to start a sound.
            if (--playSoundCooldown > 0) {
                return;
            }

            // If this machine isn't fully muffled and we don't seem to be playing a sound for it, go ahead and
            // play it
            if (!isFullyMuffled() && (activeSound == null || !Minecraft.getInstance().getSoundHandler().isPlaying(activeSound))) {
                activeSound = SoundHandler.startTileSound(soundEvent, getSoundCategory(), getInitialVolume(), getPos());
            }
            // Always reset the cooldown; either we just attempted to play a sound or we're fully muffled; either way
            // we don't want to try again
            playSoundCooldown = 20;
        } else if (activeSound != null) {
            SoundHandler.stopTileSound(getPos());
            activeSound = null;
            playSoundCooldown = 0;
        }
    }

    private boolean isFullyMuffled() {
        if (!hasSound() || !supportsUpgrades()) {
            return false;
        }
        if (getComponent().supports(Upgrade.MUFFLING)) {
            return getComponent().getUpgrades(Upgrade.MUFFLING) == Upgrade.MUFFLING.getMax();
        }
        return false;
    }
    //End methods ITileSound
}