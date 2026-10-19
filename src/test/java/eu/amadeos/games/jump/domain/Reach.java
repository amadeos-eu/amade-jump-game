package eu.amadeos.games.jump.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where in a level the player can get to, found by trying: from every place the player can stand, every
 * kind of jump (standing or running, to the left, straight up or to the right, short or full) and every
 * step off an edge is played with the game's own rules, and wherever the player lands is a new place to
 * try from: a breadth-first search, as in Blast's board check, but over jumps instead of tiles. Enemies
 * are left out: they can be stomped or waited for, the tiles cannot.
 *
 * <p>A level designer's test: if the goal cannot be reached, or a coin cannot be fetched, the level is
 * broken, whatever it looks like.
 */
// --8<-- [start:reach]
final class Reach {
    private static final double STEP = 1.0 / 120;

    /**
     * A place to stand: the column of the player's middle, and the row of the tile under the feet. Tries
     * start where the player first stood in it, {@code x}: the middle of the column may be off the edge.
     */
    record Spot(int col, int row) {
    }

    /**
     * A way to leave a place: the speed already there, where to steer and from when on, how long to hold
     * jump (0: no jump). Steering late is how a spring is ridden: straight up first, then to the side.
     */
    private record Try(double vx, int move, double steerFrom, double hold) {
    }

    private static final List<Try> TRIES = tries();

    final Map<Spot, Double> spots = new HashMap<>();
    final Set<Long> coins = new HashSet<>();
    final Level level;
    boolean goal;

    private Reach(Level level) {
        this.level = level;
    }

    static Reach of(Level level) {
        Reach reach = new Reach(level);
        Game first = new Game(level);
        Spot start = spot(first);
        Deque<Spot> queue = new ArrayDeque<>();
        reach.spots.put(start, first.player().body().centerX());
        queue.add(start);
        while (!queue.isEmpty()) {
            Spot from = queue.poll();
            for (Try t : TRIES) {
                Game game = reach.attempt(from, t);
                if (game == null) continue;
                Spot to = spot(game);
                if (reach.spots.putIfAbsent(to, game.player().body().centerX()) == null) queue.add(to);
            }
        }
        return reach;
    }

    /** Plays one try; the game with the player standing somewhere afterwards, or null if lost. */
    private Game attempt(Spot from, Try t) {
        Game game = new Game(level);
        game.withoutEnemies();
        game.put(spots.get(from), from.row() * Field.TILE, t.vx());
        if (!game.player().onGround()) return null;
        boolean airborne = false;
        for (double time = 0; time < 3; time += STEP) {
            boolean held = time < t.hold();
            int move = time >= t.steerFrom() ? t.move() : 0;
            game.update(STEP, new Input(move, t.hold() > 0 && time == 0, held, false));
            for (Event event : game.events()) {
                if (event instanceof Event.CoinTaken coin) coins.add(((long) coin.col() << 32) | coin.row());
            }
            if (game.phase() == Game.Phase.CLEARED) goal = true;
            if (game.phase() != Game.Phase.PLAYING) return null;
            if (!game.player().onGround()) airborne = true;
            else if (airborne) return game;
            else if (t.hold() == 0 && time > 0.12) return game;      // a step along the ground
        }
        return null;
    }
    // --8<-- [end:reach]

    private static Spot spot(Game game) {
        var body = game.player().body();
        return new Spot((int) Math.floor(body.centerX() / Field.TILE), (int) Math.round(body.bottom() / Field.TILE));
    }

    private static List<Try> tries() {
        List<Try> tries = new ArrayList<>();
        for (double vx : new double[]{-Field.RUN_SPEED, 0, Field.RUN_SPEED}) {
            for (int move = -1; move <= 1; move++) {
                for (double hold : new double[]{0, 0.12, 1}) {
                    if (hold == 0 && move == 0) continue;
                    tries.add(new Try(vx, move, 0, hold));
                    if (move != 0 && hold > 0 && vx == 0) tries.add(new Try(vx, move, 0.6, hold));
                }
            }
        }
        return tries;
    }

    /** The level as text, a letter per tile, with the reached places marked: for looking at a failure. */
    String picture() {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < level.height(); row++) {
            for (int col = 0; col < level.width(); col++) {
                char c = switch (level.tile(col, row)) {
                    case SOLID -> '#';
                    case PLATFORM -> '=';
                    case SPIKES -> '^';
                    case WATER -> '~';
                    case COIN -> coins.contains(((long) col << 32) | row) ? 'o' : '?';
                    case AIR -> '.';
                };
                if (spots.containsKey(new Spot(col, row + 1))) c = '*';
                text.append(c);
            }
            text.append('\n');
        }
        return text.toString();
    }
}
