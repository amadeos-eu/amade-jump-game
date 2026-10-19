package eu.amadeos.games.jump.domain;

/** Puts the player where a test or a screenshot needs it, from outside the rules' package. */
public final class Stage {
    private Stage() {
    }

    /** The player's feet at {@code x}, {@code feet}, running at {@code vx}. */
    public static void put(Game game, double x, double feet, double vx) {
        game.put(x, feet, vx);
    }

    /** No beetles and birds in the way. */
    public static void withoutEnemies(Game game) {
        game.withoutEnemies();
    }
}
