package com.camistudios.aegisguard.checks;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.data.PlayerData;
import org.bukkit.Location;

public abstract class Check {

    protected final AegisGuard plugin;
    protected final PlayerData data;
    private final String name;
    private double buffer = 0.0;

    public Check(AegisGuard plugin, PlayerData data, String name) {
        this.plugin = plugin;
        this.data = data;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected void flag(String info, double weight) {
        double currentConf = plugin.getAlertManager().logViolationWeight(data.getPlayer(), name.toLowerCase(), weight);
        plugin.getAlertManager().sendAlert(data.getPlayer(), name.toLowerCase(), currentConf, info);
        plugin.getPunishmentManager().checkThreshold(data.getPlayer(), name.toLowerCase(), currentConf);
    }
    
    protected void flagAndSetback(String info, double weight) {
        flag(info, weight);
        if (data.getLastLocation() != null) {
            data.getPlayer().teleport(data.getLastLocation());
        }
    }

    protected void increaseBuffer() {
        buffer += 1.0;
    }
    
    protected void increaseBuffer(double amount) {
        buffer += amount;
    }

    protected void decreaseBuffer(double amount) {
        buffer = Math.max(0, buffer - amount);
    }

    protected void resetBuffer() {
        buffer = 0;
    }

    protected double getBuffer() {
        return buffer;
    }
    
    // Abstract Methods that individual hacks can choose to implement
    public void onMove(Location from, Location to) {}
    public void onCombat(org.bukkit.entity.Player victim) {}
    public void onArmSwing() {}
}
