package eu.amadeos.games.jump.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.collision.Box;
import eu.amadeos.gameengine.input.Gamepad;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.save.SaveFile;
import eu.amadeos.games.jump.bootstrap.JumpModule;
import eu.amadeos.games.jump.domain.Field;
import eu.amadeos.games.jump.domain.Game;
import eu.amadeos.games.jump.domain.Stage;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.repository.Progress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys and buttons and calls the
 * steps itself, so everything happens in order on the test thread.
 */
class JumpGameTest {
    private static final double STEP = JumpGame.STEP;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final Gamepads gamepads = Gamepads.none();
    private Progress progress;
    private JumpGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(JumpModule.class, JumpModule.MESSAGES, Locale.ENGLISH);
        progress = new Progress(new SaveFile(dir.resolve("progress.save")));
        game = new JumpGame(keyboard, gamepads, msg, new JumpSettings(dir.resolve("jump.properties")), progress,
                new Mixer());
    }

    /** Pressed and let go in one step, then a few steps more, so a second tap is not taken for key repeat. */
    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(5);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double seconds) {
        steps((int) Math.round(seconds * JumpGame.UPDATES_PER_SECOND));
    }

    @Test
    void fromTheTitleToTheFirstLevel() {
        assertInstanceOf(JumpGame.TitleScene.class, game.top());
        tap(KeyEvent.VK_SPACE);
        JumpGame.ChoiceScene choice = assertInstanceOf(JumpGame.ChoiceScene.class, game.top());
        tap(KeyEvent.VK_RIGHT);
        assertEquals(1, choice.chosen(), "level 2 is still locked");
        tap(KeyEvent.VK_ENTER);
        JumpGame.PlayScene play = assertInstanceOf(JumpGame.PlayScene.class, game.top());
        assertEquals(1, play.game().level().number());
        tap(KeyEvent.VK_ESCAPE);
        assertInstanceOf(JumpGame.PauseScene.class, game.top());
        tap(KeyEvent.VK_ESCAPE);
        assertInstanceOf(JumpGame.ChoiceScene.class, game.top());
    }

    // --8<-- [start:test-camera]
    @Test
    void theCameraFollowsOnlyOnceThePlayerLeavesTheDeadZone() {
        JumpGame.PlayScene play = game.play(1);
        Stage.withoutEnemies(play.game());
        Stage.put(play.game(), 560, 320, 0);           // a flat stretch of the meadow
        Body body = play.game().player().body();
        play.camera().jumpTo(body.centerX(), body.centerY());
        double start = play.camera().x();
        keyboard.pressed(KeyEvent.VK_RIGHT, now[0]);
        seconds(0.1);
        assertEquals(start, play.camera().x(), "a few steps stay inside the dead zone");
        seconds(1.7);
        double player = play.game().player().body().centerX();
        double middle = play.camera().x() + Field.WIDTH / 2;
        assertTrue(play.camera().x() > start + 100, "the camera scrolled along");
        // At a steady run the camera trails the dead zone's edge by the speed over its rate, -ln(1 - catchUp).
        double lag = Field.RUN_SPEED / -Math.log(1 - JumpGame.CATCH_UP);
        assertEquals(JumpGame.DEAD_ZONE_WIDTH / 2 + lag, player - middle, 1);
    }
    // --8<-- [end:test-camera]

    @Test
    void aJumpKeyHeldLongerJumpsHigher() {
        double shortHop = highest(1);
        double fullJump = highest(40);
        assertTrue(fullJump > shortHop + Field.TILE, shortHop + " " + fullJump);
    }

    /** The highest the feet come, with the jump key held for {@code steps} steps. */
    private double highest(int steps) {
        JumpGame.PlayScene play = game.play(1);
        Game level = play.game();
        double ground = level.player().body().bottom();
        keyboard.pressed(KeyEvent.VK_SPACE, now[0]);
        steps(steps);
        keyboard.released(KeyEvent.VK_SPACE, now[0]);
        double top = ground;
        for (int i = 0; i < 120; i++) {
            steps(1);
            top = Math.min(top, level.player().body().bottom());
        }
        steps(10);
        return ground - top;
    }

    // --8<-- [start:test-goal]
    @Test
    void theGoalSavesTheTimeAndOpensTheNextLevel() {
        JumpGame.PlayScene play = game.play(1);
        Box goal = play.game().level().goal();
        Stage.put(play.game(), goal.x() - 24, goal.bottom(), 0);
        keyboard.pressed(KeyEvent.VK_RIGHT, now[0]);
        seconds(0.5);
        keyboard.released(KeyEvent.VK_RIGHT, now[0]);
        assertEquals(Game.Phase.CLEARED, play.game().phase());
        seconds(JumpGame.CHEER_SECONDS);
        assertInstanceOf(JumpGame.ClearedScene.class, game.top());
        assertEquals(2, progress.open());
        assertTrue(progress.best(1).isPresent());
        assertTrue(play.record().bestTime() && play.record().opened());
        seconds(1);
        tap(KeyEvent.VK_ENTER);
        JumpGame.PlayScene next = assertInstanceOf(JumpGame.PlayScene.class, game.top());
        assertEquals(2, next.game().level().number());
    }
    // --8<-- [end:test-goal]

    @Test
    void aGamepadPlaysToo() {
        Gamepads pads = Gamepads.none();
        Gamepad pad = pads.pad(0);
        game = new JumpGame(keyboard, pads, Messages.load(JumpModule.class, JumpModule.MESSAGES, Locale.ENGLISH),
                new JumpSettings(dir.resolve("jump.properties")), progress, new Mixer());
        pad.report(Gamepad.bits(Gamepad.Button.A), 0, 0);
        pad.report(0, 0, 0);
        steps(1);
        assertInstanceOf(JumpGame.ChoiceScene.class, game.top());
        pad.report(Gamepad.bits(Gamepad.Button.START), 0, 0);
        pad.report(0, 0, 0);
        steps(1);
        JumpGame.PlayScene play = assertInstanceOf(JumpGame.PlayScene.class, game.top());
        double x = play.game().player().body().x;
        pad.report(Gamepad.bits(Gamepad.Button.RIGHT), 0, 0);
        seconds(0.5);
        assertTrue(play.game().player().body().x > x + 30);
    }

    @Test
    void everySceneAndLevelCanBeDrawn() {
        BufferedImage image = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(2, 2);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        for (int level = 1; level <= 3; level++) {
            game.play(level);
            seconds(1);
            game.render(g, 0.5);
            game.renderOverlay(g, 0.5);
        }
        game.chooseLevel();
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        g.dispose();
        assertTrue(image.getRGB(640, 400) != 0, "something was drawn");
    }
}
