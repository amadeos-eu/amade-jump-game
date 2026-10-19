package eu.amadeos.games.jump.ui.game;

/**
 * What the presenter may ask of the running game. The game runs on its own thread; every method here is
 * safe to call from the Swing thread and takes effect at the start of the game's next step.
 */
public interface Arena {
    /** Back to the title screen. */
    void showTitle();

    /** To the choice of a level. */
    void chooseLevel();

    /** Pauses a running level; does nothing elsewhere. */
    void pause();

    void setSound(boolean on);

    /** Which gamepads the game sees, for the About dialog: "Gamepads: XInput, 1 connected". */
    String gamepads();
}
