package eu.amadeos.games.jump.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/**
 * The music of Jump, written in MML and made into sounds once at start: a loop for each theme of the
 * levels, four bars each, and a fanfare at the goal.
 */
final class Music {
    /** The meadow: C, A minor, F, G, and a tune that skips along. */
    final Sound meadow = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t140 o5 l8 e g >c< g e g a g | f a >c< a g4 r4 | e g >c< g e g a >c | d4 c4 < g4 r4"),
            Tune.voice(Wave.TRIANGLE, 0.18, "t140 o3 l4 c g a e | f c g d | c g a e | f g c r"));

    /** The cave: slow, in A minor, with rests for the echo. */
    final Sound cave = Sound.mix(
            Tune.voice(Wave.TRIANGLE, 0.16, "t100 o4 l8 a r >c r <b r g r | a4 r4 e4 r4 | f r a r g r e r | a2 r2"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t100 o2 l2 a e | a a | f g | a a"));

    /** The heights: light and high. */
    final Sound sky = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.05, "t150 o5 l8 c d e g e d c r | < a > c d e d c < a r | > f e d c d e g r | e2 c2"),
            Tune.voice(Wave.TRIANGLE, 0.18, "t150 o3 l4 c e g e | a e f e | f a g b | c g c r"));

    /** The goal is reached. */
    final Sound cleared = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.08, "t160 o5 l8 c e g >c4 < g >c2"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t160 o3 l8 c g >c4 < c4 c2"));

    /** The loop of a theme; the meadow's for one this game does not know. */
    Sound of(String theme) {
        return switch (theme) {
            case "cave" -> cave;
            case "sky" -> sky;
            default -> meadow;
        };
    }
}
