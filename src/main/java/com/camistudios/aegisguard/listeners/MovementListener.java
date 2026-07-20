package com.camistudios.aegisguard.listeners;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import com.camistudios.aegisguard.data.PlayerDataManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {

    private final AegisGuard plugin;

    public MovementListener(AegisGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.POISON || 
                event.getCause() == EntityDamageEvent.DamageCause.DROWNING || 
                event.getCause() == EntityDamageEvent.DamageCause.STARVATION ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                return;
            }
            PlayerData data = PlayerDataManager.get(player);
            if (data != null) {
                data.setLastDamageTime(System.currentTimeMillis());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerDataManager.get(player);
        if (data == null) return;
        
        if (player.isOp() || player.getAllowFlight() || player.getGameMode() == GameMode.CREATIVE || 
            player.getGameMode() == GameMode.SPECTATOR || player.isInsideVehicle() || 
            player.hasPermission("aegisguard.bypass") || player.isGliding() || player.isRiptiding()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        
        if (from.distanceSquared(to) > 25.0) return;
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;

        double deltaX = to.getX() - from.getX();
        double deltaY = to.getY() - from.getY();
        double deltaZ = to.getZ() - from.getZ();

        // Update PlayerData
        data.setLastLocation(from);
        data.setMovement(deltaX, deltaY, deltaZ);
        
        if (player.isOnGround()) {
            data.setOnGround(true);
            data.incrementGroundTicks();
        } else {
            data.setOnGround(false);
            data.incrementAirTicks();
        }

        long now = System.currentTimeMillis();
        if (now - data.getLastDamageTime() > 300) {
            data.setLastDamageTime(0); // clear damage immunity
        }

        // Dispatch to all registered checks
        for (Check check : data.getCheckManager().getChecks()) {
            check.onMove(from, to);
        }
        
        data.setLastYVelocity(deltaY);
    }
}