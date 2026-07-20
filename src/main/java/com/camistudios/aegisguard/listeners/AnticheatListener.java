package com.camistudios.aegisguard.listeners;

import com.camistudios.aegisguard.AegisGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AnticheatListener implements Listener {

    private final AegisGuard plugin;
    private final File logFile;

    public AnticheatListener(AegisGuard plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder() + File.separator + "logs", "anticheat_history.txt");
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo crear el archivo de logs.");
            }
        }
    }

    public void logViolation(String playerName, String hack, double weight) {
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pw.println("[" + time + "] " + playerName + " fue detectado por " + hack.toUpperCase() + " (VL: " + weight + ")");
        } catch (IOException e) {
            plugin.getLogger().warning("Error escribiendo en logs.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("aegisguard.alerts")) {
            event.getPlayer().sendMessage(plugin.getPrefix() + "§aEl motor AegisGuard V4 está activo en este servidor.");
        }
    }
}
