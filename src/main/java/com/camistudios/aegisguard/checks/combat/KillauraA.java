package com.camistudios.aegisguard.checks.combat;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class KillauraA extends Check {

    public KillauraA(AegisGuard plugin, PlayerData data) {
        super(plugin, data, "KillAura");
    }

    @Override
    public void onCombat(Player victim) {
        Location attLoc = data.getPlayer().getEyeLocation();
        Location vicLoc = victim.getLocation().clone().add(0, victim.getHeight() / 2.0, 0);
        
        double distance = attLoc.distance(vicLoc);
        
        org.bukkit.util.BoundingBox box = victim.getBoundingBox().clone().expand(0.5);
        org.bukkit.util.RayTraceResult result = box.rayTrace(attLoc.toVector(), attLoc.getDirection(), 6.0);
        
        if (result == null && distance > 2.5) {
            increaseBuffer();
            if (getBuffer() > 3) {
                flag(String.format("Ataque Ciego Vectorial | Dist: %.1f", distance), 12.0);
                resetBuffer();
            }
        } else {
            decreaseBuffer(1.0);
            
            double maxLegalDistance = 3.65;
            Vector attVel = data.getPlayer().getVelocity();
            Vector vicVel = victim.getVelocity();
            double speedDiff = Math.abs(attVel.length()) + Math.abs(vicVel.length());
            
            if (speedDiff > 0.2) {
                maxLegalDistance += (speedDiff * 1.5);
            }

            if (distance > maxLegalDistance) {
                increaseBuffer();
                if (getBuffer() > 2) {
                    double weight = distance > 5.0 ? 30.0 : 15.0;
                    flag(String.format("Reach (AABB): %.2fm", distance), weight);
                    resetBuffer();
                }
            } else {
                decreaseBuffer(1.0);
            }
        }
    }
}
