package eu.amadeos.games.jump.repository;

import eu.amadeos.gameengine.swing.RenderMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumpSettingsTest {
    @TempDir
    Path dir;

    @Test
    void settingsAreStoredAndReadBack() {
        JumpSettings settings = new JumpSettings(dir.resolve("jump.properties"));
        assertEquals(RenderMode.DIRECT, settings.renderMode());
        assertTrue(settings.sound());
        settings.setRenderMode(RenderMode.PIXEL);
        settings.setSound(false);
        JumpSettings again = new JumpSettings(dir.resolve("jump.properties"));
        assertEquals(RenderMode.PIXEL, again.renderMode());
        assertFalse(again.sound());
    }

    @Test
    void keysComeFromTheFileOrTheDefaults() throws Exception {
        Path file = dir.resolve("jump.properties");
        Files.writeString(file, "keys.jump=X SPACE\nkeys.left=NOT_A_KEY\n");
        JumpSettings settings = new JumpSettings(file);
        assertArrayEquals(new int[]{KeyEvent.VK_X, KeyEvent.VK_SPACE}, settings.keys("jump", KeyEvent.VK_UP));
        assertArrayEquals(new int[]{KeyEvent.VK_LEFT}, settings.keys("left", KeyEvent.VK_LEFT));
    }
}
