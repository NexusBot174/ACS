package com.camistudios.aegisguard.commands;

import com.camistudios.aegisguard.AegisGuard;
import com.camistudios.aegisguard.managers.AlertManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comando Principal e Interfaz Administrativa de AegisGuard V2.
 * Maneja configuraciones via GUI, webhooks de discord avanzados, 
 * unverify, y telemetria de clientes entrantes.
 */
public class AegisCommand implements CommandExecutor, TabCompleter, Listener, PluginMessageListener {

    private final AegisGuard plugin;
    private static AegisCommand instance;
    private final String prefix = "§8[§3§lAEGIS§8] §r";
    private static final ConcurrentHashMap<UUID, String> playerClientBrands = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();

    public AegisCommand(AegisGuard plugin) {
        this.plugin = plugin;
        instance = this;
        // Registrar canales Bungee/Forge para interceptar clientes (Solo 1.13+ NamespacedKeys)
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static AegisCommand getInstance() {
        return instance;
    }

    // ------------------------------------------------------------------------
    // SISTEMA DE DETECCION DE CLIENTES (BRAND DETECTION)
    // ------------------------------------------------------------------------
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("minecraft:brand")) {
            try {
                String brand = new String(message, StandardCharsets.UTF_8).trim();
                // Limpiar basura del buffer de bytes si existe
                brand = brand.replaceAll("[^a-zA-Z0-9_ -]", "");
                playerClientBrands.put(player.getUniqueId(), brand);
                
                // Alertar a Discord secretamente si es un cliente modificado (ej. Lunar, Forge, Fabric, Cheat)
                if (!brand.toLowerCase().contains("vanilla") && !brand.isEmpty()) {
                    sendDiscordAlert("🌐 **Telemetría de Ingreso:** El jugador `" + player.getName() + "` se conectó usando el cliente/modpack: `" + brand + "`");
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Enviar peticion al cliente para que revele su marca
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!playerClientBrands.containsKey(p.getUniqueId())) {
                playerClientBrands.put(p.getUniqueId(), "Vanilla/Unknown");
            }
        }, 40L);
    }

    public static String getClientBrand(Player player) {
        return playerClientBrands.getOrDefault(player.getUniqueId(), "Desconocido");
    }

    // ------------------------------------------------------------------------
    // COMANDOS PRINCIPALES
    // ------------------------------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aegisguard.admin")) {
            sender.sendMessage(prefix + "§cNo tienes permisos para administrar el anticheat.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player player) {
                com.camistudios.aegisguard.listeners.MenuListener.openMainMenu(player);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "verify":
                handleVerify(sender, args);
                break;
            case "unverify":
                handleUnverify(sender);
                break;
            case "settings":
                handleSettings(sender, args);
                break;
            case "freeze":
                handleFreeze(sender, args);
                break;
            case "spy":
                handleSpy(sender);
                break;
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(prefix + "§aConfiguración recargada con éxito.");
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§eComandos de §bAegisGuard V4§e:");
        sender.sendMessage("§8» §e/ag alerts §7- Alterna las alertas de detección.");
        sender.sendMessage("§8» §e/ag reload §7- Recarga todos los archivos.");
        sender.sendMessage("§8» §e/ag menu §7- Abre la interfaz de control.");
        sender.sendMessage("§8» §e/ag unverify §7- Desvincula el bot de Discord.");
        sender.sendMessage("§8» §e/ag settings §7- Configura Discord.");
        sender.sendMessage("§8» §b/ag freeze <jugador> §7- Congela un sospechoso.");
        sender.sendMessage("§8» §b/ag spy §7- Alterna alertas en el chat.");
        sender.sendMessage("§8» §b/ag reload §7- Recarga la configuración.");
        sender.sendMessage("§8§m----------------------------------------");
    }

    // ------------------------------------------------------------------------
    // DISCORD WEBHOOKS
    // ------------------------------------------------------------------------
    private void handleVerify(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUso correcto: /ag verify <URL_DEL_WEBHOOK>");
            return;
        }

        String urlString = args[1];
        if (!urlString.startsWith("https://discord.com/api/webhooks/")) {
            sender.sendMessage(prefix + "§cEsa URL no parece ser un Webhook válido de Discord.");
            return;
        }

        sender.sendMessage(prefix + "§eIniciando protocolo de validación y handshake...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isValid = validateWebhook(urlString);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isValid) {
                    plugin.getConfig().set("discord.webhook-url", urlString);
                    plugin.saveConfig();
                    
                    sender.sendMessage(prefix + "§a¡Vinculación Exitosa! El webhook ha sido validado.");
                    sender.sendMessage(prefix + "§7Todas las alertas y telemetría irán a tu servidor de Discord.");
                    
                    sendDiscordAlert("✅ **AegisGuard Premium ha sido vinculado exitosamente a este canal.**\n> Autorizado por: `" + sender.getName() + "`");
                } else {
                    sender.sendMessage(prefix + "§cError 401: Discord rechazó la conexión. ¿Borraste el webhook o la URL está rota?");
                }
            });
        });
    }

    private void handleUnverify(CommandSender sender) {
        String currentUrl = plugin.getConfig().getString("discord.webhook-url");
        if (currentUrl == null || currentUrl.isEmpty() || currentUrl.equals("URL_AQUI")) {
            sender.sendMessage(prefix + "§cEl servidor no tiene ningún webhook vinculado.");
            return;
        }
        
        sendDiscordAlert("🛑 **AegisGuard ha sido DESVINCULADO de este canal por el administrador: `" + sender.getName() + "`**");
        
        plugin.getConfig().set("discord.webhook-url", "URL_AQUI");
        plugin.saveConfig();
        
        sender.sendMessage(prefix + "§aDesvinculación exitosa. AegisGuard ya no enviará datos a Discord.");
    }

    private boolean validateWebhook(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AegisGuard-Engine");
            connection.setConnectTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void sendDiscordAlert(String message) {
        String urlString = plugin.getConfig().getString("discord.webhook-url");
        if (urlString == null || urlString.isEmpty() || urlString.equals("URL_AQUI")) {
            return; // Discord desactivado
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "AegisGuard-Engine");
                connection.setDoOutput(true);

                // Payload basico que soporta Markdown.
                String jsonPayload = "{\"content\": \"" + message.replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode(); // Ejecutar
            } catch (Exception ignored) {
            }
        });
    }

    public void sendDiscordEmbed(String jsonPayload) {
        String urlString = plugin.getConfig().getString("discord.webhook-url");
        if (urlString == null || urlString.isEmpty() || urlString.equals("URL_AQUI")) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "AegisGuard-Engine");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                connection.getResponseCode();
            } catch (Exception ignored) {}
        });
    }

    // ------------------------------------------------------------------------
    // SISTEMA DE CONGELAMIENTO (FREEZE)
    // ------------------------------------------------------------------------
    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUso: /ag freeze <jugador>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + "§cEse jugador no está en línea.");
            return;
        }
        
        UUID tuuid = target.getUniqueId();
        if (frozenPlayers.getOrDefault(tuuid, false)) {
            frozenPlayers.put(tuuid, false);
            sender.sendMessage(prefix + "§aHas descongelado a §e" + target.getName());
            target.sendMessage(prefix + "§aHas sido descongelado. Perdona las molestias.");
        } else {
            frozenPlayers.put(tuuid, true);
            sender.sendMessage(prefix + "§cHas CONGELADO a §e" + target.getName());
            target.sendMessage(prefix + "§c§l¡HAS SIDO CONGELADO POR SOSPECHA DE HACKS!");
            target.sendMessage("§7No te desconectes o serás castigado severamente.");
        }
    }

    public static boolean isPlayerFrozen(UUID uuid) {
        return frozenPlayers.getOrDefault(uuid, false);
    }

    // ------------------------------------------------------------------------
    // MODO ESPIA
    // ------------------------------------------------------------------------
    private void handleSpy(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cSolo jugadores pueden usar spy.");
            return;
        }
        
        if (player.hasPermission(AlertManager.ALERT_PERMISSION)) {
            // Este comando delega en LuckPerms, asumiendo un entorno profesional moderno
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset " + AlertManager.ALERT_PERMISSION);
            player.sendMessage(prefix + "§eModo Espía §cDESACTIVADO§e. Ya no verás alertas.");
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + AlertManager.ALERT_PERMISSION + " true");
            player.sendMessage(prefix + "§eModo Espía §aACTIVADO§e. Ahora verás alertas críticas.");
        }
    }

    // ------------------------------------------------------------------------
    // INTERFAZ GRAFICA (GUI SETTINGS) Y CONFIGURACIONES
    // ------------------------------------------------------------------------
    private void handleSettings(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cLa GUI solo está disponible In-Game.");
            return;
        }

        if (args.length > 1) {
            String roleId = args[1];
            plugin.getConfig().set("discord.staff-role-id", roleId);
            plugin.saveConfig();
            player.sendMessage(prefix + "§aHas establecido el ID del Rol de Discord a: §e" + roleId);
            player.sendMessage(prefix + "§7(AegisGuard ahora mencionará este rol en alertas críticas)");
            return;
        }
        
        Inventory inv = Bukkit.createInventory(null, 27, "§8§l🛡 " + AlertManager.toSmallCapsClean("aegisguard ajustes"));
        
        // Rellenar modulos principales
        inv.setItem(10, createModuleItem("KillAura", Material.DIAMOND_SWORD));
        inv.setItem(11, createModuleItem("HitBox", Material.CROSSBOW));
        inv.setItem(12, createModuleItem("Speed", Material.SUGAR));
        inv.setItem(13, createModuleItem("Fly", Material.FEATHER));
        inv.setItem(14, createModuleItem("Jesus", Material.LILY_PAD));
        inv.setItem(15, createModuleItem("Step", Material.OAK_STAIRS));
        inv.setItem(16, createModuleItem("AutoClicker", Material.TRIPWIRE_HOOK));
        
        // Info global central
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§b§lESTADÍSTICAS DEL MOTOR");
        im.setLore(Arrays.asList(
                "§8--------------------",
                "§7Baneos Ejecutados: §c" + com.camistudios.aegisguard.managers.PunishmentManager.getTotalBansExecuted(),
                "§7Jugadores Online: §a" + Bukkit.getOnlinePlayers().size(),
                "§8--------------------",
                "§7Discord Webhook: " + (plugin.getConfig().getString("discord.webhook-url").length() > 20 ? "§aVinculado" : "§cNo Vinculado")
        ));
        info.setItemMeta(im);
        inv.setItem(22, info);
        
        player.openInventory(inv);
    }

    private ItemStack createModuleItem(String moduleName, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        boolean enabled = plugin.getConfig().getBoolean("checks." + moduleName.toLowerCase() + ".enabled", true);
        
        meta.setDisplayName("§3§l" + AlertManager.toSmallCapsClean(moduleName));
        List<String> lore = new ArrayList<>();
        lore.add("§8Modulo de Detección");
        lore.add("");
        lore.add("§7Estado actual: " + (enabled ? "§a§lACTIVADO" : "§c§lDESACTIVADO"));
        lore.add("");
        lore.add("§e▶ Click para alternar");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8§l🛡 " + AlertManager.toSmallCapsClean("aegisguard ajustes"))) {
            event.setCancelled(true); 
            
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            
            Player player = (Player) event.getWhoClicked();
            String rawName = event.getCurrentItem().getItemMeta().getDisplayName();
            String stripped = ChatColor.stripColor(rawName).trim(); 
            
            // Revertir SmallCaps a normal text para buscar en config
            String moduleKey = revertSmallCaps(stripped).toLowerCase();
            
            if (plugin.getConfig().contains("checks." + moduleKey + ".enabled")) {
                boolean currentState = plugin.getConfig().getBoolean("checks." + moduleKey + ".enabled");
                plugin.getConfig().set("checks." + moduleKey + ".enabled", !currentState);
                plugin.saveConfig();
                
                player.sendMessage(prefix + "§aMódulo §e" + stripped + " §aahora está: " + (!currentState ? "ACTIVADO" : "DESACTIVADO"));
                
                // Refrescar GUI inmediatamente
                handleSettings(player, new String[]{"settings"});
            }
        }
    }
    
    private String revertSmallCaps(String smallCaps) {
        String normal = "abcdefghijklmnopqrstuvwxyz";
        String small  = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
        StringBuilder builder = new StringBuilder();
        for (char c : smallCaps.toCharArray()) {
            int idx = small.indexOf(c);
            if (idx != -1) {
                builder.append(normal.charAt(idx));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("aegisguard.admin")) {
            List<String> options = Arrays.asList("verify", "unverify", "settings", "spy", "freeze", "reload");
            for (String opt : options) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    completions.add(opt);
                }
            }
        }
        return completions;
    }
}