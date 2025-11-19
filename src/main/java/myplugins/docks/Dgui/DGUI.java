package myplugins.docks.Dgui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DGUI extends JavaPlugin implements Listener, CommandExecutor {

    private String guiTitle;
    private int guiSize;
    private int guiItemSlot;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        // Register events and command
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("agui") != null) {
            getCommand("agui").setExecutor(this);
        }

        getLogger().info("Docks's Command GUI enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Docks's Command GUI disabled.");
    }


    private void loadSettings() {
        guiTitle = color(getConfig().getString("gui.title", "&Docks's &7Command GUI"));
        guiSize = getConfig().getInt("gui.size", 9);

        // safety: snap to nearest valid size (1-6 rows, multiples of 9)
        if (guiSize <= 0 || guiSize % 9 != 0 || guiSize > 54) {
            getLogger().warning("Invalid gui.size in config.yml, defaulting to 9.");
            guiSize = 9;
        }

        guiItemSlot = getConfig().getInt("gui.item.slot", 4);
        if (guiItemSlot < 0 || guiItemSlot >= guiSize) {
            getLogger().warning("Invalid gui.item.slot in config.yml, defaulting to 4.");
            guiItemSlot = 4;
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("docks.use")) {
            player.sendMessage(color("&cYou do not have permission to use this command."));
            return true;
        }

        openGui(player);
        return true;
    }

    private void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, guiSize, guiTitle);

        // Build the single item from config
        String materialName = getConfig().getString("gui.item.material", "DIAMOND");
        Material mat = Material.matchMaterial(materialName);
        if (mat == null || mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
            getLogger().warning("Invalid material '" + materialName + "' in config.yml, defaulting to DIAMOND.");
            mat = Material.DIAMOND;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = getConfig().getString("gui.item.name", "&bClick to run commands");
            meta.setDisplayName(color(displayName));

            List<String> loreRaw = getConfig().getStringList("gui.item.lore");
            if (!loreRaw.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : loreRaw) {
                    lore.add(color(line));
                }
                meta.setLore(lore);
            }

            // Just for style
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        inv.setItem(guiItemSlot, item);

        player.openInventory(inv);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;

        Inventory inventory = event.getInventory();
        if (inventory == null) return;

        // Only handle chest-like inventories with our title
        if (inventory.getType() != InventoryType.CHEST) return;
        if (!event.getView().getTitle().equals(guiTitle)) return;

        // We are in the GUI, cancel all clicks
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (event.getSlot() != guiItemSlot) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) clicker;

        // Run configured commands
        runConfiguredCommands(player);
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    private void runConfiguredCommands(Player player) {
        List<String> commands = getConfig().getStringList("commands");
        if (commands == null || commands.isEmpty()) {
            player.sendMessage(color("&cNo commands configured in config.yml."));
            return;
        }

        String playerName = player.getName();
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) continue;

            String line = raw.replace("{player}", playerName);
            String lower = line.toLowerCase();

            if (lower.startsWith("console:")) {
                String cmd = line.substring("console:".length()).trim();
                if (!cmd.isEmpty()) {
                    Bukkit.dispatchCommand(console, cmd);
                }
            } else if (lower.startsWith("player:")) {
                String cmd = line.substring("player:".length()).trim();
                if (!cmd.isEmpty()) {
                    player.performCommand(cmd);
                }
            } else {
                // default: console
                Bukkit.dispatchCommand(console, line.trim());
            }
        }
    }

    private String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
