package nl.scoutcraft.hangman;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * It's a cuboid that stores the block data of all the blocks inside it
 */
public class BlockVector extends Cuboid {

    private boolean saveAir;
    private int x, y, z;
    private BlockData[] blockData;
    protected Location origin;

    public BlockVector(Location locationA, Location locationB, Location origin) throws InstantiationException {
        super(locationA, locationB);
        this.saveAir = false;
        this.x = this.getLocationB().getBlockX() - this.getLocationA().getBlockX() + 1;
        this.y = this.getLocationB().getBlockY() - this.getLocationA().getBlockY() + 1;
        this.z = this.getLocationB().getBlockZ() - this.getLocationA().getBlockZ() + 1;
        this.origin = origin;
        this.blockData = new BlockData[(this.x) * (this.y) * (this.z)];
        for (int xI = 0; xI < this.x; xI++) {
            for (int yI = 0; yI < this.y; yI++) {
                for (int zI = 0; zI < this.z; zI++) {
                    this.registerBlock(xI, yI, zI, this.getLocationA().add(xI, yI, zI).getBlock());
                }
            }
        }
    }

    // It's a constructor that takes a JsonObject and a Location as parameters. It then calls the
    // deserialize method with the JsonObject as parameter and sets the origin to the Location.
    public BlockVector(Location origin, JsonObject jsonObject) {
        this.deserialize(jsonObject);
        this.origin = origin;
    }

    /**
     * This function takes in the x, y, and z coordinates of a block, and the block itself, and then
     * stores the block's data in the blockData array.
     * 
     * @param x The x coordinate of the block
     * @param y The y-coordinate of the block to register.
     * @param z The z coordinate of the block
     * @param block The block to register
     */
    private void registerBlock(int x, int y, int z, Block block) {
        this.blockData[(x * this.y * this.z) + (y * this.z) + z] = block.getBlockData();
    }

    /**
     * Get the block data at the specified coordinates.
     * 
     * @param x The x coordinate of the block
     * @param y The height of the chunk
     * @param z The z coordinate of the block
     * @return The block data at the given coordinates.
     */
    public BlockData getBlockData(int x, int y, int z) {
        return this.blockData[(x * this.y * this.z) + (y * this.z) + z];
    }

    /**
     * It loops through every block in the structure and calls the consumer function with the block and
     * block data
     * 
     * @param consumer The consumer to use.
     */
    public void forEachBlock(BiConsumer<Block, BlockData> consumer) {
        for (int xI = 0; xI < this.x; xI++) {
            for (int yI = 0; yI < this.y; yI++) {
                for (int zI = 0; zI < this.z; zI++) {
                    consumer.accept(this.getBlockAt(xI, yI, zI), this.getBlockData(xI, yI, zI));
                }
            }
        }
    }

    /**
     * For each block in the schematic, set the block at the origin to the block data of the schematic
     * block.
     */
    public void build() {
        this.forEachBlock((b, bd) -> {
            if (bd != null)
                b.getRelative(this.origin.getBlockX(), this.origin.getBlockY(), this.origin.getBlockZ()).setBlockData(bd);
        });
    }

    /**
     * It takes the data from the class and puts it into a JsonObject
     * 
     * @return A JsonObject
     */
    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("saveAir", this.saveAir);
        jsonObject.addProperty("x", this.x);
        jsonObject.addProperty("y", this.y);
        jsonObject.addProperty("z", this.z);
        jsonObject.addProperty("origin", JsonUtil.locationToString(this.origin));
        JsonArray jsonBlockDataArray = new JsonArray();
        Arrays.stream(this.blockData).map(JsonUtil::blockDataToString).forEach(jsonBlockDataArray::add);
        jsonObject.add("blockData", jsonBlockDataArray);
        return jsonObject;
    }

    /**
     * It takes a JsonObject, and uses it to set the values of the fields of the object
     * 
     * @param jsonObject The JsonObject that contains the data to deserialize.
     */
    @Override
    public void deserialize(JsonObject jsonObject) {
        this.saveAir = jsonObject.get("saveAir").getAsBoolean();
        this.x = jsonObject.get("x").getAsInt();
        this.y = jsonObject.get("y").getAsInt();
        this.z = jsonObject.get("z").getAsInt();
        this.origin = JsonUtil.stringToLocation(jsonObject.get("origin").getAsString());
        JsonArray blockDataJsonArray = jsonObject.getAsJsonArray("blockData");
        this.blockData = new BlockData[blockDataJsonArray.size()];
        IntStream.range(0, blockDataJsonArray.size()).forEach(i -> this.blockData[i] = JsonUtil.stringToBlockData(blockDataJsonArray.get(i).getAsString()));
        super.deserialize(jsonObject);
    }
}
