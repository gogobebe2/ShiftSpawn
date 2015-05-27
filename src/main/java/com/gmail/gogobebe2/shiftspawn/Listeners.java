package com.gmail.gogobebe2.shiftspawn;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Listeners implements Listener {
    private ShiftSpawn plugin;
    private Game game;

    public Listeners(ShiftSpawn plugin) {
        this.plugin = plugin;
        this.game = plugin.getGame();
    }

    public boolean tryBeginStarting() {
        boolean wasSuccessful = false;
        if (Bukkit.getOnlinePlayers().size() >= plugin.getConfig().getInt(ShiftSpawn.MIN_PLAYERS_KEY) && game.getGameState().equals(GameState.WAITING)) {
            game.setGameState(GameState.STARTING);
            game.setTime(plugin.getConfig().getString(ShiftSpawn.TIME_BEFORE_START_KEY));
            wasSuccessful = true;
        }
        game.startTimer(false);
        return wasSuccessful;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (game.getGameState().equals(GameState.WAITING)) {
            if (tryBeginStarting()) {
                game.setGameState(GameState.STARTING);
            }
            else {
                event.setJoinMessage(ChatColor.DARK_PURPLE + playerName + " joined. We need "
                        + (plugin.getConfig().getInt(ShiftSpawn.MIN_PLAYERS_KEY) - Bukkit.getOnlinePlayers().size())
                        + " more players to start.");
            }
        } else {
            event.setJoinMessage(ChatColor.DARK_PURPLE + playerName + " joined the game.");
        }
        spawn(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeaveEvent(PlayerQuitEvent event) {
        int minPlayers = plugin.getConfig().getInt(ShiftSpawn.MIN_PLAYERS_KEY);
        Player player = event.getPlayer();
        String playerName = player.getName();
        event.setQuitMessage(ChatColor.DARK_PURPLE + playerName + " left the game.");
        if (minPlayers < Bukkit.getOnlinePlayers().size()) {
            if (game.getGameState().equals(GameState.WAITING)) {
                event.setQuitMessage(ChatColor.DARK_PURPLE + playerName + " left the game. We need "
                        + (minPlayers - Bukkit.getOnlinePlayers().size())
                        + " more players to start.");
            } else if (game.getGameState().equals(GameState.STARTING)) {
                event.setQuitMessage(ChatColor.DARK_PURPLE + "Well, " + playerName
                        + " left, so there's not enough players to start. Blame them!");
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamageEvent(BlockDamageEvent event) {
        //noinspection deprecation
        if (event.getBlock().getTypeId() == plugin.getConfig().getInt(ShiftSpawn.ALPHA_CORE_ID)) {
            event.setCancelled(true);
        }
    }

    private void spawn(final Player PLAYER) {
        PLAYER.spigot().respawn();
        String id;
        if (game.getGameState().equals(GameState.WAITING)) {
            id = "main";
        } else {
            if (plugin.containsPlayer(PLAYER)) {
                id = plugin.getParticipant(PLAYER).getSpawnID();
            } else {
                id = plugin.getNextSpawnIndex();
                plugin.getParticipants().add(new Participant(PLAYER, id));

                PLAYER.setHealth(20);
                PLAYER.setFoodLevel(20);
                Inventory inventory = PLAYER.getInventory();
                inventory.clear();

                ItemStack pickaxe = new ItemStack(Material.WOOD_PICKAXE, 1);
                ItemMeta pickaxeMeta = pickaxe.getItemMeta();
                pickaxeMeta.setDisplayName(ChatColor.AQUA + "Trusy old pickaxe");
                pickaxe.setItemMeta(pickaxeMeta);

                ItemStack sword = new ItemStack(Material.WOOD_SWORD, 1);
                ItemMeta swordMeta = sword.getItemMeta();
                swordMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Trusy old sword");
                sword.setItemMeta(swordMeta);

                inventory.addItem(pickaxe);
                inventory.addItem(sword);
                PLAYER.updateInventory();
            }
        }
        PLAYER.teleport(loadSpawn(id));
    }


    private Location loadSpawn(String id) {
        final World WORLD = Bukkit.getWorld(plugin.getConfig().getString("Spawns." + id + ".World"));
        final double X = plugin.getConfig().getDouble("Spawns." + id + ".X");
        final double Y = plugin.getConfig().getDouble("Spawns." + id + ".Y");
        final double Z = plugin.getConfig().getDouble("Spawns." + id + ".Z");
        final float PITCH = (float) plugin.getConfig().getDouble("Spawns." + id + ".Pitch");
        final float YAW = (float) plugin.getConfig().getDouble("Spawns." + id + ".Yaw");
        return new Location(WORLD, X, Y, Z, YAW, PITCH);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo().getY() < 0 && player.getHealth() > 0) {
            player.setHealth(0);
            spawn(player);
        }
    }
}
