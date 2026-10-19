package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.data.TextData.DataException;
import eu.amadeos.gameengine.physics.Solidity;
import eu.amadeos.gameengine.tiled.TiledMap;

/**
 * What a tile of a level's {@code ground} layer is to the rules. The level designer decides it in Tiled,
 * by the class of each tile in the tileset: {@code solid}, {@code platform}, {@code spikes},
 * {@code water} or {@code coin}; a tile without a class is only a picture.
 */
// --8<-- [start:tile]
public enum Tile {
    AIR(Solidity.EMPTY), SOLID(Solidity.SOLID), PLATFORM(Solidity.PLATFORM),
    SPIKES(Solidity.EMPTY), WATER(Solidity.EMPTY), COIN(Solidity.EMPTY);

    public final Solidity solidity;

    Tile(Solidity solidity) {
        this.solidity = solidity;
    }

    /** The tile a gid of the map stands for, by its class; an unknown class is an error naming the map. */
    static Tile of(TiledMap map, int gid) {
        return switch (map.type(gid)) {
            case "" -> AIR;
            case "solid" -> SOLID;
            case "platform" -> PLATFORM;
            case "spikes" -> SPIKES;
            case "water" -> WATER;
            case "coin" -> COIN;
            default -> throw new DataException(map.source() + ": the tile class \"" + map.type(gid)
                    + "\" means nothing to the game; use solid, platform, spikes, water or coin");
        };
    }
}
// --8<-- [end:tile]
