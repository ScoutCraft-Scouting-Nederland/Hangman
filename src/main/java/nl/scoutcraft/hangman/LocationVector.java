package nl.scoutcraft.hangman;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * It's a class that stores a bunch of vectors that represent the relative locations of blocks to a
 * given origin
 */
public class LocationVector {

    private Vector[] locationVectors;
    protected Location origin;

    public LocationVector(Location origin) {
        this.locationVectors = new Vector[0];
        this.origin = origin;
    }

    public LocationVector(Location origin, JsonObject jsonObject) {
        this.deserialize(jsonObject);
        this.origin = origin;
    }

    /**
     * It adds a new vector to the array of vectors
     * 
     * @param location The location to add to the list of locations.
     */
    public void addLocationVector(Location location) {
        Vector[] holder = this.locationVectors;
        this.locationVectors = new Vector[this.locationVectors.length + 1];
        System.arraycopy(holder, 0, this.locationVectors, 0, holder.length);
        this.locationVectors[this.locationVectors.length - 1] = new Vector(location.getX() - this.origin.getX(), location.getY() - this.origin.getY(), location.getZ() - this.origin.getZ());
    }

    public Vector[] getLocationVectors() {
        return this.locationVectors;
    }

    /**
     * It returns an array of blocks that are the same as the blocks in the schematic
     * 
     * @return An array of blocks.
     */
    public Block[] getBlocks() {
        Block[] blocks = new Block[this.getLocationVectors().length];
        for (int i = 0; i < this.getLocationVectors().length; i++) {
            blocks[i] = this.origin.clone().add(this.getLocationVectors()[i].getX(), this.getLocationVectors()[i].getY(), this.getLocationVectors()[i].getZ()).getBlock();
        }
        return blocks;
    }

    /**
     * For each location vector, get the block at the origin plus the location vector, and then accept
     * the block into the consumer.
     * 
     * @param consumer The consumer to be used.
     */
    public void forEachLocationVectorBlock(Consumer<Block> consumer) {
        Arrays.stream(this.locationVectors).map(locationVector -> this.origin.clone().add(locationVector.getX(), locationVector.getY(), locationVector.getZ()).getBlock()).forEach(consumer::accept);
    }

   /**
    * It takes a `Location` object, converts it to a `JsonObject`, and returns it
    * 
    * @return A JsonObject
    */
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("origin", JsonUtil.locationToString(this.origin));
        JsonArray jsonLocationVectorArray = new JsonArray();
        Arrays.stream(this.locationVectors).map(JsonUtil::vectorToString).forEach(jsonLocationVectorArray::add);
        jsonObject.add("locationVectors", jsonLocationVectorArray);
        return jsonObject;
    }

    /**
     * It takes a JsonObject, and it sets the origin and locationVectors fields to the values of the
     * "origin" and "locationVectors" fields in the JsonObject
     * 
     * @param jsonObject The JsonObject that contains the data to deserialize.
     */
    public void deserialize(JsonObject jsonObject) {
        this.origin = JsonUtil.stringToLocation(jsonObject.get("origin").getAsString());
        JsonArray locationVectorsJsonArray = jsonObject.getAsJsonArray("locationVectors");
        this.locationVectors = new Vector[locationVectorsJsonArray.size()];
        IntStream.range(0, locationVectorsJsonArray.size()).forEach(i -> this.locationVectors[i] = JsonUtil.stringToVector(locationVectorsJsonArray.get(i).getAsString()));
    }
}
