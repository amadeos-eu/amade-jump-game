package eu.amadeos.games.jump.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JumpPresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private JumpPresenter presenter;
    private JumpSettings settings;

    private final JumpView view = new JumpView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode) {
            log.add("options " + sound + " " + mode);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public void showAbout(String gamepads) {
            log.add("about " + gamepads);
        }
    };

    private final Arena arena = new Arena() {
        @Override
        public void showTitle() {
            log.add("title");
        }

        @Override
        public void chooseLevel() {
            log.add("choose");
        }

        @Override
        public void pause() {
            log.add("pause");
        }

        @Override
        public void setSound(boolean on) {
            log.add("sound " + on);
        }

        @Override
        public String gamepads() {
            return "Gamepads: none, 0 connected.";
        }
    };

    @BeforeEach
    void create() {
        settings = new JumpSettings(dir.resolve("jump.properties"));
        presenter = new JumpPresenter(view, arena, settings);
        presenter.start();
    }

    @Test
    void theSettingsAreAppliedAtStartAndKept() {
        assertEquals(List.of("options true DIRECT", "mode DIRECT", "sound true", "title"), log);
        log.clear();
        presenter.setRenderMode(RenderMode.PIXEL);
        presenter.setSound(false);
        assertEquals(List.of("mode PIXEL", "sound false"), log);
        assertEquals(RenderMode.PIXEL, settings.renderMode());
    }

    @Test
    void losingTheFocusPausesTheLevel() {
        log.clear();
        presenter.focusLost();
        presenter.newGame();
        presenter.showAbout();
        assertEquals(List.of("pause", "choose", "about Gamepads: none, 0 connected."), log);
    }
}
