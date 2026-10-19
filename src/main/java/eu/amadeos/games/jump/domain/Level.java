package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.collision.Box;
import eu.amadeos.gameengine.data.TextData.DataException;
import eu.amadeos.gameengine.tile.TileMap;
import eu.amadeos.gameengine.tiled.TiledMap;
import eu.amadeos.gameengine.tiled.TiledObject;
import eu.amadeos.gameengine.tiled.TiledProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * A level as drawn in Tiled: {@code levelN.tmx} in the folder {@code levels}, with the tileset
 * {@code tiles.tsx}. The map has a tile layer {@code ground}, whose tiles' classes are the rules
 * ({@link Tile}); a layer {@code back} of pictures behind it; and an object layer {@code things} with
 * the player's start, the goal, the checkpoints, the springs and the enemies. The map's properties give
 * the level its name and its look, {@code theme}.
 *
 * <p>Everything here is fixed; a {@link Game} plays on a copy of the tiles, from which it takes the coins.
 */
public final class Level {
    /** The levels of the game, in order. */
    public static final List<String> FILES = List.of("level1.tmx", "level2.tmx", "level3.tmx");
    /** Where the maps and their tilesets lie among the classes. */
    public static final String FOLDER = "/eu/amadeos/games/jump/levels/";

    /** A beetle or a bird where the level designer put it, with what its object says. */
    public record Spawn(Enemy.Kind kind, double x, double y, TiledProperties properties) {
    }

    private final int number;
    private final TiledMap map;
    private final TileMap<Tile> tiles;
    private final String name;
    private final String theme;
    private final Box start;
    private final Box goal;
    private final List<Box> checkpoints;
    private final List<Box> springs;
    private final List<Spawn> enemies;
    private final int coins;

    private Level(int number, TiledMap map, TileMap<Tile> tiles, Box start, Box goal, List<Box> checkpoints,
                  List<Box> springs, List<Spawn> enemies) {
        this.number = number;
        this.map = map;
        this.tiles = tiles;
        this.name = map.properties().string("name", "Level " + number);
        this.theme = map.properties().string("theme", "meadow");
        this.start = start;
        this.goal = goal;
        this.checkpoints = List.copyOf(checkpoints);
        this.springs = List.copyOf(springs);
        this.enemies = List.copyOf(enemies);
        this.coins = tiles.count(t -> t == Tile.COIN);
    }

    /** Level {@code number}, counted from 1. */
    public static Level load(int number) {
        return read(number, TiledMap.load(Level.class, FOLDER + FILES.get(number - 1)));
    }

    // --8<-- [start:read]
    /** The rules' view of a map: its ground as tiles, its things by their class. */
    public static Level read(int number, TiledMap map) {
        TileMap<Tile> tiles = map.tiles("ground", gid -> Tile.of(map, gid));
        Box start = null;
        Box goal = null;
        List<Box> checkpoints = new ArrayList<>();
        List<Box> springs = new ArrayList<>();
        List<Spawn> enemies = new ArrayList<>();
        for (TiledObject thing : map.objects("things")) {
            Box box = new Box(thing.x(), thing.y(), Math.max(1, thing.width()), Math.max(1, thing.height()));
            switch (thing.type()) {
                case "start" -> start = once(map, thing, start, box);
                case "goal" -> goal = once(map, thing, goal, box);
                case "checkpoint" -> checkpoints.add(box);
                case "spring" -> springs.add(box);
                case "beetle" -> enemies.add(new Spawn(Enemy.Kind.BEETLE, box.centerX(), box.bottom(), thing.properties()));
                case "bird" -> enemies.add(new Spawn(Enemy.Kind.BIRD, box.centerX(), box.centerY(), thing.properties()));
                default -> throw error(map, thing, "the class \"" + thing.type()
                        + "\" means nothing to the game; use start, goal, checkpoint, spring, beetle or bird");
            }
        }
        if (start == null) throw new DataException(map.source() + ": no object of the class start");
        if (goal == null) throw new DataException(map.source() + ": no object of the class goal");
        checkpoints.sort((a, b) -> Double.compare(a.x(), b.x()));
        return new Level(number, map, tiles, start, goal, checkpoints, springs, enemies);
    }
    // --8<-- [end:read]

    private static Box once(TiledMap map, TiledObject thing, Box before, Box box) {
        if (before != null) throw error(map, thing, "a second " + thing.type() + "; a level has one");
        return box;
    }

    private static DataException error(TiledMap map, TiledObject thing, String message) {
        return new DataException(map.source() + ": object " + thing.id() + " at (" + (int) thing.x() + ", "
                + (int) thing.y() + "): " + message);
    }

    public int number() {
        return number;
    }

    /** The map as Tiled saved it, for drawing: its layers, tilesets and animations. */
    public TiledMap map() {
        return map;
    }

    /** A new copy of the rules' tiles, with all coins in place. */
    public TileMap<Tile> tiles() {
        return tiles.copy();
    }

    public Tile tile(int col, int row) {
        return tiles.get(col, row);
    }

    public int width() {
        return tiles.width();
    }

    public int height() {
        return tiles.height();
    }

    public String name() {
        return name;
    }

    /** The look: {@code meadow}, {@code cave} or {@code sky}. */
    public String theme() {
        return theme;
    }

    /** The player starts standing on the bottom of this box, in its middle. */
    public Box start() {
        return start;
    }

    public Box goal() {
        return goal;
    }

    /** The checkpoints from left to right. */
    public List<Box> checkpoints() {
        return checkpoints;
    }

    public List<Box> springs() {
        return springs;
    }

    public List<Spawn> enemies() {
        return enemies;
    }

    /** How many coins there are. */
    public int coins() {
        return coins;
    }
}
