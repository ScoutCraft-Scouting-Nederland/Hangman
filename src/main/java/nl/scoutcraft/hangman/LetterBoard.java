package nl.scoutcraft.hangman;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * It's a LocationVector that has a BlockData that is used to initialize the blocks in the
 * LocationVector
 */
public class LetterBoard extends LocationVector {

    private BlockData letterBlockData;

    public LetterBoard(Location origin) throws InstantiationException {
        super(origin);
        this.letterBlockData = JsonUtil.stringToBlockData("minecraft:air");
    }

    public LetterBoard(Location origin, JsonObject jsonObject) {
        super(origin, jsonObject);
    }

    /**
     * This function sets the letter block data to the block data passed in.
     * 
     * @param blockData The block data that the letter block will be set to.
     */
    public void setLetterBlockData(BlockData blockData) {
        this.letterBlockData = blockData;
    }

    /**
     * For each block in the structure, if it's not a player head, set it to the letter block data.
     */
    public void initializeLetterBlocks() {
        super.forEachLocationVectorBlock((b) -> {
            if (b.getType() != Material.PLAYER_HEAD)
                b.setBlockData(this.letterBlockData);
        });
    }

    /**
     * If the block is null, return false, otherwise return true if the block is in the list of blocks.
     * 
     * @param block The block to check
     * @return A boolean value.
     */
    public boolean isLetter(Block block) {
        if (block == null)
            return false;
        return Arrays.asList(this.getBlocks()).contains(block);
    }

    /**
     * It creates a string of spaces the same length as the word, and then calls the showLetter
     * function on each letter of the word, passing in the string of spaces and the index of the letter
     */
    public void reset() {
        String dummy = new String(new char[this.getLocationVectors().length]).replace('\0', ' ');
        IntStream.range(0, this.getLocationVectors().length).forEach(i -> this.showLetter(dummy, i, false));
    }

    /**
     * It takes a word, an index, and a boolean, and if the boolean is true, it shows the letter at the
     * index of the word, otherwise it shows a question mark
     * 
     * @param word The word that the player is trying to guess.
     * @param index The index of the letter in the word.
     * @param show Whether or not to show the letter.
     */
    public void showLetter(String word, int index, boolean show) {
        Block block = this.getBlocks()[index];
        if (!(block.getState() instanceof Skull))
            this.initializeLetterBlocks();
        Skull skullState = (Skull) block.getState();
        char letter = show ? word.charAt(index) : '?';
        PlayerProfile profile = Bukkit
                .createProfile(UUID.fromString(HangmanGame.Constants.WHITEHEADS.get(Character.toUpperCase(letter))[0]));
        profile.getProperties()
                .add(new ProfileProperty("textures", HangmanGame.Constants.WHITEHEADS.get(Character.toUpperCase(letter))[1]));
        skullState.setPlayerProfile(profile);
        skullState.update();
    }

    /**
     * This function takes a JsonObject and sets the letterBlockData variable to the value of the
     * letterBlockData key in the JsonObject.
     * 
     * @param jsonObject The JsonObject that contains the data for the letter block.
     */
    @Override
    public void deserialize(JsonObject jsonObject) {
        this.letterBlockData = JsonUtil.stringToBlockData(jsonObject.get("letterBlockData").getAsString());
        super.deserialize(jsonObject);
    }

    /**
     * This function serializes the letterBlockData field into a JsonObject.
     * 
     * @return A JsonObject
     */
    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("letterBlockData", JsonUtil.blockDataToString(this.letterBlockData));
        return jsonObject;
    }

}
