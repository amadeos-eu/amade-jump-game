package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.physics.Jump;

/**
 * The one the player steers: a {@link Body} that runs and a {@link Jump}, and what the drawing needs to
 * know about them: which way it looks, how far it has run (for the steps of its feet), and whether it is
 * tumbling out of the picture.
 */
public final class Player {
    final Body body = new Body(0, 0, Field.PLAYER_WIDTH, Field.PLAYER_HEIGHT);
    final Jump jump = new Jump(Field.JUMP_SPEED, Field.COYOTE_SECONDS, Field.BUFFER_SECONDS, Field.JUMP_CUT);
    private boolean facingLeft;
    private double ran;
    private boolean dying;

    /** Stands the player on {@code x}, {@code feet}, at rest: at the start and after a fall. */
    void place(double x, double feet) {
        body.place(x - body.width / 2, feet - body.height);
        jump.reset();
        dying = false;
    }

    // --8<-- [start:run]
    /** Speeds up towards the wanted running speed, or slows down without one; slower in the air. */
    void run(int move, double seconds) {
        double target = move * Field.RUN_SPEED;
        double rate;
        if (move != 0) rate = body.onGround ? Field.GROUND_ACCELERATION : Field.AIR_ACCELERATION;
        else rate = body.onGround ? Field.GROUND_FRICTION : Field.AIR_FRICTION;
        double change = rate * seconds;
        body.vx = body.vx < target ? Math.min(target, body.vx + change) : Math.max(target, body.vx - change);
        if (move != 0) facingLeft = move < 0;
        if (body.onGround) ran += Math.abs(body.vx) * seconds;
    }
    // --8<-- [end:run]

    /** Thrown up and out: falls through everything until the level starts again. */
    void die() {
        dying = true;
        body.vx = 0;
        body.vy = -Field.JUMP_SPEED * 0.7;
    }

    public Body body() {
        return body;
    }

    public boolean facingLeft() {
        return facingLeft;
    }

    /** Units run on the ground so far: the feet take a step every few of them. */
    public double ran() {
        return ran;
    }

    public boolean dying() {
        return dying;
    }

    public boolean onGround() {
        return body.onGround;
    }
}
