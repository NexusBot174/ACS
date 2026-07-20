package com.camistudios.aegisguard.checks.movement;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffectType;

public class StepA extends Check {

    public StepA(AegisGuard plugin, PlayerData data) {
        super(plugin, data, "Step");
    }

    @Override
    public void onMove(Location from, Location to) {
        if (data.getLastDamageTime() > 0) return;
        
        double deltaY = data.getDeltaY();
        if (deltaY >= 1.0) { 
            Block atFeet = from.getBlock();
            Block inFront = to.getBlock();
            String nameAtFeet = atFeet.getType().name();
            String nameInFront = inFront.getType().name();
            
            boolean hasPistonsOrBeds = nameAtFeet.contains("BED") || nameAtFeet.contains("SLIME") || data.getPlayer().hasPotionEffect(PotionEffectType.JUMP_BOOST);
            boolean isStairs = nameInFront.contains("STAIRS") || nameInFront.contains("SLAB") || nameAtFeet.contains("STAIRS") || nameAtFeet.contains("SLAB");
            
            if (!hasPistonsOrBeds && !isStairs) {
                increaseBuffer();
                if (getBuffer() > 2) {
                    flagAndSetback(String.format("Subida Instantánea | dY: %.2f", deltaY), 2.0);
                    resetBuffer();
                }
            }
        } else {
            decreaseBuffer(1.0);
        }
    }
}
