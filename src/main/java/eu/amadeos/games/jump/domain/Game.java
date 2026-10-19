package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.collision.Box;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.physics.Solidity;
import eu.amadeos.gameengine.physics.TileCollider;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One level being played. Plain Java without a clock: {@link #update} advances everything by a step, with
 * what the player wants in it, and the {@link #events()} tell what happened. No chance anywhere: the same
 * inputs always play the same level.
 *
 * <p>There are no lives. A player who falls into spikes, water, a pit or an enemy tumbles out of the
 * picture and starts again at the last checkpoint reached; the clock goes on, and the falls are counted.
 * What was collected stays collected.
 */
public final class Game {
    /** Running and jumping; tumbling out after a fall; the goal is reached. */
    public enum Phase { PLAYING, DYING, CLEARED }

    private final Level level;
    private final TileMap<Tile> tiles;
    private final TileCollider world;
    private final Player player = new Player();
    private final List<Enemy> enemies = new ArrayList<>();
    /** Seconds each spring stays pressed down. */
    private final double[] springs;
    private final boolean[] reached;
    private final List<Event> events = new ArrayList<>();
    private Phase phase = Phase.PLAYING;
    private double phaseTime;
    private double respawnX;
    private double respawnFeet;
    private double time;
    private int coins;
    private int deaths;
    private LevelResult result;

    public Game(Level level) {
        this.level = level;
        this.tiles = level.tiles();
        // --8<-- [start:world]
        // Walls left and right of the level, air above and below it: a pit is a way out of the world.
        this.world = new TileCollider((col, row) -> {
            if (col < 0 || col >= tiles.width()) return Solidity.SOLID;
            if (row < 0 || row >= tiles.height()) return Solidity.EMPTY;
            return tiles.get(col, row).solidity;
        }, Field.TILE, Field.TILE);
        // --8<-- [end:world]
        for (Level.Spawn spawn : level.enemies()) enemies.add(new Enemy(spawn));
        springs = new double[level.springs().size()];
        reached = new boolean[level.checkpoints().size()];
        respawnX = level.start().centerX();
        respawnFeet = level.start().bottom();
        player.place(respawnX, respawnFeet);
        world.move(player.body, 0, false);      // stands on the ground from the first step
    }

    // --8<-- [start:update]
    /** One step of {@code seconds} with what the player wants in it. */
    public void update(double seconds, Input input) {
        events.clear();
        phaseTime += seconds;
        for (int i = 0; i < springs.length; i++) springs[i] = Math.max(0, springs[i] - seconds);
        double bottom = tiles.height() * Field.TILE + Field.FALL_OUT;
        for (Enemy enemy : enemies) enemy.update(seconds, world, bottom);
        switch (phase) {
            case PLAYING -> play(seconds, input);
            case DYING -> tumble(seconds);
            case CLEARED -> player.body.remember();
        }
    }

    private void play(double seconds, Input input) {
        time += seconds;
        Body body = player.body;
        if (player.jump.update(body, input.jump(), input.held(), seconds)) {
            events.add(new Event.Jumped(body.centerX(), body.bottom()));
        }
        player.run(input.move(), seconds);
        body.fall(Field.GRAVITY, Field.MAX_FALL, seconds);
        double falling = body.vy;
        boolean flying = !body.onGround;
        world.move(body, seconds, input.down());
        if (flying && body.onGround) events.add(new Event.Landed(body.centerX(), body.bottom(), falling));
        springs(falling);
        if (touchTiles() || enemies(falling)) return;
        checkpoints();
        if (body.overlaps(asBody(level.goal()))) clear();
        else if (body.y > tiles.height() * Field.TILE + Field.FALL_OUT) die(Event.Cause.FALL);
    }
    // --8<-- [end:update]

    // --8<-- [start:touch]
    /** Coins, spikes and water: what lies in the tiles the player overlaps. True if it was deadly. */
    private boolean touchTiles() {
        Body body = player.body;
        Event.Cause[] death = {null};
        world.forEachCell(body, (col, row) -> {
            if (!tiles.contains(col, row) || death[0] != null) return;
            double x = col * Field.TILE;
            double y = row * Field.TILE;
            switch (tiles.get(col, row)) {
                case COIN -> {
                    if (overlaps(body, x + 3, y + 3, 10, 10)) {
                        tiles.set(col, row, Tile.AIR);
                        coins++;
                        events.add(new Event.CoinTaken(col, row, coins));
                    }
                }
                // The points of the spikes are the lower half of the tile; water starts below its waves.
                case SPIKES -> {
                    if (overlaps(body, x + 2, y + 8, 12, 8)) death[0] = Event.Cause.SPIKES;
                }
                case WATER -> {
                    if (overlaps(body, x, y + 6, 16, 10)) death[0] = Event.Cause.WATER;
                }
                default -> {
                }
            }
        });
        if (death[0] == null) return false;
        die(death[0]);
        return true;
    }
    // --8<-- [end:touch]

    // --8<-- [start:stomp]
    /**
     * Enemies: whoever comes down onto one from above, with the feet no lower than a little below its top
     * in the step before, stomps it and bounces off; whoever touches one in any other way is lost.
     */
    private boolean enemies(double falling) {
        Body body = player.body;
        for (Enemy enemy : enemies) {
            if (!enemy.alive() || !body.overlaps(enemy.body)) continue;
            double feetBefore = body.previousY() + body.height;
            if (falling > 0 && feetBefore <= enemy.body.previousY() + Field.STOMP_MARGIN) {
                enemy.squash();
                body.y = enemy.body.y - body.height;
                player.jump.launch(body, Field.STOMP_SPEED, true);
                events.add(new Event.Stomped(enemy));
            } else {
                die(Event.Cause.ENEMY);
                return true;
            }
        }
        return false;
    }
    // --8<-- [end:stomp]

    /** A spring throws whoever lands on it nine tiles up, whether jump is held or not. */
    private void springs(double falling) {
        Body body = player.body;
        for (int i = 0; i < springs.length; i++) {
            Box spring = spring(i);
            double feetBefore = body.previousY() + body.height;
            if (falling > 0 && feetBefore <= spring.y() + 2 && overlaps(body, spring.x(), spring.y(), spring.width(),
                    spring.height())) {
                body.y = spring.y() - body.height;
                player.jump.launch(body, Field.SPRING_SPEED, false);
                springs[i] = Field.SPRING_SECONDS;
                events.add(new Event.Sprung(spring.centerX(), spring.y()));
            }
        }
    }

    private void checkpoints() {
        Body body = player.body;
        List<Box> flags = level.checkpoints();
        for (int i = 0; i < flags.size(); i++) {
            Box flag = flags.get(i);
            if (reached[i] || !body.overlaps(asBody(flag))) continue;
            reached[i] = true;
            respawnX = flag.centerX();
            respawnFeet = flag.bottom();
            events.add(new Event.CheckpointReached(i, flag.centerX(), flag.y()));
        }
    }

    private void clear() {
        phase = Phase.CLEARED;
        phaseTime = 0;
        player.body.vx = 0;
        result = new LevelResult(level.number(), time, coins, level.coins(), deaths);
        events.add(new Event.Cleared(result));
    }

    private void die(Event.Cause cause) {
        phase = Phase.DYING;
        phaseTime = 0;
        deaths++;
        player.die();
        events.add(new Event.Died(player.body.centerX(), player.body.centerY(), cause));
    }

    /** Thrown up and falling through everything, until the level starts again at the last checkpoint. */
    private void tumble(double seconds) {
        time += seconds;
        Body body = player.body;
        body.remember();
        body.fall(Field.GRAVITY, Field.MAX_FALL, seconds);
        body.y += body.vy * seconds;
        if (phaseTime >= Field.DYING_SECONDS) {
            player.place(respawnX, respawnFeet);
            world.move(body, 0, false);
            phase = Phase.PLAYING;
            phaseTime = 0;
            events.add(new Event.Respawned(respawnX, respawnFeet));
        }
    }

    /** The spring's top surface: half a tile high, standing on the bottom of its object. */
    public Box spring(int index) {
        Box box = level.springs().get(index);
        double height = springs[index] > 0 ? 4 : 8;
        return new Box(box.x(), box.bottom() - height, box.width(), height);
    }

    private static boolean overlaps(Body body, double x, double y, double width, double height) {
        return body.x < x + width && x < body.right() && body.y < y + height && y < body.bottom();
    }

    private static Body asBody(Box box) {
        return new Body(box.x(), box.y(), box.width(), box.height());
    }

    // ---- for tests ---------------------------------------------------------------------------------

    /** Puts the player somewhere at once, running at {@code vx}: to try a jump from there. */
    void put(double x, double feet, double vx) {
        player.place(x, feet);
        player.body.vx = vx;
        world.move(player.body, 0, false);
    }

    /** No enemies: to find out where the player can go at all. */
    void withoutEnemies() {
        enemies.clear();
    }

    // ---- what the drawing and the tests see ------------------------------------------------------

    public Level level() {
        return level;
    }

    /** The tiles as they are now: the coins taken are air. */
    public TileMap<Tile> tiles() {
        return tiles;
    }

    public TileCollider world() {
        return world;
    }

    public Player player() {
        return player;
    }

    public List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    public Phase phase() {
        return phase;
    }

    /** Seconds in the current phase. */
    public double phaseTime() {
        return phaseTime;
    }

    /** Seconds since the level started; the clock stops at the goal. */
    public double time() {
        return time;
    }

    public int coins() {
        return coins;
    }

    public int deaths() {
        return deaths;
    }

    public boolean reached(int checkpoint) {
        return reached[checkpoint];
    }

    /** True while spring {@code index} is pressed down after throwing the player. */
    public boolean pressed(int spring) {
        return springs[spring] > 0;
    }

    /** How the level was finished; null before the goal. */
    public LevelResult result() {
        return result;
    }

    /** What happened in the last step. */
    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }
}
