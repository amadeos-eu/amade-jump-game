package eu.amadeos.games.jump.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;

/** What {@link JumpPresenter} needs from the window; tests replace it. The level itself is drawn by the game. */
public interface JumpView {
    /** Shows the settings in the menu. */
    void showOptions(boolean sound, RenderMode mode);

    /** Switches the canvas to a render mode. */
    void useRenderMode(RenderMode mode);

    /** The About dialog, with a line on the gamepads. */
    void showAbout(String gamepads);
}
