package com.camistudios.aegisguard.listeners;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import com.camistudios.aegisguard.data.PlayerDataManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public class CombatListener implements Listener {

    private final AegisGuard plugin;

    public CombatListener(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        
        Player player = event.getPlayer();
        if (player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.hasPermission("aegisguard.bypass")) {
            return;
        }
        
        PlayerData data = PlayerDataManager.get(player);
        if (data == null) return;
        
        for (Check check : data.getCheckManager().getChecks()) {
            check.onArmSwing();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.isOp() || attacker.hasPermission("aegisguard.bypass") || attacker.getGameMode() == GameMode.CREATIVE) return;

        if (!(event.getEntity() instanceof Player victim)) return;

        PlayerData data = PlayerDataManager.get(attacker);
        if (data == null) return;

        for (Check check : data.getCheckManager().getChecks()) {
            check.onCombat(victim);
        }
    }
}