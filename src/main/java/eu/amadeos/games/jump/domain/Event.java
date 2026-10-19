package eu.amadeos.games.jump.domain;

/** Something that happened in a step; the user interface decides what it looks and sounds like. */
// --8<-- [start:event]
public sealed interface Event {
    record Jumped(double x, double y) implements Event {
    }

    /** The feet came down after a fall at {@code speed}: a puff of dust for a hard landing. */
    record Landed(double x, double y, double speed) implements Event {
    }

    record CoinTaken(int col, int row, int coins) implements Event {
    }

    record Stomped(Enemy enemy) implements Event {
    }

    record Sprung(double x, double y) implements Event {
    }

    record CheckpointReached(int index, double x, double y) implements Event {
    }

    /** The player is lost: spikes, water, an enemy, or a fall out of the level. */
    record Died(double x, double y, Cause cause) implements Event {
    }

    /** Back at the last checkpoint. */
    record Respawned(double x, double y) implements Event {
    }

    record Cleared(LevelResult result) implements Event {
    }

    enum Cause { SPIKES, WATER, ENEMY, FALL }
}
// --8<-- [end:event]
