package com.camistudios.aegisguard;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.camistudios.aegisguard.commands.AegisCommand;
import com.camistudios.aegisguard.listeners.MovementListener;
import com.camistudios.aegisguard.listeners.CombatListener;
import com.camistudios.aegisguard.managers.AlertManager;
import com.camistudios.aegisguard.managers.PunishmentManager;

public final class AegisGuard extends JavaPlugin {
    
    private static AegisGuard instance;
    private static final Pattern HEX_PATTERN = Pattern.compile("(?:&#|<#)([A-Fa-f0-9]{6})>?");
    
    private String prefix;
    private AlertManager alertManager;
    private PunishmentManager punishmentManager;
    private FileConfiguration messagesConfig;
    private com.camistudios.aegisguard.listeners.AnticheatListener anticheatListener;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inicializacion y carga de archivos
        saveDefaultConfig();
        loadMessages();
        
        File logsDir = new File(getDataFolder(), "logs");
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            getLogger().severe("Fallo critico al inicializar la estructura fisica de almacenamiento de logs.");
        }

        // Carga de los Sub-Modulos del Nucleo Central
        this.alertManager = new AlertManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.anticheatListener = new com.camistudios.aegisguard.listeners.AnticheatListener(this);

        // Registro de Interceptores y Comandos
        AegisCommand cmd = new AegisCommand(this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.camistudios.aegisguard.data.PlayerDataManager(), this);
        getServer().getPluginManager().registerEvents(new com.camistudios.aegisguard.listeners.MenuListener(), this);
        getServer().getPluginManager().registerEvents(anticheatListener, this);
        getServer().getPluginManager().registerEvents(cmd, this);

        // Registro de canal para escaneo de marcas de cliente (Se gestiona en AegisCommand.java)

        if (getCommand("aegisguard") != null) {
            getCommand("aegisguard").setExecutor(cmd);
            getCommand("aegisguard").setTabCompleter(cmd);
        }

        printPremiumBanner();
    }

    @Override
    public void onDisable() {
        String c1 = "§b";
        String c4 = "§c";
        String gray = "§7";
        
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(c4 + "  [⚡] " + toSmallCaps("desactivando el motor de seguridad") + "...");
        Bukkit.getConsoleSender().sendMessage(gray + "   ▪ " + toSmallCaps("vaciando cache de hilos concurrentes"));
        Bukkit.getConsoleSender().sendMessage(gray + "   ▪ " + toSmallCaps("desvinculando interceptores de movimiento e inercia"));
        Bukkit.getConsoleSender().sendMessage(gray + "   ▪ " + toSmallCaps("cerrando almacenamiento log de telemetria"));
        Bukkit.getConsoleSender().sendMessage(c1 + "  [✔] " + toSmallCaps("aegisguard core se ha descargado de la memoria de forma segura") + ".");
        Bukkit.getConsoleSender().sendMessage("");
        
        instance = null;
    }

    public void loadMessages() {
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (!msgFile.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(msgFile);
        this.prefix = colorize(getConfig().getString("messages.prefix", "<#00D2FF>&lAegisGuard <#808080>» &r"));
    }

    public FileConfiguration getMessagesConfig() { return messagesConfig; }

    public void reloadPlugin() {
        reloadConfig();
        loadMessages();
    }

    @SuppressWarnings("deprecation")
    public String colorize(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("§x");
            for (char ch : matcher.group(1).toCharArray()) {
                replacement.append('§').append(ch);
            }
            matcher.appendReplacement(builder, replacement.toString());
        }
        matcher.appendTail(builder);
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    private String toSmallCaps(String input) {
        if (input == null) return "";
        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String small  = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
        StringBuilder builder = new StringBuilder();
        for (char c : input.toCharArray()) {
            int index = normal.indexOf(c);
            builder.append(index != -1 ? small.charAt(index) : c);
        }
        return builder.toString();
    }

    private void printPremiumBanner() {
        String c1 = "§b";
        String c2 = "§3";
        String c3 = "§9";
        String gray = "§7";
        String green = "§a";

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(c1 + "    ___            _       ______                  __");
        Bukkit.getConsoleSender().sendMessage(c1 + "   /   |  ___ ____ (_)____ / ____/__  ____ ______ ____/ /");
        Bukkit.getConsoleSender().sendMessage(c2 + "  / /| | / _ \\/ __ `/ / ___// / __ / / / / __ `/ ___/ __  / ");
        Bukkit.getConsoleSender().sendMessage(c2 + " / ___ |/  __/ /_/ / (__  )/ /_/ / /_/ / /_/ / /  / /_/ /  ");
        Bukkit.getConsoleSender().sendMessage(c3 + "/_/  |_|\\___/\\__, /_/____/ \\____/\\__,_/\\__,_/_/   \\__,_/   ");
        Bukkit.getConsoleSender().sendMessage(c3 + "            /____/                                         ");
        Bukkit.getConsoleSender().sendMessage(gray + "   » " + c1 + toSmallCaps("aegisguard anticheat engine") + " " + gray + "|" + c2 + " " + toSmallCaps("version") + ": " + getDescription().getVersion() + " " + gray + "«");
        Bukkit.getConsoleSender().sendMessage("");
        
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        Bukkit.getConsoleSender().sendMessage(c1 + "  [📊] " + toSmallCaps("telemetria del entorno de ejecucion") + ":");
        Bukkit.getConsoleSender().sendMessage(gray + "       ▪ " + toSmallCaps("nucleo de software") + ": " + c2 + Bukkit.getName() + " (Ver: " + Bukkit.getBukkitVersion() + ")");
        Bukkit.getConsoleSender().sendMessage(gray + "       ▪ " + toSmallCaps("asignacion de cpu") + ": " + c2 + availableProcessors + " " + toSmallCaps("hilos nucleos logicos"));
        
        Bukkit.getConsoleSender().sendMessage(c1 + "  [🔗] " + toSmallCaps("modulo de integracion de red y canales") + ":");
        Bukkit.getConsoleSender().sendMessage(gray + "       ▪ " + toSmallCaps("gestor de permisos interactivos") + ": " + (Bukkit.getPluginManager().isPluginEnabled("LuckPerms") ? green + toSmallCaps("luckperms verificado") : "§e" + toSmallCaps("superperms nativo")));
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(green + "  [✔] " + toSmallCaps("el motor core se ha acoplado e iniciado sin errores de hilos") + ".");
        Bukkit.getConsoleSender().sendMessage("");
    }

    public static AegisGuard getInstance() { return instance; }
    public String getPrefix() { return prefix; }
    public AlertManager getAlertManager() { return alertManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public com.camistudios.aegisguard.listeners.AnticheatListener getAnticheatListener() { return anticheatListener; }
}