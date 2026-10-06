package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.SelectSetting;
import net.minecraft.block.*;

@ModuleInfo(name = "No Interact", category = ModuleCategory.UTILITY)
public class NoInteract extends BaseModule {
   private final SelectSetting blocks = new SelectSetting(this, "modules.settings.no_interact.blocks");
   private final SelectSetting.Value craftingTables = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.craftingTables").select();
   private final SelectSetting.Value enchantingTables = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.enchantingTables").select();
   private final SelectSetting.Value beds = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.beds").select();
   private final SelectSetting.Value chests = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.chests").select();
   private final SelectSetting.Value enderChests = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.enderChests").select();
   private final SelectSetting.Value trappedChests = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.trappedChests").select();
   private final SelectSetting.Value furnaces = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.furnaces").select();
   private final SelectSetting.Value barrels = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.barrels").select();
   private final SelectSetting.Value shulkers = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.shulkers").select();
   private final SelectSetting.Value droppers = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.droppers").select();
   private final SelectSetting.Value dispensers = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.dispensers").select();
   private final SelectSetting.Value hoppers = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.hoppers").select();
   private final SelectSetting.Value anvils = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.anvils").select();
   private final SelectSetting.Value cauldrons = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.cauldrons").select();
   private final SelectSetting.Value signs = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.signs").select();
   private final SelectSetting.Value bells = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.bells").select();
   private final SelectSetting.Value composters = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.composters").select();
   private final SelectSetting.Value brewingStands = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.brewingStands").select();
   private final SelectSetting.Value jukeBoxes = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.jukeBoxes").select();
   private final SelectSetting.Value commandBlocks = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.commandBlocks").select();
   private final SelectSetting.Value beacons = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.beacons").select();
   private final SelectSetting.Value respawnAnchors = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.respawnAnchors").select();
   private final SelectSetting.Value grindstones = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.grindstones").select();
   private final SelectSetting.Value lecterns = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.lecterns").select();
   private final SelectSetting.Value cartographyTables = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.cartographyTables").select();
   private final SelectSetting.Value looms = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.looms").select();
   private final SelectSetting.Value smithingTables = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.smithingTables").select();
   private final SelectSetting.Value stonecutters = new SelectSetting.Value(this.blocks, "modules.settings.no_interact.stonecutter").select();

   public boolean shouldPreventInteract(Block block) {
      if (block instanceof CraftingTableBlock) return this.craftingTables.isSelected();
      if (block instanceof EnchantingTableBlock) return this.enchantingTables.isSelected();
      if (block instanceof BedBlock) return this.beds.isSelected();
      if (block instanceof ChestBlock) return this.chests.isSelected();
      if (block instanceof EnderChestBlock) return this.enderChests.isSelected();
      if (block instanceof TrappedChestBlock) return this.trappedChests.isSelected();
      if (block instanceof AbstractFurnaceBlock) return this.furnaces.isSelected();
      if (block instanceof BarrelBlock) return this.barrels.isSelected();
      if (block instanceof ShulkerBoxBlock) return this.shulkers.isSelected();
      if (block instanceof DropperBlock) return this.droppers.isSelected();
      if (block instanceof DispenserBlock) return this.dispensers.isSelected();
      if (block instanceof HopperBlock) return this.hoppers.isSelected();
      if (block instanceof AnvilBlock) return this.anvils.isSelected();
      if (block instanceof AbstractCauldronBlock) return this.cauldrons.isSelected();
      if (block instanceof AbstractSignBlock) return this.signs.isSelected();
      if (block instanceof BellBlock) return this.bells.isSelected();
      if (block instanceof ComposterBlock) return this.composters.isSelected();
      if (block instanceof BrewingStandBlock) return this.brewingStands.isSelected();
      if (block instanceof JukeboxBlock) return this.jukeBoxes.isSelected();
      if (block instanceof CommandBlock) return this.commandBlocks.isSelected();
      if (block instanceof BeaconBlock) return this.beacons.isSelected();
      if (block instanceof RespawnAnchorBlock) return this.respawnAnchors.isSelected();
      if (block instanceof GrindstoneBlock) return this.grindstones.isSelected();
      if (block instanceof LecternBlock) return this.lecterns.isSelected();
      if (block instanceof CartographyTableBlock) return this.cartographyTables.isSelected();
      if (block instanceof LoomBlock) return this.looms.isSelected();
      if (block instanceof SmithingTableBlock) return this.smithingTables.isSelected();
      if (block instanceof StonecutterBlock) return this.stonecutters.isSelected();
      return false;
   }
}