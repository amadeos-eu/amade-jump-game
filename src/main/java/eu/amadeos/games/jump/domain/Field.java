package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.physics.Jump;

/** The numbers of the game: sizes in logical units (a tile is 16), speeds in units per second, times in seconds. */
public final class Field {
    /** The logical size of the screen: 40 × 25 tiles of a level are seen at once. */
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;
    public static final int TILE = 16;

    // --8<-- [start:physics]
    /** Gravity, and the speed at which falling stops getting faster: not quite a tile per step at 120 steps. */
    public static final double GRAVITY = 1400;
    public static final double MAX_FALL = 480;
    /** Running: the top speed, how fast it is reached, and how fast the player stops on the ground and in the air. */
    public static final double RUN_SPEED = 140;
    public static final double GROUND_ACCELERATION = 1000;
    public static final double AIR_ACCELERATION = 700;
    public static final double GROUND_FRICTION = 1400;
    public static final double AIR_FRICTION = 250;
    /** A full jump is four and a half tiles high: walls of four tiles can be jumped onto, five cannot. */
    public static final double JUMP_HEIGHT = 4.5 * TILE;
    public static final double JUMP_SPEED = Jump.speedFor(JUMP_HEIGHT, GRAVITY);
    public static final double COYOTE_SECONDS = 0.08;
    public static final double BUFFER_SECONDS = 0.1;
    /** Letting go of jump while rising keeps this part of the speed. */
    public static final double JUMP_CUT = 0.45;
    /** Off a stomped beetle: four tiles high with jump held, much less without. */
    public static final double STOMP_SPEED = Jump.speedFor(4 * TILE, GRAVITY);
    /** A spring throws the player nine tiles high, held or not. */
    public static final double SPRING_SPEED = Jump.speedFor(9 * TILE, GRAVITY);
    // --8<-- [end:physics]

    /** The player's box: narrower than a tile, so gaps of one tile can be fallen into. */
    public static final double PLAYER_WIDTH = 10;
    public static final double PLAYER_HEIGHT = 14;
    public static final double BEETLE_WIDTH = 14;
    public static final double BEETLE_HEIGHT = 10;
    public static final double BEETLE_SPEED = 30;
    public static final double BIRD_WIDTH = 14;
    public static final double BIRD_HEIGHT = 10;
    /** A bird flies to and fro over this many tiles, if its object says nothing else, in this many seconds. */
    public static final double BIRD_RANGE = 4;
    public static final double BIRD_PERIOD = 4;
    /** How far above a beetle's top the feet may come from and still stomp it, rather than run into it. */
    public static final double STOMP_MARGIN = 6;

    /** The player tumbles out of the picture before starting again at the last flag. */
    public static final double DYING_SECONDS = 1.4;
    /** A stomped beetle stays flat for a moment. */
    public static final double SQUASHED_SECONDS = 0.6;
    /** A spring stays pressed down for a moment after throwing someone. */
    public static final double SPRING_SECONDS = 0.25;
    /** Below the bottom of the level by so much, a falling player is lost. */
    public static final double FALL_OUT = 2 * TILE;

    private Field() {
    }
}
