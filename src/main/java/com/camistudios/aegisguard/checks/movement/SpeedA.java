package com.camistudios.aegisguard.checks.movement;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class SpeedA extends Check {

    public SpeedA(AegisGuard plugin, PlayerData data) {
        super(plugin, data, "Speed");
    }

    @Override
    public void onMove(Location from, Location to) {
        if (!data.isOnGround()) return;

        double friction = 0.91;
        Material blockUnder = from.clone().subtract(0, 0.5, 0).getBlock().getType();
        
        if (blockUnder.name().contains("ICE")) {
            friction = 0.98;
        } else if (blockUnder == Material.SLIME_BLOCK) {
            friction = 0.8;
        }

        double maxLegalSpeed = (data.getPlayer().isSprinting() ? 0.28 : 0.22) / (1.0 - friction);
        
        if (data.getPlayer().hasPotionEffect(PotionEffectType.SPEED)) {
            maxLegalSpeed *= 1.0 + (0.2 * (data.getPlayer().getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1)); 
        }

        double dist = data.getHorizontalDistance();
        
        if (dist > maxLegalSpeed) {
            boolean blatant = dist >= maxLegalSpeed + 0.4;
            increaseBuffer();
            
            if (getBuffer() > 4 || blatant) {
                double weight = blatant ? 50.0 : 10.0;
                flagAndSetback(String.format("Excesiva Inercia | Dist: %.3f / Max: %.3f", dist, maxLegalSpeed), weight);
                resetBuffer();
            }
        } else {
            decreaseBuffer(1.0);
        }
    }
}
