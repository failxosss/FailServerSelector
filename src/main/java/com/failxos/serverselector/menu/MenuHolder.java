package com.failxos.serverselector.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marks an inventory as one of our menus. */
public final class MenuHolder implements InventoryHolder {

    private final MenuDef menu;
    private Inventory inventory;

    public MenuHolder(MenuDef menu) {
        this.menu = menu;
    }

    public MenuDef menu() {
        return menu;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
