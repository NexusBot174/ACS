package com.camistudios.aegisguard.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.CheckManager;

public class PlayerData {

    private final UUID uuid;
    private final Player player;
    
    // Movement Data
    private Location lastLocation;
    private double deltaX, deltaY, deltaZ;
    private double horizontalDistance;
    private int airTicks, waterTicks, groundTicks;
    private boolean onGround;
    private double lastYVelocity;
    
    // Combat Data
    private List<Long> clickHistory = new ArrayList<>();
    private float lastYawDelta, lastPitchDelta;
    
    // Generics
    private long lastDamageTime;
    
    // Violation tracking
    private double totalViolations;
    
    private final CheckManager checkManager;

    public PlayerData(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.checkManager = new CheckManager(AegisGuard.getInstance(), this);
    }

    public CheckManager getCheckManager() { return checkManager; }

    public Player getPlayer() { return player; }
    public UUID getUuid() { return uuid; }

    public void setLastLocation(Location loc) { this.lastLocation = loc; }
    public Location getLastLocation() { return lastLocation; }
    
    public void setMovement(double dx, double dy, double dz) {
        this.deltaX = dx;
        this.deltaY = dy;
        this.deltaZ = dz;
        this.horizontalDistance = Math.sqrt(dx * dx + dz * dz);
    }
    
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getDeltaZ() { return deltaZ; }
    public double getHorizontalDistance() { return horizontalDistance; }
    
    public void incrementAirTicks() { airTicks++; groundTicks = 0; }
    public void incrementGroundTicks() { groundTicks++; airTicks = 0; }
    public void incrementWaterTicks() { waterTicks++; }
    public void resetWaterTicks() { waterTicks = 0; }
    
    public int getAirTicks() { return airTicks; }
    public int getGroundTicks() { return groundTicks; }
    public int getWaterTicks() { return waterTicks; }
    
    public void setOnGround(boolean ground) { this.onGround = ground; }
    public boolean isOnGround() { return onGround; }
    
    public void setLastYVelocity(double vel) { this.lastYVelocity = vel; }
    public double getLastYVelocity() { return lastYVelocity; }
    
    public List<Long> getClickHistory() { return clickHistory; }
    
    public void setLastRotations(float yawD, float pitchD) {
        this.lastYawDelta = yawD;
        this.lastPitchDelta = pitchD;
    }
    public float getLastYawDelta() { return lastYawDelta; }
    public float getLastPitchDelta() { return lastPitchDelta; }
    
    public void setLastDamageTime(long time) { this.lastDamageTime = time; }
    public long getLastDamageTime() { return lastDamageTime; }
    
    public double getTotalViolations() { return totalViolations; }
    public void addViolations(double vl) { this.totalViolations += vl; }
}
