package com.gmail.gogobebe2.shiftspawn;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class ShiftSpawn extends JavaPlugin {
    public final static String MIN_PLAYERS_KEY = "Minimum players before game starts";
    public final static String TIME_BEFORE_START_KEY = "Time before games starts";
    public final static String GAME_TIME = "Game time";
    public final static String ALPHA_CORE_ID = "Alpha Core Block ID";
    private Game game;
    private String spawnID = "-1";
    private List<Participant> participants = new ArrayList<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("shiftspawn")) {
            if (!sender.hasPermission("shiftspawn.*")) {
                sender.sendMessage(ChatColor.RED + "You sneaky kid. You aren't allowed to use this command silly!");
                return true;
            } else if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Now how the hell would that work? Idiot, you have to be a player to use this command!!");
                return true;
            }
            Player player = (Player) sender;
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Enter a spawn id /shiftspawn <'main':number>");
                return true;
            }
            String id = args[0].toLowerCase();
            if (!id.equalsIgnoreCase("main")) {
                try {
                    if (Integer.parseInt(args[0]) < 0) {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.DARK_RED + args[0] + ChatColor.RED + " is not an appropriate number or is not main!");
                    return true;
                }
            }
            final Location LOCATION = player.getLocation();
            final String WORLD = LOCATION.getWorld().getName();
            final double X = LOCATION.getX();
            final double Y = LOCATION.getY();
            final double Z = LOCATION.getZ();
            final float PITCH = LOCATION.getPitch();
            final float YAW = LOCATION.getYaw();

            getConfig().set("Spawns." + id + ".World", WORLD);
            getConfig().set("Spawns." + id + ".X", X);
            getConfig().set("Spawns." + id + ".Y", Y);
            getConfig().set("Spawns." + id + ".Z", Z);
            getConfig().set("Spawns." + id + ".Pitch", PITCH);
            getConfig().set("Spawns." + id + ".Yaw", YAW);
            saveConfig();
            player.sendMessage(ChatColor.GREEN + "Set " + (id.equalsIgnoreCase("main") ? "the main spawn." : "the spawn id " + id));
            return true;
        }
        return false;
    }

    public void spawn(final Player PLAYER) {
        PLAYER.spigot().respawn();
        String id;
        if (game.getGameState().equals(GameState.STARTED)) {
            id = getParticipant(PLAYER).getSpawnID();
            PLAYER.setGameMode(GameMode.SURVIVAL);
            PLAYER.setHealth(20);
            PLAYER.setFoodLevel(20);
            Inventory inventory = PLAYER.getInventory();
            inventory.clear();

            ItemStack pickaxe = new ItemStack(Material.WOOD_PICKAXE, 1);
            ItemMeta pickaxeMeta = pickaxe.getItemMeta();
            pickaxeMeta.setDisplayName(ChatColor.AQUA + "Chipped pickaxe");
            pickaxe.setDurability((short) 8);
            pickaxe.setItemMeta(pickaxeMeta);

            ItemStack sword = new ItemStack(Material.WOOD_SWORD, 1);
            ItemMeta swordMeta = sword.getItemMeta();
            swordMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Trusty sword");
            sword.setItemMeta(swordMeta);

            inventory.addItem(pickaxe);
            inventory.addItem(sword);
            PLAYER.updateInventory();
        } else {
            id = "main";
        }
        PLAYER.teleport(loadSpawn(id));
    }

    public Location loadSpawn(String id) {
        final World WORLD = Bukkit.getWorld(getConfig().getString("Spawns." + id + ".World"));
        final double X = getConfig().getDouble("Spawns." + id + ".X");
        final double Y = getConfig().getDouble("Spawns." + id + ".Y");
        final double Z = getConfig().getDouble("Spawns." + id + ".Z");
        final float PITCH = (float) getConfig().getDouble("Spawns." + id + ".Pitch");
        final float YAW = (float) getConfig().getDouble("Spawns." + id + ".Yaw");
        return new Location(WORLD, X, Y, Z, YAW, PITCH);
    }

    public String getNextSpawnID() {
        List<String> ids = new ArrayList<>();
        ids.addAll(getConfig().getConfigurationSection("Spawns").getKeys(false));
        if (ids.contains("main")) {
            ids.remove("main");
        }
        if (this.spawnID.equals("-1")) {
            this.spawnID = ids.get(new Random().nextInt(ids.size()));
        }
        else {
            Collections.sort(ids);
            Iterator<String> iterator = ids.listIterator(ids.indexOf(this.spawnID));
            if (iterator.hasNext()) {
                this.spawnID = iterator.next();
            } else {
                this.spawnID = ids.get(0);
            }
        }
        return spawnID;
    }

    @Override
    public void onEnable() {
        getLogger().info("Starting up ShiftSpawn. If you need me to update this plugin, email at gogobebe2@gmail.com");
        saveDefaultConfig();
        this.game = new Game(this, GameState.WAITING, Integer.MAX_VALUE + ":00");
        game.startTimer();
        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(ChatColor.AQUA + "You have been kicked while the game gets setup.");
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling ShiftSpawn. If you need me to update this plugin, email at gogobebe2@gmail.com");
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Scoreboard scoreboard = player.getScoreboard();
                scoreboard.clearSlot(DisplaySlot.SIDEBAR);
                scoreboard.clearSlot(DisplaySlot.BELOW_NAME);
                player.kickPlayer(ChatColor.AQUA + "You have been kicked while game restarts.");
            }
        }
    }

    public Game getGame() {
        return this.game;
    }

    public List<Participant> getParticipants() {
        return this.participants;
    }

    public boolean hasParticipantSet(Player player) {
        if (participants.isEmpty()) {
            return false;
        }
        for (Participant participant : participants) {
            if (participant.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public Participant getParticipant(Player player) throws NullPointerException {
        for (Participant participant : participants) {
            if (participant.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                return participant;
            }
        }
        throw new NullPointerException(player.getName() + " has not had his participant set yet");
    }
}
