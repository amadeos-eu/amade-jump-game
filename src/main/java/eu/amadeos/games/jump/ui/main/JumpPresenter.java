package eu.amadeos.games.jump.ui.main;

import com.google.inject.Inject;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.ui.game.Arena;

/**
 * Everything around the arena, without Swing: the menu commands, the settings, and pausing when the window
 * loses the focus. All methods run on the Swing thread. The progress through the levels is the game's
 * own: it is saved at the goal of each level.
 */
public final class JumpPresenter {
    private final JumpView view;
    private final Arena arena;
    private final JumpSettings settings;

    @Inject
    public JumpPresenter(JumpView view, Arena arena, JumpSettings settings) {
        this.view = view;
        this.arena = arena;
        this.settings = settings;
    }

    public void start() {
        boolean sound = settings.sound();
        RenderMode mode = settings.renderMode();
        view.showOptions(sound, mode);
        view.useRenderMode(mode);
        arena.setSound(sound);
        arena.showTitle();
    }

    public void newGame() {
        arena.chooseLevel();
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the keys, so a running level waits. */
    public void focusLost() {
        arena.pause();
    }

    public void setSound(boolean on) {
        settings.setSound(on);
        arena.setSound(on);
    }

    public void setRenderMode(RenderMode mode) {
        settings.setRenderMode(mode);
        view.useRenderMode(mode);
    }

    public void showAbout() {
        view.showAbout(arena.gamepads());
    }
}
