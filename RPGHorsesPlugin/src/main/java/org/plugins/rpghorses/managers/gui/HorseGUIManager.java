package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUIItem;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.instances.HorseGUI;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import rorys.library.util.ItemUtil;

import java.util.HashSet;

public class HorseGUIManager {

	private final RPGHorsesMain     plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final StableGUIManager stableGUIManager;

	private String title;
	private int rows;
	private HashSet<GUIItem> guiItems = new HashSet<>();

	public HorseGUIManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, StableGUIManager stableGUIManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.stableGUIManager = stableGUIManager;

		reload();
	}

	public void reload() {
		guiItems.clear();

		String path = "horse-gui-options.";
		FileConfiguration config = plugin.getConfig();
		this.title = RPGMessagingUtil.format(config.getString(path + "title"));
		this.rows = config.getInt(path +  "rows");
		for (String itemID : config.getConfigurationSection(path + "items").getKeys(false)) {
			path = "horse-gui-options.items." + itemID + ".";
			ItemStack item = ItemUtil.getItemStack(config, path);
			ItemPurpose purpose = ItemPurpose.valueOf(config.getString(path + "purpose", "NOTHING"));
			int slot = ItemUtil.getSlot(config, path);
			guiItems.add(new GUIItem(item, purpose, slot));
		}

		for (Player p : Bukkit.getOnlinePlayers()) {
			HorseOwner horseOwner = horseOwnerManager.getHorseOwner(p);
			if (horseOwner.getGUILocation() == GUILocation.HORSE_GUI) {
				horseOwner.openHorseGUI(getHorseGUI(horseOwner.getHorseGUI().getRpgHorse()));
			}
		}
	}

	public HorseGUI getHorseGUI(RPGHorse rpgHorse) {
		Inventory inventory = Bukkit.createInventory(null, rows * 9, RPGMessagingUtil.format(title, rpgHorse));

		for (GUIItem guiItem : guiItems) {
			if (guiItem.getItemPurpose() != ItemPurpose.TOGGLE_AUTOMOUNT_OFF && guiItem.getItemPurpose() != ItemPurpose.TOGGLE_AUTOMOUNT_ON) {
				inventory.setItem(guiItem.getSlot(), guiItem.getItem());
			}
		}

		inventory.setItem(ItemUtil.getSlot(plugin.getConfig(), "horse-gui-options.horse-item"), stableGUIManager.fillPlaceholders(stableGUIManager.getHorseItem(rpgHorse), rpgHorse));

		GUIItem autoMountItem = rpgHorse.getHorseOwner().autoMountOn() ? getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_ON) : getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_OFF);
		inventory.setItem(autoMountItem.getSlot(), autoMountItem.getItem());

		ItemStack fillItem = getGUIItem(ItemPurpose.FILL).getItem();
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			if (inventory.getItem(slot) == null) {
				inventory.setItem(slot, fillItem);
			}
		}

		return new HorseGUI(rpgHorse, inventory);
	}

	public void toggleAutoMount(HorseGUI horseGUI) {
		HorseOwner horseOwner = horseGUI.getRpgHorse().getHorseOwner();
		horseOwner.setAutoMount(!horseOwner.autoMountOn());
		Inventory inv = horseGUI.getInventory();
		GUIItem onItem = getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_ON), offItem = getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_OFF);
		ItemStack fillItem = getGUIItem(ItemPurpose.FILL).getItem();
		if (horseOwner.autoMountOn()) {
			inv.setItem(offItem.getSlot(), fillItem);
			inv.setItem(onItem.getSlot(), onItem.getItem());
		} else {
			inv.setItem(onItem.getSlot(), fillItem);
			inv.setItem(offItem.getSlot(), offItem.getItem());
		}
	}

	public ItemPurpose getItemPurpose(int slot) {
		for (GUIItem guiItem : guiItems) {
			if (guiItem.getSlot() == slot&& (guiItem.getItemPurpose() != ItemPurpose.NOTHING || guiItem.getItemPurpose() != ItemPurpose.FILL)) {
					return guiItem.getItemPurpose();
			}
		}
		return ItemPurpose.NOTHING;
	}

	public GUIItem getGUIItem(ItemPurpose itemPurpose) {
		for (GUIItem guiItem : guiItems) {
			if (guiItem.getItemPurpose() == itemPurpose) {
				return guiItem;
			}
		}
		return null;
	}

}
