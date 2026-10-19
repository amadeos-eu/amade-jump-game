package eu.amadeos.games.jump.domain;

/**
 * What the player wants in one step, whatever it came from: keys, a gamepad or a test.
 *
 * @param move   -1 left, 0 neither, 1 right
 * @param jump   the jump button went down in this step
 * @param held   the jump button is down
 * @param down   down is held: drop through a platform
 */
public record Input(int move, boolean jump, boolean held, boolean down) {
    public static final Input NONE = new Input(0, false, false, false);

    public static Input run(int move) {
        return new Input(move, false, false, false);
    }
}
