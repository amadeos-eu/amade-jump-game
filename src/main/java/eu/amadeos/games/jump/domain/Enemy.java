package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.physics.Solidity;
import eu.amadeos.gameengine.physics.TileCollider;

/**
 * A beetle that walks on the ground, or a bird that flies to and fro. Both can be stomped from above, and
 * both are deadly from any other side.
 *
 * <p>A beetle is a {@link Body} like the player, moved by the same {@link TileCollider}: it falls, stands
 * on the ground and turns at walls. A careful one turns at the edge of its ground too ({@code careful} in
 * its properties). A bird flies a fixed path through everything: along {@code axis} {@code x} or
 * {@code y}, {@code range} tiles to each side of where it was placed, in {@code period} seconds.
 */
public final class Enemy {
    public enum Kind { BEETLE, BIRD }

    private final Kind kind;
    final Body body;
    private final boolean careful;
    private final boolean vertical;
    private final double originX;
    private final double originY;
    private final double range;
    private final double period;
    private double direction;
    private double time;
    private boolean squashed;
    private double squashedTime;
    private boolean gone;

    Enemy(Level.Spawn spawn) {
        kind = spawn.kind();
        double w = kind == Kind.BEETLE ? Field.BEETLE_WIDTH : Field.BIRD_WIDTH;
        double h = kind == Kind.BEETLE ? Field.BEETLE_HEIGHT : Field.BIRD_HEIGHT;
        // A beetle stands on the bottom of its object, a bird flies around the middle of it.
        double top = kind == Kind.BEETLE ? spawn.y() - h : spawn.y() - h / 2;
        body = new Body(spawn.x() - w / 2, top, w, h);
        careful = spawn.properties().bool("careful", false);
        vertical = spawn.properties().string("axis", "x").equals("y");
        range = spawn.properties().number("range", Field.BIRD_RANGE) * Field.TILE;
        period = spawn.properties().number("period", Field.BIRD_PERIOD);
        direction = spawn.properties().string("direction", "left").equals("right") ? 1 : -1;
        originX = body.x;
        originY = body.y;
    }

    // --8<-- [start:update]
    void update(double seconds, TileCollider world, double bottom) {
        if (gone) return;
        if (squashed) {
            body.remember();
            squashedTime += seconds;
            if (squashedTime >= Field.SQUASHED_SECONDS) gone = true;
            return;
        }
        time += seconds;
        if (kind == Kind.BIRD) {
            body.remember();
            double offset = range * Math.sin(2 * Math.PI * time / period);
            double before = vertical ? body.y : body.x;
            if (vertical) body.y = originY + offset;
            else body.x = originX + offset;
            double moved = (vertical ? body.y : body.x) - before;
            if (!vertical && moved != 0) direction = Math.signum(moved);
            return;
        }
        // A beetle walks: the same gravity and collision as the player's.
        double ahead = direction > 0 ? body.right() + 1 : body.x - 1;
        if (careful && body.onGround && world.at(ahead, body.bottom() + 1) == Solidity.EMPTY) direction = -direction;
        body.vx = direction * Field.BEETLE_SPEED;
        body.fall(Field.GRAVITY, Field.MAX_FALL, seconds);
        world.move(body, seconds, false);
        if (body.hitLeft || body.hitRight) direction = -direction;
        if (body.y > bottom) gone = true;
    }
    // --8<-- [end:update]

    void squash() {
        squashed = true;
        squashedTime = 0;
    }

    public Kind kind() {
        return kind;
    }

    public Body body() {
        return body;
    }

    /** Dangerous and stompable. */
    public boolean alive() {
        return !squashed && !gone;
    }

    public boolean squashed() {
        return squashed && !gone;
    }

    public boolean gone() {
        return gone;
    }

    public boolean facingLeft() {
        return direction < 0;
    }

    /** Seconds since the start of the level: the beat of feet and wings. */
    public double time() {
        return time;
    }
}
