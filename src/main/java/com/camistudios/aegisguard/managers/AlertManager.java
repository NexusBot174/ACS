package com.camistudios.aegisguard.managers;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.commands.AegisCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.text.Normalizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor Global de Alertas y Formateo Premium de AegisGuard.
 * Se encarga de procesar el nivel de confianza (Confidence) de los hacks,
 * decaimiento de alertas, formateo SmallCaps y construccion de la interfaz de chat en bloque.
 */
public class AlertManager {

    private final AegisGuard plugin;
    
    // Almacena el Nivel de Confianza o Violacion Ponderada (VL) de forma decimal
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>> PLAYER_VL_CONFIDENCE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> ALERT_COOLDOWNS = new ConcurrentHashMap<>();

    // Permiso requerido para recibir y visualizar el canal de alertas
    public static final String ALERT_PERMISSION = "aegisguard.chat.alert";

    public AlertManager(AegisGuard plugin) {
        this.plugin = plugin;
        startDecayTask();
    }

    /**
     * Tarea programada asincrona para reducir pasivamente el nivel de sospecha de los jugadores.
     * Si un jugador deja de usar hacks, su VL baja, evitando baneos falsos por acumulacion de lag tras horas de juego.
     */
    private void startDecayTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (UUID uuid : PLAYER_VL_CONFIDENCE.keySet()) {
                ConcurrentHashMap<String, Double> vls = PLAYER_VL_CONFIDENCE.get(uuid);
                if (vls != null) {
                    for (String hack : vls.keySet()) {
                        double current = vls.get(hack);
                        if (current > 0) {
                            // Decaimiento pasivo
                            vls.put(hack, Math.max(0.0, current - 0.25));
                        }
                    }
                }
            }
        }, 200L, 200L); // Ejecuta cada 10 segundos
    }

    /**
     * Limpia un texto eliminando tildes/acentos y lo convierte a Small Caps (Mayusculas Pequeñas).
     */
    public static String toSmallCapsClean(String input) {
        if (input == null) return "";
        
        // 1. Normalizar y eliminar acentos (tildes) de manera estricta
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // 2. Mapeo a Small Caps reales
        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String small  = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
        
        StringBuilder builder = new StringBuilder(noAccents.length());
        for (char c : noAccents.toCharArray()) {
            int index = normal.indexOf(c);
            if (index != -1) {
                builder.append(small.charAt(index));
            } else {
                builder.append(c); // Mantiene numeros y simbolos
            }
        }
        return builder.toString();
    }

    /**
     * Registra una violacion con peso ponderado (Confidence System).
     * @return El VL actual resultante tras sumar el peso.
     */
    public double logViolationWeight(Player player, String hackType, double weight) {
        if (player == null || hackType == null) return 0.0;
        
        ConcurrentHashMap<String, Double> vls = PLAYER_VL_CONFIDENCE.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        return vls.merge(hackType.toLowerCase(), weight, Double::sum);
    }

    private String normalizeHackName(String hackType) {
        if (hackType == null) return "Unknown";
        
        return switch (hackType.toLowerCase().trim()) {
            case "killaura" -> "KillAura";
            case "criticals" -> "Criticals";
            case "jesus" -> "Jesus";
            case "speed" -> "Speed";
            case "fly" -> "Fly";
            case "fastbow" -> "FastBow";
            case "timer" -> "Timer";
            case "antiknockback" -> "AntiKnockback";
            case "noslowdown" -> "NoSlowdown";
            case "aimbot" -> "Aimbot";
            case "autoclicker" -> "AutoClicker";
            case "hitbox" -> "HitBox";
            case "spider" -> "Spider";
            case "step" -> "Step";
            case "nofall" -> "NoFallSpoof";
            default -> hackType.substring(0, 1).toUpperCase() + hackType.substring(1).toLowerCase();
        };
    }

    /**
     * Envía una alerta interactiva premium con bloque multi-linea estetico.
     */
    public void sendAlert(Player player, String hackType, double vl, String debugData) {
        if (player == null || hackType == null) return;
        
        // Comprobar si el hack está deshabilitado en configuracion o settings dinamicos (ver AegisCommand.java)
        if (!plugin.getConfig().getBoolean("checks." + hackType.toLowerCase() + ".enabled", true)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String hackKey = hackType.toLowerCase().trim();
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("alerts.cooldown-ms", 1500L); // Evita spam excesivo visualmente

        ConcurrentHashMap<String, Long> cooldowns = ALERT_COOLDOWNS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        long lastAlert = cooldowns.getOrDefault(hackKey, 0L);
        if (now - lastAlert < cooldownMs) {
            return; 
        }
        cooldowns.put(hackKey, now);

        String cleanName = normalizeHackName(hackType);
        
        // Estructura visual Multi-Linea Premium
        int vlDisplay = (int) Math.round(vl);
        
        String colorTheme;
        if (vlDisplay >= 20) colorTheme = "&4";
        else if (vlDisplay >= 10) colorTheme = "&c";
        else colorTheme = "&b";

        // Diseñando la UI del chat copiando la estetica de la imagen provista (símbolos y estructuracion)
        String header = "&8[&3&lAEGIS&8] " + colorTheme + "&l⚠ " + toSmallCapsClean("alerta de seguridad") + " &8[" + colorTheme + vlDisplay + "&8]";
        String linePlayer = "&8 👤 &f" + toSmallCapsClean("jugador") + ": &b" + player.getName();
        String lineReason = "&8 ✏ &f" + toSmallCapsClean("razon") + ": &c" + cleanName + " &7(" + (debugData != null ? debugData : "General") + ")";
        String lineStaff  = "&8 ⛏ &f" + toSmallCapsClean("staff") + ": &6" + toSmallCapsClean("aegis auto");
        String lineDurat  = "&8 🕒 &f" + toSmallCapsClean("confianza") + ": " + colorTheme + vlDisplay + "%";
        
        String fullAlertString = header + "\n" + linePlayer + "\n" + lineReason + "\n" + lineStaff + "\n" + lineDurat;
        
        // Data Hover para el header interactivo
        String hoverMenu = 
            "&3&l📊 " + toSmallCapsClean("telemetria avanzada aegisguard") + "\n" +
            "&8&m----------------------------------------\n" +
            "&7👤 " + toSmallCapsClean("sospechoso") + ": &e" + player.getName() + "\n" +
            "&7📶 " + toSmallCapsClean("latencia") + ": &a" + player.getPing() + "ms\n" +
            "&7🌍 " + toSmallCapsClean("ubicacion") + ": &7" + player.getWorld().getName() + " (" + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ() + ")\n" +
            "&8&m----------------------------------------\n" +
            "&a✨ " + toSmallCapsClean("usa las opciones interactivas para moderar");

        Component messageBlock = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.colorize(fullAlertString))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.colorize(hoverMenu))));

        // --------------------------------------------------------------------
        // CONSTRUCCION DEL PANEL DE ACCIONES INTERACTIVAS (BOTONES)
        // --------------------------------------------------------------------
        Component actionSpacer = Component.text("   ", NamedTextColor.DARK_GRAY);

        Component btnView = Component.text(" [" + toSmallCapsClean("ver") + "] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("§7Teletransportarse hacia §b" + player.getName())))
                .clickEvent(ClickEvent.runCommand("/tp " + player.getName()));

        Component btnSancionar = Component.text(" [" + toSmallCapsClean("sancionar") + "] ", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("§7Abrir menu de castigo para §c" + player.getName())))
                .clickEvent(ClickEvent.suggestCommand("/ban " + player.getName() + " AegisGuard: Hacks (" + cleanName + ")"));

        Component btnChequear = Component.text(" [" + toSmallCapsClean("chequear") + "] ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("§7Congelar temporalmente a §e" + player.getName())))
                .clickEvent(ClickEvent.runCommand("/ag freeze " + player.getName()));

        TextComponent actionPanel = Component.text("    ") 
                .append(btnView)
                .append(actionSpacer)
                .append(btnSancionar)
                .append(actionSpacer)
                .append(btnChequear);

        // Despacho Local
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.isOp() || target.hasPermission("aegisguard.admin") || target.hasPermission(ALERT_PERMISSION)) {
                target.sendMessage("");
                target.sendMessage(messageBlock);
                target.sendMessage(actionPanel);
                target.sendMessage("");
            }
        }

        // Registro en archivo local
        if (plugin.getAnticheatListener() != null) {
            plugin.getAnticheatListener().logViolation(player.getName(), cleanName, vlDisplay);
        }

        // Enviar la alerta al webhook de Discord con informacion extra de TPS y rol ping (EMBED)
        if (AegisCommand.getInstance() != null) {
            String rolePing = plugin.getConfig().getString("discord.staff-role-id", "");
            String mentionStr = "";
            if (vlDisplay >= 15 && rolePing != null && !rolePing.isEmpty() && !rolePing.equals("ROLE_ID_AQUI")) {
                mentionStr = "<@&" + rolePing + ">";
            }
            
            String color = vlDisplay >= 20 ? "16711680" : (vlDisplay >= 10 ? "16753920" : "4444928");
            
            String jsonEmbed = "{"
                    + "\"content\": \"" + mentionStr + "\","
                    + "\"embeds\": [{"
                    + "\"title\": \"\u26A0\uFE0F Alerta de Seguridad\","
                    + "\"color\": " + color + ","
                    + "\"thumbnail\": {\"url\": \"https://crafatar.com/avatars/" + player.getUniqueId() + "?overlay=1\"},"
                    + "\"fields\": ["
                    + "{\"name\": \"\uD83D\uDC64 Jugador\", \"value\": \"`" + player.getName() + "`\", \"inline\": true},"
                    + "{\"name\": \"\uD83D\uDCCA Confianza\", \"value\": \"`" + vlDisplay + "%`\", \"inline\": true},"
                    + "{\"name\": \"\u2694\uFE0F Módulo\", \"value\": \"`" + cleanName + "`\", \"inline\": true},"
                    + "{\"name\": \"\uD83D\uDD0D Detalles\", \"value\": \"`" + (debugData != null ? debugData : "N/A") + "`\", \"inline\": false},"
                    + "{\"name\": \"\uD83C\uDF10 Ping / Mundo\", \"value\": \"`" + player.getPing() + "ms` en `" + player.getWorld().getName() + "`\", \"inline\": false}"
                    + "],"
                    + "\"footer\": {\"text\": \"AegisGuard V4 Enterprise \u2022 Aegis Auto-Mod\"}"
                    + "}]"
                    + "}";
            
            AegisCommand.getInstance().sendDiscordEmbed(jsonEmbed);
        }
    }

    public static void clearPlayerData(UUID uuid) {
        if (uuid == null) return;
        PLAYER_VL_CONFIDENCE.remove(uuid);
        ALERT_COOLDOWNS.remove(uuid);
    }
}