package nl.scoutcraft.hangman;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Main extends JavaPlugin implements Listener {

    private static final List<HangmanGame> games = new ArrayList<>();

    private final HashMap<Player, Location> locationMap = new HashMap<>();
    private final HashMap<Player, BlockVector> blockVectorMap = new HashMap<>();
    private final HashMap<Player, List<Block>> letterBlocksMap = new HashMap<>();

    @Override
    public void onLoad() {
        Main.instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(Main.getInstance(), Main.getInstance());
        try {
            games.add(new HangmanGame(Bukkit.getWorld(Main.getInstance().getConfig().getString("world")).getBlockAt(Main.getInstance().getConfig().getInt("x"), Main.getInstance().getConfig().getInt("y"), Main.getInstance().getConfig().getInt("z")).getLocation(), 4));
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        getCommand("hangman").setExecutor(this);
    }
        

    @Override
    public void onDisable() {
    }

    /**
     * If the command is "reset", reset all games. If the command is "save", save the current structure
     * or letterboard. If the command is "build", build the structure or letterboard
     * 
     * @param sender The CommandSender that sent the command.
     * @param command The command that was executed.
     * @param label The command label.
     * @param args The arguments passed to the command.
     * @return A boolean
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (label.equalsIgnoreCase("reset")) games.forEach(HangmanGame::reset);
        if (label.equalsIgnoreCase("save")) {
            if (args.length != 2) return false;
            if (args[0].equalsIgnoreCase("letterboard")) {
                if (!this.blockVectorMap.containsKey(player)) return false;
                if (!this.letterBlocksMap.containsKey(player) || this.letterBlocksMap.get(player).size() < 1)
                    return false;
                BlockVector blockVector = this.blockVectorMap.remove(player);
                List<Block> letterBlocks = this.letterBlocksMap.remove(player);
                try {
                    LetterBoard letterBoard = new LetterBoard(blockVector.getLocationA());
                    letterBoard.setLetterBlockData(letterBlocks.get(0).getBlockData());
                    letterBlocks.stream().map(Block::getLocation).forEach(letterBoard::addLocationVector);
                    Main.getInstance().getConfig().set(player.getUniqueId() + "." + args[1], letterBoard.serialize().toString());
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
            if (args[0].equalsIgnoreCase("structure")) {
                if (!this.blockVectorMap.containsKey(player)) return false;
                Main.getInstance().getConfig().set(player.getUniqueId() + "." + args[1], this.blockVectorMap.get(player).serialize().toString());
            }
            Main.getInstance().saveConfig();
        }
        if (label.equalsIgnoreCase("build")) {
            if (args.length != 1) return false;
            if (Main.getInstance().getConfig().contains(player.getUniqueId() + "." + args[0])) {
                JsonObject jsonObject = JsonParser.parseString(Main.getInstance().getConfig().getString(player.getUniqueId() + "." + args[0])).getAsJsonObject();
                new BlockVector(player.getLocation(), jsonObject).build();
            }
            if (!this.blockVectorMap.containsKey(player)) return false;
            Main.getInstance().getConfig().set(player.getUniqueId() + "." + args[0], this.blockVectorMap.remove(player).serialize().toString());
            Main.getInstance().saveConfig();
        }
        return true;
    }

    /**
     * If the player is right clicking with a bamboo item, and they are not sneaking, then the location
     * of the block they clicked is stored in a map. If they are sneaking, then the location of the
     * block they clicked is added to a list
     * 
     * @param event The event that is being listened to.
     */
    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.BAMBOO) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        if (player.isSneaking()) {
            this.letterBlocksMap.computeIfAbsent(player, k -> new ArrayList<>()).add(location.getBlock());
        } else {
            if (this.locationMap.containsKey(player) && this.locationMap.get(player) != null) {
                try {
                    this.blockVectorMap.put(player, new BlockVector(location, this.locationMap.remove(player), player.getLocation()));
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            } else {
                this.locationMap.put(player, location);
            }
        }
    }

    private static Main instance;

    public static synchronized Main getInstance() {
        return Main.instance;
    }

}
