package nl.scoutcraft.hangman;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * It's a class that represents a cuboid (a 3D rectangle) in Minecraft
 */
public class Cuboid {

    private Location locA, locB; // A is smallest, B is biggest

    // It's a constructor that takes two locations as parameters. It checks if the locations are in the
    // same world, and if not, it throws an exception.
    public Cuboid(Location locA, Location locB) throws InstantiationException {
        if (locA.getWorld() != locB.getWorld())
            throw new InstantiationException();

        this.locA = locA;
        this.locB = locB;
        this.sortLocations();
    }

    /**
     * It sorts the two locations so that the first location is the smallest and the second location is
     * the largest
     */
    private void sortLocations() {
        double AX = this.locA.getX(), AY = this.locA.getY(), AZ = this.locA.getZ();
        double BX = this.locB.getX(), BY = this.locB.getY(), BZ = this.locB.getZ();

        this.locA = new Location(locA.getWorld(), (Math.min(AX, BX)), (Math.min(AY, BY)), (Math.min(AZ, BZ)));
        this.locB = new Location(locA.getWorld(), (Math.max(AX, BX)), (Math.max(AY, BY)), (Math.max(AZ, BZ)));
    }

    // It's a constructor that takes no parameters. It's used to create a new Cuboid object without
    //         specifying the locations.
    public Cuboid() {
    }

   /**
    * This function returns a clone of the location of the first point of the line.
    * 
    * @return A clone of the location of the first point.
    */
    public Location getLocationA() {
        return this.locA.clone();
    }

    /**
     * This function returns a copy of the location stored in the variable locB.
     * 
     * @return The location of the second point.
     */
    public Location getLocationB() {
        return this.locB.clone();
    }

    /**
    * This function returns a block at the specified coordinates.
    * 
    * @param x The x coordinate of the block
    * @param y The y coordinate of the block
    * @param z The z coordinate of the block.
    * @return A block at the given coordinates.
    */
    public Block getBlockAt(int x, int y, int z) {
        return locA.getWorld().getBlockAt(x, y, z);
    }

    /**
     * It converts the object into a JsonObject
     * 
     * @return A JsonObject
     */
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("locA", JsonUtil.locationToString(this.locA));
        jsonObject.addProperty("locB", JsonUtil.locationToString(this.locB));
        return jsonObject;
    }

    /**
     * This function takes a JsonObject and uses it to set the values of the fields of this object.
     * 
     * @param jsonObject The JsonObject that contains the data to deserialize.
     */
    public void deserialize(JsonObject jsonObject) {
        this.locA = JsonUtil.stringToLocation(jsonObject.get("locA").getAsString());
        this.locB = JsonUtil.stringToLocation(jsonObject.get("locB").getAsString());
    }
}
