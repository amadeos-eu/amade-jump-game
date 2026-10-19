package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.data.TextData.DataException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    private static final double STEP = 1.0 / 120;

    /** Steps the game with the same input; returns all events. */
    private static List<Event> run(Game game, Input input, double seconds) {
        List<Event> events = new ArrayList<>();
        for (double t = 0; t < seconds - 1e-9; t += STEP) {
            game.update(STEP, input);
            events.addAll(game.events());
        }
        return events;
    }

    private static <T extends Event> List<T> of(List<Event> events, Class<T> kind) {
        return events.stream().filter(kind::isInstance).map(kind::cast).toList();
    }

    @Test
    void theRealLevelsAreRead() {
        Level meadow = Level.load(1);
        assertEquals("Meadow", meadow.name());
        assertEquals("meadow", meadow.theme());
        assertTrue(meadow.coins() > 40);
        assertEquals(2, meadow.checkpoints().size());
        assertEquals(3, Level.FILES.size());
    }

    // --8<-- [start:test-coins]
    @Test
    void runningThroughCoinsTakesThem() {
        Game game = new Game(Levels.of(
                "...........",
                ".P.ooo...G.",
                "###########"));
        List<Event> events = run(game, Input.run(1), 1.5);
        assertEquals(3, game.coins());
        assertEquals(List.of(3, 4, 5), of(events, Event.CoinTaken.class).stream().map(Event.CoinTaken::col).toList());
        assertEquals(Tile.AIR, game.tiles().get(4, 1), "the coin is gone from the game's tiles");
        assertEquals(Tile.COIN, game.level().tile(4, 1), "but not from the level's");
    }
    // --8<-- [end:test-coins]

    // --8<-- [start:test-stomp]
    @Test
    void aBeetleIsStompedFromAboveAndDeadlyFromTheSide() {
        Level level = Levels.of(
                "..........",
                "..........",
                "..........",
                ".P.....b.G",
                "##########");
        Game side = new Game(level);
        List<Event> events = run(side, Input.run(1), 2);
        assertEquals(Event.Cause.ENEMY, of(events, Event.Died.class).getFirst().cause(), "walked into it");

        Game above = new Game(level);
        above.put(7 * 16 + 4, 16, 0);                // high above the beetle, falling onto it
        events = run(above, Input.NONE, 1);
        assertEquals(1, of(events, Event.Stomped.class).size());
        assertTrue(of(events, Event.Died.class).isEmpty());
        assertFalse(above.enemies().getFirst().alive());
    }
    // --8<-- [end:test-stomp]

    @Test
    void holdingJumpBouncesHigherOffAStompedBeetle() {
        Level level = Levels.of(
                "..........",
                "..........",
                "..........",
                "..........",
                "..........",
                ".P.....b.G",
                "##########");
        double[] highest = new double[2];
        for (int held = 0; held < 2; held++) {
            Game game = new Game(level);
            game.put(7 * 16 + 4, 16, 0);
            Input input = new Input(0, false, held == 1, false);
            run(game, input, 0.6);                  // falls onto the beetle and bounces
            double top = game.player().body().y;
            for (int i = 0; i < 60; i++) {
                game.update(STEP, input);
                top = Math.min(top, game.player().body().y);
            }
            highest[held] = top;
        }
        assertTrue(highest[1] < highest[0] - 30, "held: " + highest[1] + ", not held: " + highest[0]);
    }

    // --8<-- [start:test-respawn]
    @Test
    void afterAFallThePlayerStartsAgainAtTheLastCheckpoint() {
        Game game = new Game(Levels.of(
                "...........",
                ".P..C..^..G",
                "###########"));
        List<Event> events = run(game, Input.run(1), 1.2);
        assertEquals(1, of(events, Event.CheckpointReached.class).size());
        assertEquals(Event.Cause.SPIKES, of(events, Event.Died.class).getFirst().cause());
        assertEquals(Game.Phase.DYING, game.phase());
        events = run(game, Input.NONE, Field.DYING_SECONDS);
        Event.Respawned back = of(events, Event.Respawned.class).getFirst();
        assertEquals(4 * 16 + 8, back.x(), 1e-9, "at the flag");
        assertEquals(1, game.deaths());
        assertEquals(Game.Phase.PLAYING, game.phase());
    }

    @Test
    void fallingOutOfTheLevelIsAFall() {
        Game game = new Game(Levels.of(
                "........",
                ".P......",
                "###...#G",
                "......##"));
        List<Event> events = run(game, Input.run(1), 2);
        assertEquals(Event.Cause.FALL, of(events, Event.Died.class).getFirst().cause());
    }
    // --8<-- [end:test-respawn]

    @Test
    void waterIsDeadly() {
        Game game = new Game(Levels.of(
                "........",
                ".P....G.",
                "###~~###"));
        List<Event> events = run(game, Input.run(1), 2);
        assertEquals(Event.Cause.WATER, of(events, Event.Died.class).getFirst().cause());
    }

    // --8<-- [start:test-goal]
    @Test
    void theGoalEndsTheLevelWithItsResult() {
        Game game = new Game(Levels.of(
                ".........",
                ".P.o.o.G.",
                "#########"));
        List<Event> events = run(game, Input.run(1), 2);
        LevelResult result = of(events, Event.Cleared.class).getFirst().result();
        assertEquals(2, result.coins());
        assertEquals(2, result.totalCoins());
        assertTrue(result.allCoins());
        assertEquals(0, result.deaths());
        assertTrue(result.seconds() > 0.5 && result.seconds() < 1, "about 90 units at up to 140 a second");
        double stopped = game.time();
        run(game, Input.run(1), 1);
        assertEquals(stopped, game.time(), "the clock stops at the goal");
    }
    // --8<-- [end:test-goal]

    @Test
    void aSpringThrowsThePlayerNineTilesHigh() {
        String[] rows = new String[16];
        for (int i = 0; i < 14; i++) rows[i] = "......";
        rows[14] = ".PJ..G";
        rows[15] = "######";
        Game game = new Game(Levels.of(rows));
        game.put(2 * 16 + 8, 6 * 16, 0);              // falling onto the spring
        List<Event> events = new ArrayList<>();
        double top = Double.MAX_VALUE;
        for (int i = 0; i < 240 && of(events, Event.Sprung.class).size() < 2; i++) {
            game.update(STEP, Input.NONE);
            events.addAll(game.events());
            if (!of(events, Event.Sprung.class).isEmpty()) top = Math.min(top, game.player().body().bottom());
        }
        assertEquals(2, of(events, Event.Sprung.class).size(), "up, and down onto it again");
        double springTop = 15 * 16 - 8;
        assertEquals(9 * 16, springTop - top, 4, "nine tiles, not cut by letting go");
    }

    @Test
    void sameInputsSameGame() {
        Level level = Level.load(1);
        Game a = new Game(level);
        Game b = new Game(level);
        for (int i = 0; i < 1200; i++) {
            Input input = new Input(1, i % 90 == 0, i % 90 < 50, false);
            a.update(STEP, input);
            b.update(STEP, input);
        }
        assertEquals(a.player().body().x, b.player().body().x);
        assertEquals(a.coins(), b.coins());
        assertEquals(a.deaths(), b.deaths());
    }

    @Test
    void aLevelNeedsAStartAndAGoalAndKnownClasses() {
        var noGoal = assertThrows(DataException.class, () -> Levels.of(".P.", "###"));
        assertEquals("test.tmx: no object of the class goal", noGoal.getMessage());
    }
}
