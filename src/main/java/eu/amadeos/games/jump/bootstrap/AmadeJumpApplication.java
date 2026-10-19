package eu.amadeos.games.jump.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.games.jump.ui.game.JumpGame;
import eu.amadeos.games.jump.ui.main.JumpPresenter;
import eu.amadeos.games.jump.ui.main.JumpWindow;

public final class AmadeJumpApplication {
    public static final AppInfo APP = new AppInfo("amade-jump-game", "Jump", "jump");

    private AmadeJumpApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeJumpApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            Gamepads gamepads = injector.getInstance(Gamepads.class);
            JumpWindow window = injector.getInstance(JumpWindow.class);
            JumpPresenter presenter = injector.getInstance(JumpPresenter.class);
            JumpGame game = injector.getInstance(JumpGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                gamepads.close();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new JumpModule());
    }
    // --8<-- [end:main]
}
