package com.camistudios.aegisguard.checks;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.data.PlayerData;

import com.camistudios.aegisguard.checks.movement.SpeedA;
import com.camistudios.aegisguard.checks.movement.FlyA;
import com.camistudios.aegisguard.checks.movement.StepA;
import com.camistudios.aegisguard.checks.combat.AutoClickerA;
import com.camistudios.aegisguard.checks.combat.KillauraA;
import com.camistudios.aegisguard.checks.combat.AimbotA;

import java.util.ArrayList;
import java.util.List;

public class CheckManager {

    private final List<Check> checks = new ArrayList<>();

    public CheckManager(AegisGuard plugin, PlayerData data) {
        // Register Movement Checks
        checks.add(new SpeedA(plugin, data));
        checks.add(new FlyA(plugin, data));
        checks.add(new StepA(plugin, data));
        
        // Register Combat Checks
        checks.add(new AutoClickerA(plugin, data));
        checks.add(new KillauraA(plugin, data));
        checks.add(new AimbotA(plugin, data));
    }

    public List<Check> getChecks() {
        return checks;
    }
}
