package com.camistudios.aegisguard.checks.movement;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class FlyA extends Check {

    public FlyA(AegisGuard plugin, PlayerData data) {
        super(plugin, data, "Fly");
    }

    @Override
    public void onMove(Location from, Location to) {
        if (data.isOnGround() || data.getPlayer().isFlying() || data.getLastDamageTime() > 0) {
            return;
        }
        
        Block blockAtFeet = to.getBlock();
        Block blockBelow = to.clone().subtract(0, 0.1, 0).getBlock();
        
        boolean inOrOnWater = blockAtFeet.isLiquid() || blockBelow.isLiquid();
        boolean hasSolidNearby = false;
        org.bukkit.util.BoundingBox playerBox = data.getPlayer().getBoundingBox().clone().expand(0.3);
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 0; y++) {
                    Block b = to.clone().add(x, y, z).getBlock();
                    if (b.getType().isSolid() || b.getType().name().contains("LILY") || b.getType().name().contains("CARPET") || b.getType().name().contains("SLAB")) {
                        if (b.getBoundingBox().overlaps(playerBox)) {
                            hasSolidNearby = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!inOrOnWater && !hasSolidNearby) {
            double expectedYVelocity = (data.getLastYVelocity() - 0.08) * 0.98;
            
            if (data.getAirTicks() > 6 && expectedYVelocity < 0.0) {
                double diff = Math.abs(data.getDeltaY() - expectedYVelocity);
                
                if (diff > 0.05 && data.getDeltaY() >= 0.0) {
                    flagAndSetback(String.format("Predicción de Gravedad Errónea | dY: %.4f | Exp: %.4f", data.getDeltaY(), expectedYVelocity), 15.0);
                }
            }
        }
    }
}
