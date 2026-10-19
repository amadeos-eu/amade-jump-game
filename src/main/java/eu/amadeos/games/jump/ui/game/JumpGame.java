package eu.amadeos.games.jump.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.camera.Camera;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Gamepad;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.jump.domain.Event;
import eu.amadeos.games.jump.domain.Field;
import eu.amadeos.games.jump.domain.Game;
import eu.amadeos.games.jump.domain.Input;
import eu.amadeos.games.jump.domain.Level;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.repository.Progress;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The running game: the loop, the controls, the effects and the scenes (the title with the first level
 * scrolling by, the choice of a level, a level being played, the pause and the goal). Everything here runs
 * on the game loop thread, except the {@link Arena} methods, which only post commands to it.
 */
public final class JumpGame implements Arena {
    enum Action { LEFT, RIGHT, DOWN, JUMP, PAUSE, BACK, OK }

    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    static final double STEP = 1.0 / UPDATES_PER_SECOND;
    /** How long the player stands at the goal before the results show. */
    static final double CHEER_SECONDS = 1.2;
    /** The box in the middle of the view in which the player moves without the camera: jumps don't shake it. */
    static final double DEAD_ZONE_WIDTH = 48;
    static final double DEAD_ZONE_HEIGHT = 96;
    /**
     * The part of the distance the camera closes in a second. At 0.9999 it trails a running player by about
     * 15 units beyond the dead zone: smooth, and still most of the view ahead.
     */
    static final double CATCH_UP = 0.9999;
    /** How fast the title scrolls through the first level. */
    static final double TITLE_SPEED = 40;

    private final Keyboard keyboard;
    private final Gamepads gamepads;
    private final Messages msg;
    private final Progress progress;
    private final Mixer mixer;
    private final JumpPainter painter = new JumpPainter();
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private final Controls<Action> controls;
    private final List<Level> levels = new ArrayList<>();
    /** The level chosen last, to start the next choice there. */
    private int chosen = 1;
    private GameLoop loop;

    @Inject
    public JumpGame(Keyboard keyboard, Gamepads gamepads, Messages msg, JumpSettings settings, Progress progress,
                    Mixer mixer) {
        this.keyboard = keyboard;
        this.gamepads = gamepads;
        this.msg = msg;
        this.progress = progress;
        this.mixer = mixer;
        this.hud = new Hud(msg);
        this.effects = new Effects(mixer, painter);
        for (int i = 1; i <= Level.FILES.size(); i++) levels.add(Level.load(i));
        // --8<-- [start:controls]
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT, KeyEvent.VK_A))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT, KeyEvent.VK_D))
                .bind(Action.DOWN, settings.keys("down", KeyEvent.VK_DOWN, KeyEvent.VK_S))
                .bind(Action.JUMP, settings.keys("jump", KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_W))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.BACK, KeyEvent.VK_ESCAPE)
                .bind(Action.OK, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        // One player, any gamepad: whichever is plugged in plays.
        for (Gamepad pad : gamepads.pads()) {
            controls.bind(Action.LEFT, pad, Gamepad.Button.LEFT)
                    .bind(Action.RIGHT, pad, Gamepad.Button.RIGHT)
                    .bind(Action.DOWN, pad, Gamepad.Button.DOWN)
                    .bind(Action.JUMP, pad, Gamepad.Button.A, Gamepad.Button.B)
                    .bind(Action.PAUSE, pad, Gamepad.Button.START)
                    .bind(Action.BACK, pad, Gamepad.Button.BACK)
                    .bind(Action.OK, pad, Gamepad.Button.A, Gamepad.Button.START);
        }
        // --8<-- [end:controls]
        scenes.push(new TitleScene());
    }

    /** Starts the loop; the canvas draws the scenes' worlds and then their overlays. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    void update(double seconds) {
        keyboard.poll();
        gamepads.poll();
        scenes.update(seconds);
    }

    /** The world of all scenes, in logical coordinates. */
    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    /** Texts of all scenes, drawn after the world at the screen's full size. */
    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void chooseLevel() {
        run(() -> scenes.replace(new ChoiceScene()));
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public String gamepads() {
        return msg.format("about.gamepads", gamepads.backend(), gamepads.connected());
    }

    /** On the loop thread when it runs; directly before the loop starts and in tests. */
    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    int sceneCount() {
        return scenes.size();
    }

    Effects effects() {
        return effects;
    }

    /** Plays level {@code number} at once, without the title and the choice. */
    PlayScene play(int number) {
        PlayScene play = new PlayScene(levels.get(number - 1));
        scenes.replace(play);
        return play;
    }

    // ---- the scenes ------------------------------------------------------------------------------

    /** A camera that follows its target at once: the title and the choice scroll it along themselves. */
    private static Camera fixedCamera(Level level) {
        return new Camera(Field.WIDTH, Field.HEIGHT, level.width() * Field.TILE, level.height() * Field.TILE)
                .catchUp(1);
    }

    /** The first level scrolls by, back and forth, with its beetles and birds; no player. */
    final class TitleScene implements Scene {
        private final Game demo = new Game(levels.getFirst());
        private final Camera camera = fixedCamera(levels.getFirst());
        private double time;

        TitleScene() {
            effects.clear();
            camera.jumpTo(Field.WIDTH / 2, Field.HEIGHT / 2);
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.OK) || controls.wasPressed(Action.JUMP)) {
                scenes.replace(new ChoiceScene());
                return;
            }
            time += seconds;
            demo.update(seconds, Input.NONE);
            effects.update(seconds);
            double way = camera.worldWidth() - Field.WIDTH;
            double along = (time * TITLE_SPEED) % (2 * way);
            double left = along < way ? along : 2 * way - along;
            camera.follow(left + Field.WIDTH / 2, Field.HEIGHT / 2, seconds);
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, demo, camera, effects, effects.time(), false, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("window.title"), 160, 84, true, new Color(0, 0, 0, 120));
            hud.text(g, msg.get("window.title"), 156, 84, true, Hud.GOLD);
            hud.text(g, msg.get("title.start"), 230, 18, false, Hud.CHALK);
            hud.text(g, msg.get("title.keys"), 290, 11, false, Hud.CHALK);
            hud.text(g, msg.get("title.screen"), 312, 11, false, Hud.CHALK);
        }
    }

    // --8<-- [start:choice]
    /** Three cards, one for each level; a level opens when the one before it is finished. */
    final class ChoiceScene implements Scene {
        private double time;

        ChoiceScene() {
            effects.clear();
            chosen = Math.min(chosen, progress.open());
        }

        @Override
        public void update(double seconds) {
            time += seconds;
            if (controls.wasPressed(Action.BACK)) {
                scenes.replace(new TitleScene());
                return;
            }
            int before = chosen;
            if (controls.wasPressed(Action.LEFT)) chosen = Math.max(1, chosen - 1);
            if (controls.wasPressed(Action.RIGHT)) chosen = Math.min(Math.min(progress.open(), levels.size()), chosen + 1);
            if (chosen != before) effects.tick();
            if (controls.wasPressed(Action.OK) || controls.wasPressed(Action.JUMP)) {
                scenes.replace(new PlayScene(levels.get(chosen - 1)));
            }
        }
        // --8<-- [end:choice]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.backdrop(g, levels.get(chosen - 1), time * 20, 0);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("select.title"), 70, 30, true, Hud.GOLD);
            double gap = 16;
            double left = (Field.WIDTH - levels.size() * Hud.CARD_WIDTH - (levels.size() - 1) * gap) / 2;
            for (int i = 0; i < levels.size(); i++) {
                int number = i + 1;
                hud.card(g, left + i * (Hud.CARD_WIDTH + gap), 110, number, levels.get(i).coins(),
                        number <= progress.open(), number == chosen, progress);
            }
            hud.text(g, msg.get("select.keys"), 320, 12, false, Hud.CHALK);
        }

        int chosen() {
            return chosen;
        }
    }

    // --8<-- [start:play]
    /** A level. The camera follows the player, but not into the depths after a fall. */
    final class PlayScene implements Scene {
        private final Game game;
        private final Camera camera;
        private double sinceCheckpoint = Double.MAX_VALUE;
        private Progress.Record record;

        PlayScene(Level level) {
            game = new Game(level);
            camera = new Camera(Field.WIDTH, Field.HEIGHT, level.width() * Field.TILE, level.height() * Field.TILE)
                    .deadZone(DEAD_ZONE_WIDTH, DEAD_ZONE_HEIGHT).catchUp(CATCH_UP);
            Body body = game.player().body();
            camera.jumpTo(body.centerX(), body.centerY());
            effects.clear();
            effects.music(level.theme());
        }

        @Override
        public void update(double seconds) {
            if (game.phase() == Game.Phase.CLEARED && game.phaseTime() >= CHEER_SECONDS) {
                scenes.push(new ClearedScene(this));
                return;
            }
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            Input input = new Input(controls.axis(Action.LEFT, Action.RIGHT), controls.wasPressed(Action.JUMP),
                    controls.isDown(Action.JUMP), controls.isDown(Action.DOWN));
            step(seconds, input);
        }

        /** One step of the level: the rules, what they did, and the camera. */
        void step(double seconds, Input input) {
            game.update(seconds, input);
            effects.react(game.events(), true);
            effects.update(seconds);
            sinceCheckpoint += seconds;
            for (Event event : game.events()) {
                switch (event) {
                    case Event.CheckpointReached reached -> sinceCheckpoint = 0;
                    case Event.Cleared(var result) -> {
                        record = progress.finished(result, levels.size());
                        chosen = Math.min(levels.size(), result.level() + 1);
                    }
                    default -> {
                    }
                }
            }
            Body body = game.player().body();
            if (game.phase() != Game.Phase.DYING) camera.follow(body.centerX(), body.centerY(), seconds);
        }
        // --8<-- [end:play]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, game, camera, effects, effects.time(), true, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game, sinceCheckpoint);
        }

        Game game() {
            return game;
        }

        Camera camera() {
            return camera;
        }

        Progress.Record record() {
            return record;
        }
    }

    /** Drawn over the frozen level until P, Enter or Start; Esc leaves the level. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.BACK)) scenes.replace(new ChoiceScene());
            else if (controls.wasPressed(Action.PAUSE) || controls.wasPressed(Action.OK)) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("pause.title"), 190, 40, true, Hud.CHALK);
            hud.text(g, msg.get("pause.continue"), 235, 15, false, Hud.CHALK);
        }
    }

    /** The goal: the level goes on behind the results; Enter goes to the next level. */
    final class ClearedScene implements Scene {
        private final PlayScene play;
        private double shown;

        ClearedScene(PlayScene play) {
            this.play = play;
        }

        @Override
        public void update(double seconds) {
            play.step(seconds, Input.NONE);
            shown += seconds;
            if (shown < 0.8) return;     // a short wait, so a jump meant for the level does not skip the results
            int next = play.game.level().number() + 1;
            if (controls.wasPressed(Action.BACK)) scenes.replace(new ChoiceScene());
            else if (controls.wasPressed(Action.OK) || controls.wasPressed(Action.JUMP)) {
                scenes.replace(next <= levels.size() ? new PlayScene(levels.get(next - 1)) : new ChoiceScene());
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.cleared(g, play.game.result(), play.record, play.game.level().number() == levels.size());
        }
    }
}
