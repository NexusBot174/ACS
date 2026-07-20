package com.camistudios.aegisguard.checks.combat;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import org.bukkit.Location;

public class AimbotA extends Check {

    public AimbotA(AegisGuard plugin, PlayerData data) {
        super(plugin, data, "Aimbot");
    }

    @Override
    public void onMove(Location from, Location to) {
        float yawDelta = Math.abs(to.getYaw() - from.getYaw());
        float pitchDelta = Math.abs(to.getPitch() - from.getPitch());
        
        if (Math.abs(to.getPitch()) > 90.1f) {
            flagAndSetback("Rotación Ilegal: " + to.getPitch() + " (DerpHack)", 50.0);
        }
        
        float lastYawD = data.getLastYawDelta();
        float lastPitchD = data.getLastPitchDelta();
        
        if (yawDelta > 1.0f && yawDelta == lastYawD && pitchDelta > 1.0f && pitchDelta == lastPitchD) {
            increaseBuffer();
            if (getBuffer() > 4.0) {
                flag("Velocidad Angular Constante (Script)", 8.0);
                resetBuffer();
            }
        } else {
            decreaseBuffer(0.25);
        }
        
        data.setLastRotations(yawDelta, pitchDelta);
    }
}
