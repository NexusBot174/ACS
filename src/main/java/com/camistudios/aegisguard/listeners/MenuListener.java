package com.camistudios.aegisguard.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuListener implements Listener {

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lAegisGuard V4 §8| §7Menú Principal");

        ItemStack status = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta statusMeta = status.getItemMeta();
        if (statusMeta != null) {
            statusMeta.setDisplayName("§a§lSISTEMA ACTIVO");
            statusMeta.setLore(Arrays.asList("§7El motor anticheat está", "§7monitoreando en tiempo real."));
            status.setItemMeta(statusMeta);
        }

        ItemStack reload = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta reloadMeta = reload.getItemMeta();
        if (reloadMeta != null) {
            reloadMeta.setDisplayName("§c§lRecargar Configuración");
            reloadMeta.setLore(Arrays.asList("§7Haz click para recargar", "§7los archivos YAML y Discord."));
            reload.setItemMeta(reloadMeta);
        }

        inv.setItem(11, status);
        inv.setItem(15, reload);

        // Fill empty
        ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fill.getItemMeta();
        if (fillMeta != null) {
            fillMeta.setDisplayName(" ");
            fill.setItemMeta(fillMeta);
        }
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, fill);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("AegisGuard")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem().getType() == Material.REDSTONE_TORCH) {
                player.closeInventory();
                player.performCommand("ag reload");
            }
        }
    }
}
