package com.camistudios.aegisguard.data;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager implements Listener {

    private static final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public static PlayerData get(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
    }
    
    public static PlayerData get(UUID uuid) {
        return playerDataMap.get(uuid);
    }
    
    public static void remove(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        get(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }
}
