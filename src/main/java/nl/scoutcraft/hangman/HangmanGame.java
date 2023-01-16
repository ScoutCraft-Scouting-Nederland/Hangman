package nl.scoutcraft.hangman;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonParser;
import nl.scoutcraft.eagle.libs.utils.TextUtils;
import nl.scoutcraft.eagle.server.gui.inventory.base.AbstractInventoryMenu;
import nl.scoutcraft.eagle.server.gui.inventory.base.Button;
import nl.scoutcraft.eagle.server.gui.inventory.base.ButtonClickType;
import nl.scoutcraft.eagle.server.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * It's a class that manages a hangman game
 */
public class HangmanGame implements Listener {

    private final static long GAME_COOLDOWN = 20L * Main.getInstance().getConfig().getLong("gameCooldown");
    private final static long PLAYER_GUES_COOLDOWN = 1000L * Main.getInstance().getConfig().getLong("playerCooldown");
    private final static long GUESS_COOLDOWN = 1000L * Main.getInstance().getConfig().getLong("guessCooldown");

    private final LetterBoard letterBoard;
    private final BlockVector[] stages;

    private String word;
    private boolean[] guessed;
    private int wrongGuesses;
    private final int maxGuesses;

    private final List<String> wordList;
    private final HashMap<Player, Long> lastGuessMap;
    private final long lastGuess;

    // Creating a new HangmanGame object.
    public HangmanGame(Location gameLocation, int maxGuesses) throws InstantiationException {
        this.maxGuesses = maxGuesses;

        this.lastGuessMap = new HashMap<>();
        this.lastGuess = 0L;

        this.wordList = Main.getInstance().getConfig().getStringList("wordList");
        Bukkit.getLogger().warning(Main.getInstance().getConfig().getString("letterBoard"));
        this.letterBoard = new LetterBoard(gameLocation, JsonParser.parseString(Main.getInstance().getConfig().getString("letterBoard")).getAsJsonObject());
        List<String> stagesStringList = Main.getInstance().getConfig().getStringList("stages");
        if (maxGuesses > stagesStringList.size()) throw new InstantiationException("More guesses than stages!");
        this.stages = new BlockVector[stagesStringList.size()];
        IntStream.range(0, stagesStringList.size()).forEach(i -> this.stages[i] = new BlockVector(gameLocation, JsonParser.parseString(stagesStringList.get(i)).getAsJsonObject()));

        this.reset();
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    /**
     * This function returns true if the given block is a letter on the board.
     * 
     * @param block The block that was clicked.
     * @return A boolean value.
     */
    private boolean isLetter(Block block) {
        return this.letterBoard.isLetter(block);
    }

    /**
     * If the current time is greater than the last time the player guessed plus the player's cooldown,
     * and the current time is greater than the last time anyone guessed plus the global cooldown, then
     * the player can guess
     * 
     * @param player The player who is guessing
     * @return The time in milliseconds since the last guess was made.
     */
    private boolean canGuess(Player player) {
        return System.currentTimeMillis() > (this.lastGuessMap.getOrDefault(player, 0L) + PLAYER_GUES_COOLDOWN) && System.currentTimeMillis() > (this.lastGuess + GUESS_COOLDOWN);
    }

    /**
     * If the letter is not a letter or is not uppercase, return false. Otherwise, if the letter is in
     * the word and has not been guessed, return false. Otherwise, return true
     * 
     * @param letter the letter to check
     * @return The index of the letter in the word.
     */
    private boolean isGuessed(char letter) {
        if (!Character.isLetter(letter) || !Character.isUpperCase(letter)) return false;
        int index = this.indexOf(letter);
        return index != -1 && this.guessed[index];
    }

    /**
     * If the letter is in the word, return true
     * 
     * @param letter The letter that the user guessed.
     * @return A boolean value.
     */
    private boolean isInWord(char letter) {
        return this.word.toUpperCase().contains(String.valueOf(letter));
    }

    /**
     * If the word has been guessed, return true, otherwise return false
     * 
     * @return A boolean value.
     */
    private boolean hasWon() {
        for (int i = 0; i < this.word.length(); i++) {
            if (!this.isGuessed(Character.toUpperCase(this.word.charAt(i)))) return false;
        }
        return true;
    }

    /**
     * If the number of wrong guesses is equal to the maximum number of guesses, then the player has
     * lost.
     * 
     * @return A boolean value.
     */
    private boolean hasLost() {
        return this.wrongGuesses == this.maxGuesses;
    }

    /**
     * Return the index of the given letter in the LETTERS array, or -1 if the letter is not in the
     * array.
     * 
     * @param letter The letter to be found
     * @return The index of the letter in the array.
     */
    private int indexOf(char letter) {
        for (int i = 0; i < LetterMenu.LETTERS.length; i++) {
            if (LetterMenu.LETTERS[i] == letter) return i;
        }
        return -1;
    }

    /**
     * "Return a list of all the indices of the given letter in the word."
     * 
     * @param letter The letter that the user guessed.
     * @return A list of integers.
     */
    private List<Integer> getIndicesOfLetter(char letter) {
        return IntStream.range(0, this.word.length()).filter(i -> Character.toUpperCase(this.word.charAt(i)) == letter).boxed().collect(Collectors.toList());
    }

    /**
     * It sends an action bar to all online players, replacing %PLAYER% with the player's name and
     * %LETTER% with the letter.
     * 
     * @param key The key in the config.yml file.
     * @param player The player who is sending the message.
     * @param letter The letter that the player guessed.
     */
    private void announce(String key, Player player, char letter) {
        Bukkit.getOnlinePlayers().forEach(sendToPlayer -> sendToPlayer.sendActionBar(TextUtils.colorize(Main.getInstance().getConfig().getString(key).replace("%PLAYER%", player.getName()).replace("%LETTER%", String.valueOf(letter)))));
    }

    /**
     * It sends an action bar to a player
     * 
     * @param target The player you want to send the action bar to.
     * @param key The key in the config.yml file.
     * @param letter The letter that the player is currently typing.
     */
    private void tellPlayer(Player target, String key, char letter) {
        target.sendActionBar(TextUtils.colorize(Main.getInstance().getConfig().getString(key).replace("%LETTER%", String.valueOf(letter))));
    }

    /**
     * If the game is not over, the player can guess, the letter has not been guessed, and the letter
     * is in the word, then register the guess as correct and return true
     * 
     * @param letterBlock The block that the player clicked on.
     * @param letter The letter that the player guessed.
     * @param player The player who is making the guess.
     * @return A boolean value
     */
    public boolean makeGuess(Block letterBlock, char letter, Player player) {
        if (this.hasWon() || this.hasLost()) {
            return false;
        }
        if (!this.canGuess(player)) {
            this.tellPlayer(player, "MESSAGE_COOLDOWN", letter);
            return false;
        }
        if (this.isGuessed(letter)) {
            this.tellPlayer(player, "MESSAGE_ALREADY_GUESSED", letter);
            return false;
        }
        this.guessed[this.indexOf(letter)] = true;
        if (!this.isInWord(letter)) {
            this.registerWrongGuess(player, letter);
            return false;
        }
        this.registerRightGuess(player, letter);
        return true;
    }

    /**
     * If the player has guessed the right letter, then update the last guess map, show the letter on
     * the board, and if the player has won, then win the game, otherwise announce that the player has
     * guessed the right letter.
     * 
     * @param player The player who guessed the letter
     * @param letter The letter that the player guessed
     */
    private void registerRightGuess(Player player, char letter) {
        this.lastGuessMap.put(player, System.currentTimeMillis());
        this.getIndicesOfLetter(letter).forEach(index -> this.letterBoard.showLetter(this.word, index, true));
        if (this.hasWon()) {
            this.winGame(player, letter);
        } else {
            this.announce("MESSAGE_RIGHT_GUESS", player, letter);
        }
    }

    /**
     * This function announces that the player has won the game, and then resets the game after a
     * delay.
     * 
     * @param player The player who won the game
     * @param letter The letter that the player guessed
     */
    private void winGame(Player player, char letter) {
        this.announce("MESSAGE_GAME_WON", player, letter);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::reset, GAME_COOLDOWN);
    }

    /**
     * If the player has guessed wrong, add the current time to the lastGuessMap, increment the
     * wrongGuesses counter, and if the wrongGuesses counter is equal to the maxGuesses, fail the game,
     * otherwise announce the wrong guess and build the stage.
     * 
     * @param player The player who guessed the letter
     * @param letter The letter that the player guessed.
     */
    private void registerWrongGuess(Player player, char letter) {
        this.lastGuessMap.put(player, System.currentTimeMillis());
        this.wrongGuesses++;
        if (this.wrongGuesses == this.maxGuesses) {
            this.failGame(player, letter);
        } else {
            this.announce("MESSAGE_WRONG_GUESS", player, letter);
        }
        this.buildStage(this.wrongGuesses);
    }

    /**
     * If the player loses, announce it and reset the game after a cooldown.
     * 
     * @param player The player who lost the game
     * @param letter The letter that the player guessed
     */
    private void failGame(Player player, char letter) {
        this.announce("MESSAGE_GAME_LOST", player, letter);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::reset, GAME_COOLDOWN);
    }

    /**
     * Get a random word from the word list.
     * 
     * @return A random word from the wordList array.
     */
    private String getRandomWord() {
        return this.wordList.get(new Random().nextInt(this.wordList.size()));
    }

    /**
     * This function resets the game by resetting the letter board, setting the guessed array to false,
     * setting the wrong guesses to 0, getting a new random word, and building the stage
     */
    public void reset() {
        this.letterBoard.reset();
        this.guessed = new boolean[LetterMenu.LETTERS.length];
        this.wrongGuesses = 0;
        this.word = this.getRandomWord();
        this.buildStage(this.wrongGuesses);
    }

    /**
     * "Build the stage that corresponds to the number of wrong guesses."
     * 
     * The first thing we do is get the stage that corresponds to the number of wrong guesses. We do
     * this by getting the stage from the `stages` array
     * 
     * @param wrongGuesses The number of wrong guesses the player has made.
     */
    public void buildStage(int wrongGuesses) {
        BlockVector stage = this.stages[wrongGuesses];
        if (stage == null) Bukkit.getLogger().log(Level.SEVERE, "Something went wrong!");
        stage.build();
    }

    /**
     * If the player has not won or lost, and they clicked a letter, open the letter menu
     * 
     * @param event The event that was called.
     */
    @EventHandler
    public void onClickLetter(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (this.hasWon() || this.hasLost()) return;
        if (!this.isLetter(event.getClickedBlock())) return;
        new LetterMenu(this, event.getClickedBlock()).open(event.getPlayer());
        event.setCancelled(true);
    }

    /**
     * When a player quits, remove them from the lastGuessMap
     * 
     * @param event The event that was fired.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.lastGuessMap.remove(event.getPlayer());
    }

    /**
     * It's a HashMap that contains all the heads that correspond to a letter
     */
    public static class Constants {

        public static HashMap<Character, String[]> WHITEHEADS = new HashMap<>();

        static {
            WHITEHEADS.put('A', new String[]{"5a48a074-296f-42c0-b6f2-02de0a5828e2", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU0MTc0ODEyMTYyNmYyMmFlMTZhNGM2NjRjNzMwMWE5ZjhlYTU5MWJmNGQyOTg4ODk1NzY4MmE5ZmRhZiJ9fX0="});
            WHITEHEADS.put('B', new String[]{"6659159e-57b8-4177-b2f5-9268b9add0e3", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDJiOWUxNmUyNjIwNmE3MDliZjA3YzI0OTNjYTRjNWQyNGY1Njc1NjU0ZmMxMzBkMWQ1ZWM1ZThjNWJlNSJ9fX0="});
            WHITEHEADS.put('C', new String[]{"73ca3c0b-ef4c-4b03-9e77-4db5a27824ef", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjJhNTg3NjExMzMyMmYzOWFhMmJiZWY0YmQ2Yjc5ZWM2YjUyYTk3YmI2ZmFiNjc0YmRkYmQ3YjZlYWIzYmEifX19"});
            WHITEHEADS.put('D', new String[]{"eea0c86a-be64-4004-9419-2945070dc5fe", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmE2NjE0MTlkZTQ5ZmY0YTJjOTdiMjdmODY4MDE0ZmJkYWViOGRkN2Y0MzkyNzc3ODMwYjI3MTRjYWFmZDFmIn19fQ=="});
            WHITEHEADS.put('E', new String[]{"40777fac-0c0e-44e5-85e9-5cad15e0bfff", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFlZWY4OGUyYzkyOGI0NjZjNmVkNWRlYWE0ZTE5NzVhOTQzNmMyYjFiNDk4ZjlmN2NiZjkyYTliNTk5YTYifX19"});
            WHITEHEADS.put('F', new String[]{"f1c8b8e5-5b3f-430e-a5ba-372a496b208d", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Q5ZDZlOTZiNWI5MmZmY2FmNDdkZDFjYWY2MWQzZjZlODQyOTEzZmM4ODg0OWYzZGU1NDhiZWVkNzFmYTgifX19"});
            WHITEHEADS.put('G', new String[]{"d34a2268-53e2-45f5-90c9-899a7ada1109", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjIwYzNiMmJiZmExZWQzYWM4YzM1YjNkZDM4MjQ3NDU2NTYzYzkyYWNlZmQ1OTI2YjEyNWNjYzY3ZDdkNWZkIn19fQ=="});
            WHITEHEADS.put('H', new String[]{"3539c341-0f8e-4108-8cfa-08b0197a0af6", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2JhOWMzM2E5NWZhMWU1MTlmODVhNDFjYTU2Nzk5Mzg0ZGI0MWZlN2UxZDdhNzkxNzUxZWNlOWJiYWU1ZDI3ZiJ9fX0="});
            WHITEHEADS.put('I', new String[]{"05f5c4c3-7a37-4893-bad5-9318991a1cb0", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWM5OWRmYjI3MDRlMWJkNmU3ZmFjZmI0M2IzZTZmYmFiYWYxNmViYzdlMWZhYjA3NDE3YTZjNDY0ZTFkIn19fQ=="});
            WHITEHEADS.put('J', new String[]{"a8859c64-d8b8-4796-8ad3-af41e9442d88", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzgxZWZhYTliYzNiNjA3NDdhNzUwYTY0OGIxOTg3ODdmMTg2ZWI5Mzg1OWFlYTUyMDMxZDVhOGM4ODEwNzUifX19"});
            WHITEHEADS.put('K', new String[]{"4ada0d9d-546d-4954-a3a0-82d82a244b65", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzVkYzZkNTEzY2YxNjMzMzcyZjQxY2FhMDI2MTM5NmU2Y2M2NzIwZTA1OTMwOGM2NzlkNDRiNDdlZDYifX19"});
            WHITEHEADS.put('L', new String[]{"e23fdeb8-791b-4a51-9f93-68280470adc3", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDRhZmZhNDU1YjdmNTgyMTdkZThhY2JiZDkyMDFjOWVhODdjMTM0YWEzNTYyNTQ5NGY1ZDNmNjVjZTk0NiJ9fX0="});
            WHITEHEADS.put('M', new String[]{"bea97301-e9dc-4115-9844-4f1b09f0a890", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzhkZWM0NjY2YjRjNjdkODc1OTcxNGM4NTcxNGJlNmVhNGUzOWZmOTYyODg0OWY5OGI1MTRlZGYxYzNlNDY4MCJ9fX0="});
            WHITEHEADS.put('N', new String[]{"acf1430e-5b8f-4468-96f4-69d3591eb59b", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGEyMjFlNGY5NmJlZTYyNjE3NTIzOTZhMzI2NWZmYTRkZWRmOGZmNDgzOWFiZDE0ZjQ5ZWRlZTFlNTMwOTIifX19"});
            WHITEHEADS.put('O', new String[]{"9299a92b-b0e4-4386-9dce-0566c74a3ebf", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2JiMWQxN2NlYmM1ZjBlY2M5ODdiODBlZmMwM2UzMmVjYjFjYjQwZGJjNWJjZTJmYWYzZTYwNTQyYTQwIn19fQ=="});
            WHITEHEADS.put('P', new String[]{"51e6761d-8cf2-494b-b9e8-1c0d70c77ad8", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDAyYzEwYWRjMzFiMWMyMWNjOThlY2Y4MDkyMjVmODdlMjVlNzIzNzhjZjQxN2RiNGJlM2Y2N2U5MWJiMSJ9fX0="});
            WHITEHEADS.put('Q', new String[]{"07ee041b-4d6d-4d2c-8b14-8c527fc0e46b", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDUyNGQyOGM4MmYzNzExYTk3NTAxNDExZWNjM2NiNDY2ODc3NDgzYjEyMmEyNjU2YzhlZWFkZmI4ZDIxIn19fQ=="});
            WHITEHEADS.put('R', new String[]{"29d15adf-36c8-4b19-89a6-1aeff5e8331a", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzhhODFlZmRhZTQ3YmNiNDgwYTI1ZWQ5MWZmNmRlOTc3MmIwN2FlODdjM2M0ZTI3NzcwNWFiYmJkMzQxOSJ9fX0="});
            WHITEHEADS.put('S', new String[]{"5961be5a-b47f-46b1-9776-4d1e1b9d3a9c", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDcxMDEzODQxNjUyODg4OTgxNTU0OGI0NjIzZDI4ZDg2YmJiYWU1NjE5ZDY5Y2Q5ZGJjNWFkNmI0Mzc0NCJ9fX0="});
            WHITEHEADS.put('T', new String[]{"4a8ae907-f4fe-41b1-8562-10ef8cd57636", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNmYjUwZmU3NTU5YmM5OWYxM2M0NzM1NmNjZTk3ZmRhM2FhOTIzNTU3ZmI1YmZiMTdjODI1YWJmNGIxZDE5In19fQ=="});
            WHITEHEADS.put('U', new String[]{"08345212-4e65-43c7-8a74-393381f6eb88", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDY5OTFkY2JhMjVlYWUyNDNlYjVjZWI4MzI1ZjRhYjc4ZjlmMTQxMjdjMzgyZjZjZDQyYzRjNzgwNDJkNGI1In19fQ=="});
            WHITEHEADS.put('V', new String[]{"d431bcd6-7bdf-4c5a-b929-ed5da081babf", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZThkNjgwMTg5OTRmMmYyZjU5ZDllNWYyZTNiYTNkNDZkOGIwYjllYTBlNjg0YmZiYjhlY2M3Yjg2ZWI5MjRjIn19fQ=="});
            WHITEHEADS.put('W', new String[]{"f7dcead1-c5bd-458f-8d1a-5b9c291fa500", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODBjOTQ4M2Y1MWQxMjY3NDMyZTBmMmYzYmFhOGNkOTNlNjViNWVhYzc0ODJiMjdkYmNjZWJhZmI3MjE3NDhiIn19fQ=="});
            WHITEHEADS.put('X', new String[]{"0ba39a4c-083f-4943-a9e3-4dcd5f6f7bf3", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQxYTNjOTY1NjIzNDg1MjdkNTc5OGYyOTE2MDkyODFmNzJlMTZkNjExZjFhNzZjMGZhN2FiZTA0MzY2NSJ9fX0="});
            WHITEHEADS.put('Y', new String[]{"f3e98344-021b-45ed-a815-ee2c209b040b", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2RhOGI2NDczMDUyYWRhMjJlNmNhMzBjNDlmNmRjZTliOTk5MTZlNDIzYWM0ZmM2YjMwMWFkNzMzNjk3ZiJ9fX0="});
            WHITEHEADS.put('Z', new String[]{"3162c17f-71b9-43f1-a412-dc0cea4ffdac", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBkNDRmZWUwMzAzZjZkMzdmYWNhN2U5YzMxNTMwOTU1NmZhM2RmMzc5YmRkNTgyMzE3YWEzNjhhYTg0M2UifX19"});
            WHITEHEADS.put('.', new String[]{"f9bd4a81-b5dd-4e03-96b8-d37215f42b73", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzIzZTYxOWRjYjc1MTFjZGMyNTJhNWRjYTg1NjViMTlkOTUyYWM5ZjgyZDQ2N2U2NmM1MjI0MmY5Y2Q4OGZhIn19fQ=="});
            WHITEHEADS.put('?', new String[]{"a8806521-1911-4885-87ac-5a65755364e2", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmM4ZWExZjUxZjI1M2ZmNTE0MmNhMTFhZTQ1MTkzYTRhZDhjM2FiNWU5YzZlZWM4YmE3YTRmY2I3YmFjNDAifX19"});
        }
    }

    /**
     * It's a menu that shows all the letters of the alphabet, and when you click on one, it closes the
     * menu and makes a guess in the game
     */
    private static class LetterMenu extends AbstractInventoryMenu {

        private static final char[] LETTERS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        private final HangmanGame game;
        private final Block block;

        public LetterMenu(HangmanGame game, Block block) {
            this.game = game;
            this.block = block;
        }

        @Override
        protected List<Button> getButtons() {
            List<Button> buttons = new ArrayList<>();
            IntStream.range(0, LetterMenu.LETTERS.length).forEach(i -> {
                char letter = LetterMenu.LETTERS[i];
                PlayerProfile profile = Bukkit.createProfile(UUID.fromString(Constants.WHITEHEADS.get(letter)[0]));
                profile.getProperties().add(new ProfileProperty("textures", Constants.WHITEHEADS.get(letter)[1]));
                buttons.add(Button.builder().setSlots(i).addAction(ButtonClickType.ANY, p -> {
                    this.close();
                    this.game.makeGuess(this.block, letter, p);
                }).setItem(new ItemBuilder(Material.PLAYER_HEAD).name(String.valueOf(letter)).skull(profile)).build());
            });
            buttons.add(Button.builder().setSlots(26).addAction(ButtonClickType.ANY, p -> this.close()).setItem(new ItemBuilder(Material.BARRIER).name("Sluit menu")).build());
            return buttons;
        }
    }
}
