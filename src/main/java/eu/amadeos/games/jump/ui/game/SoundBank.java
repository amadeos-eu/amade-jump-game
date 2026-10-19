package eu.amadeos.games.jump.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;

/** The sound effects of Jump, made once at start from tones: no recordings. */
final class SoundBank {
    /** A jump: a short rising square, the sound every platformer since 1985 has had. */
    final Sound jump = Sound.tone(Wave.SQUARE, 280, 620, 0.12, 0.08);
    /** The feet come down hard. */
    final Sound land = Sound.tone(Wave.NOISE, 0, 0, 0.05, 0.08);
    /** A coin: two high notes, the second a fourth above the first. */
    final Sound coin = Sound.tone(Wave.SQUARE, 988, 988, 0.05, 0.07)
            .then(Sound.tone(Wave.SQUARE, 1319, 1319, 0.16, 0.07));
    final Sound stomp = Sound.mix(Sound.tone(Wave.SQUARE, 520, 160, 0.12, 0.09),
            Sound.tone(Wave.NOISE, 0, 0, 0.06, 0.08));
    /** A spring: a long slide up. */
    final Sound spring = Sound.tone(Wave.TRIANGLE, 180, 900, 0.35, 0.3);
    final Sound checkpoint = Sound.tone(Wave.SQUARE, 523, 523, 0.07, 0.07)
            .then(Sound.tone(Wave.SQUARE, 659, 659, 0.07, 0.07))
            .then(Sound.tone(Wave.SQUARE, 784, 784, 0.07, 0.07))
            .then(Sound.tone(Wave.SQUARE, 1046, 1046, 0.14, 0.07));
    /** Lost: a wail down. */
    final Sound die = Sound.tone(Wave.SQUARE, 760, 140, 0.55, 0.09);
    final Sound splash = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.3, 0.12),
            Sound.tone(Wave.TRIANGLE, 300, 80, 0.25, 0.2));
    /** Back at the checkpoint. */
    final Sound respawn = Sound.tone(Wave.TRIANGLE, 300, 600, 0.15, 0.25);
    /** A menu choice moves. */
    final Sound tick = Sound.tone(Wave.SQUARE, 880, 880, 0.03, 0.05);
}
