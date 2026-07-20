package com.camistudios.aegisguard.managers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.commands.AegisCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Gestor avanzado de Castigos de AegisGuard.
 * Intercepta el nivel de confianza (Confidence %) y determina la accion a tomar.
 * Formatea y distribuye las alertas globales usando SmallCaps y logeo fisico.
 */
public class PunishmentManager {

    private final AegisGuard plugin;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Contador global para telemetria y monitoreo en vivo del motor
    private static final AtomicInteger TOTAL_BANS_EXECUTED = new AtomicInteger(0);

    public PunishmentManager(AegisGuard plugin) {
        this.plugin = plugin;
    }

    /**
     * Verifica si el nivel de confianza (% VL Ponderado) cruzó el umbral de baneo.
     * Soporta acciones intermedias como advertencias o kicks basados en el porcentaje.
     */
    public void checkThreshold(Player player, String hackType, double currentConfidence) {
        if (player == null || hackType == null) return;

        // Umbral base de confianza requerida para banear (100% es auto-ban)
        String configPath = "checks." + hackType.toLowerCase() + ".ban-threshold";
        double banThreshold = plugin.getConfig().getDouble(configPath, 100.0);

        // Sistema Progresivo
        if (currentConfidence >= banThreshold) {
            executeBan(player, hackType, currentConfidence);
        } else if (currentConfidence >= banThreshold * 0.85) {
            // Kick Preventivo si esta por encima del 85% de confianza para evitar daños inminentes (opcional)
            boolean autoKick = plugin.getConfig().getBoolean("punishments.preventive-kick", false);
            if (autoKick) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.kick(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.colorize("&c[AegisGuard]\n&7Fuiste expulsado por detección anómala de red.\n&eContacta al soporte si crees que es un error.")));
                });
            }
        }
    }

    /**
     * Ejecuta el baneo detectando automaticamente el sistema de sanciones disponible.
     */
    private void executeBan(Player player, String hackType, double confidence) {
        String path = "checks." + hackType.toLowerCase() + ".ban-command";
        
        String systemUsed;
        String defaultCmd;
        
        // Deteccion avanzada del ecosistema de gestion de sanciones de la network
        if (Bukkit.getPluginManager().isPluginEnabled("LiteBans")) {
            defaultCmd = "litebans:ban %player% AegisGuard: Modulo prohibido (%check%).";
            systemUsed = "LiteBans Database Engine";
        } else if (Bukkit.getPluginManager().isPluginEnabled("AdvancedBan")) {
            defaultCmd = "ban %player% AegisGuard: Modulo prohibido (%check%) -s";
            systemUsed = "AdvancedBan Core Network";
        } else if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
            defaultCmd = "essentials:ban %player% AegisGuard: Modulo prohibido (%check%).";
            systemUsed = "EssentialsX Core Sync";
        } else {
            defaultCmd = "ban %player% AegisGuard: Modulo prohibido (%check%).";
            systemUsed = "Bukkit Vanilla System";
        }
        
        String rawCmd = plugin.getConfig().getString(path, defaultCmd);
        if (rawCmd == null || rawCmd.isEmpty()) rawCmd = defaultCmd;

        String cleanHackName = hackType.toUpperCase().trim();

        String formattedCmd = rawCmd
                .replace("%player%", player.getName())
                .replace("%check%", cleanHackName);

        if (formattedCmd.startsWith("/")) {
            formattedCmd = formattedCmd.substring(1);
        }

        final String finalCmd = formattedCmd;

        // Corte de memoria de violaciones inmediato para mitigar overflows
        AlertManager.clearPlayerData(player.getUniqueId());
        
        // Incremento atomico del registro de actividad del motor
        int currentTotalBans = TOTAL_BANS_EXECUTED.incrementAndGet();

        // Despachar la notificacion avanzada al Staff con la nueva estética
        sendAdvancedBanNotification(player, cleanHackName, systemUsed, currentTotalBans, confidence);

        // Despacho seguro del comando respetando el hilo principal de Bukkit
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
        }

        logToFileAsync(player, cleanHackName, confidence);
    }

    /**
     * Envia una notificacion interactiva premium multi-línea con la estética oficial.
     */
    private void sendAdvancedBanNotification(Player suspect, String hackType, String systemUsed, int globalBanCount, double confidence) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String playerName = suspect.getName();
        int ping = suspect.getPing();
        String worldName = suspect.getWorld().getName();
        
        // Formato Multi-linea para Sanciones Críticas basado en la interfaz Premium (Sin tildes y Small Caps)
        String header = "&8[&4&lAEGIS&8] &4&l⚠ " + AlertManager.toSmallCapsClean("suspension definitiva") + " &8[B-" + globalBanCount + "]";
        String linePlayer = "&8 👤 &f" + AlertManager.toSmallCapsClean("jugador") + ": &c" + playerName;
        String lineReason = "&8 ✏ &f" + AlertManager.toSmallCapsClean("razon") + ": &4" + hackType + " &8(&c" + Math.round(confidence) + "% Confianza&8)";
        String lineStaff  = "&8 ⛏ &f" + AlertManager.toSmallCapsClean("staff") + ": &6" + AlertManager.toSmallCapsClean("aegis auto");
        String lineDurat  = "&8 🕒 &f" + AlertManager.toSmallCapsClean("duracion") + ": &cPermanente";
        
        String blockAlert = header + "\n" + linePlayer + "\n" + lineReason + "\n" + lineStaff + "\n" + lineDurat;
        
        // Construccion del Menu de Datos exclusivo para Staff
        String hoverMenu = 
            "&4&l📊 " + AlertManager.toSmallCapsClean("aegisguard panel de ejecucion") + "\n" +
            "&8&m----------------------------------------\n" +
            "&7👤 " + AlertManager.toSmallCapsClean("usuario eliminado") + ": &e" + playerName + "\n" +
            "&7⚔️ " + AlertManager.toSmallCapsClean("modulo detectado") + ": &c" + hackType + "\n" +
            "&7📶 " + AlertManager.toSmallCapsClean("ping del usuario") + ": &a" + ping + "ms\n" +
            "&7🌍 " + AlertManager.toSmallCapsClean("mundo de ejecucion") + ": &f" + worldName + "\n" +
            "&7📅 " + AlertManager.toSmallCapsClean("registro temporal") + ": &7" + timestamp + "\n" +
            "&7🛡️ " + AlertManager.toSmallCapsClean("origen del baneo") + ": &4&lCONSOLE (Auto-Ban)\n" +
            "&7🔌 " + AlertManager.toSmallCapsClean("controlador de red") + ": &b" + systemUsed + "\n" +
            "&8&m----------------------------------------\n" +
            "&7📈 " + AlertManager.toSmallCapsClean("baneos globales hoy") + ": &f" + globalBanCount + "\n" +
            "&8&m----------------------------------------\n" +
            "&a✨ " + AlertManager.toSmallCapsClean("accion irreversible generada por el motor");

        Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.colorize(blockAlert));
        Component hoverComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.colorize(hoverMenu));
        
        Component finalNotification = messageComponent.hoverEvent(HoverEvent.showText(hoverComponent));

        // Enviar exclusivamente a administradores u OPs
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.isOp() || staff.hasPermission("aegisguard.admin") || staff.hasPermission(AlertManager.ALERT_PERMISSION)) {
                staff.sendMessage("");
                staff.sendMessage(finalNotification);
                staff.sendMessage("");
            }
        }
        
        // Despachar a Discord
        if (AegisCommand.getInstance() != null) {
            String rolePing = plugin.getConfig().getString("discord.staff-role-id", "");
            String mentionStr = "";
            if (rolePing != null && !rolePing.isEmpty() && !rolePing.equals("ROLE_ID_AQUI")) {
                mentionStr = "<@&" + rolePing + "> ";
            }
            
            String discordMsg = mentionStr + "🚨 **[BAN EJECUTADO]**";
            discordMsg += "\n> 👤 **Jugador Baneado:** `" + playerName + "`";
            discordMsg += "\n> ✏️ **Razón:** `" + hackType + "` *(Confianza: " + Math.round(confidence) + "%)*";
            discordMsg += "\n> 🕒 **Duración:** `Permanente`";
            
            AegisCommand.getInstance().sendDiscordAlert(discordMsg);
        }
    }

    /**
     * Registra el baneo en los archivos locales de forma asincrona para evitar picos de lag.
     */
    private void logToFileAsync(Player player, String hack, double confidence) {
        final String playerName = player.getName();
        final String playerUuid = player.getUniqueId().toString();
        final String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File logsFolder = new File(plugin.getDataFolder(), "logs");
            if (!logsFolder.exists() && !logsFolder.mkdirs()) {
                return;
            }

            File file = new File(logsFolder, playerUuid + ".txt");
            try (FileWriter fw = new FileWriter(file, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                 
                out.println("[" + timestamp + "] BAN EJECUTADO -> Hack: " + hack + " | Usuario: " + playerName + " | Confianza: " + String.format("%.2f", confidence) + "%");
            } catch (Exception e) {
                plugin.getLogger().warning("Error al guardar registro fisico de baneo para: " + playerName);
            }
        });
    }

    public static int getTotalBansExecuted() {
        return TOTAL_BANS_EXECUTED.get();
    }
}