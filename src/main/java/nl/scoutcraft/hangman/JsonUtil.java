package nl.scoutcraft.hangman;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

/**
 * It converts locations, vectors, and block data to strings and vice versa
 */
public class JsonUtil {

    private static final String SEPARATOR = ";";

    /**
     * It takes a location and returns a string that can be used to recreate the location
     * 
     * @param location The location to convert to a string.
     * @return A string of the location.
     */
    public static String locationToString(Location location) {
        return location.getWorld().getUID() + SEPARATOR
                + location.getX() + SEPARATOR
                + location.getY() + SEPARATOR
                + location.getZ() + SEPARATOR
                + location.getYaw() + SEPARATOR
                + location.getPitch();
    }

    /**
     * It takes a string, splits it into an array, and then returns a new location with the values from
     * the array
     * 
     * @param string The string to convert to a location
     * @return A location
     */
    public static Location stringToLocation(String string) {
        String[] values = string.split(SEPARATOR);
        return new Location(Bukkit.getWorld(Main.getInstance().getConfig().getString("world"))
                , Double.parseDouble(values[1])
                , Double.parseDouble(values[2])
                , Double.parseDouble(values[3])
                , Float.parseFloat(values[4])
                , Float.parseFloat(values[5]));
    }

    /**
     * It takes a vector and returns a string that represents that vector
     * 
     * @param vector The vector to convert to a string.
     * @return A string of the vector's x, y, and z values.
     */
    public static String vectorToString(Vector vector) {
        return vector.getX() + SEPARATOR
                + vector.getY() + SEPARATOR
                + vector.getZ();
    }

    /**
     * It takes a string, splits it into an array of strings, and then parses each of those strings
     * into a double, and then returns a new Vector object with those three doubles
     * 
     * @param string The string to convert to a vector.
     * @return A vector
     */
    public static Vector stringToVector(String string) {
        String[] values = string.split(SEPARATOR);
        return new Vector(Double.parseDouble(values[0])
                , Double.parseDouble(values[1])
                , Double.parseDouble(values[2]));
    }

    /**
     * It converts a BlockData object into a string
     * 
     * @param blockData The BlockData to convert to a string.
     * @return The block data as a string.
     */
    public static String blockDataToString(BlockData blockData) {
        if (blockData == null) return "";
        return blockData.getAsString();
    }

    /**
     * It takes a string and returns a BlockData object
     * 
     * @param string The string to convert to a BlockData object.
     * @return A BlockData object
     */
    public static BlockData stringToBlockData(String string) {
        if (string.equals("")) return null;
        return Bukkit.createBlockData(string);
    }
}
