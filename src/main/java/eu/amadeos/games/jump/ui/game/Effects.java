package eu.amadeos.games.jump.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.fx.Particles;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.jump.domain.Event;
import eu.amadeos.games.jump.domain.Field;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * What the events of a step look and sound like: dust under hard landings, the glitter of a coin, stars
 * of a stomp, a splash, and the sounds. The rules know nothing of this. Everything here lives in world
 * coordinates and is drawn by the painter, with the camera's shift.
 */
final class Effects {
    /** Dust, dark dust, water, white, gold, red. */
    static final Color[] PALETTE = {new Color(0xE0D2B0), new Color(0xA88E68), new Color(0x70D8F4),
            new Color(0xF4F4F0), new Color(0xFFD84A), new Color(0xE04040)};
    static final int DUST = 0;
    static final int DARK_DUST = 1;
    static final int WATER = 2;
    static final int WHITE = 3;
    static final int GOLD = 4;
    static final int RED = 5;
    /** Particles fall a little, slower than the player. */
    static final double GRAVITY = 300;
    /** A landing faster than this raises dust. */
    static final double HARD_LANDING = 300;
    static final double GLITTER_SECONDS = 0.3;

    /** The glitter left where a coin was taken. */
    private record Glitter(double x, double y, double born) {
    }

    private final SoundBank sounds = new SoundBank();
    private final Music music = new Music();
    private final Mixer mixer;
    private final JumpPainter painter;
    private final SeededRandom random = new SeededRandom(1);
    private final Particles particles = new Particles(400, PALETTE);
    private final List<Glitter> glitters = new ArrayList<>();
    /** Seconds since the effects started: the clock of everything that moves on its own. */
    private double time;

    Effects(Mixer mixer, JumpPainter painter) {
        this.mixer = mixer;
        this.painter = painter;
    }

    double time() {
        return time;
    }

    /** A new level, or the title: nothing left from before, no music. */
    void clear() {
        particles.clear();
        glitters.clear();
        mixer.stopMusic();
    }

    /** The loop of a level's theme, from its start. */
    void music(String theme) {
        mixer.playMusic(music.of(theme), true);
    }

    void tick() {
        mixer.play(sounds.tick);
    }

    void update(double seconds) {
        time += seconds;
        particles.update(seconds, GRAVITY);
        glitters.removeIf(glitter -> time - glitter.born() > GLITTER_SECONDS);
    }

    // --8<-- [start:react]
    /** Shows and, if {@code audible}, plays what happened in the last step. */
    void react(List<Event> events, boolean audible) {
        for (Event event : events) {
            switch (event) {
                case Event.Jumped(double x, double y) -> {
                    puff(x, y, 3, DUST);
                    if (audible) mixer.play(sounds.jump);
                }
                case Event.Landed(double x, double y, double speed) -> {
                    if (speed < HARD_LANDING) break;
                    puff(x, y, 6, DUST);
                    if (audible) mixer.play(sounds.land);
                }
                case Event.CoinTaken(int col, int row, int coins) -> {
                    glitters.add(new Glitter(col * Field.TILE, row * Field.TILE, time));
                    if (audible) mixer.play(sounds.coin);
                }
                case Event.Stomped stomped -> {
                    var body = stomped.enemy().body();
                    burst(body.centerX(), body.y, 8, WHITE, 60);
                    if (audible) mixer.play(sounds.stomp);
                }
                case Event.Sprung(double x, double y) -> {
                    puff(x, y, 4, WHITE);
                    if (audible) mixer.play(sounds.spring);
                }
                case Event.CheckpointReached(int index, double x, double y) -> {
                    burst(x, y + 6, 12, GOLD, 70);
                    if (audible) mixer.play(sounds.checkpoint);
                }
                case Event.Died(double x, double y, Event.Cause cause) -> {
                    if (cause == Event.Cause.WATER) burst(x, y, 16, WATER, 90);
                    else if (cause != Event.Cause.FALL) burst(x, y, 10, RED, 60);
                    if (audible) mixer.play(cause == Event.Cause.WATER ? sounds.splash : sounds.die);
                }
                case Event.Respawned(double x, double y) -> {
                    burst(x, y - 8, 10, WHITE, 40);
                    if (audible) mixer.play(sounds.respawn);
                }
                case Event.Cleared cleared -> {
                    if (audible) mixer.playMusic(music.cleared, false);
                }
            }
        }
    }
    // --8<-- [end:react]

    /** Dust to both sides of the feet. */
    private void puff(double x, double y, int count, int colour) {
        for (int i = 0; i < count; i++) {
            double side = i % 2 == 0 ? -1 : 1;
            particles.spawn(x + side * 4, y - 1, side * (20 + random.nextDouble() * 30), -10 - random.nextDouble() * 25,
                    0.25 + random.nextDouble() * 0.2, 1.5 + random.nextDouble() * 1.5,
                    random.nextBoolean() ? colour : (colour == DUST ? DARK_DUST : colour));
        }
    }

    /** Sparks in all directions. */
    private void burst(double x, double y, int count, int colour, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double v = speed * (0.4 + random.nextDouble() * 0.6);
            particles.spawn(x, y, Math.cos(angle) * v, Math.sin(angle) * v - 30, 0.4 + random.nextDouble() * 0.3,
                    1.5 + random.nextDouble() * 1.5, colour);
        }
    }

    /** In world coordinates, after the things of the level. */
    void render(Graphics2D g, double alpha) {
        particles.render(g, alpha);
        for (Glitter glitter : glitters) {
            int frame = time - glitter.born() < GLITTER_SECONDS / 2 ? 6 : 7;
            painter.sprite(frame, 4).draw(g, glitter.x(), glitter.y());
        }
    }

    /** How many particles fly: for the tests. */
    int particles() {
        return particles.count();
    }
}
