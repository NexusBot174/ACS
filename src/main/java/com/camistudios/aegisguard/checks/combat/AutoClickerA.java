package com.camistudios.aegisguard.checks.combat;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.checks.Check;
import com.camistudios.aegisguard.data.PlayerData;
import java.util.List;
import java.util.ArrayList;

public class AutoClickerA extends Check {

    public AutoClickerA(AegisGuard plugin, PlayerData data) {
        super(plugin, data, "AutoClicker");
    }

    @Override
    public void onArmSwing() {
        long now = System.currentTimeMillis();
        List<Long> clicks = data.getClickHistory();
        clicks.add(now);
        
        if (clicks.size() > 20) {
            clicks.remove(0);
        }
        
        if (clicks.size() == 20) {
            List<Long> delays = new ArrayList<>();
            for (int i = 1; i < clicks.size(); i++) {
                delays.add(clicks.get(i) - clicks.get(i - 1));
            }
            
            delays.removeIf(d -> d > 200);
            
            if (delays.size() > 10) {
                double mean = delays.stream().mapToLong(Long::longValue).average().orElse(0.0);
                double variance = delays.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0.0);
                double stdDev = Math.sqrt(variance);
                
                if (stdDev < 4.0) {
                    flag(String.format("Varianza Cero (Macro) | StdDev: %.2f", stdDev), 15.0);
                    clicks.clear();
                } else if (mean < 45.0) {
                    flag(String.format("CPS Humano Imposible | Promedio: %.1fms", mean), 8.0);
                    clicks.clear();
                }
            }
        }
    }
}
